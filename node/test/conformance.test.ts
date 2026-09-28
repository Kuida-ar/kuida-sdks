/**
 * Suite de conformidad (SDK_DESIGN.md §9): 17 escenarios contra el mock.
 *
 *   PORT=12121 node ../conformance/mock-server.mjs &
 *   KUIDA_API_BASE=http://localhost:12121/api npm test
 */
import assert from "node:assert/strict";
import { createHmac, randomUUID } from "node:crypto";
import { existsSync, readFileSync } from "node:fs";
import { createServer } from "node:net";
import { dirname, join } from "node:path";
import { test } from "node:test";

import {
  APIConnectionError,
  APIError,
  AuthenticationError,
  IdempotencyError,
  InvalidRequestError,
  Kuida,
  KuidaError,
  PermissionError,
  SignatureVerificationError,
  Webhook,
  type KuidaConfig,
  type PatientCreateParams,
} from "../src/index.js";

const BASE = (process.env.KUIDA_API_BASE ?? "http://localhost:12121/api").replace(/\/+$/, "");
const MOCK = BASE.replace(/\/api$/, "");

const KEY = "kd_test_000000000000_mocksecretmocksecret00";
const KEY_EVENTS_ONLY = "kd_test_1e9ac7000000_mocksecretmocksecret00";
const KEY_REVOKED = "kd_test_dead00000000_mocksecretmocksecret00";
const KEY_FLAKY_503 = "kd_test_fa11ed000000_mocksecretmocksecret00";
const KEY_FLAKY_429 = "kd_test_42900000000a_mocksecretmocksecret00";

const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const noSleep = () => Promise.resolve();

interface LoggedRequest {
  method: string;
  path: string;
  query: Record<string, string | string[]>;
  body: unknown;
  headers: Record<string, string | null>;
}

function client(key: string = KEY, config: KuidaConfig = {}): Kuida {
  return new Kuida(key, { baseUrl: BASE, sleep: noSleep, ...config });
}

async function resetMock(): Promise<void> {
  const res = await fetch(`${MOCK}/__mock/reset`, { method: "POST" });
  assert.equal(res.status, 200, "no se pudo resetear el mock: ¿está corriendo?");
}

async function mockRequests(): Promise<LoggedRequest[]> {
  const res = await fetch(`${MOCK}/__mock/requests`);
  return ((await res.json()) as { requests: LoggedRequest[] }).requests;
}

const phone = (n: number) => `+54 9 342 555 ${String(n).padStart(4, "0")}`;

function findUp(file: string): string {
  let dir = __dirname;
  for (;;) {
    const candidate = join(dir, file);
    if (existsSync(candidate)) return candidate;
    const parent = dirname(dir);
    if (parent === dir) throw new Error(`No encontré ${file}`);
    dir = parent;
  }
}

async function closedPort(): Promise<number> {
  return new Promise((ok, fail) => {
    const srv = createServer();
    srv.once("error", fail);
    srv.listen(0, "127.0.0.1", () => {
      const address = srv.address();
      const port = typeof address === "object" && address ? address.port : 0;
      srv.close(() => ok(port));
    });
  });
}

// 1
test("account_retrieve", async () => {
  const account = await client().account.retrieve();
  assert.equal(account.object, "account");
  assert.ok(Array.isArray(account.apiKey.scopes));
  assert.ok(account.apiKey.scopes.includes("patients:write"));
  assert.match(account.lastResponse.requestId ?? "", /^req_/);
  assert.equal(Object.keys(account).includes("lastResponse"), false, "lastResponse no tiene que ser enumerable");
});

// 2
test("headers", async () => {
  await resetMock();
  const kuida = client();
  await kuida.account.retrieve();
  await kuida.patients.create({ phone: phone(0), fullName: "Paciente Demo" });
  await kuida.patients.list({ limit: 1 });

  const log = await mockRequests();
  assert.equal(log.length, 3);
  for (const r of log) {
    assert.equal(r.headers.authorization, `Bearer ${KEY}`);
    assert.equal(r.headers["kuida-version"], "2026-09-28");
    assert.match(r.headers["user-agent"] ?? "", /^Kuida\/v1 NodeBindings\/0\.1\.0$/);
    const ua = JSON.parse(r.headers["x-kuida-client-user-agent"] ?? "{}");
    assert.equal(ua.bindings_version, "0.1.0");
    assert.equal(ua.lang, "node");
    assert.ok(ua.lang_version && ua.platform);
    if (r.method === "POST") {
      assert.match(r.headers["idempotency-key"] ?? "", UUID_V4);
      assert.equal(r.headers["content-type"], "application/json");
    } else {
      assert.equal(r.headers["idempotency-key"], null);
    }
  }
  assert.deepEqual(
    log.map((r) => r.method),
    ["GET", "POST", "GET"],
  );
});

// 3
test("patients_crud", async () => {
  await resetMock();
  const kuida = client();
  const created = await kuida.patients.create({ phone: phone(0), fullName: "Paciente Demo", dni: "11111111" });
  assert.equal(created.object, "patient");
  assert.match(created.id, /^pat_/);

  const fetched = await kuida.patients.retrieve(created.id);
  assert.equal(fetched.id, created.id);
  assert.equal(fetched.fullName, "Paciente Demo");

  const updated = await kuida.patients.update(created.id, { email: "paciente.demo@example.com" });
  assert.equal(updated.email, "paciente.demo@example.com");

  await kuida.patients.create({ phone: phone(1), fullName: "Paciente Demo Dos" });
  const page = await kuida.patients.list({ phone: "3425550000" });
  assert.equal(page.object, "list");
  assert.deepEqual(
    page.data.map((p) => p.id),
    [created.id],
  );
});

// 4
test("idempotency_explicit", async () => {
  await resetMock();
  const kuida = client();
  const idempotencyKey = randomUUID();
  const a = await kuida.patients.create({ phone: phone(0), fullName: "Paciente Demo" }, { idempotencyKey });
  const b = await kuida.patients.create({ phone: phone(0), fullName: "Paciente Demo" }, { idempotencyKey });
  assert.equal(a.id, b.id);
  assert.equal(b.lastResponse.headers["idempotent-replayed"], "true");

  await assert.rejects(
    kuida.patients.create({ phone: phone(1), fullName: "Paciente Demo" }, { idempotencyKey }),
    (err: unknown) => {
      assert.ok(err instanceof IdempotencyError);
      assert.equal(err.code, "idempotency_key_reused");
      assert.equal(err.httpStatus, 400);
      return true;
    },
  );
  const log = await mockRequests();
  assert.ok(log.filter((r) => r.method === "POST").every((r) => r.headers["idempotency-key"] === idempotencyKey));
});

// 5
test("pagination_manual", async () => {
  await resetMock();
  const kuida = client();
  for (let i = 0; i < 5; i++) await kuida.patients.create({ phone: phone(i), fullName: `Paciente Demo ${i}` });

  const first = await kuida.patients.list({ limit: 2 });
  assert.equal(first.data.length, 2);
  assert.equal(first.hasMore, true);

  const second = await kuida.patients.list({ limit: 2, startingAfter: first.data[1]!.id });
  assert.equal(second.data.length, 2);
  assert.equal(second.hasMore, true);
  const ids = [...first.data, ...second.data].map((p) => p.id);
  assert.equal(new Set(ids).size, 4);

  const third = await kuida.patients.list({ limit: 2, startingAfter: second.data[1]!.id });
  assert.equal(third.data.length, 1);
  assert.equal(third.hasMore, false);

  const log = await mockRequests();
  const gets = log.filter((r) => r.method === "GET");
  assert.equal(gets[1]!.query.startingAfter, first.data[1]!.id);
  assert.equal(gets[1]!.query.limit, "2");
});

// 6
test("pagination_auto", async () => {
  await resetMock();
  const kuida = client();
  const created = new Set<string>();
  for (let i = 0; i < 5; i++) created.add((await kuida.patients.create({ phone: phone(i) })).id);
  const before = (await mockRequests()).length;

  const seen: string[] = [];
  for await (const patient of kuida.patients.list({ limit: 2 })) seen.push(patient.id);
  assert.equal(seen.length, 5);
  assert.equal(new Set(seen).size, 5);
  assert.deepEqual(new Set(seen), created);

  const pages = (await mockRequests()).slice(before);
  assert.equal(pages.length, 3);
  assert.ok(pages.every((r) => r.method === "GET" && r.path === "/v1/patients" && r.query.limit === "2"));
  assert.equal(pages[0]!.query.startingAfter, undefined);
  assert.equal(pages[1]!.query.startingAfter, seen[1]);
  assert.equal(pages[2]!.query.startingAfter, seen[3]);

  const firstThree = await kuida.patients.list({ limit: 2 }).autoPagingToArray({ limit: 3 });
  assert.deepEqual(
    firstThree.map((p) => p.id),
    seen.slice(0, 3),
  );

  // La misma ListPromise sirve como promesa de la primera página.
  const listed = kuida.patients.list({ limit: 2 });
  const page = await listed;
  assert.equal(page.data.length, 2);
});

// 7
test("appointments_flow", async () => {
  await resetMock();
  const kuida = client();
  const appointment = await kuida.appointments.create({
    patient: { phone: phone(0), fullName: "Paciente Demo", dni: "11111111" },
    doctor: { externalId: "prof-001", fullName: "Dra. Profesional Demo", specialty: "Clínica médica" },
    startAt: new Date("2026-10-05T13:00:00.000Z"),
    type: "Control",
    externalId: "turno-001",
  });
  assert.equal(appointment.object, "appointment");
  assert.match(appointment.patient, /^pat_/);
  assert.match(appointment.doctor ?? "", /^doc_/);
  assert.equal(appointment.startAt, "2026-10-05T13:00:00.000Z");

  const moved = await kuida.appointments.update(appointment.id, { startAt: "2026-10-06T10:30:00-03:00" });
  assert.equal(moved.startAt, "2026-10-06T13:30:00.000Z");

  const cancelled = await kuida.appointments.cancel(appointment.id);
  assert.equal(cancelled.status, "cancelled");
  const cancelRequest = (await mockRequests()).find((r) => r.path.endsWith("/cancel"));
  assert.deepEqual(cancelRequest?.body, {});
  assert.match(cancelRequest?.headers["idempotency-key"] ?? "", UUID_V4);

  await assert.rejects(kuida.appointments.update(appointment.id, { startAt: "2026-10-07T10:00:00-03:00" }), InvalidRequestError);
});

// 8
test("visit_closes_appointment", async () => {
  await resetMock();
  const kuida = client();
  const appointment = await kuida.appointments.create({
    patient: { phone: phone(0), fullName: "Paciente Demo" },
    startAt: "2026-10-05T10:00:00-03:00",
    externalId: "turno-002",
  });
  const visit = await kuida.visits.create({
    patient: { phone: phone(0) },
    visitedAt: "2026-10-05T10:20:00-03:00",
    appointment: "turno-002",
    diagnosis: "Control sin novedades",
  });
  assert.equal(visit.object, "visit");
  assert.equal(visit.appointment, appointment.id);
  assert.equal(visit.patient, appointment.patient);

  const after = await kuida.appointments.retrieve(appointment.id);
  assert.equal(after.status, "completed");
  assert.equal((await kuida.visits.retrieve(visit.id)).id, visit.id);
});

// 9
test("intake_and_treatment", async () => {
  await resetMock();
  const kuida = client();
  const patient = await kuida.patients.create({ phone: phone(0), fullName: "Paciente Demo", dni: "11111111" });

  // En el OpenAPI, IntakeRequestCreateParams.patient es PatientIdentity (objeto), no PatientReference.
  const intake = await kuida.intakeRequests.create({
    patient: { phone: phone(0), fullName: "Paciente Demo", dni: "11111111" },
    coverage: { insurer: "Obra Social Demo", memberId: "0000-0000" },
    summary: "Ingreso programado",
  });
  assert.equal(intake.object, "intake_request");
  assert.equal(intake.patient, patient.id);
  assert.equal((await kuida.intakeRequests.retrieve(intake.id)).id, intake.id);

  const treatment = await kuida.treatments.create({
    patient: patient.id,
    name: "Medicación demo",
    dosage: "1 comprimido",
    frequency: "cada 12 horas",
    startDate: new Date("2026-10-01T12:00:00.000Z"),
  });
  assert.equal(treatment.object, "treatment");
  assert.equal(treatment.patient, patient.id);
  assert.equal(treatment.startDate, "2026-10-01T12:00:00.000Z");

  const treatments = await kuida.treatments.list({ patient: patient.id, active: true });
  assert.deepEqual(
    treatments.data.map((t) => t.id),
    [treatment.id],
  );
  const last = (await mockRequests()).at(-1)!;
  assert.equal(last.query.active, "true");
});

// 10
test("events_batch", async () => {
  await resetMock();
  const kuida = client();
  const valid = {
    id: `paciente-demo-${randomUUID()}`,
    type: "patient.upserted" as const,
    data: { phone: phone(0), fullName: "Paciente Demo", dni: "11111111" },
  };
  const batch = await kuida.events.createBatch([valid, { id: "evento-invalido", type: "tipo.inexistente", data: {} }]);
  assert.equal(batch.object, "event_batch");
  assert.equal(batch.results.length, 2);
  assert.equal(batch.results[0]!.status, "processed");
  assert.equal(batch.results[0]!.accepted, true);
  assert.equal(batch.results[1]!.status, "invalid");
  assert.equal(batch.results[1]!.accepted, false);

  const again = await kuida.events.create(valid);
  assert.equal(again.results[0]!.status, "duplicate");
  assert.equal(again.results[0]!.event, batch.results[0]!.event);

  const event = await kuida.events.retrieve(batch.results[0]!.event!);
  assert.equal(event.type, "patient.upserted");
  assert.deepEqual(event.data, valid.data);
});

// 11
test("webhook_endpoints_flow", async () => {
  await resetMock();
  const kuida = client();
  const endpoint = await kuida.webhookEndpoints.create({
    url: "https://sistema.example.com/webhooks/kuida",
    enabledEvents: ["intake.ready", "appointment.cancelled"],
  });
  assert.equal(endpoint.object, "webhook_endpoint");
  assert.match(endpoint.secret ?? "", /^whsec_/);

  const fetched = await kuida.webhookEndpoints.retrieve(endpoint.id);
  assert.equal(fetched.id, endpoint.id);
  assert.equal("secret" in fetched, false);

  const updated = await kuida.webhookEndpoints.update(endpoint.id, { enabledEvents: ["*"] });
  assert.deepEqual(updated.enabledEvents, ["*"]);

  const delivery = await kuida.webhookEndpoints.ping(endpoint.id);
  assert.equal(delivery.object, "webhook_delivery");
  assert.equal(delivery.status, "pending");
  assert.equal(delivery.eventType, "webhook.ping");
  const pingRequest = (await mockRequests()).find((r) => r.path.endsWith("/ping"));
  assert.deepEqual(pingRequest?.body, {});

  const deliveries = await kuida.webhookDeliveries.list({ webhookEndpoint: endpoint.id });
  assert.deepEqual(
    deliveries.data.map((d) => d.id),
    [delivery.id],
  );
  assert.equal((await kuida.webhookDeliveries.retrieve(delivery.id)).id, delivery.id);

  const deleted = await kuida.webhookEndpoints.delete(endpoint.id);
  assert.equal(deleted.deleted, true);
  assert.equal(deleted.id, endpoint.id);
});

// 12
test("errors", async () => {
  await resetMock();

  // Sin clave: falla al construir.
  const saved = process.env.KUIDA_API_KEY;
  delete process.env.KUIDA_API_KEY;
  try {
    assert.throws(
      () => new Kuida(undefined, { baseUrl: BASE }),
      (err: unknown) => err instanceof AuthenticationError && err.code === "api_key_missing",
    );
  } finally {
    if (saved !== undefined) process.env.KUIDA_API_KEY = saved;
  }

  await assert.rejects(client("kd_test_invalida").account.retrieve(), (err: unknown) => {
    assert.ok(err instanceof AuthenticationError);
    assert.ok(err instanceof KuidaError);
    assert.equal(err.httpStatus, 401);
    assert.equal(err.code, "api_key_invalid");
    return true;
  });

  await assert.rejects(client(KEY_REVOKED).account.retrieve(), (err: unknown) => {
    assert.ok(err instanceof AuthenticationError);
    assert.equal(err.code, "api_key_revoked");
    return true;
  });

  await assert.rejects(client(KEY_EVENTS_ONLY).patients.list(), (err: unknown) => {
    assert.ok(err instanceof PermissionError);
    assert.equal(err.httpStatus, 403);
    assert.equal(err.code, "scope_missing");
    return true;
  });

  await assert.rejects(client().patients.retrieve("pat_noexiste"), (err: unknown) => {
    assert.ok(err instanceof InvalidRequestError);
    assert.equal(err.httpStatus, 404);
    assert.equal(err.type, "invalid_request_error");
    assert.equal(err.code, "resource_missing");
    assert.match(err.requestId ?? "", /^req_/);
    assert.equal(err.requestId, err.headers["request-id"]);
    assert.ok(err.docUrl);
    return true;
  });

  await assert.rejects(
    client().patients.create({ fullName: "Paciente Demo" } as unknown as PatientCreateParams),
    (err: unknown) => {
      assert.ok(err instanceof InvalidRequestError);
      assert.equal(err.param, "phone");
      assert.equal(err.code, "parameter_missing");
      return true;
    },
  );
});

// 13
test("retries_5xx", async () => {
  await resetMock();
  const waits: number[] = [];
  const kuida = client(KEY_FLAKY_503, {
    sleep: (ms) => {
      waits.push(ms);
      return Promise.resolve();
    },
  });
  const patient = await kuida.patients.create({ phone: phone(0), fullName: "Paciente Demo" });
  assert.match(patient.id, /^pat_/);
  assert.equal(waits.length, 1);
  assert.ok(waits[0]! >= 375 && waits[0]! <= 625, `backoff del primer reintento fuera de 0,5 s ±25 %: ${waits[0]}`);

  const posts = (await mockRequests()).filter((r) => r.method === "POST" && r.path === "/v1/patients");
  assert.equal(posts.length, 2);
  assert.match(posts[0]!.headers["idempotency-key"] ?? "", UUID_V4);
  assert.equal(posts[0]!.headers["idempotency-key"], posts[1]!.headers["idempotency-key"]);
});

// 14
test("retries_429", async () => {
  await resetMock();
  const waits: number[] = [];
  const kuida = client(KEY_FLAKY_429, {
    sleep: (ms) => {
      waits.push(ms);
      return new Promise((r) => setTimeout(r, ms));
    },
  });
  const started = Date.now();
  const account = await kuida.account.retrieve();
  const elapsed = Date.now() - started;
  assert.equal(account.object, "account");
  assert.deepEqual(waits, [1000], "tiene que esperar exactamente lo que dice Retry-After");
  assert.ok(elapsed >= 950, `esperó ${elapsed} ms`);
  assert.equal((await mockRequests()).length, 2);
});

// 15
test("no_retry", async () => {
  await resetMock();
  const kuida = client(KEY_FLAKY_503, { maxRetries: 0 });
  await assert.rejects(kuida.patients.create({ phone: phone(0) }), (err: unknown) => {
    assert.ok(err instanceof APIError);
    assert.equal(err.httpStatus, 503);
    assert.equal(err.type, "api_error");
    return true;
  });
  assert.equal((await mockRequests()).length, 1);

  // maxRetries por pedido también se respeta.
  await resetMock();
  await assert.rejects(client(KEY_FLAKY_503).account.retrieve({ maxRetries: 0 }), APIError);
  assert.equal((await mockRequests()).length, 1);
});

// 16
test("connection_error", async () => {
  const port = await closedPort();
  const kuida = client(KEY, { baseUrl: `http://127.0.0.1:${port}/api`, maxRetries: 0 });
  await assert.rejects(kuida.account.retrieve(), (err: unknown) => {
    assert.ok(err instanceof APIConnectionError);
    assert.ok(err instanceof KuidaError);
    assert.equal(err.httpStatus, null);
    return true;
  });

  // Con reintentos, también termina en APIConnectionError.
  const retrying = client(KEY, { baseUrl: `http://127.0.0.1:${port}/api`, maxRetries: 2 });
  await assert.rejects(retrying.account.retrieve(), APIConnectionError);
});

// 17
test("webhook_signature", () => {
  const v = JSON.parse(readFileSync(findUp(join("conformance", "webhook-vectors.json")), "utf8")) as {
    secret: string;
    timestamp: string;
    payload: string;
    signatureHeader: string;
    wrongSecret: string;
    tamperedPayload: string;
  };
  const now = Number(v.timestamp);

  assert.equal(Webhook.verifySignature(v.payload, v.signatureHeader, v.timestamp, v.secret, 300, now), true);
  assert.equal(Webhook.verifySignature(v.payload, v.signatureHeader, v.timestamp, v.secret, 0), true);
  assert.equal(Webhook.verifySignature(Buffer.from(v.payload), v.signatureHeader, v.timestamp, v.secret, 0), true);

  const event = Webhook.constructEvent(v.payload, v.signatureHeader, v.timestamp, v.secret, 300, now);
  assert.equal(event.object, "event");
  assert.equal(event.type, "intake.ready");
  assert.equal(event.apiVersion, "2026-09-28");
  assert.deepEqual(event.data, JSON.parse(v.payload).data);

  const rejects = (fn: () => unknown) => assert.throws(fn, SignatureVerificationError);
  rejects(() => Webhook.verifySignature(v.payload, v.signatureHeader, v.timestamp, v.wrongSecret, 300, now));
  rejects(() => Webhook.verifySignature(v.tamperedPayload, v.signatureHeader, v.timestamp, v.secret, 300, now));
  rejects(() => Webhook.verifySignature(v.payload, v.signatureHeader, v.timestamp, v.secret, 300, now + 301));
  rejects(() => Webhook.verifySignature(v.payload, v.signatureHeader, v.timestamp, v.secret));
  rejects(() => Webhook.verifySignature(v.payload, undefined, v.timestamp, v.secret, 0));
  rejects(() => Webhook.verifySignature(v.payload, v.signatureHeader, undefined, v.secret, 0));
  rejects(() => Webhook.constructEvent(v.tamperedPayload, v.signatureHeader, v.timestamp, v.secret, 0));

  // Un cuerpo firmado ahora verifica con la tolerancia default.
  const body = JSON.stringify({
    id: "whd_prueba",
    object: "event",
    type: "webhook.ping",
    apiVersion: "2026-09-28",
    livemode: false,
    occurredAt: new Date().toISOString(),
    ref: "ping_prueba",
    data: { message: "Evento de prueba" },
  });
  const ts = String(Math.floor(Date.now() / 1000));
  const signature = `sha256=${createHmac("sha256", v.secret).update(`${ts}.${body}`).digest("hex")}`;
  assert.equal(Webhook.computeSignature(body, ts, v.secret), signature);
  assert.equal(Webhook.verifySignature(body, signature, ts, v.secret), true);
  assert.equal(Webhook.constructEvent(Buffer.from(body), [signature], ts, v.secret).type, "webhook.ping");
});
