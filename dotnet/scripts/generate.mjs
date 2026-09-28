#!/usr/bin/env node
/**
 * Genera los modelos C# del SDK de .NET a partir de ../openapi/kuida-v1.json.
 *
 *   node scripts/generate.mjs
 *
 * Escribe un archivo por clase en src/Kuida/Generated/. La salida se commitea
 * junto con este script; los métodos de recurso (src/Kuida/Services) se
 * escriben a mano. Sin dependencias: Node >= 18.
 *
 * Reglas:
 *  - Propiedades en PascalCase con [JsonPropertyName("camelCase")] del cable.
 *  - date-time → DateTimeOffset; format "date" → string (netstandard2.0 no tiene DateOnly).
 *  - Mapas libres (object sin properties) → Dictionary<string, JsonElement> en respuestas.
 *  - Enums → string (la API agrega valores sin cambiar de versión); los valores van en la doc.
 *  - Los esquemas *List se exponen como KuidaList<T>; Error, EventBatchInput y las
 *    referencias (PatientReference, DoctorReference) se escriben a mano.
 *  - EventInput (oneOf discriminado por type) se genera como un sobre genérico con
 *    Data de tipo object (diccionario u objeto anónimo, viaja tal cual).
 *  - Parámetros de listas: una clase {Modelo}ListParams por operación paginada, con
 *    los filtros del OpenAPI y su serialización a query string.
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "..");
const specPath = path.resolve(root, "../openapi/kuida-v1.json");
const outDir = path.resolve(root, "src/Kuida/Generated");

const spec = JSON.parse(fs.readFileSync(specPath, "utf8"));
const schemas = spec.components.schemas;

const HANDWRITTEN = new Set(["Error", "EventBatchInput", "PatientReference", "DoctorReference"]);
const REF_TYPES = { PatientReference: "PatientReference", DoctorReference: "DoctorReference" };

// Nombres de las clases anidadas (objetos inline). Dos lugares con la misma forma comparten clase.
const INLINE_NAMES = {
  "Account.apiKey": "AccountApiKey",
  "Event.result": "ResultReference",
  "EventResult.result": "ResultReference",
  "WebhookEndpoint.lastError": "WebhookEndpointLastError",
  "IntakeRequestCreateParams.sender": "IntakeSender",
  "EventInput.source": "EventSource",
};

const pascal = (s) => s.replace(/(^|[_\-.\s])([a-zA-Z0-9])/g, (_, __, c) => c.toUpperCase()).replace(/^[a-z]/, (c) => c.toUpperCase());
const refName = (ref) => ref.split("/").pop();

function xml(text) {
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/`([^`]+)`/g, "<c>$1</c>");
}

function docLines(summary, extra = [], indent = "    ", summaryIsXml = false) {
  const parts = [];
  if (summary) parts.push(summaryIsXml ? summary : xml(summary));
  for (const e of extra) parts.push(e);
  if (parts.length === 0) return [];
  return [`${indent}/// <summary>`, ...parts.map((p) => `${indent}/// ${p}`), `${indent}/// </summary>`];
}

// Descripción de respaldo para los campos que el OpenAPI no documenta.
const FALLBACK_DOCS = {
  name: "Nombre.", fullName: "Nombre completo.", phone: "Teléfono.", email: "Email.", dni: "DNI.",
  timeZone: "Zona horaria IANA de la organización.", apiKey: "La clave de API con la que se hizo el pedido.",
  prefix: "Prefijo público de la clave.", scopes: "Permisos de la clave.",
  doctor: "Profesional.", patient: "Paciente.", type: "Tipo de turno, consulta o práctica.",
  deleted: "Siempre <c>true</c>.", id: "Id del objeto.", object: "Tipo de objeto.",
  externalId: "Id en su sistema.", specialty: "Especialidad.", data: "Datos libres, tal cual.",
  error: "Error, si hubo.", results: "Un resultado por evento, en el mismo orden.",
  accepted: "Si el evento se aceptó.", result: "Objeto que produjo el evento, si hubo.",
  system: "Nombre del sistema que emite el evento.", version: "Versión del sistema que emite el evento.",
  relationship: "Vínculo con el paciente.", plan: "Plan de la cobertura.",
  discardReason: "Motivo del descarte, si se descartó.", summary: "Resumen.",
  address: "Domicilio.", contacts: "Referentes o familiares.", coverage: "Cobertura médica.",
  requestedService: "Servicio o práctica solicitada.", organization: "Institución que deriva.",
  active: "Si está activo.", dosage: "Dosis.", frequency: "Frecuencia.", instructions: "Indicaciones.",
  diagnosis: "Diagnóstico.", notes: "Notas.", attempts: "Intentos de entrega hechos.",
  eventType: "Tipo de evento entregado.", lastError: "Último error de entrega, si hubo.",
  lastStatusCode: "Último código HTTP que respondió el endpoint.", url: "URL https del endpoint.",
  description: "Descripción para identificarlo.", message: "Mensaje.", apiVersion: "Versión de la API con la que se armó el cuerpo.",
  status: "Estado.", kind: "Clase.", source: "Origen.", startAt: "Fecha y hora de inicio.",
  enabledEvents: "Eventos que recibe el endpoint (<c>*</c> = todos).",
  livemode: "<c>true</c> en producción, <c>false</c> en el entorno de pruebas.",
};

// ─── clasificación: entrada (params) o salida (respuestas) ───────────────────

const inputSchemas = new Set();
const outputSchemas = new Set();
function collect(node, set) {
  if (!node || typeof node !== "object") return;
  if (Array.isArray(node)) return node.forEach((n) => collect(n, set));
  if (node.$ref) {
    const n = refName(node.$ref);
    if (!set.has(n)) {
      set.add(n);
      collect(schemas[n], set);
    }
    return;
  }
  for (const v of Object.values(node)) collect(v, set);
}
for (const ops of Object.values(spec.paths)) {
  for (const op of Object.values(ops)) {
    if (!op || typeof op !== "object") continue;
    if (op.requestBody) collect(op.requestBody, inputSchemas);
    for (const [code, r] of Object.entries(op.responses ?? {})) if (code.startsWith("2")) collect(r, outputSchemas);
  }
}
for (const hooks of Object.values(spec.webhooks ?? {})) for (const op of Object.values(hooks)) collect(op.requestBody, outputSchemas);

const isListSchema = (name) => /List$/.test(name) && schemas[name]?.properties?.data?.items?.$ref && schemas[name]?.properties?.hasMore;

// ─── tipos ───────────────────────────────────────────────────────────────────

const classes = new Map(); // nombre → { name, doc, props, output, interfaces }

function unwrapNullable(s) {
  if (s?.anyOf && s.anyOf.length === 2 && s.anyOf.some((x) => x.type === "null")) {
    const inner = s.anyOf.find((x) => x.type !== "null");
    return { schema: { ...inner, description: s.description ?? inner.description }, nullable: true };
  }
  return { schema: s, nullable: false };
}

const VALUE_TYPES = new Set(["bool", "int", "long", "double", "DateTimeOffset"]);

/** Devuelve { type, isValue } para un esquema de propiedad. */
function csType(s, ctx) {
  if (s.$ref) {
    const n = refName(s.$ref);
    if (REF_TYPES[n]) return { type: REF_TYPES[n], isValue: false };
    if (isListSchema(n)) return { type: `KuidaList<${refName(schemas[n].properties.data.items.$ref)}>`, isValue: false };
    ensureClass(n);
    return { type: n, isValue: false };
  }
  if (s.anyOf) {
    // anyOf no nulo: sin forma común en C#, se deja como JsonElement.
    return { type: "JsonElement", isValue: true };
  }
  switch (s.type) {
    case "string":
      if (s.format === "date-time") return { type: "DateTimeOffset", isValue: true };
      return { type: "string", isValue: false };
    case "boolean":
      return { type: "bool", isValue: true };
    case "integer":
      return { type: "int", isValue: true };
    case "number":
      return Number.isInteger(s.const) ? { type: "int", isValue: true } : { type: "double", isValue: true };
    case "array": {
      const item = csType(s.items ?? {}, { ...ctx, prop: ctx.prop });
      return { type: `List<${item.type}${item.isValue && item.nullable ? "?" : ""}>`, isValue: false };
    }
    case "object": {
      if (!s.properties || Object.keys(s.properties).length === 0) {
        return ctx.output ? { type: "Dictionary<string, JsonElement>", isValue: false } : { type: "object", isValue: false };
      }
      const key = `${ctx.parent}.${ctx.prop}`;
      const name = INLINE_NAMES[key] ?? `${ctx.parent}${pascal(ctx.prop)}`;
      if (!classes.has(name)) buildClass(name, s, ctx.output, false);
      return { type: name, isValue: false };
    }
    default:
      return ctx.output ? { type: "JsonElement", isValue: true } : { type: "object", isValue: false };
  }
}

function enumDoc(s) {
  const vals = s.enum ?? (s.items?.enum) ?? null;
  if (!vals) return [];
  return [`Valores: ${vals.map((v) => `<c>${xml(v)}</c>`).join(", ")}.`];
}

function buildClass(name, schema, output, topLevel) {
  const cls = { name, doc: schema.description, props: [], output, topLevel, interfaces: [] };
  classes.set(name, cls);
  const required = new Set(schema.required ?? []);
  for (const [wire, raw] of Object.entries(schema.properties ?? {})) {
    const { schema: s, nullable: explicitNull } = unwrapNullable(raw);
    let propName = pascal(wire);
    if (propName === name) propName = `${propName}Value`;
    let { type, isValue } = csType(s, { parent: name, prop: wire, output });
    const req = required.has(wire);
    const extra = [...enumDoc(s)];
    if (s.const !== undefined && s.type === "string") extra.push(`Siempre <c>${xml(s.const)}</c>.`);
    if (!output && req) extra.push("Obligatorio.");
    if (s.format === "date") extra.push("Fecha ISO 8601 sin hora (<c>AAAA-MM-DD</c>).");
    // Nulabilidad: en salida, lo que puede faltar o venir null; en entrada, todo es opcional
    // para poder omitirlo (el JSON no manda nulls) salvo las referencias obligatorias.
    let nullable;
    if (output) nullable = explicitNull || !req;
    else nullable = isValue || !req;
    const init = !nullable && !isValue ? " = null!;" : "";
    let doc = s.description ?? raw.description ?? (s.$ref ? schemas[refName(s.$ref)]?.description : undefined);
    let docIsXml = false;
    if (!doc && FALLBACK_DOCS[wire]) { doc = FALLBACK_DOCS[wire]; docIsXml = true; }
    if (!doc) { doc = `Campo <c>${xml(wire)}</c>.`; docIsXml = true; }
    cls.props.push({ wire, propName, type: `${type}${nullable ? "?" : ""}`, doc, docIsXml, extra, init });
  }
  if (output && topLevel && schema.properties?.id && required.has("id")) cls.interfaces.push("IHasId");
  return cls;
}

function ensureClass(name) {
  if (classes.has(name) || HANDWRITTEN.has(name) || isListSchema(name)) return;
  const s = schemas[name];
  if (name === "EventInput") return buildEventInput();
  const output = outputSchemas.has(name) && !inputSchemas.has(name);
  buildClass(name, s, output, true);
}

function buildEventInput() {
  const variants = schemas.EventInput.oneOf;
  const types = variants.map((v) => v.properties.type.const);
  const common = Object.keys(variants[0].properties).filter((k) => variants.every((v) => v.properties[k]));
  const base = variants[0];
  const cls = { name: "EventInput", doc: schemas.EventInput.description, props: [], output: false, topLevel: true, interfaces: [] };
  classes.set("EventInput", cls);
  for (const wire of common) {
    const s = base.properties[wire];
    const propName = pascal(wire);
    if (wire === "type") {
      cls.props.push({ wire, propName, type: "string", doc: "Tipo del evento; define la forma de `data`.", extra: [`Valores: ${types.map((t) => `<c>${t}</c>`).join(", ")}.`, "Obligatorio."], init: " = null!;", raw: true });
      continue;
    }
    if (wire === "data") {
      cls.props.push({ wire, propName, type: "object", doc: "Datos del evento: un diccionario o un objeto anónimo. Viaja tal cual, sin convertir claves.", extra: ["Obligatorio."], init: " = null!;", raw: true });
      continue;
    }
    const { type, isValue } = csType(s, { parent: "EventInput", prop: wire, output: false });
    const req = base.required?.includes(wire);
    const nullable = isValue || !req;
    cls.props.push({ wire, propName, type: `${type}${nullable ? "?" : ""}`, doc: s.description ?? FALLBACK_DOCS[wire], docIsXml: !s.description, extra: req ? ["Obligatorio."] : [], init: !nullable ? " = null!;" : "" });
  }
}

// Todos los esquemas públicos.
for (const name of Object.keys(schemas)) ensureClass(name);

// ─── parámetros de listas ────────────────────────────────────────────────────

const listParams = [];
for (const [p, ops] of Object.entries(spec.paths)) {
  const op = ops.get;
  if (!op || op["x-kuida-method"] !== "list") continue;
  const resp = op.responses["200"].content["application/json"].schema.$ref;
  const listName = refName(resp);
  const item = refName(schemas[listName].properties.data.items.$ref);
  const params = (op.parameters ?? []).filter((x) => !x.$ref && x.in === "query" && !["limit", "startingAfter", "endingBefore"].includes(x.name));
  listParams.push({ name: `${item}ListParams`, item, path: p, op, params });
}

function listParamsClass({ name, item, path: p, op, params }) {
  const lines = [];
  lines.push(...docLines(`Parámetros de \`GET ${p}\`.${op.summary ? " " + op.summary.replace(/\.$/, "") + "." : ""}`, [], "    "));
  lines.push(`    public partial class ${name} : ListParams`);
  lines.push("    {");
  const adds = [];
  for (const prm of params) {
    const s = prm.schema ?? {};
    const propName = pascal(prm.name);
    let type = "string";
    let conv = `${propName}`;
    const extra = [];
    if (s.enum && s.enum.length === 2 && s.enum.includes("true") && s.enum.includes("false")) {
      type = "bool?";
      conv = `${propName}.HasValue ? (${propName}.Value ? "true" : "false") : null`;
    } else if (s.format === "date-time") {
      type = "DateTimeOffset?";
      conv = `${propName}.HasValue ? KuidaJson.FormatDate(${propName}.Value) : null`;
    } else {
      type = "string?";
      if (s.enum) extra.push(`Valores: ${s.enum.map((v) => `<c>${xml(v)}</c>`).join(", ")}.`);
    }
    lines.push(...docLines(prm.description ?? `Filtra por \`${prm.name}\`.`, extra, "        "));
    lines.push(`        public ${type} ${propName} { get; set; }`);
    lines.push("");
    adds.push(`            Add(query, "${prm.name}", ${conv});`);
  }
  lines.push("        /// <inheritdoc />");
  lines.push("        internal override void AppendQuery(IList<KeyValuePair<string, string>> query)");
  lines.push("        {");
  lines.push("            base.AppendQuery(query);");
  lines.push(...adds);
  lines.push("        }");
  lines.push("    }");
  return lines;
}

// ─── emisión ─────────────────────────────────────────────────────────────────

const HEADER = [
  "// <auto-generated>",
  "// Generado por scripts/generate.mjs desde openapi/kuida-v1.json. No editar a mano:",
  "// cambiar el OpenAPI y volver a correr `node scripts/generate.mjs`.",
  "// </auto-generated>",
  "#nullable enable",
  "using System;",
  "using System.Collections.Generic;",
  "using System.Text.Json;",
  "using System.Text.Json.Serialization;",
  "",
  "namespace Kuida",
  "{",
];

function emitClass(cls) {
  const lines = [...HEADER];
  lines.push(...docLines(cls.doc ?? (cls.output ? `Objeto <c>${cls.name}</c> de la API.` : `Parámetros <c>${cls.name}</c>.`)));
  const bases = [];
  if (cls.output && cls.topLevel) bases.push("KuidaObject");
  bases.push(...cls.interfaces);
  lines.push(`    public partial class ${cls.name}${bases.length ? " : " + bases.join(", ") : ""}`);
  lines.push("    {");
  cls.props.forEach((p, i) => {
    if (i > 0) lines.push("");
    lines.push(...docLines(p.doc, p.extra, "        ", p.docIsXml));
    lines.push(`        [JsonPropertyName("${p.wire}")]`);
    lines.push(`        public ${p.type} ${p.propName} { get; set; }${p.init}`);
  });
  lines.push("    }");
  lines.push("}");
  return lines.join("\n") + "\n";
}

fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(outDir, { recursive: true });
const written = [];
for (const cls of [...classes.values()].sort((a, b) => a.name.localeCompare(b.name))) {
  fs.writeFileSync(path.join(outDir, `${cls.name}.cs`), emitClass(cls));
  written.push(cls.name);
}
for (const lp of listParams) {
  const body = [...HEADER, ...listParamsClass(lp), "}"].join("\n") + "\n";
  fs.writeFileSync(path.join(outDir, `${lp.name}.cs`), body);
  written.push(lp.name);
}
// Versión de la API que declara el OpenAPI.
fs.writeFileSync(
  path.join(outDir, "ApiVersion.cs"),
  [...HEADER.slice(0, 5), "", "namespace Kuida", "{", "    internal static class GeneratedApiVersion", "    {",
    `        /// <summary>Versión de la API del OpenAPI con el que se generó el SDK.</summary>`,
    `        internal const string Value = "${spec.info.version}";`, "    }", "}", ""].join("\n"),
);
written.push("ApiVersion");
console.log(`${written.length} archivos en ${path.relative(root, outDir)}: ${written.join(", ")}`);
