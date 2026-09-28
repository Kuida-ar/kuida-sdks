# Kuida para Java

SDK oficial de Java para la [API de Kuida](https://www.kuida.ar/desarrolladores): pacientes, turnos, consultas, solicitudes de ingreso, tratamientos, eventos y webhooks.

- Java 8 o posterior (el jar se compila con `--release 8`).
- Una sola dependencia: [Gson](https://github.com/google/gson).
- HTTP con `HttpURLConnection` del JDK, sin clientes externos.

## Instalación

Maven:

```xml
<dependency>
  <groupId>ar.kuida</groupId>
  <artifactId>kuida-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle:

```groovy
implementation "ar.kuida:kuida-java:0.1.0"
```

## Configuración

```java
import ar.kuida.KuidaClient;

KuidaClient kuida = new KuidaClient("kd_live_...");
```

Sin argumentos, el cliente lee la clave de la variable de entorno `KUIDA_API_KEY`. Si no hay clave, el constructor lanza `AuthenticationException`; el error aparece al construir, no en el primer pedido.

Todas las opciones:

```java
import java.time.Duration;

KuidaClient kuida = KuidaClient.builder()
    .apiKey("kd_live_...")
    .baseUrl("https://www.kuida.ar/api")   // también se lee KUIDA_API_BASE
    .timeout(Duration.ofSeconds(30))       // por intento; default 30 s
    .maxRetries(2)                         // default 2 (3 intentos)
    .apiVersion("2026-09-28")              // default: la versión del SDK
    .build();
```

El cliente es thread-safe: conviene crear uno por proceso y reutilizarlo.

Cada método acepta al final opciones por pedido:

```java
import ar.kuida.RequestOptions;

RequestOptions opts = RequestOptions.builder()
    .idempotencyKey("alta-HC-1234")
    .timeout(Duration.ofSeconds(10))
    .maxRetries(0)
    .build();
```

## Uso por recurso

Los parámetros se arman con builders y los campos llevan el mismo nombre que en el JSON de la API (camelCase). Las respuestas se leen con getters (`getFullName()`); las fechas `date-time` llegan como `OffsetDateTime`.

### Cuenta

```java
Account account = kuida.account().retrieve();
System.out.println(account.getName() + " " + account.getApiKey().getScopes());
```

### Pacientes

```java
Patient patient = kuida.patients().create(PatientCreateParams.builder()
    .phone("+54 9 342 555 0000")
    .fullName("Paciente Demo")
    .dni("11111111")
    .externalId("HC-1234")
    .build());

patient = kuida.patients().retrieve(patient.getId());

patient = kuida.patients().update(patient.getId(), PatientUpdateParams.builder()
    .email("paciente.demo@example.com")
    .build());

KuidaList<Patient> found = kuida.patients().list(PatientListParams.builder()
    .phone("+54 9 342 555 0000")
    .build());
```

### Profesionales

```java
KuidaList<Doctor> doctors = kuida.doctors().list(DoctorListParams.builder().active(true).build());
Doctor doctor = kuida.doctors().retrieve("doc_...");
```

### Turnos

El paciente y el profesional se pueden indicar por id o por identidad; Kuida los busca y, si no existen, los crea.

```java
Appointment apt = kuida.appointments().create(AppointmentCreateParams.builder()
    .patient(PatientReference.identity(PatientIdentity.builder()
        .phone("+54 9 342 555 0000")
        .fullName("Paciente Demo")
        .build()))
    .doctor(DoctorReference.identity(DoctorIdentity.builder()
        .externalId("MED-1")
        .fullName("Dra. Demo")
        .build()))
    .startAt(OffsetDateTime.parse("2026-10-01T09:30:00-03:00"))
    .externalId("turno-123")
    .build());

// Atajos equivalentes: .patient("pat_...") o .patient(PatientIdentity...)
apt = kuida.appointments().update(apt.getId(), AppointmentUpdateParams.builder()
    .startAt(OffsetDateTime.parse("2026-10-02T09:30:00-03:00"))
    .build());

apt = kuida.appointments().cancel(apt.getId());
```

### Consultas

```java
Visit visit = kuida.visits().create(VisitCreateParams.builder()
    .patient("pat_...")
    .visitedAt(OffsetDateTime.now())
    .appointment("turno-123")   // id del turno o su externalId: el turno queda "completed"
    .build());
```

### Solicitudes de ingreso

```java
IntakeRequest intake = kuida.intakeRequests().create(IntakeRequestCreateParams.builder()
    .patient(PatientIdentity.builder()
        .phone("+54 9 342 555 0000")
        .fullName("Paciente Demo")
        .dni("11111111")
        .build())
    .coverage(IntakeCoverage.builder().insurer("Obra Social Demo").build())
    .summary("Pedido de internación domiciliaria")
    .build());
System.out.println(intake.getStatus() + " " + intake.getMissing());
```

### Tratamientos

```java
Treatment treatment = kuida.treatments().create(TreatmentCreateParams.builder()
    .patient("pat_...")
    .name("Enalapril 10 mg")
    .kind("medication")
    .frequency("cada 12 horas")
    .build());
```

### Eventos

`events().create` manda un evento y `events().createBatch` un lote de hasta 100. Los dos devuelven un resultado por evento. El `id` del evento es su clave de idempotencia: reenviarlo devuelve `duplicate`.

```java
EventInput event = EventInput.builder()
    .id("paciente-HC-1234")
    .type(EventInput.TYPE_PATIENT_UPSERTED)
    .putData("phone", "+54 9 342 555 0000")
    .putData("fullName", "Paciente Demo")
    .build();

EventBatchResponse res = kuida.events().createBatch(Arrays.asList(event));
System.out.println(res.getResults().get(0).getStatus());   // processed

Event stored = kuida.events().retrieve(res.getResults().get(0).getEvent());
```

Los mapas libres (`data` de los eventos y de las solicitudes, `payload` de las entregas) viajan tal cual, sin conversión de claves, y se leen como `Map<String, Object>`.

### Endpoints de webhooks y entregas

```java
WebhookEndpoint endpoint = kuida.webhookEndpoints().create(WebhookEndpointCreateParams.builder()
    .url("https://example.com/webhooks/kuida")
    .enabledEvents("intake.ready", "conversation.handoff")
    .build());
String secret = endpoint.getSecret();   // guardarlo: no se vuelve a mostrar

kuida.webhookEndpoints().update(endpoint.getId(), WebhookEndpointUpdateParams.builder().disabled(true).build());
WebhookDelivery ping = kuida.webhookEndpoints().ping(endpoint.getId());
KuidaList<WebhookDelivery> deliveries = kuida.webhookDeliveries().list(
    WebhookDeliveryListParams.builder().webhookEndpoint(endpoint.getId()).build());
kuida.webhookEndpoints().delete(endpoint.getId());
```

## Paginación

Las listas usan cursor. `KuidaList<T>` expone `getData()` y `hasMore()`; la página siguiente se pide con `startingAfter`:

```java
KuidaList<Patient> page = kuida.patients().list(PatientListParams.builder().limit(50).build());
if (page.hasMore()) {
  String last = page.getData().get(page.getData().size() - 1).getId();
  page = kuida.patients().list(PatientListParams.builder().limit(50).startingAfter(last).build());
}
```

O dejar que el SDK recorra todas las páginas, pidiéndolas a medida que hacen falta:

```java
for (Patient p : kuida.patients().list(PatientListParams.builder().limit(100).build()).autoPagingIterable()) {
  System.out.println(p.getFullName());
}

long total = kuida.visits().list().autoPagingStream().count();
```

## Manejo de errores

Toda respuesta no 2xx lanza una subclase de `KuidaException` según `error.type`:

| `error.type` | Excepción |
|---|---|
| `invalid_request_error` | `InvalidRequestException` |
| `authentication_error` | `AuthenticationException` |
| `permission_error` | `PermissionException` |
| `idempotency_error` | `IdempotencyException` |
| `rate_limit_error` | `RateLimitException` |
| `api_error` u otro | `ApiException` |
| sin respuesta (red, DNS, TLS, timeout) | `ApiConnectionException` |

Las excepciones son unchecked (`RuntimeException`): un error de la API rara vez se resuelve en el mismo lugar donde se llama, y una excepción checked obligaría a envolverla en cada lambda y en el iterador de paginación.

```java
try {
  kuida.patients().retrieve("pat_...");
} catch (InvalidRequestException e) {
  System.out.println(e.getHttpStatus() + " " + e.getCode() + " " + e.getParam());
  System.out.println("requestId: " + e.getRequestId() + " — " + e.getDocUrl());
} catch (KuidaException e) {
  System.out.println(e.getType() + ": " + e.getMessage());
}
```

Cada excepción expone `getMessage()`, `getHttpStatus()`, `getType()`, `getCode()`, `getParam()`, `getRequestId()`, `getDocUrl()`, `getHeaders()` y `getRawBody()`.

## Idempotencia

Todo `POST` lleva `Idempotency-Key`. Si no se indica una, el SDK genera un UUID v4 y **reusa la misma clave en todos los reintentos del pedido**, así un reintento nunca duplica un paciente o un turno. Para que la idempotencia abarque también reintentos propios (por ejemplo, un proceso que se reinicia), conviene pasar una clave estable:

```java
kuida.patients().create(params, RequestOptions.builder().idempotencyKey("alta-HC-1234").build());
```

La misma clave con el mismo cuerpo devuelve la respuesta original (`getLastResponse().isIdempotentReplayed()`); con otro cuerpo lanza `IdempotencyException`.

## Reintentos

El SDK reintenta hasta `maxRetries` veces (default 2) ante errores de conexión o timeout, `409` con `idempotency_key_in_use`, `429` y `500`, `502`, `503`, `504`. Ningún otro 4xx se reintenta. La espera es `min(0,5 s × 2^intento, 8 s)` con jitter de ±25 %; si la respuesta trae `Retry-After`, se respeta ese valor (hasta 60 s).

## Verificación de webhooks

Kuida firma cada envío. `Webhook.constructEvent` verifica la firma sobre el **cuerpo crudo** (nunca un JSON re-serializado) y devuelve el evento:

```java
import ar.kuida.Webhook;
import ar.kuida.exception.SignatureVerificationException;
import ar.kuida.model.WebhookEvent;

try {
  WebhookEvent event = Webhook.constructEvent(
      rawBody,                                   // String o byte[]
      request.getHeader("x-kuida-signature"),
      request.getHeader("x-kuida-timestamp"),
      System.getenv("KUIDA_WEBHOOK_SECRET"));
  if ("intake.ready".equals(event.getType())) {
    Map<String, Object> data = event.getData();
    // ...
  }
} catch (SignatureVerificationException e) {
  response.setStatus(400);
}
```

La firma se compara en tiempo constante. Se rechaza si falta un header, si la firma no coincide o si el timestamp se aleja más de 300 segundos de la hora local (`Webhook.constructEvent(..., tolerance)` cambia la tolerancia; `0` la desactiva). `Webhook.verifySignature` hace solo la verificación.

## Versión de la API

El SDK manda `Kuida-Version: 2026-09-28` en cada pedido (`KuidaClient.API_VERSION`). Para fijar otra versión se usa `KuidaClient.builder().apiVersion(...)`. Los campos nuevos que agregue la API se ignoran sin error y quedan accesibles en el JSON crudo (`getRawJson()`); los parámetros nuevos se pueden mandar con `putExtra("campo", valor)` en cualquier builder.

## PATCH en `HttpURLConnection`

`HttpURLConnection` no acepta el método `PATCH`. El transporte por defecto lo manda igual:

1. En Java 8 a 15 asigna el método por reflexión. En Java 16 o posterior eso requiere abrir los paquetes del JDK: `--add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED`.
2. Si la reflexión no está permitida, manda el `PATCH` con un cliente HTTP/1.1 mínimo sobre un socket (TLS con verificación de hostname). Este camino no pasa por el proxy del sistema.

Si hace falta un proxy o un cliente HTTP propio, se puede inyectar una implementación de `ar.kuida.net.HttpTransport` con `KuidaClient.builder().httpTransport(...)`.

## Desarrollo

```bash
node scripts/generate.mjs                      # regenera src/main/java/ar/kuida/model desde ../openapi/kuida-v1.json
PORT=12124 node ../conformance/mock-server.mjs &
KUIDA_API_BASE=http://localhost:12124/api mvn test
mvn package                                    # target/kuida-java-0.1.0.jar
mvn -Prelease package                          # además, jars de fuentes y Javadoc para Maven Central
```

Los modelos y parámetros de `ar.kuida.model` son generados: no se editan a mano. Los servicios (`ar.kuida.service`) se escriben a mano.

## Licencia

MIT.
