# Diseño de los SDKs de Kuida

Contrato común de las librerías oficiales. Todo SDK de este repo lo cumple; la suite de conformidad (§9) lo verifica contra el mismo mock. Si un lenguaje necesita apartarse, se escribe acá por qué.

Fuente de verdad de la API: `openapi/kuida-v1.json` (OpenAPI 3.1, generado desde el código de Kuida). Si el SDK y el OpenAPI discrepan, gana el OpenAPI.

## 1. Lenguajes, paquetes y compatibilidad

| Carpeta | Paquete | Runtime mínimo | Dependencias |
|---|---|---|---|
| `node/` | npm `@kuida/sdk` | Node 18 (fetch nativo); ESM + CJS; tipos incluidos | ninguna |
| `python/` | PyPI `kuida` | Python 3.9 | `httpx` |
| `dotnet/` | NuGet `Kuida` | `netstandard2.0` (incluye .NET Framework 4.6.2+) y `net8.0` | `System.Text.Json` (solo netstandard2.0) |
| `java/` | Maven `ar.kuida:kuida-java` | Java 8 | `com.google.code.gson:gson` |
| `php/` | Packagist `kuida/kuida-php` | PHP 7.4 con `ext-curl` y `ext-json` | ninguna |
| `go/` | `github.com/Kuida-ar/kuida-sdks/go` | Go 1.21 | ninguna (stdlib) |

Runtimes viejos a propósito: el parque de sistemas de salud en Argentina corre .NET Framework, Java 8 y PHP 7 en servidores que nadie actualiza. Un SDK que exige lo último no se instala.

Versión inicial de todos: `0.1.0`. Semver: agregar un método o un campo es minor; romper una firma es major.

## 2. Cliente

- Se construye con la clave: `new Kuida("kd_live_…")`, `Kuida(api_key="kd_live_…")`, `new KuidaClient("kd_live_…")`, `kuida.NewClient("kd_live_…")`. Si no se pasa, se lee `KUIDA_API_KEY` del entorno. Sin clave: error al construir, no al primer pedido.
- Opciones: `baseUrl` (default `https://www.kuida.ar/api`; también se lee `KUIDA_API_BASE`), `timeout` (default 30 s), `maxRetries` (default 2), `apiVersion` (default la del SDK, hoy `2026-09-28`).
- Recursos como propiedades del cliente, en plural y con el nombre del OpenAPI (`x-kuida-resource`) en la convención del lenguaje: `client.patients`, `client.intakeRequests` / `client.intake_requests` / `client.IntakeRequests`.
- Cada método acepta al final opciones por pedido: `idempotencyKey`, `timeout`, `maxRetries`.
- Thread-safe / reutilizable: un cliente por proceso.

## 3. Métodos

Uno por operación del OpenAPI (`x-kuida-resource` + `x-kuida-method`), 32 en total:

```
account.retrieve()
patients.create(params) · retrieve(id) · update(id, params) · list(params?)
doctors.list(params?) · retrieve(id)
appointments.create(params) · retrieve(id) · update(id, params) · cancel(id, params?) · list(params?)
visits.create(params) · retrieve(id) · list(params?)
intakeRequests.create(params) · retrieve(id) · list(params?)
treatments.create(params) · retrieve(id) · list(params?)
events.create(event) · createBatch(events) · retrieve(id) · list(params?)
webhookEndpoints.create(params) · retrieve(id) · update(id, params) · delete(id) · list(params?) · ping(id)
webhookDeliveries.list(params?) · retrieve(id)
```

`events.create` manda un solo sobre y `events.createBatch` una lista (hasta 100); los dos devuelven el `EventBatchResponse`. Es el único recurso con dos métodos para una operación. Si ningún evento del lote entra, la API responde el error normal (400 `invalid_request_error` o 500 `api_error`) y el cuerpo trae además `results` con el detalle por evento: el SDK lanza la excepción y deja `results` accesible en el cuerpo crudo.

`list(params)` manda los parámetros como query string (`?limit=10&startingAfter=pat_…`). Los booleanos de filtro van como `"true"`/`"false"`.

## 4. Modelos

- **Nombres de campo del cable, en camelCase, sin traducir.** El JSON de la API ya está en camelCase y así se usa en todos los lenguajes. En los lenguajes tipados se respeta la convención de propiedades (`FullName` en C# con `[JsonPropertyName("fullName")]`, `FullName` en Go con tag `json:"fullName"`, getters `getFullName()` en Java); en Python los atributos son `snake_case` (`patient.full_name`) y el objeto también acepta `patient["fullName"]`. Los parámetros de entrada en Python aceptan kwargs en `snake_case` y se convierten a camelCase **solo en las claves conocidas del esquema**.
- **Los mapas libres no se tocan:** `data` (de `Event`, `IntakeRequest`, `WebhookEvent`), `payload` (`WebhookDelivery`) y los `data` de los eventos que manda el integrador viajan tal cual, sin conversión de claves.
- Modelos generados del OpenAPI por un script propio del SDK que vive en su carpeta (`scripts/generate`), commiteado junto con su salida. El script lee `../openapi/kuida-v1.json`. Los métodos de recurso se escriben a mano: son pocos y es donde está la ergonomía.
- Fechas: se exponen como el tipo nativo cuando el lenguaje lo tiene sin fricción (`DateTimeOffset` en C#, `time.Time` en Go, `datetime` en Python, `OffsetDateTime`/`String` en Java a criterio, `string` ISO en TS y PHP). Al mandar, cualquier fecha se serializa ISO 8601 con zona.
- Campos desconocidos en la respuesta se ignoran sin error (la API agrega campos sin cambiar de versión). Todo objeto conserva el JSON crudo accesible (`lastResponse` / `raw`).
- `PatientReference` y `DoctorReference` son uniones: un id (`"pat_…"`) o un objeto de identidad. Cada lenguaje lo modela como puede (sobrecarga, tipo unión, `object`), pero las dos formas tienen que funcionar.
- `EventInput`: unión discriminada por `type`. Alcanza con un tipo genérico `{ id, type, data, occurredAt?, source?, schemaVersion? }` con `data` como mapa; los tipos por evento son un plus.

## 5. HTTP

- `Authorization: Bearer <clave>`, `Content-Type: application/json` en POST/PATCH, `Accept: application/json`.
- `Kuida-Version: <apiVersion>` en todos los pedidos.
- `User-Agent: Kuida/v1 <Lenguaje>Bindings/<versión SDK>` (ej. `Kuida/v1 NodeBindings/0.1.0`) y `X-Kuida-Client-User-Agent` con JSON `{ "bindings_version", "lang", "lang_version", "platform" }`.
- **Idempotencia automática:** todo POST lleva `Idempotency-Key`. Si el usuario pasó una, esa; si no, el SDK genera un UUID v4. **La misma clave se reusa en todos los reintentos del mismo pedido.** GET, PATCH y DELETE no la mandan.
- `POST /v1/appointments/{id}/cancel` y `POST /v1/webhook_endpoints/{id}/ping` sin parámetros mandan `{}`.

## 6. Reintentos

Hasta `maxRetries` reintentos (default 2, o sea 3 intentos) cuando:

- error de conexión o timeout;
- `409` con `code: idempotency_key_in_use`;
- `429`;
- `500`, `502`, `503`, `504`.

Nunca se reintenta otro 4xx. Espera: `min(0.5 s × 2^intento, 8 s)` con jitter de ±25 %; si la respuesta trae `Retry-After` (segundos) se usa ese valor, hasta 60 s. Los tests pueden inyectar una espera nula.

## 7. Errores

Toda respuesta no 2xx con cuerpo `{ "error": { … } }` se convierte en una excepción (o `error` en Go) según `error.type`:

| `type` | Clase |
|---|---|
| `invalid_request_error` | `InvalidRequestError` |
| `authentication_error` | `AuthenticationError` |
| `permission_error` | `PermissionError` (`PermissionDeniedError` donde choque con el lenguaje) |
| `idempotency_error` | `IdempotencyError` |
| `rate_limit_error` | `RateLimitError` |
| `api_error` o cualquier otro | `APIError` |
| sin respuesta (red, timeout, DNS, TLS) | `APIConnectionError` |

Todas heredan de `KuidaError` y exponen: `message`, `httpStatus`, `type`, `code`, `param`, `requestId` (del cuerpo o del header `Request-Id`), `docUrl`, `headers` y el cuerpo crudo. Un cuerpo que no es JSON termina en `APIError` con el texto en `message`.

Además `SignatureVerificationError` (§8), también subclase de `KuidaError`.

## 8. Webhooks

Utilidad estática, sin cliente ni clave:

```
Webhook.constructEvent(payload, signatureHeader, timestampHeader, secret, tolerance = 300) → WebhookEvent
Webhook.verifySignature(payload, signatureHeader, timestampHeader, secret, tolerance = 300) → true | lanza
```

- `payload` es el cuerpo **crudo** (string o bytes), nunca un JSON re-serializado.
- Firma esperada: `sha256=` + hex de HMAC-SHA256(secret, `"${timestamp}.${payload}"`). Comparación en tiempo constante.
- Headers que manda Kuida: `x-kuida-signature`, `x-kuida-timestamp`, `x-kuida-event-type`, `x-kuida-delivery-attempt`.
- Rechaza con `SignatureVerificationError` si falta un header, si la firma no coincide o si `|ahora − timestamp| > tolerance` segundos. `tolerance = 0` desactiva el chequeo de tiempo. Para tests se puede inyectar el "ahora".
- `constructEvent` devuelve el evento parseado: `{ id, object: "event", type, apiVersion, livemode, occurredAt, ref, data }`.

## 9. Conformidad

Mock: `node conformance/mock-server.mjs` (puerto 12111, ver claves en su encabezado). Cada SDK tiene una suite que corre con `KUIDA_API_BASE=http://localhost:12111/api` y cubre, con estos nombres (en la convención del lenguaje):

1. `account_retrieve` — `account.retrieve()` devuelve `object: "account"` y `apiKey.scopes`.
2. `headers` — revisando `GET /__mock/requests`: `Authorization`, `Kuida-Version: 2026-09-28`, `User-Agent` con el formato de §5, y un `Idempotency-Key` UUID v4 en cada POST y ninguno en GET.
3. `patients_crud` — create → retrieve → update (email) → list filtrando por `phone`.
4. `idempotency_explicit` — dos `patients.create` con la misma `idempotencyKey` devuelven el mismo id; la misma clave con otro cuerpo lanza `IdempotencyError`.
5. `pagination_manual` — crear 5 pacientes; `list(limit: 2)` trae 2 y `hasMore`; `startingAfter` trae los siguientes sin repetir.
6. `pagination_auto` — el iterador automático recorre los 5 (o todos los que haya) sin repetidos, pidiendo páginas de a `limit`.
7. `appointments_flow` — create con paciente por identidad y profesional por identidad → update `startAt` → cancel sin parámetros → `status: "cancelled"`; update de un cancelado lanza `InvalidRequestError`.
8. `visit_closes_appointment` — `visits.create` con `appointment` = `externalId` del turno → la consulta trae el `apt_…` y el turno queda `completed`.
9. `intake_and_treatment` — `intakeRequests.create` con `patient` como identidad (en una solicitud de ingreso el paciente puede no existir todavía, por eso no acepta id) y `treatments.create` con `patient` como `pat_…`.
10. `events_batch` — `events.createBatch` con uno válido (`patient.upserted`) y uno inválido → resultados `processed` e `invalid`; `events.create` de uno ya enviado → `duplicate`.
11. `webhook_endpoints_flow` — create (trae `secret`) → retrieve (sin `secret`) → update → ping (`webhook_delivery` pendiente) → `webhookDeliveries.list(webhookEndpoint)` → delete (`deleted: true`).
12. `errors` — sin clave o clave inválida → `AuthenticationError`; clave revocada (`kd_test_dead…`) → `AuthenticationError` con `code: "api_key_revoked"`; clave `kd_test_1e9ac7…` en `patients.list` → `PermissionError` con `code: "scope_missing"`; `patients.retrieve("pat_noexiste")` → `InvalidRequestError` con `httpStatus 404`, `code: "resource_missing"` y `requestId`; `patients.create` sin `phone` → `InvalidRequestError` con `param: "phone"`.
13. `retries_5xx` — con la clave `kd_test_fa11ed…` un `patients.create` termina bien tras un 503, y en `/__mock/requests` los dos intentos llevan **la misma** `Idempotency-Key`.
14. `retries_429` — con `kd_test_42900000000a…` un `account.retrieve` termina bien tras un 429 respetando `Retry-After`.
15. `no_retry` — con `maxRetries: 0` y la clave `fa11ed` el pedido lanza `APIError` con `httpStatus 503`.
16. `connection_error` — `baseUrl` a un puerto cerrado → `APIConnectionError` (con `maxRetries: 0`).
17. `webhook_signature` — con `conformance/webhook-vectors.json`: el vector fijo verifica (con "ahora" = su timestamp o `tolerance: 0`); secreto equivocado, cuerpo alterado y timestamp fuera de tolerancia lanzan `SignatureVerificationError`; un cuerpo firmado en el test con la hora actual verifica con la tolerancia default.

Cada suite resetea el mock (`POST /__mock/reset`) donde lo necesite y no depende del orden de las demás. `conformance/run.sh <lenguaje|all>` levanta el mock y corre las suites.

## 10. Documentación de cada SDK

`README.md` por carpeta, en español neutro: instalación, configuración, un ejemplo por recurso, paginación automática, manejo de errores, idempotencia, reintentos, verificación de webhooks y versión de la API. Comentarios de documentación en el código (JSDoc, docstrings, XML doc, Javadoc, PHPDoc, GoDoc) en español neutro, en los métodos públicos. Todos los ejemplos con datos sintéticos (`Paciente Demo`, `+54 9 342 555 0000`, `DNI 11111111`); nunca datos reales.
