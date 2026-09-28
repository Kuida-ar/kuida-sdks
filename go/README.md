# Kuida para Go

SDK oficial de Go para la [API de Kuida](https://www.kuida.ar/desarrolladores): pacientes, turnos, consultas, solicitudes de ingreso, tratamientos, eventos y webhooks.

- Go 1.21 o posterior. Sin dependencias fuera de la biblioteca estándar.
- Idempotencia automática en cada POST, reintentos con backoff, errores tipados, paginación automática y verificación de firma de webhooks.
- Versión del SDK: `0.1.0`. Versión de la API: `2026-09-28`.

## Instalación

```bash
go get github.com/Kuida-ar/kuida-sdks/go
```

```go
import kuida "github.com/Kuida-ar/kuida-sdks/go"
```

## Configuración

```go
client, err := kuida.NewClient("kd_live_…")
if err != nil {
	log.Fatal(err) // kuida.ErrMissingAPIKey si no hay clave
}
```

Si la clave es `""`, se lee `KUIDA_API_KEY`. Sin clave, `NewClient` devuelve `kuida.ErrMissingAPIKey` al construir, no en el primer pedido.

| Opción | Por defecto | Para qué |
|---|---|---|
| `kuida.WithBaseURL(url)` | `KUIDA_API_BASE` o `https://www.kuida.ar/api` | Apuntar al mock o a otro entorno |
| `kuida.WithTimeout(d)` | 30 s | Tiempo máximo de cada intento |
| `kuida.WithMaxRetries(n)` | 2 (hasta 3 intentos) | Reintentos ante red, 409 en curso, 429 y 5xx |
| `kuida.WithHTTPClient(c)` | `&http.Client{}` | Proxy, transporte o trazas propias |
| `kuida.WithAPIVersion(v)` | `kuida.APIVersion` | Fijar el header `Kuida-Version` |
| `kuida.WithRetrySleep(fn)` | espera real | Tests: reemplazar la espera entre reintentos |

El cliente es seguro para usar desde varias goroutines: lo recomendable es crear uno por proceso y reutilizarlo.

Todos los métodos reciben un `context.Context` primero y aceptan opciones por pedido al final:

```go
patient, err := client.Patients.Create(ctx, params,
	kuida.WithIdempotencyKey("alta-paciente-11111111"),
	kuida.WithRequestTimeout(10*time.Second),
	kuida.WithRequestMaxRetries(0),
)
```

### Parámetros y modelos

- Los campos obligatorios de texto van por valor (`Phone: "…"`); los opcionales son punteros y se completan con los helpers `kuida.String`, `kuida.Int64`, `kuida.Bool` y `kuida.Time`. Los campos en `nil` no se envían.
- Los enumerados son tipos con constantes (`kuida.TreatmentKindMedication`, `kuida.AppointmentStatusCancelled`); si quedan vacíos no se envían.
- Los nombres de campo del cable (camelCase) están en los tags JSON: `FullName` ↔ `fullName`, `ExternalID` ↔ `externalId`.
- Las fechas son `time.Time` y se envían en ISO 8601 con zona. En las respuestas, los campos que pueden venir `null` son punteros.
- Los mapas libres (`Data` de eventos y solicitudes, `Payload` de envíos) son `map[string]any` y viajan tal cual.
- Cada objeto devuelto tiene `LastResponse` (status, headers, `RequestID`, cuerpo crudo) y `Raw()` con el JSON original, incluidos los campos que el SDK todavía no conoce.

## Ejemplos por recurso

Todos los ejemplos usan datos sintéticos.

### Cuenta

```go
account, err := client.Account.Retrieve(ctx)
fmt.Println(account.Name, account.APIKey.Scopes)
```

### Pacientes

```go
patient, err := client.Patients.Create(ctx, &kuida.PatientCreateParams{
	Phone:      "+54 9 342 555 0000",
	FullName:   kuida.String("Paciente Demo"),
	DNI:        kuida.String("11111111"),
	ExternalID: kuida.String("HC-0001"),
})

patient, err = client.Patients.Retrieve(ctx, patient.ID)

patient, err = client.Patients.Update(ctx, patient.ID, &kuida.PatientUpdateParams{
	Email: kuida.String("paciente.demo@example.com"),
})

page, err := client.Patients.ListPage(ctx, &kuida.PatientListParams{
	DNI: kuida.String("11111111"),
})
```

### Profesionales

```go
it := client.Doctors.List(ctx, &kuida.DoctorListParams{Active: kuida.Bool(true)})
for it.Next() {
	fmt.Println(it.Current().FullName)
}

doctor, err := client.Doctors.Retrieve(ctx, "doc_…")
```

### Turnos

El paciente y el profesional se pueden indicar por id de Kuida o por los datos con que los conoce su sistema:

```go
art := time.FixedZone("ART", -3*60*60)

appointment, err := client.Appointments.Create(ctx, &kuida.AppointmentCreateParams{
	Patient: kuida.PatientByIdentity(kuida.PatientIdentity{
		Phone:    kuida.String("+54 9 342 555 0000"),
		FullName: kuida.String("Paciente Demo"),
	}),
	Doctor:     kuida.DoctorByIdentity(kuida.DoctorIdentity{ExternalID: kuida.String("PROF-01")}),
	StartAt:    time.Date(2026, 10, 5, 10, 30, 0, 0, art),
	Type:       kuida.String("Control"),
	ExternalID: kuida.String("turno-123"),
})

appointment, err = client.Appointments.Update(ctx, appointment.ID, &kuida.AppointmentUpdateParams{
	StartAt: kuida.Time(time.Date(2026, 10, 6, 11, 0, 0, 0, art)),
})

appointment, err = client.Appointments.Cancel(ctx, appointment.ID, &kuida.AppointmentCancelParams{
	Reason: kuida.String("El paciente pidió cancelar"),
}) // o Cancel(ctx, id, nil)

it := client.Appointments.List(ctx, &kuida.AppointmentListParams{
	Status:     kuida.AppointmentStatusConfirmed,
	StartAtGte: kuida.Time(time.Now()),
})
```

### Consultas

```go
visit, err := client.Visits.Create(ctx, &kuida.VisitCreateParams{
	Patient:     kuida.PatientID("pat_…"),
	VisitedAt:   time.Now(),
	Appointment: kuida.String("turno-123"), // id apt_… o externalId; el turno queda atendido
	ExternalID:  kuida.String("consulta-456"),
})

visit, err = client.Visits.Retrieve(ctx, visit.ID)
it := client.Visits.List(ctx, &kuida.VisitListParams{Patient: kuida.String("pat_…")})
```

### Solicitudes de ingreso

En una solicitud el paciente puede no existir todavía, por eso se envían sus datos y no un id:

```go
intake, err := client.IntakeRequests.Create(ctx, &kuida.IntakeRequestCreateParams{
	Patient: &kuida.PatientIdentity{
		FullName: kuida.String("Paciente Demo"),
		DNI:      kuida.String("11111111"),
	},
	Contacts: []*kuida.IntakeContact{{
		Name:         kuida.String("Familiar Demo"),
		Relationship: kuida.String("hija"),
		Phone:        kuida.String("+54 9 342 555 0000"),
	}},
	Coverage:         &kuida.IntakeCoverage{Insurer: kuida.String("Obra Social Demo")},
	RequestedService: kuida.String("Internación domiciliaria"),
})

fmt.Println(intake.Status, intake.Missing) // estado y datos que faltan
intake, err = client.IntakeRequests.Retrieve(ctx, intake.ID)
it := client.IntakeRequests.List(ctx, &kuida.IntakeRequestListParams{Status: kuida.IntakeRequestStatusReady})
```

### Tratamientos

```go
treatment, err := client.Treatments.Create(ctx, &kuida.TreatmentCreateParams{
	Patient:   kuida.PatientID("pat_…"),
	Name:      "Enalapril 10 mg",
	Kind:      kuida.TreatmentKindMedication,
	Frequency: kuida.String("cada 12 horas"),
})

treatment, err = client.Treatments.Retrieve(ctx, treatment.ID)
it := client.Treatments.List(ctx, &kuida.TreatmentListParams{Active: kuida.Bool(true)})
```

### Eventos

`Events.Create` envía un evento y `Events.CreateBatch` un lote de hasta 100. Los dos devuelven un `EventBatchResponse` con un resultado por evento, en el mismo orden. El `ID` es el identificador del evento en su sistema: si se reenvía, Kuida responde `duplicate` y no lo procesa dos veces.

```go
batch, err := client.Events.CreateBatch(ctx, []*kuida.EventInput{{
	ID:   "pms-evt-0001",
	Type: kuida.EventTypePatientUpserted,
	Data: map[string]any{"phone": "+54 9 342 555 0000", "fullName": "Paciente Demo", "dni": "11111111"},
}, {
	ID:   "pms-evt-0002",
	Type: kuida.EventTypeVisitCompleted,
	Data: map[string]any{
		"patient": map[string]any{"dni": "11111111"},
		"visit":   map[string]any{"visitedAt": "2026-10-05T10:45:00-03:00"},
	},
}})
for _, r := range batch.Results {
	fmt.Println(r.ID, r.Status) // processed, duplicate, unhandled, invalid o failed
}

event, err := client.Events.Retrieve(ctx, "evt_…")
it := client.Events.List(ctx, &kuida.EventListParams{Status: kuida.EventStatusFailed})
```

Si al menos un evento del lote entra, la respuesta es exitosa y los rechazados vienen con `Status` `invalid`. Si no entra ninguno, la API responde con error y el SDK devuelve un `*kuida.Error`; el detalle por evento queda en `Error.Body` (campo `results`).

### Endpoints de webhooks y envíos

```go
endpoint, err := client.WebhookEndpoints.Create(ctx, &kuida.WebhookEndpointCreateParams{
	URL:           "https://sistema-demo.example.com/kuida/webhooks",
	EnabledEvents: []kuida.WebhookEventType{kuida.WebhookEventTypeIntakeReady, kuida.WebhookEventTypeConversationHandoff},
	Description:   kuida.String("Sistema de gestión demo"),
})
secret := endpoint.Secret // se muestra una sola vez: guardarlo

endpoint, err = client.WebhookEndpoints.Retrieve(ctx, endpoint.ID)
endpoint, err = client.WebhookEndpoints.Update(ctx, endpoint.ID, &kuida.WebhookEndpointUpdateParams{
	Disabled: kuida.Bool(true),
})
delivery, err := client.WebhookEndpoints.Ping(ctx, endpoint.ID)
it := client.WebhookEndpoints.List(ctx, nil)
deleted, err := client.WebhookEndpoints.Delete(ctx, endpoint.ID)

deliveries := client.WebhookDeliveries.List(ctx, &kuida.WebhookDeliveryListParams{
	WebhookEndpoint: kuida.String(endpoint.ID),
	Status:          kuida.WebhookDeliveryStatusFailed,
})
delivery, err = client.WebhookDeliveries.Retrieve(ctx, "whd_…")
```

## Paginación

Los listados usan cursor. `List` devuelve un iterador que pide las páginas a medida que hacen falta, de a `Limit` objetos:

```go
it := client.Patients.List(ctx, &kuida.PatientListParams{
	ListParams: kuida.ListParams{Limit: kuida.Int64(100)},
})
for it.Next() {
	p := it.Current()
	fmt.Println(p.ID, p.Stage)
}
if err := it.Err(); err != nil {
	log.Fatal(err)
}
```

Para manejar las páginas a mano, `ListPage` devuelve una sola página (`Data`, `HasMore`) y la siguiente se pide con `StartingAfter` = id del último objeto:

```go
page, err := client.Patients.ListPage(ctx, &kuida.PatientListParams{
	ListParams: kuida.ListParams{Limit: kuida.Int64(20)},
})
if page.HasMore {
	last := page.Data[len(page.Data)-1].ID
	page, err = client.Patients.ListPage(ctx, &kuida.PatientListParams{
		ListParams: kuida.ListParams{Limit: kuida.Int64(20), StartingAfter: kuida.String(last)},
	})
}
```

## Manejo de errores

Toda respuesta no 2xx se devuelve como `*kuida.Error`, con `Message`, `HTTPStatus`, `Type`, `Code`, `Param`, `RequestID`, `DocURL`, `Headers` y `Body` (cuerpo crudo). Sin respuesta (red, timeout, DNS, TLS) el error es `*kuida.ConnectionError`.

```go
_, err := client.Patients.Retrieve(ctx, "pat_noexiste")

var kerr *kuida.Error
if errors.As(err, &kerr) {
	fmt.Println(kerr.HTTPStatus, kerr.Code, kerr.RequestID) // 404 resource_missing req_…
}

switch {
case errors.Is(err, kuida.ErrInvalidRequest): // parámetros inválidos o recurso inexistente
case errors.Is(err, kuida.ErrAuthentication): // clave faltante, inválida, revocada o vencida
case errors.Is(err, kuida.ErrPermission):     // falta el scope o la línea de servicio
case errors.Is(err, kuida.ErrIdempotency):    // la Idempotency-Key ya se usó con otro pedido
case errors.Is(err, kuida.ErrRateLimit):      // 429 (ya reintentado)
case errors.Is(err, kuida.ErrAPI):            // error de Kuida o respuesta no reconocida
case errors.Is(err, kuida.ErrConnection):     // sin respuesta
}
```

| `error.type` | Constante | Sentinela |
|---|---|---|
| `invalid_request_error` | `kuida.ErrorTypeInvalidRequest` | `kuida.ErrInvalidRequest` |
| `authentication_error` | `kuida.ErrorTypeAuthentication` | `kuida.ErrAuthentication` |
| `permission_error` | `kuida.ErrorTypePermission` | `kuida.ErrPermission` |
| `idempotency_error` | `kuida.ErrorTypeIdempotency` | `kuida.ErrIdempotency` |
| `rate_limit_error` | `kuida.ErrorTypeRateLimit` | `kuida.ErrRateLimit` |
| `api_error` o cualquier otro | `kuida.ErrorTypeAPI` | `kuida.ErrAPI` |
| sin respuesta | `*kuida.ConnectionError` | `kuida.ErrConnection` |
| firma de webhook inválida | `*kuida.SignatureVerificationError` | `kuida.ErrSignatureVerification` |

`kuida.ErrKuida` coincide con cualquier error del SDK. `Error.Class()` devuelve la clase efectiva (un `type` desconocido cuenta como `api_error`). Los códigos están en las constantes `kuida.ErrorCode…` (`kuida.ErrorCodeResourceMissing`, `kuida.ErrorCodeScopeMissing`, …).

## Idempotencia

Todo POST lleva el header `Idempotency-Key`. Si no se indica, el SDK genera un UUID v4 por pedido; la misma clave se reusa en todos los reintentos de ese pedido, así un reintento nunca crea un duplicado. Para que la idempotencia abarque reintentos propios (por ejemplo, un proceso que se reinicia), conviene derivar la clave de un dato estable:

```go
patient, err := client.Patients.Create(ctx, params, kuida.WithIdempotencyKey("alta-HC-0001"))
fmt.Println(patient.LastResponse.IdempotentReplayed) // true si la API devolvió la respuesta guardada
```

La misma clave con otro cuerpo devuelve un error `idempotency_error`. GET, PATCH y DELETE no llevan clave.

## Reintentos

El SDK reintenta hasta `MaxRetries` veces (por defecto 2) ante:

- error de conexión o timeout;
- `409` con código `idempotency_key_in_use`;
- `429`;
- `500`, `502`, `503` y `504`.

Ningún otro 4xx se reintenta. La espera es `min(0,5 s × 2^intento, 8 s)` con ±25 % de jitter; si la respuesta trae `Retry-After`, se usa ese valor (hasta 60 s). La espera respeta la cancelación del `context.Context`.

## Verificación de webhooks

Kuida firma cada webhook con HMAC-SHA256 y manda los headers `x-kuida-signature`, `x-kuida-timestamp`, `x-kuida-event-type` y `x-kuida-delivery-attempt`. El subpaquete `webhook` verifica la firma con el cuerpo **crudo** (nunca un JSON re-serializado) y devuelve el evento:

```go
import "github.com/Kuida-ar/kuida-sdks/go/webhook"

func handler(w http.ResponseWriter, r *http.Request) {
	payload, err := io.ReadAll(r.Body)
	if err != nil {
		http.Error(w, "cuerpo ilegible", http.StatusBadRequest)
		return
	}
	event, err := webhook.ConstructEvent(payload,
		r.Header.Get(webhook.SignatureHeader),
		r.Header.Get(webhook.TimestampHeader),
		os.Getenv("KUIDA_WEBHOOK_SECRET"),
	)
	if err != nil { // *kuida.SignatureVerificationError
		http.Error(w, "firma inválida", http.StatusBadRequest)
		return
	}
	switch event.Type {
	case kuida.WebhookEventTypeIntakeReady:
		// la solicitud está completa: event.Data["intake"], event.Data["patient"]
	case kuida.WebhookEventTypeConversationHandoff:
		// el paciente necesita a una persona del equipo
	}
	w.WriteHeader(http.StatusOK) // responder 2xx en menos de 10 s
}
```

Se rechaza si falta un header, si la firma no coincide o si el timestamp difiere en más de 300 s de la hora actual. `webhook.WithTolerance(d)` cambia la tolerancia (`0` la desactiva) y `webhook.WithNow(fn)` reemplaza el reloj en tests. `webhook.VerifySignature` solo verifica, y `webhook.ComputeSignature` arma firmas para pruebas. La comparación es en tiempo constante (`hmac.Equal`).

## Versión de la API

El SDK manda `Kuida-Version: 2026-09-28` (`kuida.APIVersion`) en cada pedido, así una versión nueva de la API no cambia el comportamiento de una integración existente. Para fijar otra versión: `kuida.WithAPIVersion("…")`.

## Desarrollo

Los modelos (`models_gen.go`) se generan desde `../openapi/kuida-v1.json`; los métodos de recurso (`services.go`) están escritos a mano.

```bash
go generate ./...     # regenera models_gen.go
go vet ./...
```

La suite de conformidad (`conformance_test.go`, los 17 escenarios de `SDK_DESIGN.md` §9) corre contra el mock:

```bash
PORT=12126 node ../conformance/mock-server.mjs &
KUIDA_API_BASE=http://localhost:12126/api go test ./...
```

Sin `KUIDA_API_BASE` los escenarios que usan el mock se saltean; las pruebas unitarias y la de firma de webhooks corren siempre. También se puede usar `../conformance/run.sh go`.

## Licencia

MIT. Ver [LICENSE](LICENSE).
