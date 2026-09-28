# Kuida para Python

SDK oficial de la API de [Kuida](https://www.kuida.ar/desarrolladores): pacientes, turnos, consultas, solicitudes de ingreso, tratamientos, eventos y webhooks.

- Python 3.9 o superior. Única dependencia: [`httpx`](https://www.python-httpx.org/).
- Cliente sincrónico (`kuida.Kuida`) y asyncio (`kuida.AsyncKuida`) con la misma superficie.
- Idempotencia automática en todos los `POST`, reintentos con espera exponencial y errores tipados.
- Tipado completo (`py.typed`); los modelos se generan desde el OpenAPI de la API.

## Instalación

```bash
pip install kuida
```

## Configuración

```python
import kuida

client = kuida.Kuida(api_key="kd_test_…")
```

Si no se pasa `api_key`, el SDK la lee de la variable de entorno `KUIDA_API_KEY`. Sin clave, el constructor lanza `kuida.AuthenticationError` (no se espera al primer pedido).

| Parámetro | Default | Descripción |
|---|---|---|
| `api_key` | `KUIDA_API_KEY` | Clave de API (`kd_live_…` en producción, `kd_test_…` en pruebas). |
| `base_url` | `KUIDA_API_BASE` o `https://www.kuida.ar/api` | URL base de la API. |
| `timeout` | `30` | Segundos por intento. |
| `max_retries` | `2` | Reintentos automáticos (hasta 3 intentos). `0` los desactiva. |
| `api_version` | `2026-09-28` | Versión de la API que se manda en `Kuida-Version`. |
| `http_client` | uno propio | Un `httpx.Client` (o `httpx.AsyncClient`) para proxies, certificados o transportes de prueba. |

El cliente es reutilizable y seguro entre hilos: creá uno por proceso. Se puede usar como context manager (`with kuida.Kuida(...) as client:`) para cerrar las conexiones al terminar.

### Opciones por pedido

Todos los métodos aceptan al final, por nombre, `idempotency_key=`, `timeout=` y `max_retries=`:

```python
client.patients.list(limit=100, timeout=60, max_retries=5)
```

## Parámetros y respuestas

Los parámetros van como kwargs en `snake_case`. El SDK los convierte a las claves camelCase del cable **solo cuando el esquema de la API las conoce**; las claves desconocidas se mandan tal cual y los valores `None` se omiten. También se puede pasar un `dict` como primer argumento, con claves `snake_case` o las del cable:

```python
client.patients.create(phone="+54 9 342 555 0000", full_name="Paciente Demo")
client.patients.create({"phone": "+54 9 342 555 0000", "fullName": "Paciente Demo"})
```

Las fechas aceptan `datetime` o texto ISO 8601. Un `datetime` sin zona horaria se interpreta como hora local del proceso (igual que `datetime.astimezone()`); para evitar ambigüedades, pasá siempre la zona.

Las respuestas son objetos con atributos `snake_case` y acceso por la clave del cable:

```python
patient.full_name            # "Paciente Demo"
patient["fullName"]          # "Paciente Demo"
patient.created_at           # datetime con zona
patient.raw                  # el JSON crudo, tal como llegó
patient.last_response.request_id
```

Los mapas libres (`data` de eventos y solicitudes de ingreso, `payload` de las entregas de webhooks) quedan como `dict` sin convertir. Los campos nuevos que agregue la API se conservan en `raw` y por clave, sin error.

## Ejemplos por recurso

Todos los ejemplos usan datos sintéticos.

### Cuenta

```python
account = client.account.retrieve()
print(account.name, account.api_key.scopes)
```

### Pacientes

```python
patient = client.patients.create(
    phone="+54 9 342 555 0000",
    full_name="Paciente Demo",
    dni="11111111",
    external_id="HC-1234",
)
patient = client.patients.retrieve(patient.id)
patient = client.patients.update(patient.id, email="paciente.demo@example.com")
page = client.patients.list(phone="+54 9 342 555 0000")
```

`create` no contacta al paciente: si ya existe uno con ese teléfono (o DNI), devuelve el existente y completa los datos que le faltaban.

### Profesionales

```python
for doctor in client.doctors.list(active=True):
    print(doctor.full_name, doctor.specialty)
doctor = client.doctors.retrieve("doc_…")
```

### Turnos

El paciente y el profesional se pueden indicar por id (`pat_…`, `doc_…`), con un objeto devuelto por el SDK o por identidad:

```python
from datetime import datetime, timedelta, timezone

ART = timezone(timedelta(hours=-3))

appointment = client.appointments.create(
    patient={"phone": "+54 9 342 555 0000", "full_name": "Paciente Demo"},
    doctor={"external_id": "MED-12", "full_name": "Dra. Demo"},
    start_at=datetime(2026, 10, 5, 10, 30, tzinfo=ART),
    external_id="turno-123",
)
client.appointments.update(appointment.id, start_at=datetime(2026, 10, 6, 11, 0, tzinfo=ART))
client.appointments.cancel(appointment.id, reason="El paciente pidió cancelar")
client.appointments.list(start_at_gte=datetime(2026, 10, 1, tzinfo=ART), status="confirmed")
```

### Consultas

```python
visit = client.visits.create(
    patient=patient.id,
    visited_at=datetime(2026, 10, 5, 10, 45, tzinfo=ART),
    appointment="turno-123",          # id apt_… o el external_id del turno
    diagnosis="Control",
)
client.visits.retrieve(visit.id)
client.visits.list(patient=patient.id)
```

Registrar la consulta marca el turno como `completed` y dispara el seguimiento.

### Solicitudes de ingreso

El paciente va por identidad (puede no existir todavía en Kuida):

```python
intake = client.intake_requests.create(
    patient={"full_name": "Paciente Demo", "dni": "11111111"},
    contacts=[{"name": "Familiar Demo", "relationship": "hija", "phone": "+54 9 342 555 0001"}],
    coverage={"insurer": "Obra Social Demo", "member_id": "0001"},
    requested_service="Internación domiciliaria",
)
print(intake.status, intake.missing)
client.intake_requests.list(status="ready")
```

### Tratamientos

```python
treatment = client.treatments.create(
    patient=patient.id,
    name="Enalapril 10 mg",
    dosage="1 comprimido",
    frequency="cada 12 horas",
    start_date=datetime(2026, 10, 5, tzinfo=ART),
)
client.treatments.list(patient=patient.id, active=True)
```

### Eventos

Un evento es un dict con `id` (tu id, para deduplicar), `type`, `data` y opcionalmente `occurred_at`, `source` y `schema_version`. Las claves de `data` viajan tal cual, en camelCase como las define el catálogo:

```python
event = {
    "id": "turno-123-atendido",
    "type": "visit.completed",
    "occurred_at": datetime(2026, 10, 5, 10, 45, tzinfo=ART),
    "source": {"system": "mi-sistema", "version": "3.2"},
    "data": {
        "patient": {"phone": "+54 9 342 555 0000", "fullName": "Paciente Demo"},
        "visit": {"visitedAt": "2026-10-05T10:45:00-03:00", "diagnosis": "Control"},
    },
}
result = client.events.create(event)
print(result.results[0].status)       # processed | duplicate | unhandled | invalid | failed

batch = client.events.create_batch([event, otro_evento])   # hasta 100
```

Si ningún evento del lote entra, la API responde un error (400 o 500): el SDK lanza la excepción y el detalle por evento queda en `err.json_body["results"]`.

```python
client.events.retrieve("evt_…")
client.events.list(type="visit.completed", status="processed")
```

### Webhooks

```python
endpoint = client.webhook_endpoints.create(
    url="https://sistema.example/kuida/webhooks",
    enabled_events=["intake.ready", "intake.created"],
    description="Sistema de admisión",
)
secret = endpoint.secret        # whsec_…: guardalo, solo viene en la creación

client.webhook_endpoints.update(endpoint.id, enabled_events=["*"])
delivery = client.webhook_endpoints.ping(endpoint.id)
client.webhook_deliveries.list(webhook_endpoint=endpoint.id)
client.webhook_deliveries.retrieve(delivery.id)
client.webhook_endpoints.delete(endpoint.id)
```

## Paginación

Las listas usan cursor. Cada `list()` devuelve una página iterable con `has_more`:

```python
page = client.patients.list(limit=20)
for patient in page:
    print(patient.id)
if page.has_more:
    page = client.patients.list(limit=20, starting_after=page.data[-1].id)
```

Para recorrer todo sin manejar cursores, `auto_paging_iter()` pide las páginas a medida que hacen falta, con el mismo `limit` y filtros:

```python
for patient in client.patients.list(limit=100).auto_paging_iter():
    print(patient.full_name)
```

## Cliente asyncio

```python
import asyncio
import kuida

async def main():
    async with kuida.AsyncKuida(api_key="kd_test_…") as client:
        patient = await client.patients.create(phone="+54 9 342 555 0000", full_name="Paciente Demo")
        page = await client.patients.list(limit=100)
        async for p in page.auto_paging_iter():
            print(p.full_name)

asyncio.run(main())
```

## Manejo de errores

Toda respuesta con error se convierte en una excepción de `kuida.errors`, subclase de `kuida.KuidaError`:

| Excepción | Cuándo |
|---|---|
| `InvalidRequestError` | Parámetros inválidos o faltantes, recurso inexistente (404), conflicto. |
| `AuthenticationError` | Clave faltante, inválida, revocada o vencida. |
| `PermissionDeniedError` | La clave no tiene el scope o la línea de servicio no está contratada. |
| `IdempotencyError` | La `Idempotency-Key` ya se usó con otro pedido. |
| `RateLimitError` | Demasiados pedidos (tras agotar los reintentos). |
| `APIError` | Error de Kuida (5xx), tipo desconocido o respuesta que no es JSON. |
| `APIConnectionError` | Sin respuesta: red, DNS, TLS o timeout. |
| `SignatureVerificationError` | Firma de webhook inválida. |

`PermissionError` del contrato común se llama `PermissionDeniedError` para no tapar la excepción nativa de Python; `kuida.errors.PermissionError` existe como alias.

Cada excepción expone `message`, `http_status`, `type`, `code`, `param`, `request_id`, `doc_url`, `headers`, `http_body` y `json_body`:

```python
try:
    client.patients.create(full_name="Paciente Demo")
except kuida.InvalidRequestError as err:
    print(err.code, err.param, err.request_id)   # parameter_missing phone req_…
except kuida.KuidaError as err:
    print("Error de Kuida:", err)
```

## Idempotencia

Todo `POST` lleva `Idempotency-Key`. Si no pasás una, el SDK genera un UUID v4 y **reusa la misma clave en todos los reintentos del pedido**, así un reintento nunca duplica un paciente o un turno. Para reintentar vos mismo más tarde (por ejemplo, desde una cola), pasá una clave propia y estable:

```python
client.appointments.create(
    patient="pat_…",
    start_at="2026-10-05T10:30:00-03:00",
    idempotency_key="turno-123-alta",
)
```

La misma clave con otro cuerpo lanza `IdempotencyError`. `GET`, `PATCH` y `DELETE` no la mandan.

## Reintentos

El SDK reintenta hasta `max_retries` veces (default 2) ante errores de conexión o timeout, `409` con `code: idempotency_key_in_use`, `429` y `500`, `502`, `503`, `504`. Nunca reintenta otro `4xx`.

La espera es `min(0,5 s × 2^intento, 8 s)` con ±25 % de variación; si la respuesta trae `Retry-After`, se respeta ese valor (hasta 60 s).

```python
client = kuida.Kuida(max_retries=0)                 # sin reintentos
client.visits.create(..., max_retries=5)            # por pedido
```

## Verificación de webhooks

Kuida firma cada entrega con HMAC-SHA256 y manda los headers `x-kuida-signature`, `x-kuida-timestamp`, `x-kuida-event-type` y `x-kuida-delivery-attempt`. Verificá siempre la firma con el **cuerpo crudo** del pedido (nunca un JSON re-serializado):

```python
import os
import kuida
from flask import Flask, request

app = Flask(__name__)

@app.post("/kuida/webhooks")
def kuida_webhook():
    try:
        event = kuida.Webhook.construct_event(
            request.get_data(),
            request.headers.get("x-kuida-signature"),
            request.headers.get("x-kuida-timestamp"),
            os.environ["KUIDA_WEBHOOK_SECRET"],
        )
    except kuida.SignatureVerificationError:
        return "", 400

    if event.type == "intake.ready":
        print("Solicitud lista:", event.ref, event.data)
    return "", 200
```

`construct_event` devuelve un `WebhookEvent` (`id`, `type`, `api_version`, `livemode`, `occurred_at`, `ref`, `data`). El `id` es estable entre reintentos de la misma entrega: deduplicá por ahí. `Webhook.verify_signature(...)` hace solo la verificación y devuelve `True` o lanza.

Por defecto se rechaza un timestamp con más de 300 segundos de diferencia con la hora actual; `tolerance=` lo cambia y `tolerance=0` desactiva ese chequeo. Para tus tests, `Webhook.compute_signature(payload, timestamp, secret)` arma la firma y `now=` fija el "ahora".

## Versión de la API

El SDK manda `Kuida-Version: 2026-09-28` en todos los pedidos. Para fijar otra versión:

```python
client = kuida.Kuida(api_version="2026-09-28")
```

`kuida.API_VERSION` es la versión por defecto y `kuida.VERSION` la del SDK. Agregar campos o métodos es una versión menor del SDK; romper una firma, una mayor.

## Desarrollo

```bash
python3 -m venv .venv
.venv/bin/pip install httpx pytest build
.venv/bin/python scripts/generate.py            # regenera src/kuida/_generated/models.py desde el OpenAPI

PORT=12122 node ../conformance/mock-server.mjs &
KUIDA_API_BASE=http://localhost:12122/api .venv/bin/python -m pytest -q
.venv/bin/python -m build
```

`tests/test_conformance.py` cubre los 17 escenarios de la suite común (`SDK_DESIGN.md` §9).

## Licencia

MIT.
