"""SDK oficial de Kuida para Python.

Kuida (https://www.kuida.ar) es una plataforma de seguimiento de pacientes por WhatsApp.
Este paquete envuelve su API REST v1::

    import kuida

    client = kuida.Kuida(api_key="kd_test_…")        # o la variable KUIDA_API_KEY
    patient = client.patients.create(phone="+54 9 342 555 0000", full_name="Paciente Demo")
    print(patient.id, patient.full_name)

Ver ``README.md`` para la guía completa.
"""

from . import errors
from ._client import AsyncKuida, Kuida
from ._generated.models import *  # noqa: F401,F403  (modelos generados del OpenAPI)
from ._generated.models import __all__ as _models_all
from ._object import KuidaObject, KuidaResponse
from ._pagination import AutoPagingIterator, ListObject
from ._version import API_VERSION, VERSION
from .errors import (
    APIConnectionError,
    APIError,
    AuthenticationError,
    IdempotencyError,
    InvalidRequestError,
    KuidaError,
    PermissionDeniedError,
    RateLimitError,
    SignatureVerificationError,
)
from .webhook import Webhook

__version__ = VERSION

__all__ = [
    "Kuida",
    "AsyncKuida",
    "Webhook",
    "KuidaObject",
    "KuidaResponse",
    "ListObject",
    "AutoPagingIterator",
    "errors",
    "KuidaError",
    "InvalidRequestError",
    "AuthenticationError",
    "PermissionDeniedError",
    "IdempotencyError",
    "RateLimitError",
    "APIError",
    "APIConnectionError",
    "SignatureVerificationError",
    "API_VERSION",
    "VERSION",
    "__version__",
] + list(_models_all)
