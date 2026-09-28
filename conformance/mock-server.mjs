#!/usr/bin/env node
/**
 * kuida-mock: servidor en memoria que se comporta como la API v1 de Kuida.
 *
 * Lo usan las suites de conformidad de todos los SDKs (ver SDK_DESIGN.md §9):
 * mismas respuestas, mismos errores, misma paginación e idempotencia que la
 * API real, sin base de datos ni WhatsApp. Sin dependencias: Node >= 18.
 *
 *   node conformance/mock-server.mjs            # puerto 12111
 *   PORT=12112 node conformance/mock-server.mjs
 *
 * Claves (formato real `kd_test_<12 hex>_<secreto>`):
 *   kd_test_000000000000_mocksecretmocksecret00   todos los scopes
 *   kd_test_1e9ac7000000_mocksecretmocksecret00   solo events:write (403 en el resto)
 *   kd_test_dead00000000_mocksecretmocksecret00   revocada (401 api_key_revoked)
 *   kd_test_fa11ed000000_mocksecretmocksecret00   el primer intento de cada pedido da 503
 *   kd_test_42900000000a_mocksecretmocksecret00   el primer intento de cada pedido da 429 + Retry-After: 1
 *
 * Endpoints de control (sin clave):
 *   GET  /__mock/requests   pedidos recibidos (method, path, query, headers relevantes, body)
 *   POST /__mock/reset      vacía datos y registro
 */
import http from "node:http";
import { createHash, randomBytes } from "node:crypto";

const PORT = Number(process.env.PORT ?? 12111);
const API_VERSION = "2026-09-28";
const ALL_SCOPES = [
  "events:write", "events:read", "patients:read", "patients:write", "doctors:read",
  "appointments:read", "appointments:write", "visits:read", "visits:write",
  "intake_requests:read", "intake_requests:write", "treatments:read", "treatments:write",
  "webhooks:read", "webhooks:write",
];
const KEYS = {
  kd_test_000000000000: { secret: "mocksecretmocksecret00", scopes: ALL_SCOPES, name: "mock" },
  kd_test_1e9ac7000000: { secret: "mocksecretmocksecret00", scopes: ["events:write"], name: "legacy" },
  kd_test_dead00000000: { secret: "mocksecretmocksecret00", scopes: ALL_SCOPES, name: "revoked", revoked: true },
  kd_test_fa11ed000000: { secret: "mocksecretmocksecret00", scopes: ALL_SCOPES, name: "flaky", flaky: 503 },
  kd_test_42900000000a: { secret: "mocksecretmocksecret00", scopes: ALL_SCOPES, name: "ratelimit", flaky: 429 },
};
const EVENT_TYPES = [
  "intake.requested", "patient.upserted", "appointment.created", "appointment.rescheduled",
  "appointment.cancelled", "visit.completed", "treatment.prescribed", "order.issued",
];
const WEBHOOK_EVENTS = [...EVENT_TYPES, "intake.created", "intake.ready", "conversation.handoff", "patient.silent", "webhook.ping", "*"];

// ─── estado ──────────────────────────────────────────────────────────────────

let db, log, idem, flakySeen, seq;
function reset() {
  db = { patient: [], doctor: [], appointment: [], visit: [], intake_request: [], treatment: [], event: [], webhook_endpoint: [], webhook_delivery: [] };
  log = [];
  idem = new Map();
  flakySeen = new Set();
  seq = 0;
}
reset();

const PREFIX = { patient: "pat", doctor: "doc", appointment: "apt", visit: "vis", intake_request: "int", treatment: "trt", event: "evt", webhook_endpoint: "we", webhook_delivery: "whd" };
const now = () => new Date(Date.now() + seq).toISOString();
function newId(object) {
  seq++;
  return `${PREFIX[object]}_mock${String(seq).padStart(6, "0")}${randomBytes(4).toString("hex")}`;
}
const find = (object, id) => db[object].find((o) => o.id === id);

// ─── errores ─────────────────────────────────────────────────────────────────

class ApiError extends Error {
  constructor(status, type, code, message, param = null, headers = {}) {
    super(message);
    Object.assign(this, { status, type, code, param, headers });
  }
}
const invalid = (param, message) => new ApiError(400, "invalid_request_error", "parameter_invalid", message, param);
const missing = (param) => new ApiError(400, "invalid_request_error", "parameter_missing", `Falta el parámetro obligatorio: ${param}`, param);
const notFound = (id) => new ApiError(404, "invalid_request_error", "resource_missing", `No existe un objeto con id '${id}'`, "id");

// ─── validación mínima ───────────────────────────────────────────────────────

const isStr = (v) => typeof v === "string" && v.trim().length > 0;
const ISO = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:\d{2})$/;
function reqStr(body, key, prefix = "") {
  if (body[key] === undefined || body[key] === null) throw missing(prefix + key);
  if (!isStr(body[key])) throw invalid(prefix + key, `${prefix + key} tiene que ser un texto no vacío`);
  return body[key];
}
function optDate(body, key) {
  if (body[key] === undefined) return undefined;
  if (typeof body[key] !== "string" || !ISO.test(body[key])) throw invalid(key, `${key}: fecha ISO 8601 inválida`);
  return new Date(body[key]).toISOString();
}
function reqDate(body, key) {
  if (body[key] === undefined) throw missing(key);
  return optDate(body, key);
}
function limitParam(q) {
  if (q.limit === undefined) return 10;
  const n = Number(q.limit);
  if (!Number.isInteger(n) || n < 1 || n > 100) throw invalid("limit", "limit tiene que ser un entero entre 1 y 100");
  return n;
}

function phoneKey(p) {
  const digits = String(p).replace(/\D/g, "");
  return digits.startsWith("549") ? digits : digits.startsWith("54") ? `549${digits.slice(2)}` : `549${digits}`;
}

function resolvePatient(ref, param = "patient") {
  if (ref === undefined) throw missing(param);
  if (typeof ref === "string") {
    if (!ref.startsWith("pat_")) throw invalid(param, `'${ref}' no es un id de paciente`);
    const p = find("patient", ref);
    if (!p) throw notFound(ref);
    return p;
  }
  if (typeof ref !== "object" || ref === null) throw invalid(param, "patient tiene que ser un id o un objeto");
  if (!ref.phone && !ref.dni && !ref.externalId && !ref.fullName) throw invalid(param, "El paciente necesita teléfono, DNI, id externo o nombre");
  if (!ref.phone) throw missing(`${param}.phone`);
  return upsertPatient(ref);
}

function upsertPatient(ref) {
  const key = phoneKey(ref.phone);
  let p = db.patient.find((x) => x._phoneKey === key) ?? (ref.dni ? db.patient.find((x) => x.dni === ref.dni) : undefined);
  if (p) {
    for (const [k, v] of Object.entries({ fullName: ref.fullName, dni: ref.dni, email: ref.email, externalId: ref.externalId, dateOfBirth: ref.dateOfBirth })) {
      if (v !== undefined && p[k] === null) p[k] = v;
    }
    return p;
  }
  const t = now();
  p = {
    id: newId("patient"), object: "patient", fullName: ref.fullName ?? null, phone: `+${key}`, email: ref.email ?? null,
    dni: ref.dni ?? null, externalId: ref.externalId ?? null, dateOfBirth: ref.dateOfBirth ?? null,
    stage: "pending_follow_up", optedOut: false, createdAt: t, updatedAt: t, _phoneKey: key,
  };
  db.patient.push(p);
  return p;
}

function resolveDoctor(ref) {
  if (ref === undefined) return null;
  if (typeof ref === "string") {
    const d = find("doctor", ref);
    if (!d) throw notFound(ref);
    return d;
  }
  if (!ref.externalId && !ref.fullName) throw invalid("doctor", "El profesional necesita externalId o fullName");
  let d = db.doctor.find((x) => (ref.externalId && x.externalId === ref.externalId) || (!ref.externalId && x.fullName.toLowerCase() === String(ref.fullName).toLowerCase()));
  if (!d) {
    d = { id: newId("doctor"), object: "doctor", fullName: ref.fullName ?? "Doctor", specialty: ref.specialty ?? null, licenseNumber: null, externalId: ref.externalId ?? null, active: true, createdAt: now() };
    db.doctor.push(d);
  }
  return d;
}

const pub = (o) => Object.fromEntries(Object.entries(o).filter(([k]) => !k.startsWith("_")));

// ─── paginación ──────────────────────────────────────────────────────────────

function list(object, url, q, filter = () => true) {
  const limit = limitParam(q);
  if (q.startingAfter && q.endingBefore) throw invalid("endingBefore", "Usá startingAfter o endingBefore, no los dos");
  const all = db[object].filter(filter).slice().sort((a, b) => (a.createdAt < b.createdAt ? 1 : a.createdAt > b.createdAt ? -1 : a.id < b.id ? 1 : -1));
  let start = 0;
  let items;
  if (q.startingAfter) {
    const i = all.findIndex((o) => o.id === q.startingAfter);
    if (i < 0) throw invalid("startingAfter", `No existe '${q.startingAfter}' para usar como cursor`);
    start = i + 1;
    items = all.slice(start, start + limit);
    return { object: "list", data: items.map(pub), hasMore: start + limit < all.length, url };
  }
  if (q.endingBefore) {
    const i = all.findIndex((o) => o.id === q.endingBefore);
    if (i < 0) throw invalid("endingBefore", `No existe '${q.endingBefore}' para usar como cursor`);
    const from = Math.max(0, i - limit);
    items = all.slice(from, i);
    return { object: "list", data: items.map(pub), hasMore: from > 0, url };
  }
  items = all.slice(0, limit);
  return { object: "list", data: items.map(pub), hasMore: limit < all.length, url };
}

// ─── rutas ───────────────────────────────────────────────────────────────────

const routes = [];
const route = (method, pattern, scope, handler) =>
  routes.push({ method, re: new RegExp(`^${pattern.replace(/\{id\}/g, "([^/]+)")}$`), scope, handler });

route("GET", "/v1/account", null, (_c) => ({
  id: "org_mock000000000000", object: "account", name: "Organización Mock", type: "clinic",
  timeZone: "America/Argentina/Buenos_Aires", serviceLines: ["follow_up"], livemode: false,
  apiKey: { name: _c.key.name, prefix: _c.prefix, scopes: _c.key.scopes },
}));

route("POST", "/v1/patients", "patients:write", ({ body }) => {
  reqStr(body, "phone");
  return pub(upsertPatient(body));
});
route("GET", "/v1/patients/{id}", "patients:read", ({ id }) => {
  const p = find("patient", id);
  if (!p) throw notFound(id);
  return pub(p);
});
route("PATCH", "/v1/patients/{id}", "patients:write", ({ id, body }) => {
  const p = find("patient", id);
  if (!p) throw notFound(id);
  if (body.dni && db.patient.some((x) => x !== p && x.dni === body.dni)) {
    throw new ApiError(409, "invalid_request_error", "resource_conflict", "Ya hay otro paciente con ese DNI", "dni");
  }
  for (const k of ["fullName", "email", "dni", "externalId", "dateOfBirth"]) if (body[k] !== undefined) p[k] = body[k];
  p.updatedAt = now();
  return pub(p);
});
route("GET", "/v1/patients", "patients:read", ({ q }) =>
  list("patient", "/v1/patients", q, (p) =>
    (!q.phone || p._phoneKey === phoneKey(q.phone)) && (!q.dni || p.dni === q.dni) && (!q.externalId || p.externalId === q.externalId)),
);

route("GET", "/v1/doctors", "doctors:read", ({ q }) =>
  list("doctor", "/v1/doctors", q, (d) => (!q.active || String(d.active) === q.active) && (!q.externalId || d.externalId === q.externalId)),
);
route("GET", "/v1/doctors/{id}", "doctors:read", ({ id }) => {
  const d = find("doctor", id);
  if (!d) throw notFound(id);
  return pub(d);
});

route("POST", "/v1/appointments", "appointments:write", ({ body }) => {
  if (body.externalId) {
    const existing = db.appointment.find((a) => a.externalId === body.externalId);
    if (existing) return pub(existing);
  }
  const patient = resolvePatient(body.patient);
  const doctor = resolveDoctor(body.doctor);
  const startAt = reqDate(body, "startAt");
  const t = now();
  const a = {
    id: newId("appointment"), object: "appointment", patient: patient.id, doctor: doctor?.id ?? null, startAt,
    status: "confirmed", type: body.type ?? "Turno", externalId: body.externalId ?? null, source: "api",
    cancelledAt: null, createdAt: t, updatedAt: t,
  };
  db.appointment.push(a);
  return pub(a);
});
route("GET", "/v1/appointments/{id}", "appointments:read", ({ id }) => {
  const a = find("appointment", id);
  if (!a) throw notFound(id);
  return pub(a);
});
route("PATCH", "/v1/appointments/{id}", "appointments:write", ({ id, body }) => {
  const a = find("appointment", id);
  if (!a) throw notFound(id);
  if (a.status === "cancelled" || a.status === "completed") throw invalid("id", "El turno no se puede cambiar");
  const startAt = optDate(body, "startAt");
  if (startAt) a.startAt = startAt;
  if (body.doctor !== undefined) a.doctor = resolveDoctor(body.doctor)?.id ?? null;
  if (body.type) a.type = body.type;
  a.updatedAt = now();
  return pub(a);
});
route("POST", "/v1/appointments/{id}/cancel", "appointments:write", ({ id }) => {
  const a = find("appointment", id);
  if (!a) throw notFound(id);
  if (a.status === "completed") throw invalid("id", "El turno ya fue atendido");
  if (a.status !== "cancelled") Object.assign(a, { status: "cancelled", cancelledAt: now(), updatedAt: now() });
  return pub(a);
});
route("GET", "/v1/appointments", "appointments:read", ({ q }) =>
  list("appointment", "/v1/appointments", q, (a) =>
    (!q.patient || a.patient === q.patient) && (!q.status || a.status === q.status) && (!q.externalId || a.externalId === q.externalId) &&
    (!q.startAtGte || a.startAt >= new Date(q.startAtGte).toISOString()) && (!q.startAtLt || a.startAt < new Date(q.startAtLt).toISOString())),
);

route("POST", "/v1/visits", "visits:write", ({ body }) => {
  if (body.externalId) {
    const existing = db.visit.find((v) => v.externalId === body.externalId);
    if (existing) return pub(existing);
  }
  const patient = resolvePatient(body.patient);
  const doctor = resolveDoctor(body.doctor);
  const visitedAt = reqDate(body, "visitedAt");
  let appointment = null;
  if (body.appointment) {
    appointment = find("appointment", body.appointment) ?? db.appointment.find((a) => a.externalId === body.appointment) ?? null;
    if (!appointment && String(body.appointment).startsWith("apt_")) throw notFound(body.appointment);
  }
  const v = {
    id: newId("visit"), object: "visit", patient: patient.id, doctor: doctor?.id ?? null, appointment: appointment?.id ?? null,
    visitedAt, type: body.type ?? null, diagnosis: body.diagnosis ?? null, notes: body.notes ?? null,
    externalId: body.externalId ?? null, source: "manual", createdAt: now(),
  };
  if (appointment && (appointment.status === "confirmed" || appointment.status === "pending")) {
    Object.assign(appointment, { status: "completed", updatedAt: now() });
  }
  db.visit.push(v);
  return pub(v);
});
route("GET", "/v1/visits/{id}", "visits:read", ({ id }) => {
  const v = find("visit", id);
  if (!v) throw notFound(id);
  return pub(v);
});
route("GET", "/v1/visits", "visits:read", ({ q }) =>
  list("visit", "/v1/visits", q, (v) => (!q.patient || v.patient === q.patient) && (!q.externalId || v.externalId === q.externalId)),
);

route("POST", "/v1/intake_requests", "intake_requests:write", ({ body }) => {
  const contacts = body.contacts ?? [];
  if (!(body.patient?.phone || body.patient?.fullName || body.patient?.dni || contacts.some((c) => c.phone || c.name))) {
    throw invalid("patient", "Hace falta al menos un dato del paciente o de un referente");
  }
  const patient = body.patient?.phone ? upsertPatient(body.patient) : null;
  const missingFields = [];
  if (!body.patient?.phone && !contacts.some((c) => c.phone)) missingFields.push("telefono");
  if (!body.coverage?.insurer) missingFields.push("cobertura");
  const t = now();
  const r = {
    id: newId("intake_request"), object: "intake_request", status: "new", patient: patient?.id ?? null,
    summary: body.summary ?? null, missing: missingFields, data: body, discardReason: null,
    readyAt: null, loadedAt: null, closedAt: null, createdAt: t, updatedAt: t,
  };
  db.intake_request.push(r);
  return pub(r);
});
route("GET", "/v1/intake_requests/{id}", "intake_requests:read", ({ id }) => {
  const r = find("intake_request", id);
  if (!r) throw notFound(id);
  return pub(r);
});
route("GET", "/v1/intake_requests", "intake_requests:read", ({ q }) =>
  list("intake_request", "/v1/intake_requests", q, (r) => (!q.status || r.status === q.status) && (!q.patient || r.patient === q.patient)),
);

route("POST", "/v1/treatments", "treatments:write", ({ body }) => {
  const patient = resolvePatient(body.patient);
  reqStr(body, "name");
  const t = now();
  const tr = {
    id: newId("treatment"), object: "treatment", patient: patient.id, kind: body.kind ?? "medication", name: body.name,
    dosage: body.dosage ?? null, frequency: body.frequency ?? null, instructions: body.instructions ?? null,
    startDate: optDate(body, "startDate") ?? null, endDate: optDate(body, "endDate") ?? null, active: true, createdAt: t, updatedAt: t,
  };
  db.treatment.push(tr);
  return pub(tr);
});
route("GET", "/v1/treatments/{id}", "treatments:read", ({ id }) => {
  const t = find("treatment", id);
  if (!t) throw notFound(id);
  return pub(t);
});
route("GET", "/v1/treatments", "treatments:read", ({ q }) =>
  list("treatment", "/v1/treatments", q, (t) => (!q.patient || t.patient === q.patient) && (!q.active || String(t.active) === q.active)),
);

const HANDLED = { "visit.completed": "visit", "patient.upserted": "patient", "appointment.created": "appointment", "intake.requested": "intake_request", "treatment.prescribed": "treatment" };
route("POST", "/v1/events", "events:write", ({ body, res }) => {
  const items = Array.isArray(body) ? body : Array.isArray(body?.events) ? body.events : [body];
  if (items.length === 0) throw invalid("events", "El lote está vacío");
  if (items.length > 100) throw new ApiError(413, "invalid_request_error", "batch_too_large", "El lote supera 100 eventos", "events");
  const results = items.map((e) => {
    const id = typeof e?.id === "string" ? e.id : "";
    if (!isStr(e?.id) || !EVENT_TYPES.includes(e?.type) || typeof e?.data !== "object" || e.data === null) {
      return { id, accepted: false, status: "invalid", event: null, result: null, error: "id, type o data inválidos" };
    }
    const dup = db.event.find((x) => x.sourceRef === e.id);
    if (dup) return { id, accepted: true, status: "duplicate", event: dup.id, result: dup.result, error: null };
    const object = HANDLED[e.type];
    const result = object ? { object, id: newId(object) } : null;
    const t = now();
    const ev = {
      id: newId("event"), object: "event", type: e.type, source: "api", sourceRef: e.id, status: object ? "processed" : "unhandled",
      error: null, result, data: e.data, occurredAt: e.occurredAt ?? t, receivedAt: t, processedAt: t,
    };
    db.event.push(Object.assign(ev, { createdAt: t }));
    return { id, accepted: true, status: ev.status, event: ev.id, result, error: null };
  });
  if (!results.some((r) => r.accepted)) {
    const e = new ApiError(400, "invalid_request_error", "parameter_invalid", "Ningún evento del lote es válido. El detalle de cada uno está en results.", "events");
    e.extra = { object: "event_batch", results };
    throw e;
  }
  return { object: "event_batch", results };
});
route("GET", "/v1/events/{id}", "events:read", ({ id }) => {
  const e = find("event", id);
  if (!e) throw notFound(id);
  const { createdAt, ...rest } = e;
  return rest;
});
route("GET", "/v1/events", "events:read", ({ q }) => {
  const page = list("event", "/v1/events", q, (e) => (!q.type || e.type === q.type) && (!q.status || e.status === q.status) && (!q.source || e.source === q.source));
  page.data = page.data.map(({ createdAt, ...rest }) => rest);
  return page;
});

function validateWebhookBody(body, creating) {
  if (creating || body.url !== undefined) {
    if (creating) reqStr(body, "url");
    if (body.url !== undefined && !/^https:\/\/\S+$/.test(body.url)) throw invalid("url", "La URL tiene que ser https://");
  }
  if (creating || body.enabledEvents !== undefined) {
    if (!Array.isArray(body.enabledEvents) || body.enabledEvents.length === 0) {
      throw creating && body.enabledEvents === undefined ? missing("enabledEvents") : invalid("enabledEvents", "enabledEvents tiene que ser una lista no vacía");
    }
    const bad = body.enabledEvents.find((e) => !WEBHOOK_EVENTS.includes(e));
    if (bad) throw invalid("enabledEvents", `'${bad}' no es un tipo de evento`);
  }
}
const endpointOut = (w, withSecret = false) => {
  const { secret, ...rest } = pub(w);
  return withSecret ? { ...rest, secret } : rest;
};
route("POST", "/v1/webhook_endpoints", "webhooks:write", ({ body }) => {
  validateWebhookBody(body, true);
  const w = {
    id: newId("webhook_endpoint"), object: "webhook_endpoint", url: body.url, description: body.description ?? null,
    enabledEvents: [...new Set(body.enabledEvents)], status: "enabled", secret: `whsec_${randomBytes(24).toString("base64url")}`,
    lastDeliveredAt: null, lastError: null, createdAt: now(),
  };
  db.webhook_endpoint.push(w);
  return endpointOut(w, true);
});
route("GET", "/v1/webhook_endpoints/{id}", "webhooks:read", ({ id }) => {
  const w = find("webhook_endpoint", id);
  if (!w) throw notFound(id);
  return endpointOut(w);
});
route("PATCH", "/v1/webhook_endpoints/{id}", "webhooks:write", ({ id, body }) => {
  const w = find("webhook_endpoint", id);
  if (!w) throw notFound(id);
  validateWebhookBody(body, false);
  if (body.url !== undefined) w.url = body.url;
  if (body.description !== undefined) w.description = body.description;
  if (body.enabledEvents !== undefined) w.enabledEvents = [...new Set(body.enabledEvents)];
  if (body.disabled !== undefined) w.status = body.disabled ? "disabled" : "enabled";
  return endpointOut(w);
});
route("DELETE", "/v1/webhook_endpoints/{id}", "webhooks:write", ({ id }) => {
  const w = find("webhook_endpoint", id);
  if (!w) throw notFound(id);
  db.webhook_endpoint = db.webhook_endpoint.filter((x) => x !== w);
  db.webhook_delivery = db.webhook_delivery.filter((d) => d.webhookEndpoint !== id);
  return { id, object: "webhook_endpoint", deleted: true };
});
route("GET", "/v1/webhook_endpoints", "webhooks:read", ({ q }) => {
  const page = list("webhook_endpoint", "/v1/webhook_endpoints", q);
  page.data = page.data.map((w) => endpointOut(w));
  return page;
});
route("POST", "/v1/webhook_endpoints/{id}/ping", "webhooks:write", ({ id }) => {
  const w = find("webhook_endpoint", id);
  if (!w) throw notFound(id);
  if (w.status === "disabled") throw invalid("id", "El endpoint está deshabilitado");
  const deliveryId = newId("webhook_delivery");
  const d = {
    id: deliveryId, object: "webhook_delivery", webhookEndpoint: id, eventType: "webhook.ping", status: "pending", attempts: 0,
    lastStatusCode: null, lastError: null, nextAttemptAt: now(), deliveredAt: null,
    payload: { id: deliveryId, object: "event", type: "webhook.ping", apiVersion: API_VERSION, livemode: false, occurredAt: now(), ref: "ping_mock", data: { message: "Evento de prueba de Kuida" } },
    createdAt: now(),
  };
  db.webhook_delivery.push(d);
  return pub(d);
});
route("GET", "/v1/webhook_deliveries", "webhooks:read", ({ q }) =>
  list("webhook_delivery", "/v1/webhook_deliveries", q, (d) => (!q.webhookEndpoint || d.webhookEndpoint === q.webhookEndpoint) && (!q.status || d.status === q.status)),
);
route("GET", "/v1/webhook_deliveries/{id}", "webhooks:read", ({ id }) => {
  const d = find("webhook_delivery", id);
  if (!d) throw notFound(id);
  return pub(d);
});

// ─── servidor ────────────────────────────────────────────────────────────────

function authenticate(header) {
  if (!header) throw new ApiError(401, "authentication_error", "api_key_missing", "Falta la clave de API");
  const m = /^Bearer (kd_[a-z]+_[a-f0-9]{12})_([A-Za-z0-9_-]{16,})$/.exec(header.trim());
  if (!m) throw new ApiError(401, "authentication_error", "api_key_invalid", "La clave de API no es válida");
  const key = KEYS[m[1]];
  if (!key || key.secret !== m[2]) throw new ApiError(401, "authentication_error", "api_key_invalid", "La clave de API no es válida");
  if (key.revoked) throw new ApiError(401, "authentication_error", "api_key_revoked", "La clave de API fue revocada");
  return { key, prefix: m[1] };
}

const server = http.createServer((req, res) => {
  let raw = "";
  req.on("data", (c) => (raw += c));
  req.on("end", () => handle(req, res, raw));
});

function send(res, status, body, headers = {}) {
  res.writeHead(status, { "content-type": "application/json", ...headers });
  res.end(JSON.stringify(body));
}

function handle(req, res, raw) {
  const url = new URL(req.url, `http://localhost:${PORT}`);
  const path = url.pathname.replace(/^\/api(?=\/v1\/)/, "");
  if (path === "/__mock/reset" && req.method === "POST") { reset(); return send(res, 200, { ok: true }); }
  if (path === "/__mock/requests") return send(res, 200, { requests: log });

  const requestId = `req_mock${randomBytes(8).toString("hex")}`;
  const baseHeaders = { "Request-Id": requestId, "Kuida-Version": API_VERSION, "RateLimit-Limit": "600", "RateLimit-Remaining": "599", "RateLimit-Reset": "60" };
  const q = {};
  for (const k of new Set(url.searchParams.keys())) {
    const all = url.searchParams.getAll(k);
    q[k.replace(/\[\]$/, "")] = all.length > 1 ? all : all[0];
  }
  let body;
  try { body = raw ? JSON.parse(raw) : undefined; } catch { body = Symbol.for("invalid"); }
  const h = (n) => req.headers[n] ?? null;
  log.push({
    method: req.method, path, query: q, body: typeof body === "symbol" ? raw : body ?? null,
    headers: {
      authorization: h("authorization"), "idempotency-key": h("idempotency-key"), "kuida-version": h("kuida-version"),
      "user-agent": h("user-agent"), "content-type": h("content-type"), "x-kuida-client-user-agent": h("x-kuida-client-user-agent"),
    },
  });

  try {
    const v = h("kuida-version");
    if (v && v !== API_VERSION) throw new ApiError(400, "invalid_request_error", "api_version_invalid", `Kuida-Version '${v}' no existe`, "Kuida-Version");
    const r = routes.find((x) => x.method === req.method && x.re.test(path));
    if (!r) throw new ApiError(404, "invalid_request_error", "resource_missing", `No existe ${req.method} ${path}`);
    const ctx = authenticate(h("authorization"));
    if (r.scope && !ctx.key.scopes.includes(r.scope)) {
      throw new ApiError(403, "permission_error", "scope_missing", `La clave no tiene el permiso ${r.scope}`);
    }
    if (ctx.key.flaky) {
      const sig = `${req.method} ${path} ${h("idempotency-key") ?? raw}`;
      if (!flakySeen.has(sig)) {
        flakySeen.add(sig);
        if (ctx.key.flaky === 429) {
          throw new ApiError(429, "rate_limit_error", "rate_limited", "Superaste el límite de pedidos", null, { "Retry-After": "1" });
        }
        throw new ApiError(503, "api_error", "internal_error", "Error transitorio simulado");
      }
    }
    if (typeof body === "symbol") throw new ApiError(400, "invalid_request_error", "body_invalid_json", "El cuerpo no es JSON válido");
    if (["POST", "PATCH"].includes(req.method) && body === undefined && !path.endsWith("/cancel") && !path.endsWith("/ping")) {
      throw new ApiError(400, "invalid_request_error", "parameter_missing", "El pedido no tiene cuerpo");
    }

    const idemKey = req.method === "POST" ? h("idempotency-key") : null;
    const fingerprint = createHash("sha256").update(`${req.method}\n${path}\n${raw}`).digest("hex");
    if (idemKey) {
      const prev = idem.get(idemKey);
      if (prev) {
        if (prev.fingerprint !== fingerprint) {
          throw new ApiError(400, "idempotency_error", "idempotency_key_reused", "Esta Idempotency-Key ya se usó con otro pedido", "Idempotency-Key");
        }
        return send(res, prev.status, prev.body, { ...baseHeaders, "Idempotent-Replayed": "true" });
      }
    }

    const m = r.re.exec(path);
    const out = { statusOverride: null };
    const result = r.handler({ ...ctx, body: body ?? {}, q, id: m[1] ? decodeURIComponent(m[1]) : undefined, res: out });
    const status = out.statusOverride ?? 200;
    if (idemKey) idem.set(idemKey, { fingerprint, status, body: result });
    return send(res, status, result, baseHeaders);
  } catch (err) {
    const e = err instanceof ApiError ? err : new ApiError(500, "api_error", "internal_error", String(err?.message ?? err));
    if (!(err instanceof ApiError)) console.error(err);
    return send(res, e.status, {
      error: { type: e.type, code: e.code, message: e.message, param: e.param, requestId, docUrl: `https://www.kuida.ar/desarrolladores/errores#${e.code}` },
      ...(e.extra ?? {}),
    }, { ...baseHeaders, ...e.headers });
  }
}

server.listen(PORT, () => console.log(`kuida-mock escuchando en http://localhost:${PORT} (API ${API_VERSION})`));
