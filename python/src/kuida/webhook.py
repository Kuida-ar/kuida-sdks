"""Verificación de webhooks de Kuida.

Kuida firma cada entrega con HMAC-SHA256 sobre ``"{timestamp}.{cuerpo}"`` usando el
``secret`` del endpoint, y manda los headers ``x-kuida-signature`` (``sha256=<hex>``),
``x-kuida-timestamp`` (epoch en segundos), ``x-kuida-event-type`` y
``x-kuida-delivery-attempt``.

Ejemplo con Flask::

    import kuida

    @app.post("/kuida/webhooks")
    def kuida_webhook():
        try:
            event = kuida.Webhook.construct_event(
                request.get_data(),                        # cuerpo CRUDO, no request.json
                request.headers.get("x-kuida-signature"),
                request.headers.get("x-kuida-timestamp"),
                os.environ["KUIDA_WEBHOOK_SECRET"],
            )
        except kuida.SignatureVerificationError:
            return "", 400
        if event.type == "intake.ready":
            ...
        return "", 200
"""

import hashlib
import hmac
import json
import time
from datetime import datetime
from typing import Optional, Union

from ._generated.models import WebhookEvent
from .errors import SignatureVerificationError

__all__ = ["Webhook", "WebhookEvent"]

Payload = Union[str, bytes, bytearray]
Now = Union[int, float, datetime, None]


def _to_bytes(payload: Payload) -> bytes:
    if isinstance(payload, (bytes, bytearray)):
        return bytes(payload)
    if isinstance(payload, str):
        return payload.encode("utf-8")
    raise TypeError("payload tiene que ser el cuerpo crudo (str o bytes), no %s" % type(payload).__name__)


class Webhook:
    """Utilidades estáticas para webhooks: no necesitan cliente ni clave de API."""

    #: Tolerancia por defecto entre el timestamp del header y la hora actual, en segundos.
    DEFAULT_TOLERANCE = 300
    #: Nombres de los headers que manda Kuida.
    SIGNATURE_HEADER = "x-kuida-signature"
    TIMESTAMP_HEADER = "x-kuida-timestamp"
    EVENT_TYPE_HEADER = "x-kuida-event-type"
    DELIVERY_ATTEMPT_HEADER = "x-kuida-delivery-attempt"

    @staticmethod
    def compute_signature(payload: Payload, timestamp: Union[str, int], secret: str) -> str:
        """Calcula la firma esperada: ``sha256=`` + hex de HMAC-SHA256(secret, ``"{timestamp}.{payload}"``).

        Útil para armar pedidos firmados en tus tests.
        """
        message = str(timestamp).encode("utf-8") + b"." + _to_bytes(payload)
        digest = hmac.new(secret.encode("utf-8"), message, hashlib.sha256).hexdigest()
        return "sha256=" + digest

    @staticmethod
    def verify_signature(
        payload: Payload,
        signature_header: Optional[str],
        timestamp_header: Optional[str],
        secret: str,
        tolerance: int = DEFAULT_TOLERANCE,
        *,
        now: Now = None,
    ) -> bool:
        """Verifica la firma de una entrega. Devuelve ``True`` o lanza
        :class:`~kuida.errors.SignatureVerificationError`.

        Args:
            payload: cuerpo **crudo** del pedido (``str`` o ``bytes``), nunca un JSON re-serializado.
            signature_header: valor de ``x-kuida-signature``.
            timestamp_header: valor de ``x-kuida-timestamp``.
            secret: secreto del endpoint (``whsec_…``).
            tolerance: diferencia máxima en segundos entre el timestamp y ahora; ``0`` desactiva
                el chequeo de tiempo.
            now: "ahora" a usar (epoch en segundos o ``datetime``); para tests.
        """
        if not signature_header:
            raise SignatureVerificationError("Falta el header x-kuida-signature")
        if not timestamp_header:
            raise SignatureVerificationError("Falta el header x-kuida-timestamp", sig_header=signature_header)
        if not secret:
            raise SignatureVerificationError("Falta el secreto del endpoint", sig_header=signature_header)
        try:
            timestamp = int(str(timestamp_header).strip())
        except ValueError:
            raise SignatureVerificationError(
                "x-kuida-timestamp no es un número: %r" % timestamp_header, sig_header=signature_header
            ) from None

        expected = Webhook.compute_signature(payload, str(timestamp_header).strip(), secret)
        candidates = [s.strip() for s in str(signature_header).split(",") if s.strip()]
        if not any(hmac.compare_digest(expected.encode("utf-8"), c.encode("utf-8")) for c in candidates):
            raise SignatureVerificationError(
                "La firma no coincide con el cuerpo recibido", sig_header=signature_header, http_body=_safe_text(payload)
            )

        if tolerance and tolerance > 0:
            if now is None:
                current = time.time()
            elif isinstance(now, datetime):
                current = now.timestamp()
            else:
                current = float(now)
            if abs(current - timestamp) > tolerance:
                raise SignatureVerificationError(
                    "El timestamp está fuera de la tolerancia de %d s" % tolerance, sig_header=signature_header
                )
        return True

    @staticmethod
    def construct_event(
        payload: Payload,
        signature_header: Optional[str],
        timestamp_header: Optional[str],
        secret: str,
        tolerance: int = DEFAULT_TOLERANCE,
        *,
        now: Now = None,
    ) -> WebhookEvent:
        """Verifica la firma y devuelve el evento parseado (:class:`WebhookEvent`).

        El evento tiene ``id`` (id de la entrega, estable entre reintentos: deduplicá por acá),
        ``type``, ``api_version``, ``livemode``, ``occurred_at``, ``ref`` y ``data`` (dict sin
        convertir). Lanza :class:`~kuida.errors.SignatureVerificationError` si la firma no es válida.
        """
        Webhook.verify_signature(payload, signature_header, timestamp_header, secret, tolerance, now=now)
        try:
            data = json.loads(_to_bytes(payload).decode("utf-8"))
        except ValueError:
            raise SignatureVerificationError("El cuerpo firmado no es JSON válido", sig_header=signature_header) from None
        if not isinstance(data, dict):
            raise SignatureVerificationError("El cuerpo firmado no es un objeto JSON", sig_header=signature_header)
        return WebhookEvent.construct_from(data)  # type: ignore[return-value]


def _safe_text(payload: Payload) -> Optional[str]:
    try:
        return _to_bytes(payload).decode("utf-8")
    except (UnicodeDecodeError, TypeError):
        return None
