#!/usr/bin/env python3
"""Genera ``src/kuida/_generated/models.py`` a partir de ``../openapi/kuida-v1.json``.

Uso (desde la carpeta ``python/``)::

    python3 scripts/generate.py

El script no tiene dependencias. Produce:

* una clase por esquema de respuesta (``Patient``, ``Appointment``, ``PatientList``…) con
  atributos ``snake_case`` anotados y el mapa ``_wire_fields`` que usa el runtime para
  convertir fechas, objetos anidados y listas;
* ``PARAM_SHAPES``: la forma de cada esquema de entrada, para convertir kwargs ``snake_case``
  a las claves camelCase del cable solo donde el esquema las conoce;
* ``OPERATIONS``: una entrada por operación (``x-kuida-resource`` + ``x-kuida-method``) con
  método HTTP, ruta, parámetros de query, esquema del cuerpo y modelo de respuesta.

Los métodos de los recursos se escriben a mano en ``src/kuida/_resources.py``.
"""

import json
import keyword
import re
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

ROOT = Path(__file__).resolve().parent.parent
SPEC_PATH = ROOT.parent / "openapi" / "kuida-v1.json"
OUT_PATH = ROOT / "src" / "kuida" / "_generated" / "models.py"

# Mapas libres: nunca se convierten sus claves (SDK_DESIGN.md §4).
FREE_MAP_PROPS = {"data", "payload"}

# Nombres para objetos anidados sin nombre propio en el OpenAPI (por defecto: Dueño + Propiedad).
INLINE_NAMES = {
    ("Event", "result"): "ResultReference",
    ("EventResult", "result"): "ResultReference",
}


def camel_to_snake(name: str) -> str:
    s = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()
    return s + "_" if keyword.iskeyword(s) else s


def pascal(name: str) -> str:
    return name[:1].upper() + name[1:]


class Generator:
    def __init__(self, spec: Dict[str, Any]) -> None:
        self.spec = spec
        self.schemas: Dict[str, Any] = spec["components"]["schemas"]
        self.model_order: List[str] = []
        self.models: Dict[str, Dict[str, Any]] = {}
        self.param_shapes: Dict[str, Any] = {}
        self.operations: Dict[str, Any] = {}
        self.inline_by_shape: Dict[str, str] = {}

    # ─── utilidades de esquema ───────────────────────────────────────────────

    def ref_name(self, ref: str) -> str:
        return ref.rsplit("/", 1)[-1]

    def resolve(self, schema: Dict[str, Any]) -> Dict[str, Any]:
        while "$ref" in schema:
            schema = self.schemas[self.ref_name(schema["$ref"])]
        return schema

    @staticmethod
    def strip_null(schema: Dict[str, Any]) -> Tuple[Dict[str, Any], bool]:
        """Devuelve (esquema sin la rama null, era_nullable)."""
        for key in ("anyOf", "oneOf"):
            if key in schema:
                branches = [b for b in schema[key] if b.get("type") != "null"]
                if len(branches) < len(schema[key]):
                    if len(branches) == 1:
                        return branches[0], True
                    return {key: branches}, True
        return schema, False

    @staticmethod
    def is_free_map(schema: Dict[str, Any]) -> bool:
        return schema.get("type") == "object" and not schema.get("properties")

    # ─── modelos de respuesta ────────────────────────────────────────────────

    def response_schema_names(self) -> List[str]:
        names = []
        for op in self.iter_operations():
            resp = op["op"]["responses"].get("200", {})
            schema = resp.get("content", {}).get("application/json", {}).get("schema")
            if schema and "$ref" in schema:
                names.append(self.ref_name(schema["$ref"]))
        # WebhookEvent no es respuesta de una operación pero lo devuelve Webhook.construct_event.
        names.append("WebhookEvent")
        seen = []
        for n in names:
            if n not in seen:
                seen.append(n)
        return seen

    def field_kind(self, schema: Dict[str, Any], owner: str, prop: str) -> Tuple[Any, str]:
        """Devuelve (kind, anotación de tipo) para una propiedad de respuesta."""
        schema, nullable = self.strip_null(schema)
        if "$ref" in schema:
            name = self.ref_name(schema["$ref"])
            self.add_model(name, self.schemas[name])
            kind, ann = ("model", name), name
        elif self.is_free_map(schema) or (prop in FREE_MAP_PROPS and schema.get("type") == "object"):
            kind, ann = "free", "Dict[str, Any]"
        elif schema.get("type") == "object":
            canon = json.dumps(schema, sort_keys=True)
            nested = self.inline_by_shape.get(canon) or INLINE_NAMES.get((owner, prop)) or owner + pascal(prop)
            if nested not in self.inline_by_shape.values() and (nested in self.schemas or nested in self.models):
                nested += "Object"
            self.inline_by_shape[canon] = nested
            self.add_model(nested, schema)
            kind, ann = ("model", nested), nested
        elif schema.get("type") == "array":
            item_kind, item_ann = self.field_kind(schema.get("items", {}), owner, prop)
            kind, ann = ("list", item_kind), "List[%s]" % item_ann
        elif schema.get("type") == "string" and schema.get("format") == "date-time":
            kind, ann = "dt", "datetime"
        else:
            kind = "s"
            ann = {"string": "str", "boolean": "bool", "integer": "int", "number": "float"}.get(
                schema.get("type", ""), "Any"
            )
        if nullable:
            ann = "Optional[%s]" % ann
        return kind, ann

    def add_model(self, name: str, schema: Dict[str, Any]) -> None:
        if name in self.models:
            return
        self.models[name] = {}  # reserva el nombre antes de recorrer (evita ciclos)
        props: Dict[str, Any] = dict(schema.get("properties", {}))
        # Algunas claves figuran en `required` sin estar en `properties` (inconsistencia del
        # OpenAPI, p. ej. WebhookEndpoint.description). Se agregan como campos sin tipo.
        for req in schema.get("required", []):
            if req not in props:
                props[req] = {}
        required = set(schema.get("required", []))
        fields = []
        for wire, pschema in props.items():
            kind, ann = self.field_kind(pschema, name, wire)
            if wire not in required and not ann.startswith("Optional["):
                ann = "Optional[%s]" % ann
            fields.append(
                {
                    "wire": wire,
                    "snake": camel_to_snake(wire),
                    "kind": kind,
                    "ann": ann,
                    "doc": (pschema.get("description") or "").strip(),
                }
            )
        obj_const = schema.get("properties", {}).get("object", {}).get("const")
        is_list = obj_const == "list"
        self.models[name] = {
            "doc": (schema.get("description") or "").strip(),
            "fields": fields,
            "object": obj_const,
            "is_list": is_list,
        }
        self.model_order.append(name)

    # ─── formas de parámetros ────────────────────────────────────────────────

    def param_shape(self, schema: Dict[str, Any], prop: Optional[str] = None) -> Any:
        schema, _ = self.strip_null(schema)
        if "$ref" in schema:
            name = self.ref_name(schema["$ref"])
            if name not in self.param_shapes:
                self.param_shapes[name] = None  # reserva
                self.param_shapes[name] = self.param_shape(self.schemas[name])
            return ("ref", name)
        if prop in FREE_MAP_PROPS and schema.get("type") == "object":
            return "free"
        if "oneOf" in schema and all(self.resolve(b).get("type") == "object" for b in schema["oneOf"]):
            # Unión discriminada (EventInput): se fusionan las propiedades de las ramas.
            merged: Dict[str, Any] = {}
            for branch in schema["oneOf"]:
                for k, v in self.resolve(branch).get("properties", {}).items():
                    merged.setdefault(k, v)
            return ("obj", {k: self.param_shape(v, k) for k, v in merged.items()})
        for key in ("anyOf", "oneOf"):
            if key in schema:
                return ("union", [self.param_shape(b) for b in schema[key]])
        if self.is_free_map(schema):
            return "free"
        t = schema.get("type")
        if t == "object":
            return ("obj", {k: self.param_shape(v, k) for k, v in schema.get("properties", {}).items()})
        if t == "array":
            return ("arr", self.param_shape(schema.get("items", {})))
        if t == "string" and schema.get("format") == "date-time":
            return "dt"
        if t == "string" and schema.get("format") == "date":
            return "d"
        return "s"

    # ─── operaciones ─────────────────────────────────────────────────────────

    def iter_operations(self):
        for path, item in self.spec["paths"].items():
            for method, op in item.items():
                if method not in ("get", "post", "patch", "put", "delete"):
                    continue
                yield {"path": path, "method": method.upper(), "op": op}

    def build_operations(self) -> None:
        for entry in self.iter_operations():
            op = entry["op"]
            key = "%s.%s" % (op["x-kuida-resource"], op["x-kuida-method"])
            query: Dict[str, str] = {}
            path_params: List[str] = []
            for p in op.get("parameters", []):
                if "$ref" in p:
                    continue  # headers comunes (Idempotency-Key, Kuida-Version)
                if p["in"] == "query":
                    query[p["name"]] = self.param_shape(p.get("schema", {}))
                elif p["in"] == "path":
                    path_params.append(p["name"])
            body = None
            body_required = False
            rb = op.get("requestBody")
            if rb:
                schema = rb["content"]["application/json"]["schema"]
                body = self.ref_name(schema["$ref"])
                if body not in self.param_shapes:
                    self.param_shapes[body] = None
                    self.param_shapes[body] = self.param_shape(self.schemas[body])
                body_required = bool(rb.get("required"))
            resp = op["responses"]["200"]["content"]["application/json"]["schema"]
            self.operations[key] = {
                "method": entry["method"],
                "path": entry["path"],
                "path_params": path_params,
                "query": query,
                "body": body,
                "body_required": body_required,
                "response": self.ref_name(resp["$ref"]),
                "summary": op.get("summary", ""),
            }

    # ─── salida ──────────────────────────────────────────────────────────────

    def run(self) -> str:
        for name in self.response_schema_names():
            self.add_model(name, self.schemas[name])
        self.build_operations()
        return self.render()

    def render(self) -> str:
        out: List[str] = []
        w = out.append
        w('"""Modelos, formas de parámetros y operaciones de la API de Kuida.')
        w("")
        w("GENERADO por scripts/generate.py desde openapi/kuida-v1.json. No editar a mano:")
        w("correr ``python3 scripts/generate.py`` después de actualizar el OpenAPI.")
        w('"""')
        w("")
        w("# flake8: noqa")
        w("from datetime import datetime")
        w("from typing import Any, Dict, List, Optional")
        w("")
        w("from .._object import KuidaObject, register_model")
        w("from .._pagination import ListObject")
        w("")
        w("API_VERSION = %r" % self.spec["info"]["version"])
        w("")
        # Las clases anidadas y los ítems de lista van antes de quien las usa.
        order = self.topo_order()
        for name in order:
            m = self.models[name]
            base = "ListObject[%s]" % self.list_item(name) if m["is_list"] else "KuidaObject"
            w("")
            w("@register_model")
            w("class %s(%s):" % (name, base))
            doc = m["doc"] or name
            w('    """%s"""' % doc.replace('"""', "'''"))
            w("")
            if m["object"]:
                w("    OBJECT_NAME = %r" % m["object"])
                w("")
            for f in m["fields"]:
                if f["doc"]:
                    for line in f["doc"].splitlines():
                        w("    #: %s" % line)
                w("    %s: %s" % (f["snake"], f["ann"]))
            w("")
            w("    _wire_fields = {")
            for f in m["fields"]:
                w("        %r: %r," % (f["wire"], f["kind"]))
            w("    }")
            w("")
        w("")
        w("#: Forma de cada esquema de entrada. `s` escalar, `dt` fecha y hora, `d` fecha,")
        w("#: `free` mapa libre (sin conversión de claves), ('obj', campos), ('arr', ítem),")
        w("#: ('union', ramas), ('ref', esquema).")
        w("PARAM_SHAPES: Dict[str, Any] = {")
        for name in sorted(self.param_shapes):
            w("    %r: %r," % (name, self.param_shapes[name]))
        w("}")
        w("")
        w("#: Una entrada por operación del OpenAPI (`x-kuida-resource`.`x-kuida-method`).")
        w("OPERATIONS: Dict[str, Dict[str, Any]] = {")
        for key in self.operations:
            w("    %r: %r," % (key, self.operations[key]))
        w("}")
        w("")
        w("__all__ = [")
        for name in order:
            w("    %r," % name)
        w("]")
        w("")
        return "\n".join(out)

    def list_item(self, name: str) -> str:
        for f in self.models[name]["fields"]:
            if f["wire"] == "data":
                kind = f["kind"]
                if isinstance(kind, tuple) and kind[0] == "list" and isinstance(kind[1], tuple):
                    return kind[1][1]
        return "KuidaObject"

    def topo_order(self) -> List[str]:
        done: List[str] = []

        def deps(kind: Any) -> List[str]:
            if isinstance(kind, tuple):
                if kind[0] == "model":
                    return [kind[1]]
                if kind[0] == "list":
                    return deps(kind[1])
            return []

        def visit(name: str) -> None:
            if name in done:
                return
            for f in self.models[name]["fields"]:
                for d in deps(f["kind"]):
                    if d != name:
                        visit(d)
            done.append(name)

        for name in self.model_order:
            visit(name)
        return done


def main() -> int:
    spec = json.loads(SPEC_PATH.read_text(encoding="utf-8"))
    code = Generator(spec).run()
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUT_PATH.write_text(code, encoding="utf-8")
    init = OUT_PATH.parent / "__init__.py"
    if not init.exists():
        init.write_text('"""Código generado desde el OpenAPI. No editar a mano."""\n', encoding="utf-8")
    print("escrito %s" % OUT_PATH.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
