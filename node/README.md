# @kuida/sdk

SDK oficial de la [API de Kuida](https://www.kuida.ar/desarrolladores) para Node.js y TypeScript. Permite que el sistema de gestión de una institución de salud cargue pacientes, turnos, consultas, solicitudes de ingreso y tratamientos en Kuida, envíe eventos y reciba webhooks firmados.

- Sin dependencias: usa `fetch` y `node:crypto` de Node.
- Node.js 18 o superior. Módulos ESM y CommonJS, con tipos incluidos.
- Idempotencia automática en todos los `POST` y reintentos con espera exponencial.
- Paginación automática con `for await`.

## Instalación

```bash
npm install @kuida/sdk
```

## Configuración

```ts
import { Kuida } from "@kuida/sdk";
// CommonJS: const { Kuida } = require("@kuida/sdk");

const kuida = new Kuida("kd_live_…");
```

Si no se pasa la clave, el cliente la lee de la variable de entorno `KUIDA_API_KEY`. Sin clave, el constructor lanza `AuthenticationError`.

| Opción | Default | Descripción |
|---|---|---|
| `baseUrl` | `KUIDA_API_BASE` o `https://www.kuida.ar/api` | URL base de la API. |
| `timeout` | `30000` | Timeout por intento, en milisegundos. |
| `maxRetries` | `2` | Reintentos ante errores transitorios (3 intentos en total). |
| `apiVersion` | `2026-09-28` | Versión de la API que se manda en `Kuida-Version`. |
| `fetch` | `fetch` global | Implementación alternativa de `fetch`. |

```ts
const kuida = new Kuida(process.env.KUIDA_API_KEY, { timeout: 10_000, maxRetries: 3 });
```

Conviene crear un solo cliente por proceso y reutilizarlo.

Cada método acepta al final opciones por pedido:

```ts
await kuida.patients.create(
  { phone: "+54 9 342 555 0000" },
  { idempotencyKey: "alta-paciente-11111111", timeout: 5_000, maxRetries: 0 },
);
```

## Recursos

Todos los ejemplos usan datos sintéticos.

### Cuenta

```ts
const account = await kuida.account.retrieve();
console.log(account.name, account.apiKey.scopes);
```

### Pacientes

```ts
const patient = await kuida.patients.create({
  phone: "+54 9 342 555 0000",
  fullName: "Paciente Demo",
  dni: "11111111",
  externalId: "HC-0001",
});

await kuida.patients.retrieve(patient.id);
await kuida.patients.update(patient.id, { email: "paciente.demo@example.com" });
const found = await kuida.patients.list({ phone: "3425550000" });
```

Crear un paciente no le envía ningún mensaje. Si ya existe uno con ese teléfono (o DNI), la API devuelve el existente y completa los datos faltantes.

### Profesionales

```ts
const doctors = await kuida.doctors.list({ active: true });
const doctor = await kuida.doctors.retrieve("doc_…");
```

### Turnos

`patient` y `doctor` aceptan un id (`pat_…`, `doc_…`) o un objeto de identidad. Las fechas pueden ser `Date` o texto ISO 8601 con zona horaria.

```ts
const appointment = await kuida.appointments.create({
  patient: { phone: "+54 9 342 555 0000", fullName: "Paciente Demo", dni: "11111111" },
  doctor: { externalId: "prof-001", fullName: "Dra. Profesional Demo" },
  startAt: "2026-10-05T10:00:00-03:00",
  type: "Control",
  externalId: "turno-001",
});

await kuida.appointments.update(appointment.id, { startAt: new Date("2026-10-06T13:30:00Z") });
await kuida.appointments.cancel(appointment.id, { reason: "El paciente pidió cancelar" });
await kuida.appointments.retrieve(appointment.id);
await kuida.appointments.list({ patient: patient.id, status: "confirmed" });
```

### Consultas

Si `appointment` apunta a un turno (por id o por su `externalId`), el turno queda `completed`.

```ts
const visit = await kuida.visits.create({
  patient: patient.id,
  visitedAt: "2026-10-05T10:20:00-03:00",
  appointment: "turno-001",
  diagnosis: "Control sin novedades",
});

await kuida.visits.retrieve(visit.id);
await kuida.visits.list({ patient: patient.id });
```

### Solicitudes de ingreso

```ts
const intake = await kuida.intakeRequests.create({
  patient: { phone: "+54 9 342 555 0000", fullName: "Paciente Demo", dni: "11111111" },
  coverage: { insurer: "Obra Social Demo", memberId: "0000-0000" },
  summary: "Ingreso programado",
});

await kuida.intakeRequests.retrieve(intake.id);
await kuida.intakeRequests.list({ status: "ready" });
```

### Tratamientos

```ts
const treatment = await kuida.treatments.create({
  patient: patient.id,
  name: "Medicación demo",
  dosage: "1 comprimido",
  frequency: "cada 12 horas",
});

await kuida.treatments.retrieve(treatment.id);
await kuida.treatments.list({ patient: patient.id, active: true });
```

### Eventos

`events.create` envía un evento y `events.createBatch` un lote de hasta 100. El `id` de cada evento es su clave de idempotencia: reenviarlo no repite nada y el resultado vuelve como `duplicate`.

```ts
const { results } = await kuida.events.createBatch([
  {
    id: "paciente-11111111",
    type: "patient.upserted",
    data: { phone: "+54 9 342 555 0000", fullName: "Paciente Demo", dni: "11111111" },
  },
  {
    id: "turno-001-creado",
    type: "appointment.created",
    data: {
      externalId: "turno-001",
      startAt: "2026-10-05T10:00:00-03:00",
      patient: { phone: "+54 9 342 555 0000" },
    },
  },
]);
for (const r of results) console.log(r.id, r.status); // processed | duplicate | unhandled | invalid | failed

await kuida.events.create({ id: "paciente-11111111", type: "patient.upserted", data: { phone: "+54 9 342 555 0000" } });
await kuida.events.retrieve("evt_…");
await kuida.events.list({ type: "patient.upserted" });
```

Si todos los eventos de un pedido son inválidos, la API responde 400 y el SDK lanza `InvalidRequestError`; el detalle por evento queda en `error.raw.results`.

### Webhooks: endpoints y entregas

```ts
const endpoint = await kuida.webhookEndpoints.create({
  url: "https://sistema.example.com/webhooks/kuida",
  enabledEvents: ["intake.ready", "appointment.cancelled"],
});
// endpoint.secret (whsec_…) llega solo en esta respuesta: hay que guardarlo.

await kuida.webhookEndpoints.retrieve(endpoint.id);
await kuida.webhookEndpoints.update(endpoint.id, { enabledEvents: ["*"] });
const delivery = await kuida.webhookEndpoints.ping(endpoint.id);
await kuida.webhookEndpoints.list();

await kuida.webhookDeliveries.list({ webhookEndpoint: endpoint.id });
await kuida.webhookDeliveries.retrieve(delivery.id);

await kuida.webhookEndpoints.delete(endpoint.id);
```

## Paginación

Los métodos `list` usan cursores. `await` devuelve una página:

```ts
const page = await kuida.patients.list({ limit: 10 });
page.data;     // hasta 10 pacientes, del más nuevo al más viejo
page.hasMore;  // true si hay más

const next = await kuida.patients.list({ limit: 10, startingAfter: page.data.at(-1)!.id });
```

Para recorrer todas las páginas sin manejar cursores:

```ts
for await (const patient of kuida.patients.list({ limit: 100 })) {
  console.log(patient.id);
}

// Hasta 500 objetos en un array:
const patients = await kuida.patients.list({ limit: 100 }).autoPagingToArray({ limit: 500 });

// Con corte: devolver false detiene la iteración.
await kuida.appointments.list({ status: "confirmed" }).autoPagingEach((a) => {
  console.log(a.startAt);
});
```

## Errores

Toda respuesta con error se convierte en una excepción que hereda de `KuidaError`:

| Clase | Cuándo |
|---|---|
| `InvalidRequestError` | Parámetros inválidos, objeto inexistente (404), conflicto. |
| `AuthenticationError` | Clave faltante, inválida, revocada o vencida. |
| `PermissionError` | A la clave le falta un scope o la línea de servicio no está contratada. |
| `IdempotencyError` | La misma `Idempotency-Key` se usó con otro cuerpo. |
| `RateLimitError` | Demasiados pedidos (429), después de agotar los reintentos. |
| `APIError` | Error del lado de Kuida o respuesta inesperada. |
| `APIConnectionError` | Sin respuesta: red, DNS, TLS o timeout. |
| `SignatureVerificationError` | Firma de webhook inválida. |

Todas exponen `message`, `httpStatus`, `type`, `code`, `param`, `requestId`, `docUrl`, `headers` y `raw` (el cuerpo de la respuesta).

```ts
import { InvalidRequestError, KuidaError } from "@kuida/sdk";

try {
  await kuida.patients.retrieve("pat_noexiste");
} catch (err) {
  if (err instanceof InvalidRequestError && err.code === "resource_missing") {
    // el paciente no existe
  } else if (err instanceof KuidaError) {
    console.error(err.type, err.code, err.message, err.requestId);
  } else {
    throw err;
  }
}
```

El `requestId` (`req_…`) identifica el pedido; conviene incluirlo en cualquier consulta a soporte. Cada objeto devuelto trae además `lastResponse` (no enumerable) con `statusCode`, `headers`, `requestId` y `rawBody`.

## Idempotencia

Todos los `POST` llevan el header `Idempotency-Key`. Si no se indica uno, el SDK genera un UUID v4 y lo reutiliza en todos los reintentos del mismo pedido, de modo que un reintento nunca crea un duplicado. Para que un pedido repetido desde otro proceso tampoco duplique, se puede pasar una clave propia y estable:

```ts
await kuida.appointments.create(params, { idempotencyKey: `turno-${turnoId}` });
```

La misma clave con otro cuerpo lanza `IdempotencyError`. `GET`, `PATCH` y `DELETE` no envían la clave.

## Reintentos

Por default el SDK reintenta hasta 2 veces ante errores de conexión o timeout, `409 idempotency_key_in_use`, `429` y `500`, `502`, `503`, `504`. La espera es `min(0,5 s × 2^intento, 8 s)` con ±25 % de variación; si la respuesta trae `Retry-After`, se respeta ese valor (hasta 60 s). Ningún otro 4xx se reintenta.

```ts
const kuida = new Kuida("kd_live_…", { maxRetries: 0 });       // sin reintentos
await kuida.account.retrieve({ maxRetries: 5 });                // o por pedido
```

## Verificación de webhooks

Kuida firma cada entrega con el secreto del endpoint. La verificación necesita el cuerpo **crudo** del pedido, tal como llegó, no un JSON vuelto a serializar.

```ts
import express from "express";
import { Webhook, SignatureVerificationError } from "@kuida/sdk";

const app = express();

app.post("/webhooks/kuida", express.raw({ type: "application/json" }), (req, res) => {
  let event;
  try {
    event = Webhook.constructEvent(
      req.body, // Buffer
      req.headers["x-kuida-signature"],
      req.headers["x-kuida-timestamp"],
      process.env.KUIDA_WEBHOOK_SECRET!,
    );
  } catch (err) {
    if (err instanceof SignatureVerificationError) return res.status(400).send("firma inválida");
    throw err;
  }

  switch (event.type) {
    case "intake.ready":
      // …
      break;
  }
  res.sendStatus(200);
});
```

- La firma es `sha256=` + HMAC-SHA256 en hexadecimal de `${timestamp}.${cuerpo}` con el secreto; se compara en tiempo constante.
- Por default se rechazan entregas con más de 300 s de diferencia con la hora local. El quinto parámetro cambia la tolerancia (`0` la desactiva).
- `Webhook.verifySignature(...)` recibe los mismos parámetros y devuelve `true` o lanza `SignatureVerificationError`.
- Kuida también envía `x-kuida-event-type` y `x-kuida-delivery-attempt`.

## Versión de la API

El SDK envía `Kuida-Version: 2026-09-28` en cada pedido; esa es la versión con la que se generaron los tipos. Se puede fijar otra con la opción `apiVersion`, pero los tipos del SDK corresponden a la versión por default. Agregar un método o un campo es un cambio menor del SDK; cambiar una firma, uno mayor.

## TypeScript

Los modelos (`Patient`, `Appointment`, `WebhookEvent`, …) y los parámetros (`PatientCreateParams`, `AppointmentListParams`, …) se generan del OpenAPI con `npm run generate` y se exportan desde el paquete. Los eventos tienen una interfaz por tipo (`PatientUpsertedEventInput`, `VisitCompletedEventInput`, …) unidas en `EventInput`; para tipos nuevos que el SDK aún no conozca existe `GenericEventInput`.

## Desarrollo

```bash
npm install
npm run generate   # regenera src/generated/types.ts desde ../openapi/kuida-v1.json
npm run build      # dist/esm y dist/cjs

# suite de conformidad contra el mock
PORT=12121 node ../conformance/mock-server.mjs &
KUIDA_API_BASE=http://localhost:12121/api npm test
```

## Licencia

MIT
