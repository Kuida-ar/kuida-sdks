"""Utilidades internas: nombres, fechas y serialización JSON."""

import json
import re
from datetime import date, datetime, timedelta, timezone
from typing import Any, Optional

_DT_RE = re.compile(
    r"^(?P<date>\d{4}-\d{2}-\d{2})[T ](?P<h>\d{2}):(?P<m>\d{2})"
    r"(?::(?P<s>\d{2})(?:\.(?P<frac>\d+))?)?"
    r"(?P<tz>Z|z|[+-]\d{2}:?\d{2})?$"
)


def snake_to_camel(name: str) -> str:
    """``start_at_gte`` → ``startAtGte``. Una clave sin guiones bajos queda igual."""
    if "_" not in name:
        return name
    head, *rest = name.split("_")
    return head + "".join(p[:1].upper() + p[1:] for p in rest)


def camel_to_snake(name: str) -> str:
    """``lastStatusCode`` → ``last_status_code``."""
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name).lower()


def parse_datetime(value: str) -> Optional[datetime]:
    """Parsea ISO 8601 (con ``Z`` y cualquier cantidad de decimales) en Python 3.9+.

    Devuelve ``None`` si el texto no es una fecha y hora válida.
    """
    m = _DT_RE.match(value)
    if not m:
        return None
    frac = (m.group("frac") or "0")[:6].ljust(6, "0")
    try:
        y, mo, d = (int(x) for x in m.group("date").split("-"))
        tz_text = m.group("tz")
        tz: Optional[timezone] = None
        if tz_text in ("Z", "z"):
            tz = timezone.utc
        elif tz_text:
            sign = 1 if tz_text[0] == "+" else -1
            digits = tz_text[1:].replace(":", "")
            tz = timezone(sign * timedelta(hours=int(digits[:2]), minutes=int(digits[2:])))
        return datetime(
            y, mo, d, int(m.group("h")), int(m.group("m")), int(m.group("s") or 0), int(frac), tzinfo=tz
        )
    except ValueError:
        return None


def format_datetime(value: datetime) -> str:
    """Serializa un ``datetime`` en ISO 8601 con zona.

    Un ``datetime`` sin zona (naive) se interpreta como hora local del proceso, igual que
    ``datetime.astimezone()``.
    """
    if value.tzinfo is None or value.tzinfo.utcoffset(value) is None:
        value = value.astimezone()
    text = value.isoformat()
    return text[:-6] + "Z" if text.endswith("+00:00") else text


def json_default(value: Any) -> Any:
    """``default`` de ``json.dumps``: fechas en ISO 8601 y objetos de Kuida como su JSON crudo."""
    if isinstance(value, datetime):
        return format_datetime(value)
    if isinstance(value, date):
        return value.isoformat()
    to_dict = getattr(value, "to_dict", None)
    if callable(to_dict):
        return to_dict()
    if isinstance(value, (set, frozenset, tuple)):
        return list(value)
    raise TypeError("El objeto de tipo %s no se puede serializar a JSON" % type(value).__name__)


def dumps(value: Any) -> str:
    """JSON compacto, UTF-8 sin escapar, con el ``default`` de Kuida."""
    return json.dumps(value, default=json_default, ensure_ascii=False, separators=(",", ":"))
