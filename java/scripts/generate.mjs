#!/usr/bin/env node
/**
 * Genera los modelos Java del SDK a partir de ../openapi/kuida-v1.json.
 *
 *   node scripts/generate.mjs
 *
 * Escribe src/main/java/ar/kuida/model/*.java:
 *   - modelos de respuesta (clases con getters, deserializadas con Gson);
 *   - parámetros de entrada (clases inmutables con builder);
 *   - parámetros de listado (`*ListParams`), a partir de los query params de cada operación `list`;
 *   - uniones id-o-identidad (`PatientReference`, `DoctorReference`);
 *   - el sobre genérico de eventos (`EventInput`).
 *
 * Los esquemas `*List` no se generan: toda lista se expone como `ar.kuida.KuidaList<T>`.
 * Los métodos de recurso (`ar.kuida.service`) se escriben a mano.
 * Sin dependencias: Node >= 18.
 */
import { readFileSync, writeFileSync, readdirSync, unlinkSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const SPEC = JSON.parse(readFileSync(join(ROOT, "..", "openapi", "kuida-v1.json"), "utf8"));
const OUT = join(ROOT, "src", "main", "java", "ar", "kuida", "model");
const PKG = "ar.kuida.model";
const HEADER = "// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.";
const schemas = SPEC.components.schemas;

// ─── utilidades ──────────────────────────────────────────────────────────────

const pascal = (s) => s.replace(/(^|[_\-. ])([a-z0-9])/gi, (_, __, c) => c.toUpperCase());
const cap = (s) => s[0].toUpperCase() + s.slice(1);
const refName = (ref) => ref.split("/").pop();
const singular = (s) => (s.endsWith("ies") ? s.slice(0, -3) + "y" : s.endsWith("s") ? s.slice(0, -1) : s);
const JAVA_KEYWORDS = new Set(["default", "class", "public", "private", "static", "final", "new", "package", "import", "interface", "enum", "switch", "case", "return", "this", "super"]);
const ident = (s) => (JAVA_KEYWORDS.has(s) ? s + "_" : s);

/** Texto de OpenAPI → HTML seguro para Javadoc. */
function jd(text) {
  if (!text) return "";
  return text
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/\*\//g, "*&#47;").replace(/@/g, "&#64;")
    .replace(/`([^`]+)`/g, "<code>$1</code>");
}
function javadoc(lines, indent) {
  const ls = lines.filter((l) => l !== undefined && l !== null && l !== "");
  if (ls.length === 0) return "";
  if (ls.length === 1) return `${indent}/** ${ls[0]} */\n`;
  return `${indent}/**\n${ls.map((l, i) => `${indent} * ${i === 0 ? l : "<p>" + l}`).join(`\n${indent} *\n`)}\n${indent} */\n`;
}

/** Quita `anyOf [X, null]` y devuelve { schema, nullable }. */
function unwrapNullable(s) {
  if (s && s.anyOf) {
    const nonNull = s.anyOf.filter((x) => x.type !== "null");
    if (nonNull.length === 1 && nonNull.length < s.anyOf.length) return { schema: { ...nonNull[0], description: s.description ?? nonNull[0].description }, nullable: true };
  }
  return { schema: s, nullable: false };
}
const isFreeMap = (s) => s.type === "object" && !s.properties && s.additionalProperties !== undefined && s.additionalProperties !== false;
const isInlineObject = (s) => s.type === "object" && s.properties;

function enumNote(s) {
  const values = s.enum ?? (s.items && s.items.enum);
  if (!values) return "";
  return `Valores posibles: ${values.map((v) => `<code>${jd(String(v))}</code>`).join(", ")}.`;
}

function write(name, body) {
  writeFileSync(join(OUT, `${name}.java`), `${HEADER}\npackage ${PKG};\n\n${body}`);
}

function imports(code, extra = []) {
  const candidates = {
    "ar.kuida.KuidaObject": /\bKuidaObject\b/,
    "ar.kuida.KuidaParams": /\bKuidaParams\b/,
    "ar.kuida.JsonEncodable": /\bJsonEncodable\b/,
    "com.google.gson.annotations.SerializedName": /@SerializedName/,
    "java.time.LocalDate": /\bLocalDate\b/,
    "java.time.OffsetDateTime": /\bOffsetDateTime\b/,
    "java.util.Arrays": /\bArrays\./,
    "java.util.List": /\bList</,
    "java.util.Map": /\bMap</,
  };
  const out = Object.entries(candidates).filter(([, re]) => re.test(code)).map(([k]) => k).concat(extra);
  return [...new Set(out)].sort().map((i) => `import ${i};`).join("\n") + "\n\n";
}

// ─── clasificación de esquemas: entrada o salida ─────────────────────────────

const inputSchemas = new Set();
const outputSchemas = new Set();
function collect(schema, set) {
  if (!schema || typeof schema !== "object") return;
  if (schema.$ref) {
    const n = refName(schema.$ref);
    if (set.has(n)) return;
    set.add(n);
    collect(schemas[n], set);
    return;
  }
  for (const v of Object.values(schema)) if (v && typeof v === "object") collect(v, set);
}
const listOps = [];
for (const [path, ops] of Object.entries(SPEC.paths)) {
  for (const [method, op] of Object.entries(ops)) {
    if (!op["x-kuida-method"]) continue;
    collect(op.requestBody?.content?.["application/json"]?.schema, inputSchemas);
    collect(op.responses?.["200"]?.content?.["application/json"]?.schema, outputSchemas);
    if (op["x-kuida-method"] === "list") listOps.push({ path, method, op });
  }
}
outputSchemas.add("WebhookEvent"); // lo produce Webhook.constructEvent, no una operación
const SKIP = new Set(["Error", "EventBatchInput"]);

// ─── modelos de respuesta ────────────────────────────────────────────────────

function outputType(s, ctx) {
  const { schema } = unwrapNullable(s);
  if (schema.$ref) return refName(schema.$ref);
  if (schema.anyOf || schema.oneOf) return "Object";
  if (schema.type === "string") return schema.format === "date-time" ? "OffsetDateTime" : "String";
  if (schema.type === "integer") return "Long";
  if (schema.type === "number") return "Double";
  if (schema.type === "boolean") return "Boolean";
  if (schema.type === "array") return `List<${outputType(schema.items ?? {}, ctx)}>`;
  if (isFreeMap(schema)) return "Map<String, Object>";
  if (isInlineObject(schema)) {
    const cls = pascal(ctx.prop);
    ctx.nested.push(outputClass(cls, schema, true));
    return cls;
  }
  if (schema.type === "object") return "Map<String, Object>";
  return "Object";
}

function outputClass(name, schema, nested) {
  const ctx = { nested: [] };
  const fields = [];
  const getters = [];
  for (const [prop, raw] of Object.entries(schema.properties ?? {})) {
    ctx.prop = prop;
    const { schema: s, nullable } = unwrapNullable(raw);
    const type = outputType(raw, ctx);
    const f = ident(prop);
    fields.push(`  @SerializedName("${prop}")\n  private ${type} ${f};\n`);
    const doc = [jd(raw.description ?? s.description) || `Campo <code>${prop}</code>.`, s.const !== undefined ? `Siempre <code>${jd(String(s.const))}</code>.` : "", enumNote(s), nullable ? "Puede ser <code>null</code>." : ""];
    getters.push(`${javadoc(doc, "  ")}  public ${type} get${cap(prop)}() {\n    return ${f};\n  }\n`);
  }
  const nestedCode = ctx.nested.map((c) => "\n" + c.split("\n").map((l) => (l ? "  " + l : l)).join("\n")).join("");
  const doc = javadoc([jd(schema.description) || (nested ? `Objeto anidado <code>${name}</code>.` : `Objeto <code>${name}</code> de la API de Kuida.`)], "");
  const ctor = `  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */\n  public ${name}() {}\n`;
  return `${doc}public ${nested ? "static " : ""}class ${name} extends KuidaObject {\n${fields.join("\n")}\n${ctor}\n${getters.join("\n")}${nestedCode}}\n`;
}

// ─── parámetros de entrada ───────────────────────────────────────────────────

const REFERENCE_UNIONS = new Set(Object.keys(schemas).filter((n) => n.endsWith("Reference") && schemas[n].anyOf));

/** Métodos de builder para una propiedad de entrada. */
function inputSetters(prop, raw, indent, ctx, builderName) {
  const { schema: s } = unwrapNullable(raw);
  const doc = (extra = []) => javadoc([jd(raw.description ?? s.description) || `Asigna <code>${prop}</code>.`, enumNote(s), ...extra], indent);
  const m = ident(prop);
  const set = (type, value = m) => `${indent}public ${builderName} ${m}(${type} ${m}) {\n${indent}  return set("${prop}", ${value});\n${indent}}\n`;
  if (s.$ref) {
    const n = refName(s.$ref);
    if (REFERENCE_UNIONS.has(n)) {
      const identity = refName(schemas[n].anyOf.find((x) => x.$ref).$ref);
      return [
        doc() + set(n),
        doc([`Atajo para <code>${n}.id(${m})</code>.`]) + `${indent}public ${builderName} ${m}(String ${m}Id) {\n${indent}  return set("${prop}", ${n}.id(${m}Id));\n${indent}}\n`,
        doc([`Atajo para <code>${n}.identity(${m})</code>.`]) + `${indent}public ${builderName} ${m}(${identity} ${m}) {\n${indent}  return set("${prop}", ${n}.identity(${m}));\n${indent}}\n`,
      ];
    }
    return [doc() + set(n)];
  }
  if (s.type === "string" && s.format === "date-time") {
    return [doc() + set("OffsetDateTime"), doc(["Texto ISO 8601 con zona."]) + set("String")];
  }
  if (s.type === "string" && s.format === "date") {
    return [doc() + set("LocalDate", `${m} == null ? null : ${m}.toString()`), doc(["Texto AAAA-MM-DD."]) + set("String")];
  }
  if (s.type === "string") return [doc() + set("String")];
  if (s.type === "integer") return [doc() + set("Integer")];
  if (s.type === "number") return [doc() + set("Number")];
  if (s.type === "boolean") return [doc() + set("Boolean")];
  if (isFreeMap(s)) return [doc() + set("Map<String, Object>")];
  if (isInlineObject(s)) {
    const cls = pascal(prop);
    ctx.nested.push(paramsClass(cls, s, true));
    return [doc() + set(cls)];
  }
  if (s.type === "array") {
    const items = unwrapNullable(s.items ?? {}).schema;
    let itemType = "Object";
    if (items.$ref) itemType = refName(items.$ref);
    else if (items.type === "string") itemType = "String";
    else if (isInlineObject(items)) {
      itemType = pascal(singular(prop));
      ctx.nested.push(paramsClass(itemType, items, true));
    }
    const one = singular(prop);
    const out = [
      doc() + set(`List<${itemType}>`),
      javadoc([`Agrega un elemento a <code>${prop}</code>.`], indent) + `${indent}public ${builderName} add${cap(one)}(${itemType} ${ident(one)}) {\n${indent}  return add("${prop}", ${ident(one)});\n${indent}}\n`,
    ];
    if (itemType === "String") out.push(doc() + `${indent}public ${builderName} ${m}(String... ${m}) {\n${indent}  return set("${prop}", Arrays.asList(${m}));\n${indent}}\n`);
    return out;
  }
  return [doc() + set("Object")];
}

function paramsClass(name, schema, nested, extraDoc = [], props = schema.properties ?? {}) {
  const ctx = { nested: [] };
  const setters = [];
  for (const [prop, raw] of Object.entries(props)) setters.push(...inputSetters(prop, raw, "    ", ctx, "Builder"));
  const required = schema.required?.length ? `Obligatorios: ${schema.required.map((r) => `<code>${r}</code>`).join(", ")}.` : "";
  const cls = `${javadoc([jd(schema.description ?? `Parámetros <code>${name}</code>.`), required, ...extraDoc], "")}public ${nested ? "static " : ""}final class ${name} extends KuidaParams {
  private ${name}(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link ${name}}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, ${name}> {
    Builder() {}

${setters.join("\n")}
    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public ${name} build() {
      return new ${name}(values());
    }
  }
${ctx.nested.map((c) => "\n" + c.split("\n").map((l) => (l ? "  " + l : l)).join("\n")).join("")}}
`;
  return cls;
}

// ─── uniones id-o-identidad ──────────────────────────────────────────────────

function referenceClass(name) {
  const s = schemas[name];
  const idSchema = s.anyOf.find((x) => x.type === "string");
  const identity = refName(s.anyOf.find((x) => x.$ref).$ref);
  const prefix = (idSchema.pattern ?? "").replace(/^\^/, "").replace(/\.\*$/, "");
  return `${javadoc([jd(s.description ?? "Referencia a un objeto: su id o los datos para identificarlo."), `Se arma con {@link #id(String)} (<code>${prefix}…</code>) o con {@link #identity(${identity})}.`], "")}public final class ${name} implements JsonEncodable {
  private final String id;
  private final ${identity} identity;

  private ${name}(String id, ${identity} identity) {
    this.id = id;
    this.identity = identity;
  }

  /** Referencia por id de Kuida (<code>${prefix}…</code>). */
  public static ${name} id(String id) {
    if (id == null) throw new IllegalArgumentException("id no puede ser null");
    return new ${name}(id, null);
  }

  /** Referencia por identidad: Kuida busca el objeto con estos datos o lo crea. */
  public static ${name} identity(${identity} identity) {
    if (identity == null) throw new IllegalArgumentException("identity no puede ser null");
    return new ${name}(null, identity);
  }

  /** El id, o <code>null</code> si la referencia es por identidad. */
  public String getId() {
    return id;
  }

  /** La identidad, o <code>null</code> si la referencia es por id. */
  public ${identity} getIdentity() {
    return identity;
  }

  /** <code>true</code> si la referencia es por id. */
  public boolean isId() {
    return id != null;
  }

  @Override
  public Object toJsonValue() {
    return id != null ? id : identity;
  }

  @Override
  public String toString() {
    return id != null ? id : String.valueOf(identity);
  }
}
`;
}

// ─── sobre de eventos ────────────────────────────────────────────────────────

function eventInputClass() {
  const s = schemas.EventInput;
  const variants = s.oneOf;
  const types = variants.map((v) => v.properties.type.const);
  const base = variants[0];
  const props = {};
  for (const [k, v] of Object.entries(base.properties)) {
    if (k === "type") props.type = { type: "string", description: "Tipo de evento.", enum: types };
    else if (k === "data") props.data = { type: "object", additionalProperties: {}, description: "Los datos del evento. Viajan tal cual, sin conversión de claves." };
    else if (k === "schemaVersion") props.schemaVersion = { type: "integer", description: v.description };
    else props[k] = v;
  }
  const consts = types.map((t) => `  /** Tipo <code>${t}</code>. */\n  public static final String TYPE_${t.replace(/\./g, "_").toUpperCase()} = "${t}";\n`).join("");
  let code = paramsClass("EventInput", { description: "Un evento con el sobre común de Kuida: `id`, `type`, `data` y opcionales.", required: base.required }, false, ["Los esquemas de <code>data</code> por tipo están en la referencia de la API."], props);
  code = code.replace("public final class EventInput extends KuidaParams {\n", `public final class EventInput extends KuidaParams {\n${consts}\n`);
  code = code.replace(
    "    /** Arma los parámetros.",
    `    /** Agrega una clave a <code>data</code>. */\n    public Builder putData(String key, Object value) {\n      return putInMap("data", key, value);\n    }\n\n    /** Arma los parámetros.`,
  );
  return code;
}

// ─── parámetros de listado ───────────────────────────────────────────────────

function listParamsClass({ path, op }) {
  const item = refName(op.responses["200"].content["application/json"].schema.$ref).replace(/List$/, "");
  const name = `${item}ListParams`;
  const props = {};
  for (const p of op.parameters ?? []) {
    if (p.$ref || p.in !== "query") continue;
    const sch = p.schema ?? {};
    // Filtros booleanos: el OpenAPI los declara como texto "true"/"false"; el SDK acepta Boolean y lo manda así.
    const isBoolText = sch.type === "string" && Array.isArray(sch.enum) && sch.enum.length === 2 && sch.enum.includes("true") && sch.enum.includes("false");
    props[p.name] = isBoolText
      ? { type: "boolean", description: p.description ?? sch.description }
      : { ...sch, description: p.description ?? sch.description };
  }
  return { name, code: paramsClass(name, { description: `Parámetros de \`GET ${path}\`: ${op.summary.toLowerCase()}. Viajan como query string.` }, false, [], props) };
}

// ─── salida ──────────────────────────────────────────────────────────────────

mkdirSync(OUT, { recursive: true });
for (const f of readdirSync(OUT)) {
  if (f.endsWith(".java") && readFileSync(join(OUT, f), "utf8").startsWith(HEADER)) unlinkSync(join(OUT, f));
}

const written = [];
for (const name of Object.keys(schemas).sort()) {
  if (SKIP.has(name) || /List$/.test(name)) continue;
  let code;
  if (REFERENCE_UNIONS.has(name)) code = referenceClass(name);
  else if (name === "EventInput") code = eventInputClass();
  else if (inputSchemas.has(name)) code = paramsClass(name, schemas[name], false);
  else if (outputSchemas.has(name)) code = outputClass(name, schemas[name], false);
  else continue;
  write(name, imports(code) + code);
  written.push(name);
}
for (const op of listOps) {
  const { name, code } = listParamsClass(op);
  write(name, imports(code) + code);
  written.push(name);
}
console.log(`generate: ${written.length} clases en src/main/java/ar/kuida/model`);
