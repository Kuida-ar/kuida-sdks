"""Clientes :class:`Kuida` (sincrónico) y :class:`AsyncKuida` (asyncio) y la capa HTTP."""

import asyncio
import json
import os
import platform
import random
import time
import uuid
from typing import Any, Callable, Dict, Mapping, NamedTuple, Optional, Tuple
from urllib.parse import quote

import httpx

from ._generated.models import OPERATIONS
from ._object import KuidaObject, KuidaResponse, model_class
from ._pagination import ListObject
from ._params import body_params, query_params
from ._resources import (
    Account,
    Appointments,
    AsyncAccount,
    AsyncAppointments,
    AsyncDoctors,
    AsyncEvents,
    AsyncIntakeRequests,
    AsyncPatients,
    AsyncTreatments,
    AsyncVisits,
    AsyncWebhookDeliveries,
    AsyncWebhookEndpoints,
    Doctors,
    Events,
    IntakeRequests,
    Patients,
    Treatments,
    Visits,
    WebhookDeliveries,
    WebhookEndpoints,
)
from ._util import dumps
from ._version import API_VERSION, VERSION
from .errors import APIConnectionError, AuthenticationError, error_from_response

DEFAULT_BASE_URL = "https://www.kuida.ar/api"
DEFAULT_TIMEOUT = 30.0
DEFAULT_MAX_RETRIES = 2
MAX_RETRY_AFTER = 60.0
_RETRY_STATUSES = {429, 500, 502, 503, 504}
_NOT_GIVEN: Any = object()


class _Prepared(NamedTuple):
    op: Dict[str, Any]
    method: str
    url: str
    query: Optional[Dict[str, Any]]
    content: Optional[bytes]
    headers: Dict[str, str]
    timeout: float
    max_retries: int


class _BaseClient:
    def __init__(
        self,
        api_key: Optional[str] = None,
        base_url: Optional[str] = None,
        timeout: float = DEFAULT_TIMEOUT,
        max_retries: int = DEFAULT_MAX_RETRIES,
        api_version: Optional[str] = None,
        sleep: Optional[Callable[[float], Any]] = None,
    ) -> None:
        api_key = api_key or os.environ.get("KUIDA_API_KEY")
        if not api_key:
            raise AuthenticationError(
                "Falta la clave de API. Pasala como Kuida(api_key='kd_live_…') o definí KUIDA_API_KEY.",
                code="api_key_missing",
            )
        #: Clave de API (``kd_live_…`` o ``kd_test_…``).
        self.api_key = api_key
        #: URL base de la API, sin barra final.
        self.base_url = (base_url or os.environ.get("KUIDA_API_BASE") or DEFAULT_BASE_URL).rstrip("/")
        #: Timeout por intento, en segundos.
        self.timeout = timeout
        #: Reintentos automáticos (0 los desactiva).
        self.max_retries = max_retries
        #: Versión de la API que se manda en ``Kuida-Version``.
        self.api_version = api_version or API_VERSION
        self._sleep_override = sleep

    # ─── armado del pedido ───────────────────────────────────────────────────

    def _user_agent_headers(self) -> Dict[str, str]:
        client_ua = {
            "bindings_version": VERSION,
            "lang": "python",
            "lang_version": platform.python_version(),
            "platform": platform.platform(),
        }
        return {
            "User-Agent": "Kuida/v1 PythonBindings/%s" % VERSION,
            "X-Kuida-Client-User-Agent": json.dumps(client_ua),
        }

    def _prepare(
        self,
        op_key: str,
        path_params: Tuple[str, ...],
        params: Optional[Mapping[str, Any]],
        body: Any,
        idempotency_key: Optional[str],
        timeout: Optional[float],
        max_retries: Optional[int],
        wire_query: bool = False,
    ) -> _Prepared:
        op = OPERATIONS[op_key]
        path = op["path"]
        for name, value in zip(op["path_params"], path_params):
            if not isinstance(value, str) or not value:
                raise TypeError("El id tiene que ser un texto no vacío (recibido %r)" % (value,))
            path = path.replace("{%s}" % name, quote(value, safe=""))
        method = op["method"]
        headers = {
            "Authorization": "Bearer %s" % self.api_key,
            "Accept": "application/json",
            "Kuida-Version": self.api_version,
        }
        headers.update(self._user_agent_headers())
        query: Optional[Dict[str, Any]] = None
        content: Optional[bytes] = None
        if method == "GET" or method == "DELETE":
            if params:
                query = dict(params) if wire_query else query_params(op["query"], params)
        else:
            if body is _NOT_GIVEN:
                body = body_params(op["body"], params or {})
            content = dumps(body if body is not None else {}).encode("utf-8")
            headers["Content-Type"] = "application/json"
        if method == "POST":
            headers["Idempotency-Key"] = idempotency_key or str(uuid.uuid4())
        return _Prepared(
            op=op,
            method=method,
            url=self.base_url + path,
            query=query,
            content=content,
            headers=headers,
            timeout=self.timeout if timeout is None else timeout,
            max_retries=self.max_retries if max_retries is None else max_retries,
        )

    # ─── reintentos ──────────────────────────────────────────────────────────

    @staticmethod
    def _should_retry(response: httpx.Response) -> bool:
        if response.status_code in _RETRY_STATUSES:
            return True
        if response.status_code == 409:
            try:
                return response.json().get("error", {}).get("code") == "idempotency_key_in_use"
            except (ValueError, AttributeError):
                return False
        return False

    @staticmethod
    def _retry_delay(attempt: int, response: Optional[httpx.Response]) -> float:
        if response is not None:
            retry_after = response.headers.get("retry-after")
            if retry_after:
                try:
                    return max(0.0, min(float(retry_after), MAX_RETRY_AFTER))
                except ValueError:
                    pass
        base = min(0.5 * (2**attempt), 8.0)
        return base * random.uniform(0.75, 1.25)

    # ─── respuesta ───────────────────────────────────────────────────────────

    def _interpret(self, prepared: _Prepared, response: httpx.Response) -> KuidaObject:
        text = response.text
        info = KuidaResponse(response.status_code, response.headers, text)
        cls = model_class(prepared.op["response"])
        if 200 <= response.status_code < 300:
            try:
                data = json.loads(text) if text else {}
            except ValueError:
                raise error_from_response(response.status_code, text, response.headers) from None
            return cls.construct_from(data, info)
        raise error_from_response(response.status_code, text, response.headers)

    def _connection_error(self, exc: Exception) -> APIConnectionError:
        kind = "Timeout" if isinstance(exc, httpx.TimeoutException) else "Error de conexión"
        return APIConnectionError("%s al llamar a la API de Kuida (%s): %s" % (kind, self.base_url, exc))


class Kuida(_BaseClient):
    """Cliente sincrónico de la API de Kuida.

    Ejemplo::

        import kuida

        client = kuida.Kuida(api_key="kd_test_…")
        patient = client.patients.create(phone="+54 9 342 555 0000", full_name="Paciente Demo")

    Args:
        api_key: clave de API. Si no se pasa, se lee ``KUIDA_API_KEY``. Sin clave, falla al
            construir con :class:`~kuida.errors.AuthenticationError`.
        base_url: URL base (default ``https://www.kuida.ar/api`` o ``KUIDA_API_BASE``).
        timeout: segundos por intento (default 30).
        max_retries: reintentos automáticos ante errores de red, 409 ``idempotency_key_in_use``,
            429 y 5xx (default 2, o sea hasta 3 intentos).
        api_version: header ``Kuida-Version`` (default la versión del SDK, ``2026-09-28``).
        http_client: un ``httpx.Client`` propio (proxies, certificados, transportes de prueba).
        sleep: función de espera entre reintentos; los tests pueden inyectar una nula.

    El cliente es reutilizable y seguro entre hilos: creá uno por proceso.
    """

    def __init__(
        self,
        api_key: Optional[str] = None,
        base_url: Optional[str] = None,
        timeout: float = DEFAULT_TIMEOUT,
        max_retries: int = DEFAULT_MAX_RETRIES,
        api_version: Optional[str] = None,
        *,
        http_client: Optional[httpx.Client] = None,
        sleep: Optional[Callable[[float], Any]] = None,
    ) -> None:
        super().__init__(api_key, base_url, timeout, max_retries, api_version, sleep)
        self._owns_http = http_client is None
        self._http = http_client or httpx.Client()
        #: La organización dueña de la clave.
        self.account = Account(self)
        #: Pacientes.
        self.patients = Patients(self)
        #: Profesionales (solo lectura).
        self.doctors = Doctors(self)
        #: Turnos.
        self.appointments = Appointments(self)
        #: Consultas atendidas.
        self.visits = Visits(self)
        #: Solicitudes de ingreso.
        self.intake_requests = IntakeRequests(self)
        #: Tratamientos.
        self.treatments = Treatments(self)
        #: Eventos del catálogo v1.
        self.events = Events(self)
        #: Endpoints de webhooks.
        self.webhook_endpoints = WebhookEndpoints(self)
        #: Entregas de webhooks.
        self.webhook_deliveries = WebhookDeliveries(self)

    def close(self) -> None:
        """Cierra las conexiones HTTP (si el cliente HTTP lo creó el SDK)."""
        if self._owns_http:
            self._http.close()

    def __enter__(self) -> "Kuida":
        return self

    def __exit__(self, *exc: Any) -> None:
        self.close()

    def _sleep(self, seconds: float) -> None:
        (self._sleep_override or time.sleep)(seconds)

    def _request(
        self,
        op_key: str,
        path_params: Tuple[str, ...] = (),
        params: Optional[Mapping[str, Any]] = None,
        *,
        body: Any = _NOT_GIVEN,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        wire_query: bool = False,
    ) -> Any:
        prepared = self._prepare(
            op_key, path_params, params, body, idempotency_key, timeout, max_retries, wire_query
        )
        attempt = 0
        while True:
            try:
                response = self._http.request(
                    prepared.method,
                    prepared.url,
                    params=prepared.query,
                    content=prepared.content,
                    headers=prepared.headers,
                    timeout=prepared.timeout,
                )
            except httpx.TransportError as exc:
                if attempt < prepared.max_retries:
                    self._sleep(self._retry_delay(attempt, None))
                    attempt += 1
                    continue
                raise self._connection_error(exc) from exc
            if attempt < prepared.max_retries and self._should_retry(response):
                self._sleep(self._retry_delay(attempt, response))
                attempt += 1
                continue
            result = self._interpret(prepared, response)
            if isinstance(result, ListObject):
                result._bind(self._page_fetcher(op_key, path_params, timeout, max_retries), prepared.query or {})
            return result

    def _page_fetcher(
        self, op_key: str, path_params: Tuple[str, ...], timeout: Optional[float], max_retries: Optional[int]
    ) -> Callable[[Dict[str, Any]], Any]:
        def fetch(query: Dict[str, Any]) -> Any:
            return self._request(
                op_key, path_params, query, timeout=timeout, max_retries=max_retries, wire_query=True
            )

        return fetch


class AsyncKuida(_BaseClient):
    """Cliente asyncio de la API de Kuida, con la misma superficie que :class:`Kuida`.

    Ejemplo::

        import kuida

        async with kuida.AsyncKuida(api_key="kd_test_…") as client:
            page = await client.patients.list(limit=50)
            async for patient in page.auto_paging_iter():
                print(patient.full_name)

    Acepta los mismos argumentos que :class:`Kuida`; ``http_client`` es un
    ``httpx.AsyncClient`` y ``sleep`` puede ser una función o una corrutina.
    """

    def __init__(
        self,
        api_key: Optional[str] = None,
        base_url: Optional[str] = None,
        timeout: float = DEFAULT_TIMEOUT,
        max_retries: int = DEFAULT_MAX_RETRIES,
        api_version: Optional[str] = None,
        *,
        http_client: Optional[httpx.AsyncClient] = None,
        sleep: Optional[Callable[[float], Any]] = None,
    ) -> None:
        super().__init__(api_key, base_url, timeout, max_retries, api_version, sleep)
        self._owns_http = http_client is None
        self._http = http_client or httpx.AsyncClient()
        self.account = AsyncAccount(self)
        self.patients = AsyncPatients(self)
        self.doctors = AsyncDoctors(self)
        self.appointments = AsyncAppointments(self)
        self.visits = AsyncVisits(self)
        self.intake_requests = AsyncIntakeRequests(self)
        self.treatments = AsyncTreatments(self)
        self.events = AsyncEvents(self)
        self.webhook_endpoints = AsyncWebhookEndpoints(self)
        self.webhook_deliveries = AsyncWebhookDeliveries(self)

    async def close(self) -> None:
        """Cierra las conexiones HTTP (si el cliente HTTP lo creó el SDK)."""
        if self._owns_http:
            await self._http.aclose()

    async def __aenter__(self) -> "AsyncKuida":
        return self

    async def __aexit__(self, *exc: Any) -> None:
        await self.close()

    async def _sleep(self, seconds: float) -> None:
        if self._sleep_override is None:
            await asyncio.sleep(seconds)
            return
        result = self._sleep_override(seconds)
        if asyncio.iscoroutine(result):
            await result

    async def _request(
        self,
        op_key: str,
        path_params: Tuple[str, ...] = (),
        params: Optional[Mapping[str, Any]] = None,
        *,
        body: Any = _NOT_GIVEN,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        wire_query: bool = False,
    ) -> Any:
        prepared = self._prepare(
            op_key, path_params, params, body, idempotency_key, timeout, max_retries, wire_query
        )
        attempt = 0
        while True:
            try:
                response = await self._http.request(
                    prepared.method,
                    prepared.url,
                    params=prepared.query,
                    content=prepared.content,
                    headers=prepared.headers,
                    timeout=prepared.timeout,
                )
            except httpx.TransportError as exc:
                if attempt < prepared.max_retries:
                    await self._sleep(self._retry_delay(attempt, None))
                    attempt += 1
                    continue
                raise self._connection_error(exc) from exc
            if attempt < prepared.max_retries and self._should_retry(response):
                await self._sleep(self._retry_delay(attempt, response))
                attempt += 1
                continue
            result = self._interpret(prepared, response)
            if isinstance(result, ListObject):

                def fetch(query: Dict[str, Any]) -> Any:
                    return self._request(
                        op_key, path_params, query, timeout=timeout, max_retries=max_retries, wire_query=True
                    )

                result._bind(fetch, prepared.query or {})
            return result
