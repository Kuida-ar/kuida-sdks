"""Modelos, formas de parámetros y operaciones de la API de Kuida.

GENERADO por scripts/generate.py desde openapi/kuida-v1.json. No editar a mano:
correr ``python3 scripts/generate.py`` después de actualizar el OpenAPI.
"""

# flake8: noqa
from datetime import datetime
from typing import Any, Dict, List, Optional

from .._object import KuidaObject, register_model
from .._pagination import ListObject

API_VERSION = '2026-09-28'


@register_model
class AccountApiKey(KuidaObject):
    """AccountApiKey"""

    name: str
    prefix: str
    scopes: List[str]

    _wire_fields = {
        'name': 's',
        'prefix': 's',
        'scopes': ('list', 's'),
    }


@register_model
class Account(KuidaObject):
    """La organización dueña de la clave de API."""

    OBJECT_NAME = 'account'

    #: Id de la organización.
    id: str
    object: str
    name: str
    #: Tipo de institución (clinic, pharmacy, lab, …).
    type: str
    time_zone: str
    #: Líneas de servicio contratadas: seguimiento de pacientes y red de derivaciones.
    service_lines: List[str]
    #: `true` en producción, `false` en el entorno de pruebas.
    livemode: bool
    api_key: AccountApiKey

    _wire_fields = {
        'id': 's',
        'object': 's',
        'name': 's',
        'type': 's',
        'timeZone': 's',
        'serviceLines': ('list', 's'),
        'livemode': 's',
        'apiKey': ('model', 'AccountApiKey'),
    }


@register_model
class Patient(KuidaObject):
    """Un paciente de la organización."""

    OBJECT_NAME = 'patient'

    #: Id del paciente.
    id: str
    object: str
    full_name: Optional[str]
    #: Teléfono normalizado.
    phone: Optional[str]
    email: Optional[str]
    dni: Optional[str]
    #: Id del paciente en tu sistema.
    external_id: Optional[str]
    #: Fecha de nacimiento (AAAA-MM-DD).
    date_of_birth: Optional[str]
    #: Estado básico del paciente en Kuida.
    stage: str
    #: Si el paciente pidió no recibir mensajes. Kuida no le escribe.
    opted_out: bool
    #: Fecha y hora ISO 8601.
    created_at: datetime
    #: Fecha y hora ISO 8601.
    updated_at: datetime

    _wire_fields = {
        'id': 's',
        'object': 's',
        'fullName': 's',
        'phone': 's',
        'email': 's',
        'dni': 's',
        'externalId': 's',
        'dateOfBirth': 's',
        'stage': 's',
        'optedOut': 's',
        'createdAt': 'dt',
        'updatedAt': 'dt',
    }


@register_model
class PatientList(ListObject[Patient]):
    """Página de resultados. Sigue con `startingAfter` = id del último."""

    OBJECT_NAME = 'list'

    object: str
    data: List[Patient]
    #: Si hay más objetos después de esta página.
    has_more: bool
    url: str

    _wire_fields = {
        'object': 's',
        'data': ('list', ('model', 'Patient')),
        'hasMore': 's',
        'url': 's',
    }


@register_model
class Doctor(KuidaObject):
    """Un profesional del padrón de la organización."""

    OBJECT_NAME = 'doctor'

    #: Id del profesional.
    id: str
    object: str
    full_name: str
    specialty: Optional[str]
    #: Matrícula.
    license_number: Optional[str]
    external_id: Optional[str]
    #: Si atiende hoy. Solo los activos reciben turnos del agente.
    active: bool
    #: Fecha y hora ISO 8601.
    created_at: datetime

    _wire_fields = {
        'id': 's',
        'object': 's',
        'fullName': 's',
        'specialty': 's',
        'licenseNumber': 's',
        'externalId': 's',
        'active': 's',
        'createdAt': 'dt',
    }


@register_model
class DoctorList(ListObject[Doctor]):
    """Página de resultados. Sigue con `startingAfter` = id del último."""

    OBJECT_NAME = 'list'

    object: str
    data: List[Doctor]
    #: Si hay más objetos después de esta página.
    has_more: bool
    url: str

    _wire_fields = {
        'object': 's',
        'data': ('list', ('model', 'Doctor')),
        'hasMore': 's',
        'url': 's',
    }


@register_model
class Appointment(KuidaObject):
    """Un turno. Kuida le manda al paciente los recordatorios que la organización configuró."""

    OBJECT_NAME = 'appointment'

    #: Id del turno.
    id: str
    object: str
    #: Paciente del turno.
    patient: str
    doctor: Optional[str]
    #: Fecha y hora ISO 8601.
    start_at: datetime
    status: str
    #: Tipo de turno o práctica.
    type: Optional[str]
    #: Id del turno en tu sistema.
    external_id: Optional[str]
    #: Por dónde entró: tu API, el conector del sistema de gestión, el agente de Kuida o el equipo.
    source: str
    cancelled_at: Optional[datetime]
    #: Fecha y hora ISO 8601.
    created_at: datetime
    #: Fecha y hora ISO 8601.
    updated_at: datetime

    _wire_fields = {
        'id': 's',
        'object': 's',
        'patient': 's',
        'doctor': 's',
        'startAt': 'dt',
        'status': 's',
        'type': 's',
        'externalId': 's',
        'source': 's',
        'cancelledAt': 'dt',
        'createdAt': 'dt',
        'updatedAt': 'dt',
    }


@register_model
class AppointmentList(ListObject[Appointment]):
    """Página de resultados. Sigue con `startingAfter` = id del último."""

    OBJECT_NAME = 'list'

    object: str
    data: List[Appointment]
    #: Si hay más objetos después de esta página.
    has_more: bool
    url: str

    _wire_fields = {
        'object': 's',
        'data': ('list', ('model', 'Appointment')),
        'hasMore': 's',
        'url': 's',
    }


@register_model
class Visit(KuidaObject):
    """Una consulta atendida. Crearla arranca el seguimiento post-consulta del paciente."""

    OBJECT_NAME = 'visit'

    #: Id de la consulta.
    id: str
    object: str
    #: Paciente atendido.
    patient: str
    doctor: Optional[str]
    appointment: Optional[str]
    #: Fecha y hora ISO 8601.
    visited_at: datetime
    type: Optional[str]
    diagnosis: Optional[str]
    notes: Optional[str]
    external_id: Optional[str]
    source: str
    #: Fecha y hora ISO 8601.
    created_at: datetime

    _wire_fields = {
        'id': 's',
        'object': 's',
        'patient': 's',
        'doctor': 's',
        'appointment': 's',
        'visitedAt': 'dt',
        'type': 's',
        'diagnosis': 's',
        'notes': 's',
        'externalId': 's',
        'source': 's',
        'createdAt': 'dt',
    }


@register_model
class VisitList(ListObject[Visit]):
    """Página de resultados. Sigue con `startingAfter` = id del último."""

    OBJECT_NAME = 'list'

    object: str
    data: List[Visit]
    #: Si hay más objetos después de esta página.
    has_more: bool
    url: str

    _wire_fields = {
        'object': 's',
        'data': ('list', ('model', 'Visit')),
        'hasMore': 's',
        'url': 's',
    }


@register_model
class IntakeRequest(KuidaObject):
    """Una solicitud de ingreso: alguien pide el servicio para un paciente y Kuida completa lo que falta."""

    OBJECT_NAME = 'intake_request'

    #: Id de la solicitud.
    id: str
    object: str
    #: `new` recién llegada · `validating` Kuida está completando datos por WhatsApp · `ready` completa · `loaded` cargada en el sistema de la organización · `discarded` descartada.
    status: str
    patient: Optional[str]
    summary: Optional[str]
    #: Datos que todavía faltan para que la solicitud quede lista.
    missing: List[str]
    #: Los datos de la solicitud tal como los tiene Kuida.
    data: Dict[str, Any]
    discard_reason: Optional[str]
    ready_at: Optional[datetime]
    loaded_at: Optional[datetime]
    closed_at: Optional[datetime]
    #: Fecha y hora ISO 8601.
    created_at: datetime
    #: Fecha y hora ISO 8601.
    updated_at: datetime

    _wire_fields = {
        'id': 's',
        'object': 's',
        'status': 's',
        'patient': 's',
        'summary': 's',
        'missing': ('list', 's'),
        'data': 'free',
        'discardReason': 's',
        'readyAt': 'dt',
        'loadedAt': 'dt',
        'closedAt': 'dt',
        'createdAt': 'dt',
        'updatedAt': 'dt',
    }


@register_model
class IntakeRequestList(ListObject[IntakeRequest]):
    """Página de resultados. Sigue con `startingAfter` = id del último."""

    OBJECT_NAME = 'list'

    object: str
    data: List[IntakeRequest]
    #: Si hay más objetos después de esta página.
    has_more: bool
    url: str

    _wire_fields = {
        'object': 's',
        'data': ('list', ('model', 'IntakeRequest')),
        'hasMore': 's',
        'url': 's',
    }


@register_model
class Treatment(KuidaObject):
    """Un tratamiento indicado al paciente."""

    OBJECT_NAME = 'treatment'

    #: Id del tratamiento.
    id: str
    object: str
    #: Paciente.
    patient: str
    kind: str
    name: str
    dosage: Optional[str]
    frequency: Optional[str]
    instructions: Optional[str]
    start_date: Optional[datetime]
    end_date: Optional[datetime]
    active: bool
    #: Fecha y hora ISO 8601.
    created_at: datetime
    #: Fecha y hora ISO 8601.
    updated_at: datetime

    _wire_fields = {
        'id': 's',
        'object': 's',
        'patient': 's',
        'kind': 's',
        'name': 's',
        'dosage': 's',
        'frequency': 's',
        'instructions': 's',
        'startDate': 'dt',
        'endDate': 'dt',
        'active': 's',
        'createdAt': 'dt',
        'updatedAt': 'dt',
    }


@register_model
class TreatmentList(ListObject[Treatment]):
    """Página de resultados. Sigue con `startingAfter` = id del último."""

    OBJECT_NAME = 'list'

    object: str
    data: List[Treatment]
    #: Si hay más objetos después de esta página.
    has_more: bool
    url: str

    _wire_fields = {
        'object': 's',
        'data': ('list', ('model', 'Treatment')),
        'hasMore': 's',
        'url': 's',
    }


@register_model
class ResultReference(KuidaObject):
    """ResultReference"""

    object: str
    id: str

    _wire_fields = {
        'object': 's',
        'id': 's',
    }


@register_model
class EventResult(KuidaObject):
    """EventResult"""

    #: El id que enviaste.
    id: str
    accepted: bool
    status: str
    #: Id del evento en Kuida (`evt_…`).
    event: Optional[str]
    result: Optional[ResultReference]
    error: Optional[str]

    _wire_fields = {
        'id': 's',
        'accepted': 's',
        'status': 's',
        'event': 's',
        'result': ('model', 'ResultReference'),
        'error': 's',
    }


@register_model
class EventBatchResponse(KuidaObject):
    """Un resultado por evento, en el mismo orden."""

    OBJECT_NAME = 'event_batch'

    object: str
    results: List[EventResult]

    _wire_fields = {
        'object': 's',
        'results': ('list', ('model', 'EventResult')),
    }


@register_model
class Event(KuidaObject):
    """Un evento que entró a Kuida, con su resultado."""

    OBJECT_NAME = 'event'

    #: Id del evento en Kuida.
    id: str
    object: str
    type: str
    #: Por dónde entró.
    source: str
    #: Identidad del evento en su fuente. Para la API, el `id` que enviaste.
    source_ref: str
    #: `processed` Kuida hizo lo suyo · `unhandled` guardado, sin efecto todavía · `failed` falló, reintenta con el mismo id · `duplicate` · `received` en curso.
    status: str
    error: Optional[str]
    #: Lo que produjo el evento (`{ object: "visit", id: "vis_…" }`).
    result: Optional[ResultReference]
    data: Dict[str, Any]
    #: Fecha y hora ISO 8601.
    occurred_at: datetime
    #: Fecha y hora ISO 8601.
    received_at: datetime
    processed_at: Optional[datetime]

    _wire_fields = {
        'id': 's',
        'object': 's',
        'type': 's',
        'source': 's',
        'sourceRef': 's',
        'status': 's',
        'error': 's',
        'result': ('model', 'ResultReference'),
        'data': 'free',
        'occurredAt': 'dt',
        'receivedAt': 'dt',
        'processedAt': 'dt',
    }


@register_model
class EventList(ListObject[Event]):
    """Página de resultados. Sigue con `startingAfter` = id del último."""

    OBJECT_NAME = 'list'

    object: str
    data: List[Event]
    #: Si hay más objetos después de esta página.
    has_more: bool
    url: str

    _wire_fields = {
        'object': 's',
        'data': ('list', ('model', 'Event')),
        'hasMore': 's',
        'url': 's',
    }


@register_model
class WebhookEndpointLastError(KuidaObject):
    """WebhookEndpointLastError"""

    #: Fecha y hora ISO 8601.
    at: datetime
    message: str

    _wire_fields = {
        'at': 'dt',
        'message': 's',
    }


@register_model
class WebhookEndpoint(KuidaObject):
    """Una URL de tu sistema a la que Kuida le avisa lo que pasa."""

    OBJECT_NAME = 'webhook_endpoint'

    #: Id del endpoint.
    id: str
    object: str
    url: str
    description: Optional[str]
    #: Tipos de evento que recibe. `*` = todos.
    enabled_events: List[str]
    status: str
    #: Secreto para verificar la firma (`whsec_…`). Solo viene en la respuesta de creación.
    secret: Optional[str]
    last_delivered_at: Optional[datetime]
    last_error: Optional[WebhookEndpointLastError]
    #: Fecha y hora ISO 8601.
    created_at: datetime

    _wire_fields = {
        'id': 's',
        'object': 's',
        'url': 's',
        'description': 's',
        'enabledEvents': ('list', 's'),
        'status': 's',
        'secret': 's',
        'lastDeliveredAt': 'dt',
        'lastError': ('model', 'WebhookEndpointLastError'),
        'createdAt': 'dt',
    }


@register_model
class WebhookEndpointList(ListObject[WebhookEndpoint]):
    """Página de resultados. Sigue con `startingAfter` = id del último."""

    OBJECT_NAME = 'list'

    object: str
    data: List[WebhookEndpoint]
    #: Si hay más objetos después de esta página.
    has_more: bool
    url: str

    _wire_fields = {
        'object': 's',
        'data': ('list', ('model', 'WebhookEndpoint')),
        'hasMore': 's',
        'url': 's',
    }


@register_model
class DeletedObject(KuidaObject):
    """Confirmación de un borrado."""

    id: str
    object: str
    deleted: bool

    _wire_fields = {
        'id': 's',
        'object': 's',
        'deleted': 's',
    }


@register_model
class WebhookDelivery(KuidaObject):
    """Un envío de webhook con sus reintentos."""

    OBJECT_NAME = 'webhook_delivery'

    #: Id de la entrega.
    id: str
    object: str
    #: Endpoint destino.
    webhook_endpoint: str
    event_type: str
    #: `pending` en curso o esperando reintento · `succeeded` tu endpoint respondió 2xx · `failed` se agotaron los reintentos.
    status: str
    attempts: int
    last_status_code: Optional[int]
    last_error: Optional[str]
    next_attempt_at: Optional[datetime]
    delivered_at: Optional[datetime]
    #: El cuerpo exacto que se envió.
    payload: Dict[str, Any]
    #: Fecha y hora ISO 8601.
    created_at: datetime

    _wire_fields = {
        'id': 's',
        'object': 's',
        'webhookEndpoint': 's',
        'eventType': 's',
        'status': 's',
        'attempts': 's',
        'lastStatusCode': 's',
        'lastError': 's',
        'nextAttemptAt': 'dt',
        'deliveredAt': 'dt',
        'payload': 'free',
        'createdAt': 'dt',
    }


@register_model
class WebhookDeliveryList(ListObject[WebhookDelivery]):
    """Página de resultados. Sigue con `startingAfter` = id del último."""

    OBJECT_NAME = 'list'

    object: str
    data: List[WebhookDelivery]
    #: Si hay más objetos después de esta página.
    has_more: bool
    url: str

    _wire_fields = {
        'object': 's',
        'data': ('list', ('model', 'WebhookDelivery')),
        'hasMore': 's',
        'url': 's',
    }


@register_model
class WebhookEvent(KuidaObject):
    """Lo que Kuida le manda a tu endpoint."""

    OBJECT_NAME = 'event'

    #: Id de la entrega (`whd_…`). Estable entre reintentos: deduplica por este campo.
    id: str
    object: str
    type: str
    api_version: str
    livemode: bool
    #: Cuándo pasó, ISO 8601.
    occurred_at: datetime
    #: Id interno de lo que disparó el evento.
    ref: str
    #: Los datos del hecho.
    data: Dict[str, Any]

    _wire_fields = {
        'id': 's',
        'object': 's',
        'type': 's',
        'apiVersion': 's',
        'livemode': 's',
        'occurredAt': 'dt',
        'ref': 's',
        'data': 'free',
    }


#: Forma de cada esquema de entrada. `s` escalar, `dt` fecha y hora, `d` fecha,
#: `free` mapa libre (sin conversión de claves), ('obj', campos), ('arr', ítem),
#: ('union', ramas), ('ref', esquema).
PARAM_SHAPES: Dict[str, Any] = {
    'AppointmentCancelParams': ('obj', {'reason': 's'}),
    'AppointmentCreateParams': ('obj', {'patient': ('ref', 'PatientReference'), 'doctor': ('ref', 'DoctorReference'), 'startAt': 'dt', 'type': 's', 'externalId': 's'}),
    'AppointmentUpdateParams': ('obj', {'startAt': 'dt', 'doctor': ('ref', 'DoctorReference'), 'type': 's'}),
    'DoctorIdentity': ('obj', {'externalId': 's', 'fullName': 's', 'specialty': 's'}),
    'DoctorReference': ('union', ['s', ('ref', 'DoctorIdentity')]),
    'EventBatchInput': ('union', [('ref', 'EventInput'), ('arr', ('ref', 'EventInput')), ('obj', {'events': ('arr', ('ref', 'EventInput'))})]),
    'EventInput': ('obj', {'id': 's', 'schemaVersion': 's', 'occurredAt': 'dt', 'source': ('obj', {'system': 's', 'version': 's'}), 'type': 's', 'data': 'free'}),
    'IntakeContact': ('obj', {'name': 's', 'relationship': 's', 'phone': 's', 'email': 's'}),
    'IntakeCoverage': ('obj', {'insurer': 's', 'plan': 's', 'memberId': 's'}),
    'IntakeRequestCreateParams': ('obj', {'patient': ('ref', 'PatientIdentity'), 'contacts': ('arr', ('ref', 'IntakeContact')), 'coverage': ('ref', 'IntakeCoverage'), 'address': 's', 'requestedService': 's', 'summary': 's', 'hospitalized': 's', 'sender': ('obj', {'name': 's', 'email': 's', 'organization': 's'})}),
    'PatientCreateParams': ('obj', {'phone': 's', 'fullName': 's', 'dni': 's', 'email': 's', 'externalId': 's', 'dateOfBirth': 'd'}),
    'PatientIdentity': ('obj', {'phone': 's', 'fullName': 's', 'dni': 's', 'email': 's', 'externalId': 's', 'dateOfBirth': 'd'}),
    'PatientReference': ('union', ['s', ('ref', 'PatientIdentity')]),
    'PatientUpdateParams': ('obj', {'fullName': 's', 'email': 's', 'dni': 's', 'externalId': 's', 'dateOfBirth': 'd'}),
    'TreatmentCreateParams': ('obj', {'patient': ('ref', 'PatientReference'), 'name': 's', 'kind': 's', 'dosage': 's', 'frequency': 's', 'instructions': 's', 'startDate': 'dt', 'endDate': 'dt'}),
    'VisitCreateParams': ('obj', {'patient': ('ref', 'PatientReference'), 'doctor': ('ref', 'DoctorReference'), 'visitedAt': 'dt', 'type': 's', 'externalId': 's', 'appointment': 's', 'diagnosis': 's', 'notes': 's'}),
    'WebhookEndpointCreateParams': ('obj', {'url': 's', 'enabledEvents': ('arr', 's'), 'description': 's'}),
    'WebhookEndpointUpdateParams': ('obj', {'url': 's', 'enabledEvents': ('arr', 's'), 'description': 's', 'disabled': 's'}),
}

#: Una entrada por operación del OpenAPI (`x-kuida-resource`.`x-kuida-method`).
OPERATIONS: Dict[str, Dict[str, Any]] = {
    'account.retrieve': {'method': 'GET', 'path': '/v1/account', 'path_params': [], 'query': {}, 'body': None, 'body_required': False, 'response': 'Account', 'summary': 'Obtener la cuenta'},
    'patients.create': {'method': 'POST', 'path': '/v1/patients', 'path_params': [], 'query': {}, 'body': 'PatientCreateParams', 'body_required': True, 'response': 'Patient', 'summary': 'Crear o identificar un paciente'},
    'patients.list': {'method': 'GET', 'path': '/v1/patients', 'path_params': [], 'query': {'limit': 's', 'startingAfter': 's', 'endingBefore': 's', 'phone': 's', 'dni': 's', 'externalId': 's'}, 'body': None, 'body_required': False, 'response': 'PatientList', 'summary': 'Listar pacientes'},
    'patients.retrieve': {'method': 'GET', 'path': '/v1/patients/{id}', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'Patient', 'summary': 'Obtener un paciente'},
    'patients.update': {'method': 'PATCH', 'path': '/v1/patients/{id}', 'path_params': ['id'], 'query': {}, 'body': 'PatientUpdateParams', 'body_required': True, 'response': 'Patient', 'summary': 'Actualizar un paciente'},
    'doctors.list': {'method': 'GET', 'path': '/v1/doctors', 'path_params': [], 'query': {'limit': 's', 'startingAfter': 's', 'endingBefore': 's', 'active': 's', 'externalId': 's'}, 'body': None, 'body_required': False, 'response': 'DoctorList', 'summary': 'Listar profesionales'},
    'doctors.retrieve': {'method': 'GET', 'path': '/v1/doctors/{id}', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'Doctor', 'summary': 'Obtener un profesional'},
    'appointments.create': {'method': 'POST', 'path': '/v1/appointments', 'path_params': [], 'query': {}, 'body': 'AppointmentCreateParams', 'body_required': True, 'response': 'Appointment', 'summary': 'Crear un turno'},
    'appointments.list': {'method': 'GET', 'path': '/v1/appointments', 'path_params': [], 'query': {'limit': 's', 'startingAfter': 's', 'endingBefore': 's', 'patient': 's', 'status': 's', 'externalId': 's', 'startAtGte': 'dt', 'startAtLt': 'dt'}, 'body': None, 'body_required': False, 'response': 'AppointmentList', 'summary': 'Listar turnos'},
    'appointments.retrieve': {'method': 'GET', 'path': '/v1/appointments/{id}', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'Appointment', 'summary': 'Obtener un turno'},
    'appointments.update': {'method': 'PATCH', 'path': '/v1/appointments/{id}', 'path_params': ['id'], 'query': {}, 'body': 'AppointmentUpdateParams', 'body_required': True, 'response': 'Appointment', 'summary': 'Reprogramar o modificar un turno'},
    'appointments.cancel': {'method': 'POST', 'path': '/v1/appointments/{id}/cancel', 'path_params': ['id'], 'query': {}, 'body': 'AppointmentCancelParams', 'body_required': False, 'response': 'Appointment', 'summary': 'Cancelar un turno'},
    'visits.create': {'method': 'POST', 'path': '/v1/visits', 'path_params': [], 'query': {}, 'body': 'VisitCreateParams', 'body_required': True, 'response': 'Visit', 'summary': 'Registrar una consulta atendida'},
    'visits.list': {'method': 'GET', 'path': '/v1/visits', 'path_params': [], 'query': {'limit': 's', 'startingAfter': 's', 'endingBefore': 's', 'patient': 's', 'externalId': 's'}, 'body': None, 'body_required': False, 'response': 'VisitList', 'summary': 'Listar consultas'},
    'visits.retrieve': {'method': 'GET', 'path': '/v1/visits/{id}', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'Visit', 'summary': 'Obtener una consulta'},
    'intake_requests.create': {'method': 'POST', 'path': '/v1/intake_requests', 'path_params': [], 'query': {}, 'body': 'IntakeRequestCreateParams', 'body_required': True, 'response': 'IntakeRequest', 'summary': 'Crear una solicitud de ingreso'},
    'intake_requests.list': {'method': 'GET', 'path': '/v1/intake_requests', 'path_params': [], 'query': {'limit': 's', 'startingAfter': 's', 'endingBefore': 's', 'status': 's', 'patient': 's'}, 'body': None, 'body_required': False, 'response': 'IntakeRequestList', 'summary': 'Listar solicitudes de ingreso'},
    'intake_requests.retrieve': {'method': 'GET', 'path': '/v1/intake_requests/{id}', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'IntakeRequest', 'summary': 'Obtener una solicitud de ingreso'},
    'treatments.create': {'method': 'POST', 'path': '/v1/treatments', 'path_params': [], 'query': {}, 'body': 'TreatmentCreateParams', 'body_required': True, 'response': 'Treatment', 'summary': 'Registrar un tratamiento'},
    'treatments.list': {'method': 'GET', 'path': '/v1/treatments', 'path_params': [], 'query': {'limit': 's', 'startingAfter': 's', 'endingBefore': 's', 'patient': 's', 'active': 's'}, 'body': None, 'body_required': False, 'response': 'TreatmentList', 'summary': 'Listar tratamientos'},
    'treatments.retrieve': {'method': 'GET', 'path': '/v1/treatments/{id}', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'Treatment', 'summary': 'Obtener un tratamiento'},
    'events.create': {'method': 'POST', 'path': '/v1/events', 'path_params': [], 'query': {}, 'body': 'EventBatchInput', 'body_required': True, 'response': 'EventBatchResponse', 'summary': 'Enviar eventos'},
    'events.list': {'method': 'GET', 'path': '/v1/events', 'path_params': [], 'query': {'limit': 's', 'startingAfter': 's', 'endingBefore': 's', 'type': 's', 'status': 's', 'source': 's'}, 'body': None, 'body_required': False, 'response': 'EventList', 'summary': 'Listar eventos'},
    'events.retrieve': {'method': 'GET', 'path': '/v1/events/{id}', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'Event', 'summary': 'Obtener un evento'},
    'webhook_endpoints.create': {'method': 'POST', 'path': '/v1/webhook_endpoints', 'path_params': [], 'query': {}, 'body': 'WebhookEndpointCreateParams', 'body_required': True, 'response': 'WebhookEndpoint', 'summary': 'Crear un endpoint de webhooks'},
    'webhook_endpoints.list': {'method': 'GET', 'path': '/v1/webhook_endpoints', 'path_params': [], 'query': {'limit': 's', 'startingAfter': 's', 'endingBefore': 's'}, 'body': None, 'body_required': False, 'response': 'WebhookEndpointList', 'summary': 'Listar endpoints de webhooks'},
    'webhook_endpoints.retrieve': {'method': 'GET', 'path': '/v1/webhook_endpoints/{id}', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'WebhookEndpoint', 'summary': 'Obtener un endpoint de webhooks'},
    'webhook_endpoints.update': {'method': 'PATCH', 'path': '/v1/webhook_endpoints/{id}', 'path_params': ['id'], 'query': {}, 'body': 'WebhookEndpointUpdateParams', 'body_required': True, 'response': 'WebhookEndpoint', 'summary': 'Actualizar un endpoint de webhooks'},
    'webhook_endpoints.delete': {'method': 'DELETE', 'path': '/v1/webhook_endpoints/{id}', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'DeletedObject', 'summary': 'Borrar un endpoint de webhooks'},
    'webhook_endpoints.ping': {'method': 'POST', 'path': '/v1/webhook_endpoints/{id}/ping', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'WebhookDelivery', 'summary': 'Mandar un evento de prueba'},
    'webhook_deliveries.list': {'method': 'GET', 'path': '/v1/webhook_deliveries', 'path_params': [], 'query': {'limit': 's', 'startingAfter': 's', 'endingBefore': 's', 'webhookEndpoint': 's', 'status': 's'}, 'body': None, 'body_required': False, 'response': 'WebhookDeliveryList', 'summary': 'Listar entregas de webhooks'},
    'webhook_deliveries.retrieve': {'method': 'GET', 'path': '/v1/webhook_deliveries/{id}', 'path_params': ['id'], 'query': {}, 'body': None, 'body_required': False, 'response': 'WebhookDelivery', 'summary': 'Obtener una entrega de webhook'},
}

__all__ = [
    'AccountApiKey',
    'Account',
    'Patient',
    'PatientList',
    'Doctor',
    'DoctorList',
    'Appointment',
    'AppointmentList',
    'Visit',
    'VisitList',
    'IntakeRequest',
    'IntakeRequestList',
    'Treatment',
    'TreatmentList',
    'ResultReference',
    'EventResult',
    'EventBatchResponse',
    'Event',
    'EventList',
    'WebhookEndpointLastError',
    'WebhookEndpoint',
    'WebhookEndpointList',
    'DeletedObject',
    'WebhookDelivery',
    'WebhookDeliveryList',
    'WebhookEvent',
]
