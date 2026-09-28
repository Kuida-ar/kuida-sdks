# Kuida para PHP

SDK oficial de la [API de Kuida](https://www.kuida.ar/desarrolladores) para PHP. Conecta el sistema de gestión de tu institución con Kuida: pacientes, turnos, consultas, solicitudes de ingreso, tratamientos, eventos y webhooks.

- PHP 7.4 o superior, con `ext-curl` y `ext-json`. Sin otras dependencias.
- Funciona en hostings compartidos y servidores viejos.
- Idempotencia automática, reintentos, paginación automática y verificación de webhooks.

## Instalación

Con Composer:

```bash
composer require kuida/kuida-php
```

```php
require_once __DIR__ . '/vendor/autoload.php';
```

Sin Composer (por ejemplo, en un hosting compartido): descarga esta carpeta y carga su autocarga.

```php
require_once '/ruta/a/kuida-php/init.php';
```

## Configuración

```php
$kuida = new \Kuida\KuidaClient('kd_live_…');
```

Si no pasas la clave, el cliente la lee de la variable de entorno `KUIDA_API_KEY`. Sin clave, el constructor lanza `AuthenticationException`: el error aparece al construir, no en el primer pedido.

También acepta un array de opciones:

```php
$kuida = new \Kuida\KuidaClient([
    'api_key' => getenv('KUIDA_API_KEY'),
    'base_url' => 'https://www.kuida.ar/api', // default; también se lee KUIDA_API_BASE
    'timeout' => 30,                           // segundos por intento
    'max_retries' => 2,                        // reintentos ante errores transitorios
    'api_version' => '2026-09-28',             // default: la versión de este SDK
]);
```

Crea un cliente por proceso y reutilízalo: mantiene la conexión abierta entre pedidos.

Las claves `kd_test_…` trabajan contra el entorno de pruebas y las `kd_live_…` contra producción.

## Uso

Los parámetros se pasan como arrays asociativos con los nombres de campo de la API, en camelCase (`fullName`, `startAt`, `externalId`). Las fechas pueden ir como texto ISO 8601 o como `DateTimeInterface`; el SDK las manda en ISO 8601 con zona horaria.

Las respuestas son objetos que se leen como propiedades o como arrays:

```php
$patient = $kuida->patients->retrieve('pat_…');
echo $patient->fullName;      // "Paciente Demo"
echo $patient['fullName'];    // lo mismo
$patient->toArray();          // array asociativo
$patient->getRawJson();       // JSON crudo de la respuesta
$patient->getLastResponse();  // estado, headers y cuerpo HTTP
```

Cada tipo tiene su clase en `Kuida\Model` (`Patient`, `Appointment`, `Visit`, …), con constantes para los valores posibles (`Appointment::STATUS_CANCELLED`).

### Cuenta

```php
$account = $kuida->account->retrieve();
echo $account->name;
print_r($account->apiKey->scopes);
```

### Pacientes

```php
$patient = $kuida->patients->create([
    'phone' => '+54 9 342 555 0000',
    'fullName' => 'Paciente Demo',
    'dni' => '11111111',
    'dateOfBirth' => '1980-05-17',
    'externalId' => 'HC-0001',
]);

$patient = $kuida->patients->update($patient->id, ['email' => 'paciente.demo@example.com']);
$page = $kuida->patients->list(['phone' => '+54 9 342 555 0000']);
```

### Profesionales

```php
$doctors = $kuida->doctors->list(['active' => true]);
$doctor = $kuida->doctors->retrieve('doc_…');
```

### Turnos

El paciente y el profesional pueden ir por id de Kuida o por los datos con que los conoce tu sistema:

```php
$appointment = $kuida->appointments->create([
    'patient' => ['phone' => '+54 9 342 555 0000', 'fullName' => 'Paciente Demo'],
    'doctor' => ['externalId' => 'prof-001', 'fullName' => 'Dra. Demo'],
    'startAt' => new DateTimeImmutable('2026-10-05 09:30', new DateTimeZone('America/Argentina/Buenos_Aires')),
    'type' => 'Control',
    'externalId' => 'turno-123',
]);

$kuida->appointments->update($appointment->id, ['startAt' => '2026-10-06T10:00:00-03:00']);
$kuida->appointments->cancel($appointment->id, ['reason' => 'El paciente avisó que no viene']);
$upcoming = $kuida->appointments->list(['startAtGte' => '2026-10-01T00:00:00-03:00']);
```

### Consultas

Si la consulta sale de un turno, pasa su id (`apt_…`) o el `externalId` del turno: el turno queda `completed`.

```php
$visit = $kuida->visits->create([
    'patient' => 'pat_…',
    'visitedAt' => '2026-10-05T09:45:00-03:00',
    'appointment' => 'turno-123',
    'externalId' => 'consulta-456',
]);
```

### Solicitudes de ingreso

En una solicitud de ingreso el paciente puede no existir todavía en Kuida, por eso se identifica con sus datos:

```php
$intake = $kuida->intakeRequests->create([
    'patient' => ['phone' => '+54 9 342 555 0000', 'fullName' => 'Paciente Demo', 'dni' => '11111111'],
    'contacts' => [['name' => 'Referente Demo', 'relationship' => 'hija', 'phone' => '+54 9 342 555 0001']],
    'coverage' => ['insurer' => 'Obra Social Demo', 'memberId' => '0000001'],
    'requestedService' => 'Internación domiciliaria',
    'summary' => 'Pedido de prueba',
]);
echo $intake->status; // "new"
```

### Tratamientos

```php
$treatment = $kuida->treatments->create([
    'patient' => 'pat_…',
    'name' => 'Ibuprofeno 400 mg',
    'dosage' => '1 comprimido',
    'frequency' => 'cada 8 horas',
]);
```

### Eventos

La puerta genérica: tu sistema avisa lo que pasó y Kuida reacciona. El `id` es tuyo y hace de clave de idempotencia: reenviar el mismo id no repite nada.

```php
$result = $kuida->events->create([
    'id' => 'hc-0001-alta',
    'type' => 'patient.upserted',
    'data' => ['patient' => ['phone' => '+54 9 342 555 0000', 'fullName' => 'Paciente Demo']],
]);

$batch = $kuida->events->createBatch([$evento1, $evento2]); // hasta 100
foreach ($batch->results as $r) {
    echo $r->id, ': ', $r->status, "\n"; // processed, duplicate, unhandled, invalid o failed
}
```

Si ningún evento del lote entra, la API responde 400 y el SDK lanza `InvalidRequestException`; el detalle de cada evento queda en `$e->getJsonBody()['results']`.

### Webhooks: endpoints y entregas

```php
$endpoint = $kuida->webhookEndpoints->create([
    'url' => 'https://sistema-demo.example/webhooks/kuida',
    'enabledEvents' => ['intake.ready', 'visit.completed'],
]);
$secret = $endpoint->secret; // guárdalo: solo viene en esta respuesta

$kuida->webhookEndpoints->update($endpoint->id, ['disabled' => true]);
$delivery = $kuida->webhookEndpoints->ping($endpoint->id);
$deliveries = $kuida->webhookDeliveries->list(['webhookEndpoint' => $endpoint->id]);
$kuida->webhookEndpoints->delete($endpoint->id);
```

## Paginación

`list()` devuelve una página (`Kuida\Collection`) que se recorre con `foreach` y trae `hasMore`:

```php
$page = $kuida->patients->list(['limit' => 50]);
foreach ($page as $patient) { /* … */ }

if ($page->hasMore) {
    $next = $kuida->patients->list(['limit' => 50, 'startingAfter' => $page->data[49]->id]);
    // o simplemente: $next = $page->nextPage();
}
```

Para recorrer todo, pidiendo las páginas a medida que hacen falta:

```php
foreach ($kuida->patients->list(['limit' => 100])->autoPagingIterator() as $patient) {
    echo $patient->fullName, "\n";
}
```

## Manejo de errores

Toda respuesta de error se convierte en una excepción de `Kuida\Exception`. Todas heredan de `KuidaException`:

| Excepción | Cuándo |
|---|---|
| `InvalidRequestException` | Parámetros inválidos, recurso inexistente (404) o conflicto |
| `AuthenticationException` | Clave faltante, inválida, revocada o vencida |
| `PermissionException` | A la clave le falta el permiso o la línea de servicio |
| `IdempotencyException` | La `Idempotency-Key` ya se usó con otro pedido |
| `RateLimitException` | Demasiados pedidos por minuto |
| `ApiErrorException` | Error de Kuida o respuesta que no se pudo interpretar |
| `ApiConnectionException` | Sin respuesta: red, DNS, TLS o tiempo agotado |
| `SignatureVerificationException` | Firma de webhook inválida |

```php
use Kuida\Exception\InvalidRequestException;
use Kuida\Exception\KuidaException;

try {
    $kuida->patients->create(['fullName' => 'Paciente Demo']);
} catch (InvalidRequestException $e) {
    echo $e->getMessage();     // explicación legible
    echo $e->getHttpStatus();  // 400
    echo $e->getErrorCode();   // "parameter_missing"
    echo $e->getParam();       // "phone"
    echo $e->getRequestId();   // "req_…": inclúyelo si escribes a soporte
    echo $e->getDocUrl();
} catch (KuidaException $e) {
    // cualquier otro error de la API
}
```

Los errores de uso del SDK (una opción desconocida, un id vacío) lanzan `Kuida\Exception\InvalidArgumentException`. Todas las excepciones del SDK implementan `Kuida\Exception\ExceptionInterface`.

## Idempotencia

Todo `POST` lleva un header `Idempotency-Key`. Si no pasas una, el SDK genera un UUID v4 y lo reutiliza en todos los reintentos de ese pedido, así que un reintento nunca duplica un paciente o un turno.

Para que la protección cubra también tus propios reintentos (por ejemplo, un proceso que se cae y vuelve a correr), pasa tu clave como opción del pedido:

```php
$kuida->appointments->create($params, ['idempotency_key' => 'turno-123-alta']);
```

Cada método acepta al final estas opciones por pedido: `idempotency_key`, `timeout` y `max_retries`.

## Reintentos

El SDK reintenta solo, hasta `max_retries` veces (default 2, o sea 3 intentos), ante errores de conexión o de tiempo, `409 idempotency_key_in_use`, `429` y `500`, `502`, `503`, `504`. Nunca reintenta otros errores 4xx. Espera `min(0,5 s × 2^intento, 8 s)` con ±25 % de variación, o lo que indique `Retry-After` (hasta 60 s).

```php
$kuida = new \Kuida\KuidaClient(['api_key' => 'kd_live_…', 'max_retries' => 0]); // sin reintentos
$kuida->patients->list([], ['max_retries' => 5, 'timeout' => 10]);                // solo este pedido
```

## Verificación de webhooks

Kuida firma cada envío con el secreto del endpoint. Verifica la firma con el cuerpo **crudo** del pedido, nunca con un JSON re-serializado:

```php
use Kuida\Exception\SignatureVerificationException;
use Kuida\Webhook;

$payload = file_get_contents('php://input');

try {
    $event = Webhook::constructEvent(
        $payload,
        $_SERVER['HTTP_X_KUIDA_SIGNATURE'] ?? null,
        $_SERVER['HTTP_X_KUIDA_TIMESTAMP'] ?? null,
        getenv('KUIDA_WEBHOOK_SECRET')
    );
} catch (SignatureVerificationException $e) {
    http_response_code(400);
    exit;
}

switch ($event->type) {
    case 'intake.ready':
        // $event->data->intake->id, $event->data->patient->fullName, …
        break;
}
http_response_code(200);
```

`constructEvent` rechaza el envío si falta un header, si la firma no coincide o si el timestamp tiene más de 5 minutos de diferencia con la hora del servidor (quinto argumento, `tolerance`; `0` desactiva ese chequeo). `Webhook::verifySignature(...)` hace solo la verificación y devuelve `true`.

El `id` del evento (`whd_…`) es estable entre reintentos: úsalo para no procesar dos veces el mismo envío.

## Versión de la API

La API de Kuida tiene versiones fechadas. Este SDK manda `Kuida-Version: 2026-09-28` en cada pedido (`\Kuida\KuidaClient::API_VERSION`), así que tu integración no cambia de comportamiento aunque la API evolucione. Para usar otra versión, pasa `api_version` al construir el cliente.

La versión del SDK sigue semver (`\Kuida\KuidaClient::VERSION`, hoy `0.1.0`): agregar un método o un campo es un cambio menor; romper una firma es un cambio mayor. Los campos nuevos que agregue la API se pueden leer aunque el SDK todavía no los documente.

## Desarrollo

```bash
composer install
composer generate   # regenera src/Model/ desde ../openapi/kuida-v1.json
composer compat     # PHPCompatibility: todo el código compatible con PHP 7.4+
composer analyse    # PHPStan nivel 8 con phpVersion 7.4

# Suite de conformidad contra el mock
PORT=12125 node ../conformance/mock-server.mjs &
KUIDA_API_BASE=http://localhost:12125/api composer test
```

Los modelos de `src/Model/` se generan con `scripts/generate.php`; no se editan a mano. Los servicios de `src/Service/` se escriben a mano.

## Licencia

MIT. Ver [LICENSE](LICENSE).
