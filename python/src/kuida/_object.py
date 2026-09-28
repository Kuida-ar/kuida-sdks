"""Objeto base de las respuestas: atributos ``snake_case`` y acceso por clave del cable."""

import copy
import json
from typing import Any, ClassVar, Dict, Iterator, Mapping, Optional, Type, TypeVar

from ._util import dumps, parse_datetime, snake_to_camel

_MODELS: Dict[str, Type["KuidaObject"]] = {}

T = TypeVar("T")


def register_model(cls: T) -> T:
    """Registra una clase de modelo por nombre (lo usan los modelos generados)."""
    _MODELS[cls.__name__] = cls  # type: ignore[attr-defined,assignment]
    return cls


def model_class(name: str) -> Type["KuidaObject"]:
    """Devuelve la clase de modelo registrada con ese nombre."""
    return _MODELS.get(name, KuidaObject)


class KuidaResponse:
    """Datos HTTP de la respuesta que originó un objeto (``obj.last_response``)."""

    def __init__(self, status_code: int, headers: Mapping[str, str], body: str) -> None:
        #: Código HTTP.
        self.status_code = status_code
        #: Headers de la respuesta.
        self.headers = headers
        #: Cuerpo crudo, como texto.
        self.body = body

    @property
    def request_id(self) -> Optional[str]:
        """Valor del header ``Request-Id`` (útil para soporte)."""
        return self.headers.get("request-id")

    @property
    def idempotent_replayed(self) -> bool:
        """``True`` si la respuesta es la repetición de un pedido idempotente anterior."""
        return self.headers.get("idempotent-replayed") == "true"

    def __repr__(self) -> str:
        return "<KuidaResponse status=%d request_id=%s>" % (self.status_code, self.request_id)


def _convert(value: Any, kind: Any) -> Any:
    if value is None or kind is None or kind in ("s", "free"):
        return value
    if kind == "dt":
        if isinstance(value, str):
            parsed = parse_datetime(value)
            return parsed if parsed is not None else value
        return value
    if isinstance(kind, tuple):
        if kind[0] == "model" and isinstance(value, Mapping):
            return model_class(kind[1]).construct_from(value)
        if kind[0] == "list" and isinstance(value, list):
            return [_convert(v, kind[1]) for v in value]
    return value


class KuidaObject:
    """Objeto devuelto por la API.

    * Atributos en ``snake_case``: ``patient.full_name``, ``patient.created_at`` (``datetime``).
    * Acceso por la clave del cable: ``patient["fullName"]``.
    * ``obj.raw`` (o ``obj.to_dict()``) devuelve el JSON crudo tal como llegó.
    * Los mapas libres (``data``, ``payload``) quedan como ``dict`` sin convertir.
    * Los campos que el SDK no conoce se conservan en ``raw`` y por clave, sin error.
    """

    OBJECT_NAME: ClassVar[Optional[str]] = None
    _wire_fields: ClassVar[Dict[str, Any]] = {}

    def __init__(self, data: Optional[Mapping[str, Any]] = None, **_: Any) -> None:
        raw = dict(data or {})
        values = {k: _convert(v, self._wire_fields.get(k)) for k, v in raw.items()}
        object.__setattr__(self, "_raw", raw)
        object.__setattr__(self, "_values", values)
        object.__setattr__(self, "last_response", None)

    @classmethod
    def construct_from(
        cls, data: Mapping[str, Any], last_response: Optional[KuidaResponse] = None
    ) -> "KuidaObject":
        """Construye el objeto desde el JSON del cable."""
        obj = cls(data)
        object.__setattr__(obj, "last_response", last_response)
        return obj

    # ─── acceso ──────────────────────────────────────────────────────────────

    def __getattr__(self, name: str) -> Any:
        if name.startswith("__"):
            raise AttributeError(name)
        values = self.__dict__.get("_values")
        if values is None:
            raise AttributeError(name)
        wire = snake_to_camel(name)
        if wire in values:
            return values[wire]
        if name in values:
            return values[name]
        if wire in self._wire_fields:
            return None  # campo conocido que no vino en esta respuesta
        raise AttributeError("%s no tiene el atributo %r" % (type(self).__name__, name))

    def __setattr__(self, name: str, value: Any) -> None:
        raise AttributeError("Los objetos de Kuida son de solo lectura")

    def __getitem__(self, key: str) -> Any:
        return self._values[key]

    def __contains__(self, key: object) -> bool:
        return key in self._values

    def __iter__(self) -> Iterator[Any]:
        return iter(self._values)

    def __len__(self) -> int:
        return len(self._values)

    def __bool__(self) -> bool:
        return True

    def get(self, key: str, default: Any = None) -> Any:
        """Valor por clave del cable, o ``default`` si no está."""
        return self._values.get(key, default)

    def keys(self):  # type: ignore[no-untyped-def]
        """Claves del cable (camelCase)."""
        return self._values.keys()

    @property
    def raw(self) -> Dict[str, Any]:
        """El JSON crudo del objeto, tal como lo devolvió la API."""
        return self._raw

    def to_dict(self) -> Dict[str, Any]:
        """Copia del JSON crudo (claves camelCase, fechas como texto ISO)."""
        return copy.deepcopy(self._raw)

    def to_json(self, indent: Optional[int] = 2) -> str:
        """El JSON crudo serializado."""
        return json.dumps(self._raw, ensure_ascii=False, indent=indent)

    # ─── varios ──────────────────────────────────────────────────────────────

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, KuidaObject):
            return NotImplemented
        return type(self) is type(other) and self._raw == other._raw

    def __hash__(self) -> int:
        return hash((type(self).__name__, dumps(self._raw)))

    def __repr__(self) -> str:
        ident = self._raw.get("id")
        head = "<%s%s>" % (type(self).__name__, " id=%s" % ident if ident else "")
        return "%s JSON: %s" % (head, self.to_json())

    def __copy__(self) -> "KuidaObject":
        return type(self).construct_from(self._raw, self.last_response)

    def __deepcopy__(self, memo: Dict[int, Any]) -> "KuidaObject":
        return type(self).construct_from(copy.deepcopy(self._raw, memo), self.last_response)

    def __getstate__(self) -> Dict[str, Any]:
        return {"raw": self._raw}

    def __setstate__(self, state: Dict[str, Any]) -> None:
        raw = state["raw"]
        object.__setattr__(self, "_raw", raw)
        object.__setattr__(self, "_values", {k: _convert(v, self._wire_fields.get(k)) for k, v in raw.items()})
        object.__setattr__(self, "last_response", None)
