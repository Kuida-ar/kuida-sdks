"""Excepciones del SDK de Kuida.

Toda respuesta no 2xx con cuerpo ``{"error": {...}}`` se convierte en una subclase de
:class:`KuidaError` según ``error.type``:

========================= ===========================
``type``                  Clase
========================= ===========================
``invalid_request_error`` :class:`InvalidRequestError`
``authentication_error``  :class:`AuthenticationError`
``permission_error``      :class:`PermissionDeniedError` (alias ``PermissionError``)
``idempotency_error``     :class:`IdempotencyError`
``rate_limit_error``      :class:`RateLimitError`
``api_error`` u otro      :class:`APIError`
sin respuesta             :class:`APIConnectionError`
========================= ===========================

``PermissionError`` choca con la excepción nativa de Python; por eso la clase se llama
``PermissionDeniedError`` y ``kuida.errors.PermissionError`` es un alias (no se exporta desde
``kuida`` para no tapar la nativa con ``from kuida import *``).
"""

import json
from typing import Any, Dict, Mapping, Optional, Type

__all__ = [
    "KuidaError",
    "InvalidRequestError",
    "AuthenticationError",
    "PermissionDeniedError",
    "IdempotencyError",
    "RateLimitError",
    "APIError",
    "APIConnectionError",
    "SignatureVerificationError",
]


class KuidaError(Exception):
    """Base de todos los errores del SDK.

    Atributos:
        message: descripción legible del error.
        http_status: código HTTP (``None`` si no hubo respuesta).
        type: ``error.type`` de la API (``invalid_request_error``, …).
        code: ``error.code`` de la API (``resource_missing``, ``scope_missing``, …).
        param: parámetro que causó el error, si aplica.
        request_id: id del pedido (del cuerpo o del header ``Request-Id``); mandalo a soporte.
        doc_url: página de la documentación sobre este error.
        headers: headers de la respuesta.
        http_body: cuerpo crudo de la respuesta, como texto.
        json_body: cuerpo parseado, si era JSON.
    """

    def __init__(
        self,
        message: Optional[str] = None,
        *,
        http_status: Optional[int] = None,
        type: Optional[str] = None,
        code: Optional[str] = None,
        param: Optional[str] = None,
        request_id: Optional[str] = None,
        doc_url: Optional[str] = None,
        headers: Optional[Mapping[str, str]] = None,
        http_body: Optional[str] = None,
        json_body: Any = None,
    ) -> None:
        super().__init__(message or "")
        self.message = message or ""
        self.http_status = http_status
        self.type = type
        self.code = code
        self.param = param
        self.request_id = request_id
        self.doc_url = doc_url
        self.headers: Dict[str, str] = dict(headers or {})
        self.http_body = http_body
        self.json_body = json_body

    @property
    def error(self) -> Optional[Dict[str, Any]]:
        """El objeto ``error`` del cuerpo, si vino."""
        if isinstance(self.json_body, dict) and isinstance(self.json_body.get("error"), dict):
            return self.json_body["error"]
        return None

    def __str__(self) -> str:
        parts = []
        if self.http_status is not None:
            parts.append(str(self.http_status))
        if self.code:
            parts.append(self.code)
        prefix = "(%s) " % " ".join(parts) if parts else ""
        suffix = " [param: %s]" % self.param if self.param else ""
        rid = " [request_id: %s]" % self.request_id if self.request_id else ""
        return "%s%s%s%s" % (prefix, self.message, suffix, rid)

    def __repr__(self) -> str:
        return "%s(message=%r, http_status=%r, code=%r, request_id=%r)" % (
            type(self).__name__,
            self.message,
            self.http_status,
            self.code,
            self.request_id,
        )


class InvalidRequestError(KuidaError):
    """Parámetros inválidos o faltantes, recurso inexistente (404) o conflicto (409)."""


class AuthenticationError(KuidaError):
    """Clave de API faltante, inválida, revocada o vencida (401)."""


class PermissionDeniedError(KuidaError):
    """La clave no tiene el scope o la organización no contrató la línea de servicio (403)."""


#: Alias con el nombre del contrato común. Choca con el builtin: importalo con cuidado.
PermissionError = PermissionDeniedError


class IdempotencyError(KuidaError):
    """La ``Idempotency-Key`` ya se usó con otro pedido."""


class RateLimitError(KuidaError):
    """Demasiados pedidos (429). El SDK reintenta solo respetando ``Retry-After``."""


class APIError(KuidaError):
    """Error del lado de Kuida (5xx), tipo desconocido o respuesta que no es JSON."""


class APIConnectionError(KuidaError):
    """No hubo respuesta: error de red, DNS, TLS o timeout."""


class SignatureVerificationError(KuidaError):
    """La firma de un webhook no es válida, falta un header o el timestamp está fuera de tolerancia."""

    def __init__(self, message: str, *, sig_header: Optional[str] = None, http_body: Optional[str] = None) -> None:
        super().__init__(message, http_body=http_body)
        self.sig_header = sig_header


_BY_TYPE: Dict[str, Type[KuidaError]] = {
    "invalid_request_error": InvalidRequestError,
    "authentication_error": AuthenticationError,
    "permission_error": PermissionDeniedError,
    "idempotency_error": IdempotencyError,
    "rate_limit_error": RateLimitError,
    "api_error": APIError,
}


def error_from_response(status: int, text: str, headers: Mapping[str, str]) -> KuidaError:
    """Arma la excepción que corresponde a una respuesta no 2xx."""
    header_rid = headers.get("request-id") or headers.get("Request-Id")
    try:
        body = json.loads(text) if text else None
    except ValueError:
        return APIError(
            text or "Respuesta HTTP %d sin cuerpo" % status,
            http_status=status,
            request_id=header_rid,
            headers=headers,
            http_body=text,
        )
    err = body.get("error") if isinstance(body, dict) else None
    if not isinstance(err, dict):
        return APIError(
            "Respuesta HTTP %d inesperada de la API" % status,
            http_status=status,
            request_id=header_rid,
            headers=headers,
            http_body=text,
            json_body=body,
        )
    cls = _BY_TYPE.get(str(err.get("type")), APIError)
    return cls(
        err.get("message") or "Error HTTP %d" % status,
        http_status=status,
        type=err.get("type"),
        code=err.get("code"),
        param=err.get("param"),
        request_id=err.get("requestId") or header_rid,
        doc_url=err.get("docUrl"),
        headers=headers,
        http_body=text,
        json_body=body,
    )
