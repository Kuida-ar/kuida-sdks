"""Recursos de la API: un método por operación del OpenAPI (32 en total).

Convenciones de todos los métodos:

* Los parámetros van como kwargs en ``snake_case`` (``full_name=``, ``start_at=``) o como un
  dict posicional; en el dict se aceptan también las claves del cable (``{"fullName": …}``).
  Solo se convierten a camelCase las claves que el esquema conoce; los mapas libres (el
  ``data`` de un evento) viajan tal cual. Los valores ``None`` se omiten.
* Las fechas aceptan ``datetime`` (un ``datetime`` sin zona se toma como hora local) o texto
  ISO 8601.
* Opciones por pedido, siempre por nombre: ``idempotency_key=`` (solo POST), ``timeout=`` y
  ``max_retries=``.

Las clases ``Async*`` tienen la misma superficie con métodos ``async``.
"""

from typing import TYPE_CHECKING, Any, Dict, Iterable, Mapping, Optional, Union

from ._generated.models import (
    Account as AccountObject,
    Appointment,
    AppointmentList,
    DeletedObject,
    Doctor,
    DoctorList,
    Event,
    EventBatchResponse,
    EventList,
    IntakeRequest,
    IntakeRequestList,
    Patient,
    PatientList,
    Treatment,
    TreatmentList,
    Visit,
    VisitList,
    WebhookDelivery,
    WebhookDeliveryList,
    WebhookEndpoint,
    WebhookEndpointList,
)
from ._params import body_params, merge

if TYPE_CHECKING:  # pragma: no cover
    from ._client import AsyncKuida, Kuida

Params = Optional[Mapping[str, Any]]


class _Resource:
    def __init__(self, client: "Union[Kuida, AsyncKuida]") -> None:
        self._client = client

    def _call(
        self,
        op: str,
        *path_params: str,
        params: Params = None,
        body: Any = None,
        has_body: bool = False,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
    ) -> Any:
        extra: Dict[str, Any] = {"body": body} if has_body else {}
        return self._client._request(
            op,
            tuple(path_params),
            params,
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
            **extra,
        )


# ─── account ─────────────────────────────────────────────────────────────────


class Account(_Resource):
    """La organización dueña de la clave (``GET /v1/account``)."""

    def retrieve(self, *, timeout: Optional[float] = None, max_retries: Optional[int] = None) -> AccountObject:
        """Devuelve la organización y los datos de la clave usada (nombre, prefijo, scopes).

        Sirve para comprobar que la clave funciona y qué permisos tiene.
        """
        return self._call("account.retrieve", timeout=timeout, max_retries=max_retries)


# ─── patients ────────────────────────────────────────────────────────────────


class Patients(_Resource):
    """Pacientes (``/v1/patients``)."""

    def create(
        self,
        params: Params = None,
        /,
        *,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> Patient:
        """Da de alta un paciente o identifica al existente, sin contactarlo.

        Si ya hay uno con ese teléfono (o DNI) devuelve el existente y completa los datos que
        faltaban, sin pisar lo cargado.

        Args:
            phone: teléfono (obligatorio), p. ej. ``"+54 9 342 555 0000"``.
            full_name, dni, email, external_id: datos opcionales.
            date_of_birth: ``date`` o texto ``AAAA-MM-DD``.
        """
        return self._call(
            "patients.create",
            params=merge(params, kwargs),
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
        )

    def retrieve(
        self, id: str, /, *, timeout: Optional[float] = None, max_retries: Optional[int] = None
    ) -> Patient:
        """Obtiene un paciente por su id (``pat_…``)."""
        return self._call("patients.retrieve", id, timeout=timeout, max_retries=max_retries)

    def update(
        self,
        id: str,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> Patient:
        """Actualiza datos de un paciente (``full_name``, ``email``, ``dni``, ``external_id``,
        ``date_of_birth``). El teléfono no se cambia por acá."""
        return self._call(
            "patients.update", id, params=merge(params, kwargs), timeout=timeout, max_retries=max_retries
        )

    def list(
        self,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> PatientList:
        """Lista pacientes, del más nuevo al más viejo.

        Filtros: ``phone``, ``dni``, ``external_id``. Paginación: ``limit`` (1-100, default 10),
        ``starting_after``, ``ending_before``.
        """
        return self._call("patients.list", params=merge(params, kwargs), timeout=timeout, max_retries=max_retries)


# ─── doctors ─────────────────────────────────────────────────────────────────


class Doctors(_Resource):
    """Profesionales de la organización (``/v1/doctors``, solo lectura)."""

    def list(
        self,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> DoctorList:
        """Lista profesionales. Filtros: ``active`` (``bool``), ``external_id``."""
        return self._call("doctors.list", params=merge(params, kwargs), timeout=timeout, max_retries=max_retries)

    def retrieve(self, id: str, /, *, timeout: Optional[float] = None, max_retries: Optional[int] = None) -> Doctor:
        """Obtiene un profesional por su id (``doc_…``)."""
        return self._call("doctors.retrieve", id, timeout=timeout, max_retries=max_retries)


# ─── appointments ────────────────────────────────────────────────────────────


class Appointments(_Resource):
    """Turnos (``/v1/appointments``)."""

    def create(
        self,
        params: Params = None,
        /,
        *,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> Appointment:
        """Crea un turno. Con ``external_id`` repetido devuelve el existente.

        Args:
            patient: id ``pat_…``, un :class:`Patient` o un dict de identidad
                (``{"phone": …, "full_name": …}``).
            doctor: id ``doc_…`` o dict de identidad (``{"external_id": …, "full_name": …}``).
            start_at: ``datetime`` o texto ISO 8601 con zona.
            type, external_id: opcionales.
        """
        return self._call(
            "appointments.create",
            params=merge(params, kwargs),
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
        )

    def retrieve(
        self, id: str, /, *, timeout: Optional[float] = None, max_retries: Optional[int] = None
    ) -> Appointment:
        """Obtiene un turno por su id (``apt_…``)."""
        return self._call("appointments.retrieve", id, timeout=timeout, max_retries=max_retries)

    def update(
        self,
        id: str,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> Appointment:
        """Reprograma o modifica un turno (``start_at``, ``doctor``, ``type``).

        Un turno cancelado o atendido no se puede cambiar (:class:`~kuida.errors.InvalidRequestError`).
        """
        return self._call(
            "appointments.update", id, params=merge(params, kwargs), timeout=timeout, max_retries=max_retries
        )

    def cancel(
        self,
        id: str,
        params: Params = None,
        /,
        *,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> Appointment:
        """Cancela un turno. ``reason`` es opcional; sin parámetros se manda ``{}``."""
        return self._call(
            "appointments.cancel",
            id,
            params=merge(params, kwargs),
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
        )

    def list(
        self,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> AppointmentList:
        """Lista turnos. Filtros: ``patient``, ``status``, ``external_id``, ``start_at_gte`` y
        ``start_at_lt`` (``datetime`` o ISO 8601)."""
        return self._call(
            "appointments.list", params=merge(params, kwargs), timeout=timeout, max_retries=max_retries
        )


# ─── visits ──────────────────────────────────────────────────────────────────


class Visits(_Resource):
    """Consultas atendidas (``/v1/visits``)."""

    def create(
        self,
        params: Params = None,
        /,
        *,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> Visit:
        """Registra una consulta atendida; dispara el seguimiento post-consulta.

        Args:
            patient: id ``pat_…``, :class:`Patient` o dict de identidad.
            visited_at: ``datetime`` o ISO 8601.
            appointment: id ``apt_…`` o el ``external_id`` del turno; el turno queda ``completed``.
            doctor, type, diagnosis, notes, external_id: opcionales.
        """
        return self._call(
            "visits.create",
            params=merge(params, kwargs),
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
        )

    def retrieve(self, id: str, /, *, timeout: Optional[float] = None, max_retries: Optional[int] = None) -> Visit:
        """Obtiene una consulta por su id (``vis_…``)."""
        return self._call("visits.retrieve", id, timeout=timeout, max_retries=max_retries)

    def list(
        self,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> VisitList:
        """Lista consultas. Filtros: ``patient``, ``external_id``."""
        return self._call("visits.list", params=merge(params, kwargs), timeout=timeout, max_retries=max_retries)


# ─── intake_requests ─────────────────────────────────────────────────────────


class IntakeRequests(_Resource):
    """Solicitudes de ingreso (``/v1/intake_requests``)."""

    def create(
        self,
        params: Params = None,
        /,
        *,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> IntakeRequest:
        """Crea una solicitud de ingreso; Kuida completa por WhatsApp los datos que faltan.

        Args:
            patient: dict de identidad (``phone``, ``full_name``, ``dni``, …). Según el OpenAPI
                es un objeto, no un id ``pat_…``.
            contacts: lista de referentes (``name``, ``relationship``, ``phone``, ``email``).
            coverage: ``{"insurer": …, "plan": …, "member_id": …}``.
            address, requested_service, summary, hospitalized, sender: opcionales.
        """
        return self._call(
            "intake_requests.create",
            params=merge(params, kwargs),
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
        )

    def retrieve(
        self, id: str, /, *, timeout: Optional[float] = None, max_retries: Optional[int] = None
    ) -> IntakeRequest:
        """Obtiene una solicitud de ingreso por su id (``int_…``)."""
        return self._call("intake_requests.retrieve", id, timeout=timeout, max_retries=max_retries)

    def list(
        self,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> IntakeRequestList:
        """Lista solicitudes de ingreso. Filtros: ``status``, ``patient``."""
        return self._call(
            "intake_requests.list", params=merge(params, kwargs), timeout=timeout, max_retries=max_retries
        )


# ─── treatments ──────────────────────────────────────────────────────────────


class Treatments(_Resource):
    """Tratamientos indicados (``/v1/treatments``)."""

    def create(
        self,
        params: Params = None,
        /,
        *,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> Treatment:
        """Registra un tratamiento.

        Args:
            patient: id ``pat_…``, :class:`Patient` o dict de identidad.
            name: obligatorio (p. ej. ``"Enalapril 10 mg"``).
            kind, dosage, frequency, instructions: opcionales.
            start_date, end_date: ``datetime`` o ISO 8601.
        """
        return self._call(
            "treatments.create",
            params=merge(params, kwargs),
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
        )

    def retrieve(
        self, id: str, /, *, timeout: Optional[float] = None, max_retries: Optional[int] = None
    ) -> Treatment:
        """Obtiene un tratamiento por su id (``trt_…``)."""
        return self._call("treatments.retrieve", id, timeout=timeout, max_retries=max_retries)

    def list(
        self,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> TreatmentList:
        """Lista tratamientos. Filtros: ``patient``, ``active`` (``bool``)."""
        return self._call(
            "treatments.list", params=merge(params, kwargs), timeout=timeout, max_retries=max_retries
        )


# ─── events ──────────────────────────────────────────────────────────────────


def _event_body(event: Params, kwargs: Mapping[str, Any]) -> Dict[str, Any]:
    return body_params("EventInput", merge(event, kwargs))


class Events(_Resource):
    """Eventos del catálogo v1 (``/v1/events``): la vía genérica para mandar hechos."""

    def create(
        self,
        event: Params = None,
        /,
        *,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> EventBatchResponse:
        """Manda un evento (un solo sobre).

        El evento es un dict o kwargs: ``id`` (tu id, para deduplicar), ``type``
        (``"visit.completed"``, …), ``data`` (mapa libre: sus claves viajan tal cual),
        ``occurred_at``, ``source`` y ``schema_version`` opcionales.

        Devuelve el :class:`EventBatchResponse` con un resultado (``processed``, ``duplicate``,
        ``unhandled``, ``invalid`` o ``failed``). Si ningún evento entra, la API responde el error
        normal (400 o 500): el SDK lanza la excepción y el detalle queda en
        ``err.json_body["results"]``.
        """
        return self._call(
            "events.create",
            body=_event_body(event, kwargs),
            has_body=True,
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
        )

    def create_batch(
        self,
        events: Iterable[Mapping[str, Any]],
        /,
        *,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
    ) -> EventBatchResponse:
        """Manda un lote de hasta 100 eventos. Devuelve un resultado por evento, en orden.

        Si al menos uno entra, la respuesta es 200 con todos los resultados. Si ninguno entra,
        lanza :class:`~kuida.errors.InvalidRequestError` (o :class:`~kuida.errors.APIError`) y los
        resultados quedan en ``err.json_body["results"]``.
        """
        body = {"events": [_event_body(e, {}) for e in events]}
        return self._call(
            "events.create",
            body=body,
            has_body=True,
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
        )

    def retrieve(self, id: str, /, *, timeout: Optional[float] = None, max_retries: Optional[int] = None) -> Event:
        """Obtiene un evento recibido por su id (``evt_…``)."""
        return self._call("events.retrieve", id, timeout=timeout, max_retries=max_retries)

    def list(
        self,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> EventList:
        """Lista eventos recibidos. Filtros: ``type``, ``status``, ``source``."""
        return self._call("events.list", params=merge(params, kwargs), timeout=timeout, max_retries=max_retries)


# ─── webhook_endpoints ───────────────────────────────────────────────────────


class WebhookEndpoints(_Resource):
    """Endpoints de webhooks (``/v1/webhook_endpoints``)."""

    def create(
        self,
        params: Params = None,
        /,
        *,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> WebhookEndpoint:
        """Registra una URL ``https://`` para recibir eventos.

        Args:
            url: URL ``https://`` de tu sistema.
            enabled_events: lista de tipos (``["intake.ready"]``) o ``["*"]``.
            description: opcional.

        La respuesta trae ``secret`` (``whsec_…``): guardalo, no se vuelve a mostrar.
        """
        return self._call(
            "webhook_endpoints.create",
            params=merge(params, kwargs),
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
        )

    def retrieve(
        self, id: str, /, *, timeout: Optional[float] = None, max_retries: Optional[int] = None
    ) -> WebhookEndpoint:
        """Obtiene un endpoint por su id (``we_…``). No incluye el ``secret``."""
        return self._call("webhook_endpoints.retrieve", id, timeout=timeout, max_retries=max_retries)

    def update(
        self,
        id: str,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> WebhookEndpoint:
        """Modifica ``url``, ``enabled_events``, ``description`` o ``disabled`` (``bool``)."""
        return self._call(
            "webhook_endpoints.update", id, params=merge(params, kwargs), timeout=timeout, max_retries=max_retries
        )

    def delete(
        self, id: str, /, *, timeout: Optional[float] = None, max_retries: Optional[int] = None
    ) -> DeletedObject:
        """Borra un endpoint y sus entregas pendientes. Devuelve ``{"deleted": true}``."""
        return self._call("webhook_endpoints.delete", id, timeout=timeout, max_retries=max_retries)

    def list(
        self,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> WebhookEndpointList:
        """Lista los endpoints de la organización."""
        return self._call(
            "webhook_endpoints.list", params=merge(params, kwargs), timeout=timeout, max_retries=max_retries
        )

    def ping(
        self,
        id: str,
        /,
        *,
        idempotency_key: Optional[str] = None,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
    ) -> WebhookDelivery:
        """Encola un evento ``webhook.ping`` de prueba. Devuelve la entrega (``pending``)."""
        return self._call(
            "webhook_endpoints.ping",
            id,
            body={},
            has_body=True,
            idempotency_key=idempotency_key,
            timeout=timeout,
            max_retries=max_retries,
        )


# ─── webhook_deliveries ──────────────────────────────────────────────────────


class WebhookDeliveries(_Resource):
    """Entregas de webhooks (``/v1/webhook_deliveries``), para auditar y depurar."""

    def list(
        self,
        params: Params = None,
        /,
        *,
        timeout: Optional[float] = None,
        max_retries: Optional[int] = None,
        **kwargs: Any,
    ) -> WebhookDeliveryList:
        """Lista entregas. Filtros: ``webhook_endpoint`` (id ``we_…``), ``status``."""
        return self._call(
            "webhook_deliveries.list", params=merge(params, kwargs), timeout=timeout, max_retries=max_retries
        )

    def retrieve(
        self, id: str, /, *, timeout: Optional[float] = None, max_retries: Optional[int] = None
    ) -> WebhookDelivery:
        """Obtiene una entrega por su id (``whd_…``), con el ``payload`` enviado."""
        return self._call("webhook_deliveries.retrieve", id, timeout=timeout, max_retries=max_retries)


# ─── variantes async ─────────────────────────────────────────────────────────
# Los métodos de arriba devuelven lo que devuelva `client._request`; con AsyncKuida eso es
# una corrutina. Estas subclases solo declaran la firma `async` para el tipado y el IDE.


class AsyncAccount(Account):
    async def retrieve(self, **kwargs: Any) -> AccountObject:  # type: ignore[override]
        return await super().retrieve(**kwargs)  # type: ignore[attr-defined,misc]


class AsyncPatients(Patients):
    async def create(self, params: Params = None, /, **kwargs: Any) -> Patient:  # type: ignore[override]
        return await super().create(params, **kwargs)  # type: ignore[attr-defined,misc]

    async def retrieve(self, id: str, /, **kwargs: Any) -> Patient:  # type: ignore[override]
        return await super().retrieve(id, **kwargs)  # type: ignore[attr-defined,misc]

    async def update(self, id: str, params: Params = None, /, **kwargs: Any) -> Patient:  # type: ignore[override]
        return await super().update(id, params, **kwargs)  # type: ignore[attr-defined,misc]

    async def list(self, params: Params = None, /, **kwargs: Any) -> PatientList:  # type: ignore[override]
        return await super().list(params, **kwargs)  # type: ignore[attr-defined,misc]


class AsyncDoctors(Doctors):
    async def list(self, params: Params = None, /, **kwargs: Any) -> DoctorList:  # type: ignore[override]
        return await super().list(params, **kwargs)  # type: ignore[attr-defined,misc]

    async def retrieve(self, id: str, /, **kwargs: Any) -> Doctor:  # type: ignore[override]
        return await super().retrieve(id, **kwargs)  # type: ignore[attr-defined,misc]


class AsyncAppointments(Appointments):
    async def create(self, params: Params = None, /, **kwargs: Any) -> Appointment:  # type: ignore[override]
        return await super().create(params, **kwargs)  # type: ignore[attr-defined,misc]

    async def retrieve(self, id: str, /, **kwargs: Any) -> Appointment:  # type: ignore[override]
        return await super().retrieve(id, **kwargs)  # type: ignore[attr-defined,misc]

    async def update(self, id: str, params: Params = None, /, **kwargs: Any) -> Appointment:  # type: ignore[override]
        return await super().update(id, params, **kwargs)  # type: ignore[attr-defined,misc]

    async def cancel(self, id: str, params: Params = None, /, **kwargs: Any) -> Appointment:  # type: ignore[override]
        return await super().cancel(id, params, **kwargs)  # type: ignore[attr-defined,misc]

    async def list(self, params: Params = None, /, **kwargs: Any) -> AppointmentList:  # type: ignore[override]
        return await super().list(params, **kwargs)  # type: ignore[attr-defined,misc]


class AsyncVisits(Visits):
    async def create(self, params: Params = None, /, **kwargs: Any) -> Visit:  # type: ignore[override]
        return await super().create(params, **kwargs)  # type: ignore[attr-defined,misc]

    async def retrieve(self, id: str, /, **kwargs: Any) -> Visit:  # type: ignore[override]
        return await super().retrieve(id, **kwargs)  # type: ignore[attr-defined,misc]

    async def list(self, params: Params = None, /, **kwargs: Any) -> VisitList:  # type: ignore[override]
        return await super().list(params, **kwargs)  # type: ignore[attr-defined,misc]


class AsyncIntakeRequests(IntakeRequests):
    async def create(self, params: Params = None, /, **kwargs: Any) -> IntakeRequest:  # type: ignore[override]
        return await super().create(params, **kwargs)  # type: ignore[attr-defined,misc]

    async def retrieve(self, id: str, /, **kwargs: Any) -> IntakeRequest:  # type: ignore[override]
        return await super().retrieve(id, **kwargs)  # type: ignore[attr-defined,misc]

    async def list(self, params: Params = None, /, **kwargs: Any) -> IntakeRequestList:  # type: ignore[override]
        return await super().list(params, **kwargs)  # type: ignore[attr-defined,misc]


class AsyncTreatments(Treatments):
    async def create(self, params: Params = None, /, **kwargs: Any) -> Treatment:  # type: ignore[override]
        return await super().create(params, **kwargs)  # type: ignore[attr-defined,misc]

    async def retrieve(self, id: str, /, **kwargs: Any) -> Treatment:  # type: ignore[override]
        return await super().retrieve(id, **kwargs)  # type: ignore[attr-defined,misc]

    async def list(self, params: Params = None, /, **kwargs: Any) -> TreatmentList:  # type: ignore[override]
        return await super().list(params, **kwargs)  # type: ignore[attr-defined,misc]


class AsyncEvents(Events):
    async def create(self, event: Params = None, /, **kwargs: Any) -> EventBatchResponse:  # type: ignore[override]
        return await super().create(event, **kwargs)  # type: ignore[attr-defined,misc]

    async def create_batch(  # type: ignore[override]
        self, events: Iterable[Mapping[str, Any]], /, **kwargs: Any
    ) -> EventBatchResponse:
        return await super().create_batch(events, **kwargs)  # type: ignore[attr-defined,misc]

    async def retrieve(self, id: str, /, **kwargs: Any) -> Event:  # type: ignore[override]
        return await super().retrieve(id, **kwargs)  # type: ignore[attr-defined,misc]

    async def list(self, params: Params = None, /, **kwargs: Any) -> EventList:  # type: ignore[override]
        return await super().list(params, **kwargs)  # type: ignore[attr-defined,misc]


class AsyncWebhookEndpoints(WebhookEndpoints):
    async def create(self, params: Params = None, /, **kwargs: Any) -> WebhookEndpoint:  # type: ignore[override]
        return await super().create(params, **kwargs)  # type: ignore[attr-defined,misc]

    async def retrieve(self, id: str, /, **kwargs: Any) -> WebhookEndpoint:  # type: ignore[override]
        return await super().retrieve(id, **kwargs)  # type: ignore[attr-defined,misc]

    async def update(self, id: str, params: Params = None, /, **kwargs: Any) -> WebhookEndpoint:  # type: ignore[override]
        return await super().update(id, params, **kwargs)  # type: ignore[attr-defined,misc]

    async def delete(self, id: str, /, **kwargs: Any) -> DeletedObject:  # type: ignore[override]
        return await super().delete(id, **kwargs)  # type: ignore[attr-defined,misc]

    async def list(self, params: Params = None, /, **kwargs: Any) -> WebhookEndpointList:  # type: ignore[override]
        return await super().list(params, **kwargs)  # type: ignore[attr-defined,misc]

    async def ping(self, id: str, /, **kwargs: Any) -> WebhookDelivery:  # type: ignore[override]
        return await super().ping(id, **kwargs)  # type: ignore[attr-defined,misc]


class AsyncWebhookDeliveries(WebhookDeliveries):
    async def list(self, params: Params = None, /, **kwargs: Any) -> WebhookDeliveryList:  # type: ignore[override]
        return await super().list(params, **kwargs)  # type: ignore[attr-defined,misc]

    async def retrieve(self, id: str, /, **kwargs: Any) -> WebhookDelivery:  # type: ignore[override]
        return await super().retrieve(id, **kwargs)  # type: ignore[attr-defined,misc]
