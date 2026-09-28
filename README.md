# SDKs oficiales de Kuida

Librerías para conectar el sistema de tu institución (HIS, turnero, ERP, laboratorio, farmacia) con la [API de Kuida](https://www.kuida.ar/desarrolladores): pacientes, turnos, consultas, solicitudes de ingreso, tratamientos, eventos y webhooks.

| Lenguaje | Instalación | Carpeta |
|---|---|---|
| Node.js / TypeScript | `npm install @kuida/sdk` | [`node/`](node) |
| Python | `pip install kuida` | [`python/`](python) |
| .NET (C#) | `dotnet add package Kuida` | [`dotnet/`](dotnet) |
| Java | `ar.kuida:kuida-java` (Maven / Gradle) | [`java/`](java) |
| PHP | `composer require kuida/kuida-php` | [`php/`](php) |
| Go | `go get github.com/Kuida-ar/kuida-sdks/go` | [`go/`](go) |

Todas cumplen el mismo contrato ([`SDK_DESIGN.md`](SDK_DESIGN.md)): idempotencia automática en cada POST, reintentos con backoff ante errores de red, 429 y 5xx, errores tipados, paginación automática y verificación de firma de webhooks.

## En 30 segundos (Node)

```ts
import { Kuida } from "@kuida/sdk";

const kuida = new Kuida(process.env.KUIDA_API_KEY);

const turno = await kuida.appointments.create({
  patient: { phone: "+54 9 342 555 0000", fullName: "Paciente Demo" },
  startAt: "2026-10-05T10:30:00-03:00",
  externalId: "turno-123",
});
// Kuida le manda al paciente los recordatorios por WhatsApp que tu institución configuró.
```

Ejemplos equivalentes en cada lenguaje en el README de su carpeta y en la [referencia de la API](https://www.kuida.ar/desarrolladores/api).

## Claves

Las claves (`kd_live_…`) las emite Kuida para cada institución. Pedila a desarrolladores@kuida.ar. Para desarrollar sin clave ni datos reales usá el mock de este repo:

```bash
node conformance/mock-server.mjs          # http://localhost:12111
export KUIDA_API_BASE=http://localhost:12111/api
export KUIDA_API_KEY=kd_test_000000000000_mocksecretmocksecret00
```

## Estructura

- `openapi/kuida-v1.json` — el contrato de la API (OpenAPI 3.1). Se genera desde el código de Kuida; no se edita a mano.
- `SDK_DESIGN.md` — lo que todo SDK de este repo cumple.
- `conformance/` — mock de la API, vectores de firma y `run.sh`, que corre la misma suite de 17 escenarios contra cada SDK.

## Desarrollo

```bash
conformance/run.sh all        # o node | python | dotnet | java | php | go
```

Un cambio en la API entra primero al spec (`openapi/kuida-v1.json`), después a los modelos de cada SDK (`scripts/generate` de cada carpeta), a sus métodos, al mock y a la suite.

## Licencia

MIT. Ver [LICENSE](LICENSE).
