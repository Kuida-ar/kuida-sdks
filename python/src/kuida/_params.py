"""Conversión de parámetros de entrada al formato del cable.

Los kwargs ``snake_case`` pasan a camelCase **solo** en las claves que el esquema del OpenAPI
conoce (``PARAM_SHAPES``). Las claves desconocidas se mandan tal cual y los mapas libres
(``data`` de los eventos) no se tocan. También se aceptan las claves del cable directamente.
"""

from datetime import date, datetime
from typing import Any, Dict, Mapping, Optional

from ._generated.models import PARAM_SHAPES
from ._object import KuidaObject
from ._util import format_datetime, snake_to_camel


def _resolve(shape: Any) -> Any:
    while isinstance(shape, tuple) and shape[0] == "ref":
        shape = PARAM_SHAPES.get(shape[1])
    return shape


def _scalar(value: Any, shape: Any) -> Any:
    if isinstance(value, KuidaObject):
        ident = value.get("id")
        return ident if ident is not None else value.to_dict()
    if isinstance(value, datetime):
        if shape == "d":
            return value.date().isoformat()
        return format_datetime(value)
    if isinstance(value, date):
        return value.isoformat()
    if isinstance(value, (tuple, set, frozenset)):
        return [_scalar(v, None) for v in value]
    return value


def _free(value: Any) -> Any:
    """Mapa libre: claves intactas; solo se serializan fechas y objetos de Kuida."""
    if isinstance(value, KuidaObject):
        return value.to_dict()
    if isinstance(value, Mapping):
        return {k: _free(v) for k, v in value.items()}
    if isinstance(value, (list, tuple, set, frozenset)):
        return [_free(v) for v in value]
    return _scalar(value, None)


def _object(value: Mapping[str, Any], fields: Mapping[str, Any]) -> Dict[str, Any]:
    out: Dict[str, Any] = {}
    for key, v in value.items():
        if v is None:
            continue
        if key in fields:
            wire = key
        else:
            camel = snake_to_camel(key)
            wire = camel if camel in fields else key
        out[wire] = to_wire(v, fields.get(wire))
    return out


def to_wire(value: Any, shape: Any) -> Any:
    """Convierte ``value`` según la forma del esquema (ver ``PARAM_SHAPES``)."""
    shape = _resolve(shape)
    if value is None:
        return None
    if shape == "free":
        return _free(value)
    if isinstance(shape, tuple):
        kind = shape[0]
        if kind == "obj":
            if isinstance(value, KuidaObject):
                return value.to_dict()
            if isinstance(value, Mapping):
                return _object(value, shape[1])
            return _scalar(value, None)
        if kind == "arr":
            if isinstance(value, (list, tuple, set, frozenset)):
                return [to_wire(v, shape[1]) for v in value]
            return _scalar(value, None)
        if kind == "union":
            # PatientReference / DoctorReference: un id o un objeto de identidad.
            if isinstance(value, KuidaObject) and value.get("id") is not None:
                return value.get("id")
            if isinstance(value, Mapping):
                for branch in shape[1]:
                    b = _resolve(branch)
                    if isinstance(b, tuple) and b[0] == "obj":
                        return _object(value, b[1])
                return _free(value)
            return _scalar(value, None)
    if isinstance(value, Mapping):
        # Objeto donde el esquema no declara forma: se manda sin tocar las claves.
        return _free(value)
    if isinstance(value, (list, tuple)):
        return [to_wire(v, None) for v in value]
    return _scalar(value, shape)


def body_params(schema_name: Optional[str], params: Mapping[str, Any]) -> Dict[str, Any]:
    """Cuerpo de un POST/PATCH a partir de los kwargs o del dict de parámetros."""
    if schema_name is None:
        return _free({k: v for k, v in params.items() if v is not None})
    shape = _resolve(("ref", schema_name))
    if isinstance(shape, tuple) and shape[0] == "obj":
        return _object(params, shape[1])
    if isinstance(shape, tuple) and shape[0] == "union":
        for branch in shape[1]:
            b = _resolve(branch)
            if isinstance(b, tuple) and b[0] == "obj":
                return _object(params, b[1])
    return _free({k: v for k, v in params.items() if v is not None})


def query_params(spec: Mapping[str, Any], params: Mapping[str, Any]) -> Dict[str, Any]:
    """Query string de un ``list``: camelCase en las claves conocidas y booleanos como texto."""
    out: Dict[str, Any] = {}
    for key, value in params.items():
        if value is None:
            continue
        wire = key if key in spec else snake_to_camel(key)
        if wire not in spec:
            wire = key
        out[wire] = _query_value(value)
    return out


def _query_value(value: Any) -> Any:
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (list, tuple, set, frozenset)):
        return [_query_value(v) for v in value]
    return _scalar(value, None)


def merge(params: Optional[Mapping[str, Any]], kwargs: Mapping[str, Any]) -> Dict[str, Any]:
    """Une el dict de parámetros posicional con los kwargs (los kwargs ganan)."""
    merged: Dict[str, Any] = dict(params or {})
    merged.update(kwargs)
    return merged
