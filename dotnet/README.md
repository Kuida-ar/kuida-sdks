# Kuida para .NET

SDK oficial de la [API de Kuida](https://www.kuida.ar/desarrolladores) para .NET. Conecta el sistema de gestión de una institución de salud con Kuida: pacientes, turnos, consultas, solicitudes de ingreso, tratamientos, eventos y webhooks.

- Compatible con **.NET Framework 4.6.2 o superior** (`netstandard2.0`), .NET Core 2.0+ y **.NET 8+** (`net8.0`).
- Única dependencia: `System.Text.Json` (y `Microsoft.Bcl.AsyncInterfaces`), solo en `netstandard2.0`.
- Todo es asíncrono (`async`/`await`) y acepta `CancellationToken`.
- Idempotencia automática, reintentos con espera exponencial y errores tipados.

## Instalación

```bash
dotnet add package Kuida
```

O desde la consola del Administrador de paquetes de Visual Studio:

```powershell
Install-Package Kuida
```

## Configuración

```csharp
using Kuida;

var kuida = new KuidaClient("kd_live_…");
```

Si no se pasa la clave, se lee la variable de entorno `KUIDA_API_KEY`. Sin clave, el constructor lanza `AuthenticationException`.

Todas las opciones tienen default:

```csharp
var kuida = new KuidaClient("kd_live_…", new KuidaClientOptions
{
    BaseUrl = "https://www.kuida.ar/api", // también se lee KUIDA_API_BASE
    Timeout = TimeSpan.FromSeconds(30),     // por intento
    MaxRetries = 2,                         // 3 intentos en total
    ApiVersion = "2026-09-28",              // header Kuida-Version
});
```

El cliente es thread-safe: cree uno por proceso y reúselo. Si la aplicación ya tiene un `HttpClient` (proxy, `IHttpClientFactory`, handler de pruebas), se puede pasar al construir; el SDK no lo libera:

```csharp
var kuida = new KuidaClient("kd_live_…", new KuidaClientOptions(), httpClient);
```

Cada método acepta al final opciones por pedido y un token de cancelación:

```csharp
await kuida.Patients.CreateAsync(parametros,
    new RequestOptions { IdempotencyKey = "alta-11111111", Timeout = TimeSpan.FromSeconds(10), MaxRetries = 0 },
    cancellationToken);
```

> **.NET Framework 4.6.2 / 4.7:** la API solo acepta TLS 1.2 o superior. Si la aplicación fija `ServicePointManager.SecurityProtocol`, incluya `SecurityProtocolType.Tls12`.

## Recursos

Los nombres de las propiedades son los del cable en PascalCase (`fullName` → `FullName`). Las fechas son `DateTimeOffset`; al mandarlas se serializan en ISO 8601 con zona.

### Cuenta

```csharp
var cuenta = await kuida.Account.RetrieveAsync();
Console.WriteLine($"{cuenta.Name}: {string.Join(", ", cuenta.ApiKey.Scopes)}");
```

### Pacientes

```csharp
var paciente = await kuida.Patients.CreateAsync(new PatientCreateParams
{
    Phone = "+54 9 342 555 0000",
    FullName = "Paciente Demo",
    Dni = "11111111",
});

paciente = await kuida.Patients.RetrieveAsync(paciente.Id);
paciente = await kuida.Patients.UpdateAsync(paciente.Id, new PatientUpdateParams { Email = "paciente.demo@example.com" });

var pagina = await kuida.Patients.ListAsync(new PatientListParams { Phone = "+54 9 342 555 0000" });
```

### Profesionales

```csharp
var profesionales = await kuida.Doctors.ListAsync(new DoctorListParams { Active = true });
var profesional = await kuida.Doctors.RetrieveAsync("doc_…");
```

### Turnos

El paciente y el profesional se pueden pasar por id de Kuida o por los datos con que los conoce su sistema (`PatientReference` y `DoctorReference` se convierten solos desde un `string` o desde la identidad):

```csharp
var turno = await kuida.Appointments.CreateAsync(new AppointmentCreateParams
{
    Patient = new PatientIdentity { Phone = "+54 9 342 555 0000", FullName = "Paciente Demo" },
    Doctor = new DoctorIdentity { ExternalId = "prof-1", FullName = "Profesional Demo" },
    StartAt = new DateTimeOffset(2026, 10, 1, 14, 30, 0, TimeSpan.FromHours(-3)),
    ExternalId = "turno-123",
});

turno = await kuida.Appointments.UpdateAsync(turno.Id, new AppointmentUpdateParams
{
    StartAt = new DateTimeOffset(2026, 10, 2, 9, 0, 0, TimeSpan.FromHours(-3)),
});

turno = await kuida.Appointments.CancelAsync(turno.Id); // o con new AppointmentCancelParams { Reason = "…" }

var proximos = await kuida.Appointments.ListAsync(new AppointmentListParams
{
    Status = "confirmed",
    StartAtGte = DateTimeOffset.Now,
});
```

### Consultas

```csharp
var consulta = await kuida.Visits.CreateAsync(new VisitCreateParams
{
    Patient = "pat_…",
    VisitedAt = DateTimeOffset.Now,
    Appointment = "turno-123", // id apt_… o externalId del turno: el turno queda completed
});
```

### Solicitudes de ingreso

```csharp
var ingreso = await kuida.IntakeRequests.CreateAsync(new IntakeRequestCreateParams
{
    Patient = new PatientIdentity { Phone = "+54 9 342 555 0000", FullName = "Paciente Demo", Dni = "11111111" },
    Coverage = new IntakeCoverage { Insurer = "Obra Social Demo" },
    Summary = "Derivación para control",
});
```

### Tratamientos

```csharp
var tratamiento = await kuida.Treatments.CreateAsync(new TreatmentCreateParams
{
    Patient = "pat_…",
    Name = "Ibuprofeno 400 mg",
    Kind = "medication",
    Frequency = "Cada 8 horas",
});
```

### Eventos

La puerta genérica: el sistema avisa lo que pasó con un sobre común. El `Id` del evento es su clave de idempotencia. `Data` viaja tal cual (diccionario u objeto anónimo, sin convertir claves):

```csharp
var resultado = await kuida.Events.CreateAsync(new EventInput
{
    Id = "turno-123-atendido",
    Type = "visit.completed",
    Source = new EventSource { System = "mi-sistema", Version = "3.2" },
    Data = new
    {
        patient = new { phone = "+54 9 342 555 0000", fullName = "Paciente Demo", dni = "11111111" },
        visit = new { visitedAt = "2026-10-05T10:45:00-03:00", externalId = "turno-123" },
    },
});
Console.WriteLine(resultado.Results[0].Status); // processed, duplicate, unhandled, invalid o failed

// Lote de hasta 100:
var lote = await kuida.Events.CreateBatchAsync(new[] { evento1, evento2 });
```

Si ningún evento del lote entra, la API responde un error (400 o 500) y el SDK lanza la excepción; el detalle por evento queda en `ex.RawJson?.GetProperty("results")`.

### Webhooks

```csharp
var endpoint = await kuida.WebhookEndpoints.CreateAsync(new WebhookEndpointCreateParams
{
    Url = "https://mi-sistema.example.com/webhooks/kuida",
    EnabledEvents = new List<string> { "intake.ready", "visit.completed" },
});
var secreto = endpoint.Secret; // whsec_…: solo viene al crear, guárdelo

await kuida.WebhookEndpoints.UpdateAsync(endpoint.Id, new WebhookEndpointUpdateParams { EnabledEvents = new List<string> { "*" } });
var entrega = await kuida.WebhookEndpoints.PingAsync(endpoint.Id);
var entregas = await kuida.WebhookDeliveries.ListAsync(new WebhookDeliveryListParams { WebhookEndpoint = endpoint.Id });
await kuida.WebhookEndpoints.DeleteAsync(endpoint.Id);
```

## Paginación

`ListAsync` trae una página (`KuidaList<T>`, enumerable, con `Data` y `HasMore`). Para la siguiente, pase `StartingAfter` con el id del último:

```csharp
var pagina = await kuida.Patients.ListAsync(new PatientListParams { Limit = 50 });
while (true)
{
    foreach (var p in pagina) Console.WriteLine(p.FullName);
    if (!pagina.HasMore) break;
    pagina = await kuida.Patients.ListAsync(new PatientListParams { Limit = 50, StartingAfter = pagina.Data[pagina.Data.Count - 1].Id });
}
```

O deje que el SDK recorra todas las páginas:

```csharp
await foreach (var p in kuida.Patients.ListAutoPagingAsync(new PatientListParams { Limit = 100 }))
{
    Console.WriteLine(p.FullName);
}

// Sin await foreach (C# 7.3 en .NET Framework):
List<Patient> todos = await kuida.Patients.ListAllAsync(new PatientListParams { Limit = 100 });
```

## Errores

Toda respuesta no 2xx se convierte en una excepción que hereda de `KuidaException`:

| `error.type` | Excepción |
|---|---|
| `invalid_request_error` | `InvalidRequestException` |
| `authentication_error` | `AuthenticationException` |
| `permission_error` | `PermissionException` |
| `idempotency_error` | `IdempotencyException` |
| `rate_limit_error` | `RateLimitException` |
| `api_error` u otro, o un cuerpo que no es JSON | `ApiException` |
| sin respuesta (red, DNS, TLS, timeout) | `ApiConnectionException` |

Todas exponen `Message`, `HttpStatus`, `Type`, `Code`, `Param`, `RequestId`, `DocUrl`, `Headers`, `RawBody` y `RawJson`.

```csharp
try
{
    await kuida.Patients.RetrieveAsync("pat_noexiste");
}
catch (InvalidRequestException e) when (e.Code == "resource_missing")
{
    Console.WriteLine($"No existe ({e.HttpStatus}). Request-Id: {e.RequestId}");
}
catch (KuidaException e)
{
    Console.WriteLine($"{e.Type}/{e.Code}: {e.Message}");
}
```

Cada objeto devuelto conserva el JSON crudo (`RawJson`, incluidos campos que el SDK todavía no conoce) y la respuesta HTTP (`LastResponse`, con `StatusCode`, `Headers` y `RequestId`).

## Idempotencia

Todo `POST` lleva un header `Idempotency-Key`. Si no se pasa una, el SDK genera un UUID v4 y **reusa la misma clave en todos los reintentos**, así un reintento nunca duplica un paciente ni un turno. Para que un reintento propio (por ejemplo, tras reiniciar el proceso) también sea seguro, pase una clave estable:

```csharp
await kuida.Patients.CreateAsync(parametros, new RequestOptions { IdempotencyKey = "alta-paciente-11111111" });
```

La misma clave con otro cuerpo lanza `IdempotencyException`. La clave vive 24 horas.

## Reintentos

El SDK reintenta hasta `MaxRetries` veces (default 2) ante errores de conexión o timeout, `409 idempotency_key_in_use`, `429` y `500`/`502`/`503`/`504`. Nunca reintenta otros 4xx. La espera es `min(0,5 s × 2^intento, 8 s)` con ±25 % de variación; si la respuesta trae `Retry-After`, se respeta (hasta 60 s).

## Verificación de webhooks

Kuida firma cada webhook con HMAC-SHA256. Verifique la firma sobre el cuerpo **crudo**, nunca sobre un JSON re-serializado. `Webhook` no necesita cliente ni clave de API:

```csharp
// ASP.NET Core
app.MapPost("/webhooks/kuida", async (HttpRequest request) =>
{
    using var reader = new StreamReader(request.Body);
    var cuerpo = await reader.ReadToEndAsync();
    try
    {
        var evento = Webhook.ConstructEvent(cuerpo,
            request.Headers["x-kuida-signature"], request.Headers["x-kuida-timestamp"], secreto);
        if (evento.Type == "intake.ready")
        {
            var intake = evento.Data["intake"]; // JsonElement
        }
        return Results.Ok();
    }
    catch (SignatureVerificationException)
    {
        return Results.BadRequest();
    }
});
```

Rechaza con `SignatureVerificationException` si falta un header, si la firma no coincide o si el timestamp difiere más de 300 segundos de la hora actual (`tolerance`; 0 desactiva ese chequeo). `Webhook.VerifySignature(...)` solo verifica y devuelve `true`; `Webhook.ComputeSignature(...)` arma la firma para probar su endpoint. Responda 2xx en menos de 10 segundos.

## Versión de la API

El SDK manda `Kuida-Version: 2026-09-28` (`KuidaClient.ApiVersion`) en todos los pedidos. Para fijar otra versión, use `KuidaClientOptions.ApiVersion`. Los campos nuevos que agregue la API se ignoran sin error y quedan en `RawJson`.

## Desarrollo

```bash
node scripts/generate.mjs   # regenera src/Kuida/Generated desde ../openapi/kuida-v1.json
scripts/test.sh             # levanta el mock de conformidad y corre los tests
dotnet pack -c Release src/Kuida/Kuida.csproj
```

Los tests corren contra el build `net8.0`; para probar el build `netstandard2.0` (el de .NET Framework): `scripts/test.sh -p:KuidaTarget=netstandard2.0`.

## Licencia

MIT.
