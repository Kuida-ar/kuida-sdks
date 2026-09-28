#!/usr/bin/env node
/**
 * Genera src/generated/types.ts a partir de ../openapi/kuida-v1.json.
 *
 *   node scripts/generate.mjs      (o `npm run generate`)
 *
 * Emite: los esquemas de components.schemas como tipos de TypeScript, los
 * parámetros de query de cada `list` como `<Modelo>ListParams`, las variantes
 * de EventInput con nombre propio y la tabla OPERATIONS (recurso, método,
 * verbo HTTP y ruta) que usa la prueba de cobertura. Sin dependencias.
 *
 * Reglas:
 * - `anyOf`/`oneOf` → unión; `[X, null]` → `X | null`.
 * - `format: date-time` en esquemas de entrada (alcanzables desde un
 *   requestBody) → `string | Date`: el SDK serializa Date a ISO 8601.
 * - Filtros booleanos del query (`enum: ["true","false"]`) → `boolean | "true" | "false"`.
 * - Objetos con `additionalProperties: {}` → `Record<string, unknown>`.
 */
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const specPath = resolve(here, "../../openapi/kuida-v1.json");
const outPath = resolve(here, "../src/generated/types.ts");
const spec = JSON.parse(readFileSync(specPath, "utf8"));
const schemas = spec.components.schemas;

const pascal = (s) =>
  s
    .split(/[^A-Za-z0-9]+/)
    .filter(Boolean)
    .map((p) => p[0].toUpperCase() + p.slice(1))
    .join("");
const camel = (s) => {
  const p = pascal(s);
  return p[0].toLowerCase() + p.slice(1);
};
const refName = (ref) => ref.split("/").pop();
const validIdent = (k) => /^[A-Za-z_$][A-Za-z0-9_$]*$/.test(k);
const propKey = (k) => (validIdent(k) ? k : JSON.stringify(k));

// ─── esquemas de entrada ─────────────────────────────────────────────────────

const inputSchemas = new Set();
function collectRefs(node, into) {
  if (Array.isArray(node)) return node.forEach((n) => collectRefs(n, into));
  if (!node || typeof node !== "object") return;
  if (node.$ref) {
    const name = refName(node.$ref);
    if (!into.has(name)) {
      into.add(name);
      collectRefs(schemas[name], into);
    }
  }
  for (const v of Object.values(node)) collectRefs(v, into);
}
for (const ops of Object.values(spec.paths)) {
  for (const op of Object.values(ops)) {
    if (op?.requestBody) collectRefs(op.requestBody, inputSchemas);
  }
}

// ─── JSDoc ───────────────────────────────────────────────────────────────────

function doc(schema, indent = "") {
  const lines = [];
  if (schema?.description) lines.push(...String(schema.description).split("\n"));
  const hints = [];
  if (schema?.format && schema.format !== "date-time") hints.push(`Formato: ${schema.format}.`);
  if (schema?.pattern) hints.push(`Patrón: \`${schema.pattern}\`.`);
  if (schema?.default !== undefined) hints.push(`Default: \`${JSON.stringify(schema.default)}\`.`);
  if (hints.length) lines.push(hints.join(" "));
  if (!lines.length) return "";
  if (lines.length === 1) return `${indent}/** ${lines[0].replace(/\*\//g, "*\\/")} */\n`;
  return `${indent}/**\n${lines.map((l) => `${indent} * ${l.replace(/\*\//g, "*\\/")}`.trimEnd()).join("\n")}\n${indent} */\n`;
}

// ─── JSON Schema → TypeScript ────────────────────────────────────────────────

function tsType(schema, ctx, indent) {
  if (!schema || Object.keys(schema).length === 0) return "unknown";
  if (schema.$ref) return refName(schema.$ref);
  if (schema.const !== undefined) return JSON.stringify(schema.const);
  const union = schema.anyOf ?? schema.oneOf;
  if (union) {
    const parts = [...new Set(union.map((s) => tsType(s, ctx, indent)))];
    return parts.length === 1 ? parts[0] : parts.join(" | ");
  }
  if (schema.enum) {
    const vals = schema.enum;
    if (ctx.query && vals.length === 2 && vals.includes("true") && vals.includes("false")) {
      return 'boolean | "true" | "false"';
    }
    return vals.map((v) => JSON.stringify(v)).join(" | ");
  }
  const t = schema.type;
  if (Array.isArray(t)) return t.map((x) => tsType({ ...schema, type: x }, ctx, indent)).join(" | ");
  switch (t) {
    case "string":
      return schema.format === "date-time" && ctx.input ? "string | Date" : "string";
    case "integer":
    case "number":
      return "number";
    case "boolean":
      return "boolean";
    case "null":
      return "null";
    case "array": {
      const inner = tsType(schema.items ?? {}, ctx, indent);
      return /[|&]/.test(inner) ? `Array<${inner}>` : `${inner}[]`;
    }
    case "object":
    case undefined:
      return objectType(schema, ctx, indent);
    default:
      return "unknown";
  }
}

function objectType(schema, ctx, indent) {
  const props = schema.properties ?? {};
  const required = new Set(schema.required ?? []);
  const keys = Object.keys(props);
  const extra = schema.additionalProperties;
  if (keys.length === 0) {
    if (extra === false) return "Record<string, never>";
    if (extra && typeof extra === "object" && Object.keys(extra).length) {
      return `Record<string, ${tsType(extra, ctx, indent)}>`;
    }
    return "Record<string, unknown>";
  }
  const inner = indent + "  ";
  let out = "{\n";
  for (const k of keys) {
    out += doc(props[k], inner);
    out += `${inner}${propKey(k)}${required.has(k) ? "" : "?"}: ${tsType(props[k], ctx, inner)};\n`;
  }
  // Campos exigidos por `required` pero sin definición en `properties`.
  for (const k of required) {
    if (!(k in props)) out += `${inner}/** Declarado en \`required\` sin esquema en el OpenAPI. */\n${inner}${propKey(k)}: unknown;\n`;
  }
  if (extra && typeof extra === "object") out += `${inner}[key: string]: unknown;\n`;
  return out + indent + "}";
}

function declare(name, schema, ctx) {
  const d = doc(schema);
  const isObject = (schema.type === "object" || (!schema.type && schema.properties)) && !schema.anyOf && !schema.oneOf;
  if (isObject && Object.keys(schema.properties ?? {}).length) {
    return `${d}export interface ${name} ${objectType(schema, ctx, "")}\n`;
  }
  return `${d}export type ${name} = ${tsType(schema, ctx, "")};\n`;
}

// ─── salida ──────────────────────────────────────────────────────────────────

let out = `// Generado por scripts/generate.mjs desde openapi/kuida-v1.json (API ${spec.info.version}).
// No editar a mano: regenerar con \`npm run generate\`.
/* eslint-disable */

/** Versión de la API contra la que se generaron estos tipos. */
export const API_VERSION = ${JSON.stringify(spec.info.version)};

`;

for (const [name, schema] of Object.entries(schemas)) {
  const ctx = { input: inputSchemas.has(name) };
  if (name === "EventInput") {
    // Una interfaz por tipo de evento, más la unión discriminada por `type`.
    const variants = [];
    for (const v of schema.oneOf) {
      const t = v.properties.type.const;
      const vname = `${pascal(t)}EventInput`;
      variants.push([t, vname]);
      out += `/** Evento \`${t}\` para \`events.create\` / \`events.createBatch\`. */\n`;
      out += `export interface ${vname} ${objectType(v, ctx, "")}\n\n`;
    }
    out += doc(schema);
    out += `export type EventInput =\n${variants.map(([, n]) => `  | ${n}`).join("\n")};\n\n`;
    out += `/** Tipos de evento que acepta \`POST /v1/events\`. */\nexport type EventInputType = EventInput["type"];\n\n`;
    out += `/** Mapa tipo de evento → forma de \`data\`. */\nexport interface EventInputDataMap {\n${variants
      .map(([t, n]) => `  ${JSON.stringify(t)}: ${n}["data"];`)
      .join("\n")}\n}\n\n`;
    continue;
  }
  out += declare(name, schema, ctx) + "\n";
}

// Parámetros de las operaciones `list` (query string).
const operations = [];
for (const [path, ops] of Object.entries(spec.paths)) {
  for (const [verb, op] of Object.entries(ops)) {
    if (!op?.["x-kuida-resource"]) continue;
    operations.push({
      resource: camel(op["x-kuida-resource"]),
      method: camel(op["x-kuida-method"]),
      httpMethod: verb.toUpperCase(),
      path,
    });
    if (op["x-kuida-method"] !== "list") continue;
    const listSchema = refName(op.responses["200"].content["application/json"].schema.$ref);
    const name = `${listSchema}Params`;
    const params = (op.parameters ?? []).filter((p) => !p.$ref && p.in === "query");
    let body = "{\n";
    for (const p of params) {
      body += doc({ ...p.schema, description: p.description ?? p.schema.description }, "  ");
      body += `  ${propKey(p.name)}?: ${tsType(p.schema, { input: true, query: true }, "  ")};\n`;
    }
    body += "}";
    out += `/** Parámetros de \`${camel(op["x-kuida-resource"])}.list\` (${verb.toUpperCase()} ${path}). */\nexport interface ${name} ${body}\n\n`;
  }
}

out += `/** Una operación de la API: recurso y método del SDK, verbo y ruta. */
export interface OperationSpec {
  resource: string;
  method: string;
  httpMethod: "GET" | "POST" | "PATCH" | "DELETE";
  path: string;
}

/** Las ${operations.length} operaciones del OpenAPI (\`x-kuida-resource\` + \`x-kuida-method\`). */
export const OPERATIONS: readonly OperationSpec[] = [
${operations
  .map((o) => `  { resource: ${JSON.stringify(o.resource)}, method: ${JSON.stringify(o.method)}, httpMethod: ${JSON.stringify(o.httpMethod)}, path: ${JSON.stringify(o.path)} },`)
  .join("\n")}
];
`;

mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, out);
console.log(`src/generated/types.ts: ${Object.keys(schemas).length} esquemas, ${operations.length} operaciones`);
