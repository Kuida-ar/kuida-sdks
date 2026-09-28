// Command generate lee la especificación OpenAPI de Kuida
// (../openapi/kuida-v1.json) y escribe models_gen.go con los modelos,
// los tipos enumerados, los parámetros de listado y las constantes de error.
//
// Se ejecuta desde la raíz del módulo con:
//
//	go generate ./...
//
// o directamente:
//
//	go run ./internal/cmd/generate -spec ../openapi/kuida-v1.json -out models_gen.go
//
// Los métodos de recurso no se generan: se escriben a mano en services.go.
package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"go/format"
	"log"
	"os"
	"reflect"
	"sort"
	"strings"
	"unicode"
)

type schema = map[string]any

// typeOverrides fija el nombre Go de tipos inline o enumerados cuando el
// nombre por defecto (Esquema + Propiedad) choca o se lee mal.
var typeOverrides = map[string]string{
	"Event.result":                                "ResultRef",
	"EventResult.result":                          "ResultRef",
	"IntakeRequestCreateParams.sender":            "IntakeSender",
	"Account.serviceLines[]":                      "ServiceLine",
	"WebhookEvent.type":                           "WebhookEventType",
	"WebhookEndpoint.enabledEvents[]":             "WebhookEventType",
	"WebhookEndpointCreateParams.enabledEvents[]": "WebhookEventType",
	"WebhookEndpointUpdateParams.enabledEvents[]": "WebhookEventType",
	"Error.type":                                  "ErrorType",
	"Error.code":                                  "ErrorCode",
}

// handwritten son los esquemas que no se generan: uniones que el SDK modela
// a mano (references.go), el sobre de error (errors.go) y la entrada de lotes.
var handwritten = map[string]string{
	"PatientReference": "*PatientReference",
	"DoctorReference":  "*DoctorReference",
	"Error":            "",
	"EventBatchInput":  "",
}

// voseo pasa las descripciones del OpenAPI (escritas en español rioplatense)
// a español neutro para la documentación del SDK.
var voseo = []struct{ from, to string }{
	{"Seguí con `startingAfter` = id del último.", "Para la página siguiente, usar `startingAfter` con el id del último."},
	{"reintentá con el mismo id", "se puede reintentar con el mismo id"},
	{"el `id` que mandaste", "el `id` enviado"},
	{"El id que mandaste", "El id enviado"},
	{"Respetá", "Respetar"},
	{"Reintentá", "Reintentar"},
	{"Usá ", "Usar "},
	{"usá ", "usar "},
	{"Mandá", "Enviar"},
	{"mandá", "enviar"},
	{"Pasá", "Pasar"},
	{"pasá", "pasar"},
	{"Conectá", "Conecte"},
	{"Guardalo", "Guardarlo"},
	{"guardalo", "guardarlo"},
	{"podés", "se puede"},
	{"Podés", "Se puede"},
	{"tenés", "hay"},
	{"querés", "se quiere"},
	{"deduplicá por acá", "sirve para deduplicar"},
	{"Default: ", "Por defecto: "},
	{"Default ", "Por defecto: "},
}

func main() {
	specPath := flag.String("spec", "../openapi/kuida-v1.json", "ruta a la especificación OpenAPI")
	outPath := flag.String("out", "models_gen.go", "archivo de salida")
	flag.Parse()

	raw, err := os.ReadFile(*specPath)
	if err != nil {
		log.Fatalf("leyendo la especificación: %v", err)
	}
	dec := json.NewDecoder(bytes.NewReader(raw))
	dec.UseNumber()
	v, err := decodeOrdered(dec)
	if err != nil {
		log.Fatalf("parseando la especificación: %v", err)
	}
	spec := v.(schema)
	g := newGen(spec)
	src := g.run()
	formatted, err := format.Source(src)
	if err != nil {
		os.WriteFile(*outPath+".broken", src, 0o644)
		log.Fatalf("formateando la salida: %v", err)
	}
	if err := os.WriteFile(*outPath, formatted, 0o644); err != nil {
		log.Fatalf("escribiendo %s: %v", *outPath, err)
	}
}

type enumDef struct {
	name   string
	doc    string
	values []string
}

type fieldDef struct {
	goName, jsonName, typ, doc string
	omitempty                  bool
	requiredTime               bool
}

type structDef struct {
	name, doc string
	fields    []fieldDef
	output    bool
	component bool
}

type listParamField struct {
	goName, wire, kind, typ, doc string // kind: string, bool, enum, time
}

type listParamsDef struct {
	name, resource string
	fields         []listParamField
}

type gen struct {
	spec       schema
	schemas    map[string]schema
	enums      map[string]*enumDef
	enumBySig  map[string]string
	enumOrder  []string
	structs    []*structDef
	structSeen map[string]bool
	lists      []string // nombres de modelo con alias XList
	listParams []*listParamsDef
}

func newGen(spec schema) *gen {
	comps := spec["components"].(schema)["schemas"].(schema)
	schemas := map[string]schema{}
	for k, v := range comps {
		schemas[k] = v.(schema)
	}
	return &gen{
		spec:       spec,
		schemas:    schemas,
		enums:      map[string]*enumDef{},
		enumBySig:  map[string]string{},
		structSeen: map[string]bool{},
	}
}

func isInput(name string) bool {
	for _, suf := range []string{"Params", "Input", "Identity", "Contact", "Coverage"} {
		if strings.HasSuffix(name, suf) {
			return true
		}
	}
	return false
}

func (g *gen) run() []byte {
	names := make([]string, 0, len(g.schemas))
	for n := range g.schemas {
		names = append(names, n)
	}
	sort.Strings(names)

	// 1) Constantes de error, del sobre Error.
	errObj := g.schemas["Error"]["properties"].(schema)["error"].(schema)["properties"].(schema)
	g.enum("Error.type", enumValues(errObj["type"].(schema)), "ErrorType es el tipo de error que devuelve la API en `error.type`.")
	g.enum("Error.code", enumValues(errObj["code"].(schema)), "ErrorCode es el código de error que devuelve la API en `error.code`.")

	// 2) Modelos de salida primero (fijan los nombres de los enumerados), después los de entrada.
	for _, pass := range []bool{false, true} {
		for _, n := range names {
			if _, skip := handwritten[n]; skip || isInput(n) != pass {
				continue
			}
			s := g.schemas[n]
			if g.isList(s) {
				g.lists = append(g.lists, n)
				continue
			}
			if one, ok := s["oneOf"].([]any); ok {
				s = mergeOneOf(s, one)
			}
			g.object(n, s, pass, true)
		}
	}

	// 3) Parámetros de listado, de los query params de cada operación `list`.
	g.collectListParams()

	return g.emit()
}

func (g *gen) isList(s schema) bool {
	p, ok := s["properties"].(schema)
	if !ok {
		return false
	}
	obj, _ := p["object"].(schema)
	return obj != nil && obj["const"] == "list"
}

// mergeOneOf reduce una unión discriminada a un sobre genérico: las
// propiedades comunes a todas las variantes; si difieren, `type` pasa a
// enumerado con las constantes y el resto a mapa libre.
func mergeOneOf(parent schema, variants []any) schema {
	first := variants[0].(schema)
	props := first["properties"].(schema)
	merged := schema{}
	order := orders[mapKey(props)]
	for _, k := range order {
		same, allConst := true, true
		var consts []string
		ref, _ := json.Marshal(props[k])
		present := true
		for _, v := range variants {
			vp, ok := v.(schema)["properties"].(schema)[k].(schema)
			if !ok {
				present = false
				break
			}
			b, _ := json.Marshal(vp)
			if !bytes.Equal(b, ref) {
				same = false
			}
			if c, ok := vp["const"].(string); ok {
				consts = append(consts, c)
			} else {
				allConst = false
			}
		}
		if !present {
			continue
		}
		switch {
		case same:
			merged[k] = props[k]
		case allConst:
			vals := make([]any, len(consts))
			for i, c := range consts {
				vals[i] = c
			}
			merged[k] = schema{"type": "string", "enum": vals}
		default:
			merged[k] = schema{"type": "object", "additionalProperties": schema{}, "description": "Datos del evento; su forma depende de `type` (ver la referencia de la API)."}
		}
	}
	orders[mapKey(merged)] = order
	out := schema{"type": "object", "properties": merged, "required": first["required"]}
	if d, ok := parent["description"]; ok {
		out["description"] = d
	}
	return out
}

func enumValues(s schema) []string {
	var out []string
	for _, v := range s["enum"].([]any) {
		out = append(out, v.(string))
	}
	return out
}

func (g *gen) enum(key string, values []string, doc string) string {
	if name, ok := typeOverrides[key]; ok {
		return g.addEnum(name, values, doc)
	}
	sorted := append([]string(nil), values...)
	sort.Strings(sorted)
	sig := strings.Join(sorted, "|")
	if name, ok := g.enumBySig[sig]; ok {
		return name
	}
	name := defaultName(key)
	g.enumBySig[sig] = name
	return g.addEnum(name, values, doc)
}

func (g *gen) addEnum(name string, values []string, doc string) string {
	e, ok := g.enums[name]
	if !ok {
		e = &enumDef{name: name, doc: doc}
		g.enums[name] = e
		g.enumOrder = append(g.enumOrder, name)
	}
	if e.doc == "" {
		e.doc = doc
	}
	for _, v := range values {
		if !contains(e.values, v) {
			e.values = append(e.values, v)
		}
	}
	sorted := append([]string(nil), e.values...)
	sort.Strings(sorted)
	g.enumBySig[strings.Join(sorted, "|")] = name
	return name
}

func contains(xs []string, x string) bool {
	for _, v := range xs {
		if v == x {
			return true
		}
	}
	return false
}

func defaultName(key string) string {
	key = strings.TrimSuffix(key, "[]")
	parts := strings.Split(key, ".")
	var b strings.Builder
	for _, p := range parts {
		b.WriteString(goName(p))
	}
	return b.String()
}

// object registra un struct para el esquema s bajo el nombre name.
func (g *gen) object(name string, s schema, input, component bool) string {
	if g.structSeen[name] {
		return name
	}
	g.structSeen[name] = true
	sd := &structDef{name: name, doc: clean(str(s["description"])), output: !input, component: component}
	g.structs = append(g.structs, sd)

	props, _ := s["properties"].(schema)
	required := map[string]bool{}
	if r, ok := s["required"].([]any); ok {
		for _, x := range r {
			required[x.(string)] = true
		}
	}
	keys := make([]string, 0, len(props))
	for k := range props {
		keys = append(keys, k)
	}
	// Orden estable: el del esquema no se conserva en un map; se usa el de `required` y después alfabético.
	sort.SliceStable(keys, func(i, j int) bool { return propOrder(s, keys[i]) < propOrder(s, keys[j]) })

	for _, k := range keys {
		ps := props[k].(schema)
		typ, nullable, kind := g.goType(name+"."+k, ps, input)
		f := fieldDef{goName: goName(k), jsonName: k, doc: clean(str(ps["description"]))}
		if f.doc == "" {
			f.doc = clean(str(firstNonNullDesc(ps)))
		}
		if input {
			f.omitempty = true
			switch kind {
			case "scalar":
				if required[k] && (typ == "string") {
					f.typ = typ
				} else if required[k] && typ == "time.Time" {
					f.typ = typ
					f.requiredTime = true
				} else {
					f.typ = "*" + typ
				}
			case "enum", "slice", "map", "ptr":
				f.typ = typ
			}
		} else {
			f.typ = typ
			if nullable && kind != "ptr" && kind != "slice" && kind != "map" {
				f.typ = "*" + typ
			}
			f.omitempty = !required[k]
		}
		sd.fields = append(sd.fields, f)
	}
	return name
}

// orders guarda el orden de las claves de cada objeto JSON tal como aparece
// en el archivo, para que los campos generados sigan el orden del OpenAPI.
var orders = map[uintptr][]string{}

func mapKey(m schema) uintptr { return reflect.ValueOf(m).Pointer() }

// propOrder devuelve la posición de la propiedad k en la declaración del esquema.
func propOrder(s schema, k string) int {
	props, _ := s["properties"].(schema)
	for i, x := range orders[mapKey(props)] {
		if x == k {
			return i
		}
	}
	return 1 << 20
}

// decodeOrdered decodifica JSON en maps y slices y registra el orden de las claves.
func decodeOrdered(dec *json.Decoder) (any, error) {
	tok, err := dec.Token()
	if err != nil {
		return nil, err
	}
	switch t := tok.(type) {
	case json.Delim:
		switch t {
		case '{':
			m := schema{}
			var keys []string
			for dec.More() {
				kt, err := dec.Token()
				if err != nil {
					return nil, err
				}
				k := kt.(string)
				v, err := decodeOrdered(dec)
				if err != nil {
					return nil, err
				}
				m[k] = v
				keys = append(keys, k)
			}
			if _, err := dec.Token(); err != nil {
				return nil, err
			}
			orders[mapKey(m)] = keys
			return m, nil
		case '[':
			var arr []any
			for dec.More() {
				v, err := decodeOrdered(dec)
				if err != nil {
					return nil, err
				}
				arr = append(arr, v)
			}
			if _, err := dec.Token(); err != nil {
				return nil, err
			}
			return arr, nil
		}
	case json.Number:
		f, err := t.Float64()
		return f, err
	}
	return tok, nil
}

func firstNonNullDesc(s schema) any {
	if any, ok := s["anyOf"].([]any); ok {
		for _, a := range any {
			if d, ok := a.(schema)["description"]; ok {
				return d
			}
		}
	}
	return nil
}

// goType devuelve el tipo Go, si el valor admite null y la clase de tipo:
// scalar (string, int, bool, time), enum, slice, map o ptr (struct).
func (g *gen) goType(key string, s schema, input bool) (string, bool, string) {
	if ref, ok := s["$ref"].(string); ok {
		n := strings.TrimPrefix(ref, "#/components/schemas/")
		if t, ok := handwritten[n]; ok && t != "" {
			return t, false, "ptr"
		}
		target := g.schemas[n]
		if _, ok := target["enum"]; ok {
			return g.enum(n, enumValues(target), clean(str(target["description"]))), false, "enum"
		}
		return "*" + g.object(n, target, isInput(n), true), false, "ptr"
	}
	if alts, ok := s["anyOf"].([]any); ok {
		var nonNull []schema
		nullable := false
		for _, a := range alts {
			as := a.(schema)
			if as["type"] == "null" {
				nullable = true
			} else {
				nonNull = append(nonNull, as)
			}
		}
		if len(nonNull) == 1 {
			t, _, kind := g.goType(key, nonNull[0], input)
			return t, nullable, kind
		}
		return "any", nullable, "map"
	}
	if c, ok := s["const"]; ok {
		if _, isNum := c.(float64); isNum {
			return "int", false, "scalar"
		}
	}
	if name, ok := typeOverrides[key]; ok && s["type"] == "string" && s["enum"] == nil {
		// Un campo de texto libre que en otro lugar tiene enumerado: se usa el mismo tipo.
		return g.addEnum(name, nil, ""), false, "enum"
	}
	switch s["type"] {
	case "string":
		if _, ok := s["enum"]; ok {
			return g.enum(key, enumValues(s), clean(str(s["description"]))), false, "enum"
		}
		if s["format"] == "date-time" {
			return "time.Time", false, "scalar"
		}
		return "string", false, "scalar"
	case "integer":
		return "int64", false, "scalar"
	case "number":
		return "float64", false, "scalar"
	case "boolean":
		return "bool", false, "scalar"
	case "array":
		items := s["items"].(schema)
		t, _, kind := g.goType(key+"[]", items, input)
		if kind == "scalar" || kind == "enum" || kind == "ptr" || kind == "map" {
			return "[]" + t, false, "slice"
		}
		return "[]" + t, false, "slice"
	case "object":
		if props, ok := s["properties"].(schema); ok && len(props) > 0 {
			name := typeOverrides[key]
			if name == "" {
				name = defaultName(key)
			}
			return "*" + g.object(name, s, input, false), false, "ptr"
		}
		return "map[string]any", false, "map"
	}
	return "any", false, "map"
}

func (g *gen) collectListParams() {
	paths := g.spec["paths"].(schema)
	var pathKeys []string
	for p := range paths {
		pathKeys = append(pathKeys, p)
	}
	sort.Strings(pathKeys)
	for _, p := range pathKeys {
		for _, opAny := range paths[p].(schema) {
			op, ok := opAny.(schema)
			if !ok || op["x-kuida-method"] != "list" {
				continue
			}
			resp := op["responses"].(schema)["200"].(schema)["content"].(schema)["application/json"].(schema)["schema"].(schema)
			listName := strings.TrimPrefix(resp["$ref"].(string), "#/components/schemas/")
			model := strings.TrimSuffix(listName, "List")
			lp := &listParamsDef{name: model + "ListParams", resource: model}
			for _, prmAny := range op["parameters"].([]any) {
				prm := prmAny.(schema)
				if _, ok := prm["$ref"]; ok || prm["in"] != "query" {
					continue
				}
				wire := prm["name"].(string)
				if wire == "limit" || wire == "startingAfter" || wire == "endingBefore" {
					continue
				}
				ps := prm["schema"].(schema)
				f := listParamField{goName: goName(wire), wire: wire, doc: clean(str(prm["description"]))}
				switch {
				case ps["enum"] != nil && isBoolEnum(enumValues(ps)):
					f.kind, f.typ = "bool", "*bool"
				case ps["enum"] != nil:
					f.kind, f.typ = "enum", g.enum(lp.name+"."+wire, enumValues(ps), "")
				case ps["format"] == "date-time":
					f.kind, f.typ = "time", "*time.Time"
				default:
					f.kind, f.typ = "string", "*string"
				}
				lp.fields = append(lp.fields, f)
			}
			g.listParams = append(g.listParams, lp)
		}
	}
	sort.Slice(g.listParams, func(i, j int) bool { return g.listParams[i].name < g.listParams[j].name })
}

func isBoolEnum(v []string) bool {
	return len(v) == 2 && contains(v, "true") && contains(v, "false")
}

// ─── salida ──────────────────────────────────────────────────────────────────

func (g *gen) emit() []byte {
	var b bytes.Buffer
	w := func(format string, args ...any) { fmt.Fprintf(&b, format, args...) }
	info := g.spec["info"].(schema)

	w("// Código generado por internal/cmd/generate a partir de openapi/kuida-v1.json. NO EDITAR.\n\n")
	w("package kuida\n\n")
	w("import (\n\t\"encoding/json\"\n\t\"net/url\"\n\t\"time\"\n)\n\n")
	w("// APIVersion es la versión de la API de Kuida contra la que se generó este SDK.\n")
	w("// Se manda en el header Kuida-Version de cada pedido.\n")
	w("const APIVersion = %q\n\n", info["version"])

	// Enumerados.
	for _, n := range g.enumOrder {
		e := g.enums[n]
		doc := e.doc
		if doc == "" {
			doc = "valores posibles."
		}
		w("%s", comment(n+" enumera "+lowerFirst(strings.TrimPrefix(doc, n+" es ")), ""))
		w("type %s string\n\n", n)
		w("// Valores de %s.\nconst (\n", n)
		for _, v := range e.values {
			suffix := constSuffix(v)
			if n == "ErrorType" {
				// invalid_request_error → ErrorTypeInvalidRequest (la tabla de SDK_DESIGN.md §7).
				suffix = constSuffix(strings.TrimSuffix(v, "_error"))
			}
			w("\t%s%s %s = %q\n", n, suffix, n, v)
		}
		w(")\n\n")
	}

	// Structs.
	for _, sd := range g.structs {
		doc := sd.doc
		if doc == "" {
			if sd.output {
				doc = "objeto de la API."
			} else {
				doc = "parámetros de entrada."
			}
		}
		w("%s", comment(sd.name+": "+doc, ""))
		w("type %s struct {\n", sd.name)
		for _, f := range sd.fields {
			if f.doc != "" {
				w("%s", comment(f.goName+": "+f.doc, "\t"))
			}
			tag := f.jsonName
			if f.omitempty {
				tag += ",omitempty"
			}
			if f.requiredTime {
				tag = f.jsonName + ",omitempty"
			}
			w("\t%s %s `json:%q`\n", f.goName, f.typ, tag)
		}
		if sd.output {
			if sd.component {
				w("\n\t// LastResponse tiene los datos HTTP del pedido que devolvió este objeto\n")
				w("\t// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.\n")
				w("\tLastResponse *APIResponse `json:\"-\"`\n")
			}
			w("\n\traw json.RawMessage\n")
		}
		w("}\n\n")

		if sd.output {
			w("// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.\n")
			w("func (m *%s) UnmarshalJSON(data []byte) error {\n", sd.name)
			w("\ttype alias %s\n\tvar a alias\n", sd.name)
			w("\tif err := json.Unmarshal(data, &a); err != nil {\n\t\treturn err\n\t}\n")
			w("\t*m = %s(a)\n\tm.raw = append(json.RawMessage(nil), data...)\n\treturn nil\n}\n\n", sd.name)
			w("// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos\n// que este SDK todavía no conoce.\n")
			w("func (m *%s) Raw() json.RawMessage { return m.raw }\n\n", sd.name)
			if sd.component {
				w("func (m *%s) setLastResponse(r *APIResponse) { m.LastResponse = r }\n\n", sd.name)
			}
		}

		var times []fieldDef
		for _, f := range sd.fields {
			if f.requiredTime {
				times = append(times, f)
			}
		}
		if len(times) > 0 {
			w("// MarshalJSON serializa los parámetros y omite las fechas obligatorias sin\n// completar, para que la API responda parameter_missing en lugar de recibir el año 1.\n")
			w("func (p %s) MarshalJSON() ([]byte, error) {\n", sd.name)
			w("\ttype alias %s\n", sd.name)
			w("\treturn json.Marshal(struct {\n\t\talias\n")
			for _, f := range times {
				w("\t\t%s *time.Time `json:\"%s,omitempty\"`\n", f.goName, f.jsonName)
			}
			w("\t}{alias(p)")
			for _, f := range times {
				w(", timeOrNil(p.%s)", f.goName)
			}
			w("})\n}\n\n")
		}
	}

	// Alias de listas.
	for _, l := range g.lists {
		model := strings.TrimSuffix(l, "List")
		w("// %s es una página de objetos %s.\n", l, model)
		w("type %s = ListPage[*%s]\n\n", l, model)
	}

	// Parámetros de listado.
	for _, lp := range g.listParams {
		w("// %s son los filtros de %s.List. Los campos en nil no se envían.\n", lp.name, lp.resource)
		w("type %s struct {\n\tListParams\n", lp.name)
		for _, f := range lp.fields {
			if f.doc != "" {
				w("%s", comment(f.goName+": "+f.doc, "\t"))
			}
			w("\t%s %s\n", f.goName, f.typ)
		}
		w("}\n\n")
		w("func (p *%s) query() url.Values {\n", lp.name)
		w("\tif p == nil {\n\t\treturn url.Values{}\n\t}\n")
		w("\tq := p.ListParams.query()\n")
		for _, f := range lp.fields {
			switch f.kind {
			case "string":
				w("\tif p.%s != nil {\n\t\tq.Set(%q, *p.%s)\n\t}\n", f.goName, f.wire, f.goName)
			case "bool":
				w("\tif p.%s != nil {\n\t\tq.Set(%q, boolString(*p.%s))\n\t}\n", f.goName, f.wire, f.goName)
			case "enum":
				w("\tif p.%s != \"\" {\n\t\tq.Set(%q, string(p.%s))\n\t}\n", f.goName, f.wire, f.goName)
			case "time":
				w("\tif p.%s != nil {\n\t\tq.Set(%q, p.%s.Format(time.RFC3339Nano))\n\t}\n", f.goName, f.wire, f.goName)
			}
		}
		w("\treturn q\n}\n\n")
	}
	return b.Bytes()
}

// ─── utilidades ──────────────────────────────────────────────────────────────

var initialisms = map[string]string{"id": "ID", "url": "URL", "api": "API", "dni": "DNI", "http": "HTTP", "json": "JSON", "pms": "PMS"}

// goName pasa un nombre camelCase del cable a un identificador Go exportado,
// con las siglas en mayúscula (externalId → ExternalID, apiKey → APIKey).
func goName(s string) string {
	var words []string
	var cur []rune
	for _, r := range s {
		if r == '_' || r == '.' || r == '-' {
			if len(cur) > 0 {
				words = append(words, string(cur))
				cur = nil
			}
			continue
		}
		if unicode.IsUpper(r) && len(cur) > 0 {
			words = append(words, string(cur))
			cur = nil
		}
		cur = append(cur, r)
	}
	if len(cur) > 0 {
		words = append(words, string(cur))
	}
	var b strings.Builder
	for _, w := range words {
		if up, ok := initialisms[strings.ToLower(w)]; ok {
			b.WriteString(up)
			continue
		}
		b.WriteString(strings.ToUpper(w[:1]) + w[1:])
	}
	return b.String()
}

func constSuffix(v string) string {
	if v == "*" {
		return "All"
	}
	return goName(strings.ToLower(v))
}

func str(v any) string {
	s, _ := v.(string)
	return s
}

func clean(s string) string {
	for _, r := range voseo {
		s = strings.ReplaceAll(s, r.from, r.to)
	}
	return strings.TrimSpace(s)
}

func lowerFirst(s string) string {
	if s == "" {
		return s
	}
	r := []rune(s)
	r[0] = unicode.ToLower(r[0])
	return string(r)
}

func comment(text, indent string) string {
	text = strings.ReplaceAll(text, "\n", " ")
	var b strings.Builder
	line := indent + "//"
	for _, word := range strings.Fields(text) {
		if len(line)+1+len(word) > 90 && line != indent+"//" {
			b.WriteString(line + "\n")
			line = indent + "//"
		}
		line += " " + word
	}
	b.WriteString(line + "\n")
	return b.String()
}
