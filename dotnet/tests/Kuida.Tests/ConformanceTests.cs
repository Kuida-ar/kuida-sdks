using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Xunit;

[assembly: CollectionBehavior(DisableTestParallelization = true)]

namespace Kuida.Tests
{
    /// <summary>
    /// Suite de conformidad común a todos los SDKs (SDK_DESIGN.md §9), contra el mock de
    /// conformance/mock-server.mjs. Lee KUIDA_API_BASE (default http://localhost:12111/api).
    /// </summary>
    public class ConformanceTests
    {
        private const string Key = "kd_test_000000000000_mocksecretmocksecret00";
        private const string LegacyKey = "kd_test_1e9ac7000000_mocksecretmocksecret00";
        private const string RevokedKey = "kd_test_dead00000000_mocksecretmocksecret00";
        private const string FlakyKey = "kd_test_fa11ed000000_mocksecretmocksecret00";
        private const string RateLimitKey = "kd_test_42900000000a_mocksecretmocksecret00";

        private static readonly Regex UuidV4 = new Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
        private static readonly HttpClient Control = new HttpClient();

        private static string ApiBase =>
            (Environment.GetEnvironmentVariable("KUIDA_API_BASE") is { Length: > 0 } b ? b : "http://localhost:12111/api").TrimEnd('/');

        private static string MockRoot => Regex.Replace(ApiBase, "/api$", "");

        private static KuidaClient Client(string key = Key, int? maxRetries = null, List<TimeSpan>? delays = null, string? baseUrl = null) =>
            new KuidaClient(key, new KuidaClientOptions
            {
                BaseUrl = baseUrl ?? ApiBase,
                MaxRetries = maxRetries,
                RetryDelay = (d, ct) =>
                {
                    delays?.Add(d);
                    return Task.CompletedTask;
                },
            });

        private static async Task Reset()
        {
            var r = await Control.PostAsync(MockRoot + "/__mock/reset", new StringContent("{}"));
            r.EnsureSuccessStatusCode();
        }

        private static async Task<List<JsonElement>> MockRequests()
        {
            var text = await Control.GetStringAsync(MockRoot + "/__mock/requests");
            using var doc = JsonDocument.Parse(text);
            return doc.RootElement.GetProperty("requests").EnumerateArray().Select(e => e.Clone()).ToList();
        }

        private static string? Header(JsonElement request, string name) =>
            request.GetProperty("headers").TryGetProperty(name, out var v) && v.ValueKind == JsonValueKind.String ? v.GetString() : null;

        private static PatientCreateParams DemoPatient(string phoneSuffix = "0000") => new PatientCreateParams
        {
            Phone = "+54 9 342 555 " + phoneSuffix,
            FullName = "Paciente Demo",
        };

        // 1
        [Fact]
        public async Task AccountRetrieve()
        {
            var account = await Client().Account.RetrieveAsync();
            Assert.Equal("account", account.Object);
            Assert.StartsWith("org_", account.Id);
            Assert.Contains("patients:write", account.ApiKey.Scopes);
            Assert.NotNull(account.LastResponse?.RequestId);
            Assert.Equal("account", account.RawJson.GetProperty("object").GetString());
        }

        // 2
        [Fact]
        public async Task Headers()
        {
            await Reset();
            var kuida = Client();
            await kuida.Account.RetrieveAsync();
            await kuida.Patients.CreateAsync(DemoPatient());
            var p = await kuida.Patients.CreateAsync(DemoPatient());

            var log = await MockRequests();
            Assert.Equal(3, log.Count);
            foreach (var r in log)
            {
                Assert.Equal("Bearer " + Key, Header(r, "authorization"));
                Assert.Equal("2026-09-28", Header(r, "kuida-version"));
                Assert.Matches(@"^Kuida/v1 DotNetBindings/\d+\.\d+\.\d+$", Header(r, "user-agent"));
                using var ua = JsonDocument.Parse(Header(r, "x-kuida-client-user-agent")!);
                Assert.Equal("dotnet", ua.RootElement.GetProperty("lang").GetString());
                Assert.Equal("0.1.0", ua.RootElement.GetProperty("bindings_version").GetString());
                Assert.True(ua.RootElement.TryGetProperty("lang_version", out _));
                Assert.True(ua.RootElement.TryGetProperty("platform", out _));
            }
            var get = log.Single(r => r.GetProperty("method").GetString() == "GET");
            Assert.Null(Header(get, "idempotency-key"));
            var posts = log.Where(r => r.GetProperty("method").GetString() == "POST").ToList();
            Assert.Equal(2, posts.Count);
            foreach (var post in posts)
            {
                Assert.Matches(UuidV4, Header(post, "idempotency-key"));
                Assert.Equal("application/json", Header(post, "content-type"));
            }
            Assert.NotEqual(Header(posts[0], "idempotency-key"), Header(posts[1], "idempotency-key"));
            Assert.False(p.LastResponse!.IdempotentReplayed);
        }

        // 3
        [Fact]
        public async Task PatientsCrud()
        {
            await Reset();
            var kuida = Client();
            var created = await kuida.Patients.CreateAsync(new PatientCreateParams
            {
                Phone = "+54 9 342 555 0000",
                FullName = "Paciente Demo",
                Dni = "11111111",
            });
            Assert.StartsWith("pat_", created.Id);
            Assert.Equal("patient", created.Object);
            Assert.Equal("Paciente Demo", created.FullName);

            var retrieved = await kuida.Patients.RetrieveAsync(created.Id);
            Assert.Equal(created.Id, retrieved.Id);
            Assert.Equal("11111111", retrieved.Dni);

            var updated = await kuida.Patients.UpdateAsync(created.Id, new PatientUpdateParams { Email = "paciente.demo@example.com" });
            Assert.Equal("paciente.demo@example.com", updated.Email);

            var page = await kuida.Patients.ListAsync(new PatientListParams { Phone = "+54 9 342 555 0000" });
            Assert.Equal("list", page.Object);
            var only = Assert.Single(page.Data);
            Assert.Equal(created.Id, only.Id);
            Assert.Equal("paciente.demo@example.com", only.Email);
            Assert.Equal(created.Id, only.RawJson.GetProperty("id").GetString());

            var patch = (await MockRequests()).Single(r => r.GetProperty("method").GetString() == "PATCH");
            Assert.Null(Header(patch, "idempotency-key"));
            Assert.Equal("{\"email\":\"paciente.demo@example.com\"}", patch.GetProperty("body").GetRawText());
        }

        // 4
        [Fact]
        public async Task IdempotencyExplicit()
        {
            await Reset();
            var kuida = Client();
            var key = Guid.NewGuid().ToString();
            var first = await kuida.Patients.CreateAsync(DemoPatient("0101"), new RequestOptions { IdempotencyKey = key });
            var second = await kuida.Patients.CreateAsync(DemoPatient("0101"), new RequestOptions { IdempotencyKey = key });
            Assert.Equal(first.Id, second.Id);
            Assert.True(second.LastResponse!.IdempotentReplayed);

            var ex = await Assert.ThrowsAsync<IdempotencyException>(() =>
                kuida.Patients.CreateAsync(DemoPatient("0102"), new RequestOptions { IdempotencyKey = key }));
            Assert.Equal("idempotency_error", ex.Type);
            Assert.Equal("idempotency_key_reused", ex.Code);

            var posts = (await MockRequests()).Where(r => r.GetProperty("method").GetString() == "POST").ToList();
            Assert.All(posts, r => Assert.Equal(key, Header(r, "idempotency-key")));
        }

        private static async Task<List<Patient>> CreateFive(KuidaClient kuida)
        {
            var created = new List<Patient>();
            for (var i = 1; i <= 5; i++) created.Add(await kuida.Patients.CreateAsync(DemoPatient("020" + i)));
            return created;
        }

        // 5
        [Fact]
        public async Task PaginationManual()
        {
            await Reset();
            var kuida = Client();
            var created = await CreateFive(kuida);

            var page1 = await kuida.Patients.ListAsync(new PatientListParams { Limit = 2 });
            Assert.Equal(2, page1.Data.Count);
            Assert.True(page1.HasMore);

            var page2 = await kuida.Patients.ListAsync(new PatientListParams { Limit = 2, StartingAfter = page1.Data.Last().Id });
            Assert.Equal(2, page2.Data.Count);
            Assert.Empty(page1.Data.Select(p => p.Id).Intersect(page2.Data.Select(p => p.Id)));

            var page3 = await kuida.Patients.ListAsync(new PatientListParams { Limit = 2, StartingAfter = page2.Data.Last().Id });
            Assert.Single(page3.Data);
            Assert.False(page3.HasMore);

            var seen = page1.Concat(page2).Concat(page3).Select(p => p.Id).ToList();
            Assert.Equal(created.Select(p => p.Id).OrderBy(x => x), seen.OrderBy(x => x));

            var gets = (await MockRequests()).Where(r => r.GetProperty("method").GetString() == "GET").ToList();
            Assert.Equal("2", gets[0].GetProperty("query").GetProperty("limit").GetString());
            Assert.Equal(page1.Data.Last().Id, gets[1].GetProperty("query").GetProperty("startingAfter").GetString());
        }

        // 6
        [Fact]
        public async Task PaginationAuto()
        {
            await Reset();
            var kuida = Client();
            var created = await CreateFive(kuida);

            var ids = new List<string>();
            await foreach (var p in kuida.Patients.ListAutoPagingAsync(new PatientListParams { Limit = 2 })) ids.Add(p.Id);
            Assert.Equal(5, ids.Count);
            Assert.Equal(5, ids.Distinct().Count());
            Assert.Equal(created.Select(p => p.Id).OrderBy(x => x), ids.OrderBy(x => x));

            var lists = (await MockRequests()).Where(r => r.GetProperty("method").GetString() == "GET").ToList();
            Assert.Equal(3, lists.Count);
            Assert.All(lists, r => Assert.Equal("2", r.GetProperty("query").GetProperty("limit").GetString()));

            // Variante sin await foreach, para C# 7.3.
            var all = await kuida.Patients.ListAllAsync(new PatientListParams { Limit = 2 });
            Assert.Equal(ids, all.Select(p => p.Id).ToList());
        }

        // 7
        [Fact]
        public async Task AppointmentsFlow()
        {
            await Reset();
            var kuida = Client();
            var start = new DateTimeOffset(2026, 10, 1, 14, 30, 0, TimeSpan.FromHours(-3));
            var apt = await kuida.Appointments.CreateAsync(new AppointmentCreateParams
            {
                Patient = new PatientIdentity { Phone = "+54 9 342 555 0000", FullName = "Paciente Demo", Dni = "11111111" },
                Doctor = new DoctorIdentity { ExternalId = "prof-1", FullName = "Profesional Demo" },
                StartAt = start,
                Type = "Control",
            });
            Assert.StartsWith("apt_", apt.Id);
            Assert.StartsWith("pat_", apt.Patient);
            Assert.StartsWith("doc_", apt.Doctor);
            Assert.Equal(start, apt.StartAt);
            Assert.Equal("confirmed", apt.Status);

            var moved = start.AddDays(1);
            var updated = await kuida.Appointments.UpdateAsync(apt.Id, new AppointmentUpdateParams { StartAt = moved });
            Assert.Equal(moved, updated.StartAt);

            var cancelled = await kuida.Appointments.CancelAsync(apt.Id);
            Assert.Equal("cancelled", cancelled.Status);
            Assert.NotNull(cancelled.CancelledAt);

            var cancelRequest = (await MockRequests()).Single(r => r.GetProperty("path").GetString()!.EndsWith("/cancel"));
            Assert.Equal("{}", cancelRequest.GetProperty("body").GetRawText());

            var ex = await Assert.ThrowsAsync<InvalidRequestException>(() =>
                kuida.Appointments.UpdateAsync(apt.Id, new AppointmentUpdateParams { StartAt = moved.AddDays(1) }));
            Assert.Equal(400, ex.HttpStatus);
        }

        // 8
        [Fact]
        public async Task VisitClosesAppointment()
        {
            await Reset();
            var kuida = Client();
            var patient = await kuida.Patients.CreateAsync(DemoPatient());
            var apt = await kuida.Appointments.CreateAsync(new AppointmentCreateParams
            {
                Patient = patient.Id,
                StartAt = new DateTimeOffset(2026, 10, 5, 10, 0, 0, TimeSpan.FromHours(-3)),
                ExternalId = "turno-123",
            });

            var visit = await kuida.Visits.CreateAsync(new VisitCreateParams
            {
                Patient = patient.Id,
                VisitedAt = new DateTimeOffset(2026, 10, 5, 10, 45, 0, TimeSpan.FromHours(-3)),
                Appointment = "turno-123",
                ExternalId = "consulta-123",
            });
            Assert.StartsWith("vis_", visit.Id);
            Assert.Equal(apt.Id, visit.Appointment);
            Assert.Equal(patient.Id, visit.Patient);

            var after = await kuida.Appointments.RetrieveAsync(apt.Id);
            Assert.Equal("completed", after.Status);

            Assert.Equal(visit.Id, (await kuida.Visits.RetrieveAsync(visit.Id)).Id);
            Assert.Contains(await kuida.Visits.ListAsync(new VisitListParams { Patient = patient.Id }), v => v.Id == visit.Id);
        }

        // 9
        [Fact]
        public async Task IntakeAndTreatment()
        {
            await Reset();
            var kuida = Client();
            var patient = await kuida.Patients.CreateAsync(DemoPatient());

            // En el OpenAPI, IntakeRequestCreateParams.patient es PatientIdentity (no acepta pat_…):
            // se referencia al mismo paciente por teléfono y Kuida lo reconoce.
            var intake = await kuida.IntakeRequests.CreateAsync(new IntakeRequestCreateParams
            {
                Patient = new PatientIdentity { Phone = "+54 9 342 555 0000", FullName = "Paciente Demo" },
                Coverage = new IntakeCoverage { Insurer = "Obra Social Demo" },
                Summary = "Ingreso de prueba",
            });
            Assert.StartsWith("int_", intake.Id);
            Assert.Equal(patient.Id, intake.Patient);
            Assert.Equal("new", intake.Status);
            Assert.Equal("Ingreso de prueba", intake.Data["summary"].GetString());
            Assert.Equal(intake.Id, (await kuida.IntakeRequests.RetrieveAsync(intake.Id)).Id);

            var treatment = await kuida.Treatments.CreateAsync(new TreatmentCreateParams
            {
                Patient = patient.Id,
                Name = "Ibuprofeno 400 mg",
                Kind = "medication",
                Frequency = "Cada 8 horas",
            });
            Assert.StartsWith("trt_", treatment.Id);
            Assert.Equal(patient.Id, treatment.Patient);
            Assert.True(treatment.Active);
            var bodies = (await MockRequests()).Where(r => r.GetProperty("path").GetString() == "/v1/treatments").ToList();
            Assert.Equal(patient.Id, bodies[0].GetProperty("body").GetProperty("patient").GetString());

            var list = await kuida.Treatments.ListAsync(new TreatmentListParams { Patient = patient.Id, Active = true });
            Assert.Contains(list, t => t.Id == treatment.Id);
            var listRequest = (await MockRequests()).Last();
            Assert.Equal("true", listRequest.GetProperty("query").GetProperty("active").GetString());
        }

        // 10
        [Fact]
        public async Task EventsBatch()
        {
            await Reset();
            var kuida = Client();
            var valid = new EventInput
            {
                Id = "sistema-paciente-1",
                Type = "patient.upserted",
                OccurredAt = DateTimeOffset.UtcNow,
                Source = new EventSource { System = "sistema-demo", Version = "1.0" },
                Data = new Dictionary<string, object?> { ["phone"] = "+54 9 342 555 0000", ["fullName"] = "Paciente Demo", ["dni"] = "11111111" },
            };
            var invalid = new EventInput { Id = "sistema-invalido-1", Type = "no.existe", Data = new { foo = "bar" } };

            var batch = await kuida.Events.CreateBatchAsync(new[] { valid, invalid });
            Assert.Equal("event_batch", batch.Object);
            Assert.Equal(2, batch.Results.Count);
            Assert.Equal("processed", batch.Results[0].Status);
            Assert.True(batch.Results[0].Accepted);
            Assert.StartsWith("evt_", batch.Results[0].Event);
            Assert.Equal("patient", batch.Results[0].Result!.Object);
            Assert.Equal("invalid", batch.Results[1].Status);
            Assert.False(batch.Results[1].Accepted);

            var again = await kuida.Events.CreateAsync(valid);
            Assert.Equal("duplicate", Assert.Single(again.Results).Status);

            // Los datos libres viajan tal cual, sin convertir claves.
            var sent = (await MockRequests()).First(r => r.GetProperty("path").GetString() == "/v1/events");
            var data = sent.GetProperty("body").GetProperty("events")[0].GetProperty("data");
            Assert.Equal("Paciente Demo", data.GetProperty("fullName").GetString());

            var evt = await kuida.Events.RetrieveAsync(batch.Results[0].Event!);
            Assert.Equal("patient.upserted", evt.Type);
            Assert.Equal("Paciente Demo", evt.Data["fullName"].GetString());
            Assert.Contains(await kuida.Events.ListAsync(new EventListParams { Type = "patient.upserted" }), e => e.Id == evt.Id);

            // Ninguno entra: error normal, con el detalle por evento en el cuerpo crudo.
            var allInvalid = await Assert.ThrowsAsync<InvalidRequestException>(() => kuida.Events.CreateAsync(invalid));
            Assert.Equal(400, allInvalid.HttpStatus);
            var results = allInvalid.RawJson!.Value.GetProperty("results");
            Assert.Equal("invalid", results[0].GetProperty("status").GetString());
            Assert.Contains("results", allInvalid.RawBody);
        }

        // 11
        [Fact]
        public async Task WebhookEndpointsFlow()
        {
            await Reset();
            var kuida = Client();
            var endpoint = await kuida.WebhookEndpoints.CreateAsync(new WebhookEndpointCreateParams
            {
                Url = "https://example.com/webhooks/kuida",
                EnabledEvents = new List<string> { "intake.ready", "visit.completed" },
                Description = "Sistema de gestión demo",
            });
            Assert.StartsWith("we_", endpoint.Id);
            Assert.StartsWith("whsec_", endpoint.Secret);
            Assert.Equal("enabled", endpoint.Status);

            var retrieved = await kuida.WebhookEndpoints.RetrieveAsync(endpoint.Id);
            Assert.Null(retrieved.Secret);
            Assert.False(retrieved.RawJson.TryGetProperty("secret", out _));

            var updated = await kuida.WebhookEndpoints.UpdateAsync(endpoint.Id, new WebhookEndpointUpdateParams
            {
                EnabledEvents = new List<string> { "*" },
                Description = "Todos los eventos",
            });
            Assert.Equal(new[] { "*" }, updated.EnabledEvents);
            Assert.Equal("Todos los eventos", updated.Description);

            var delivery = await kuida.WebhookEndpoints.PingAsync(endpoint.Id);
            Assert.Equal("webhook_delivery", delivery.Object);
            Assert.Equal("pending", delivery.Status);
            Assert.Equal("webhook.ping", delivery.EventType);
            Assert.Equal("webhook.ping", delivery.Payload["type"].GetString());
            var pingRequest = (await MockRequests()).Single(r => r.GetProperty("path").GetString()!.EndsWith("/ping"));
            Assert.Equal("{}", pingRequest.GetProperty("body").GetRawText());

            var deliveries = await kuida.WebhookDeliveries.ListAsync(new WebhookDeliveryListParams { WebhookEndpoint = endpoint.Id });
            Assert.Contains(deliveries, d => d.Id == delivery.Id);
            Assert.Equal(delivery.Id, (await kuida.WebhookDeliveries.RetrieveAsync(delivery.Id)).Id);
            Assert.Contains(await kuida.WebhookEndpoints.ListAsync(), e => e.Id == endpoint.Id);

            var deleted = await kuida.WebhookEndpoints.DeleteAsync(endpoint.Id);
            Assert.True(deleted.Deleted);
            Assert.Equal(endpoint.Id, deleted.Id);
            var gone = await Assert.ThrowsAsync<InvalidRequestException>(() => kuida.WebhookEndpoints.RetrieveAsync(endpoint.Id));
            Assert.Equal(404, gone.HttpStatus);
        }

        // 12
        [Fact]
        public async Task Errors()
        {
            // Sin clave: error al construir, no al primer pedido.
            var previous = Environment.GetEnvironmentVariable("KUIDA_API_KEY");
            Environment.SetEnvironmentVariable("KUIDA_API_KEY", null);
            try
            {
                var missing = Assert.Throws<AuthenticationException>(() => new KuidaClient(null, new KuidaClientOptions { BaseUrl = ApiBase }));
                Assert.Equal("api_key_missing", missing.Code);
            }
            finally
            {
                Environment.SetEnvironmentVariable("KUIDA_API_KEY", previous);
            }

            var invalid = await Assert.ThrowsAsync<AuthenticationException>(() => Client("kd_test_clave_invalida").Account.RetrieveAsync());
            Assert.Equal(401, invalid.HttpStatus);
            Assert.Equal("api_key_invalid", invalid.Code);

            var revoked = await Assert.ThrowsAsync<AuthenticationException>(() => Client(RevokedKey).Account.RetrieveAsync());
            Assert.Equal("api_key_revoked", revoked.Code);
            Assert.Equal("authentication_error", revoked.Type);

            var perm = await Assert.ThrowsAsync<PermissionException>(() => Client(LegacyKey).Patients.ListAsync());
            Assert.Equal(403, perm.HttpStatus);
            Assert.Equal("scope_missing", perm.Code);

            var notFound = await Assert.ThrowsAsync<InvalidRequestException>(() => Client().Patients.RetrieveAsync("pat_noexiste"));
            Assert.Equal(404, notFound.HttpStatus);
            Assert.Equal("resource_missing", notFound.Code);
            Assert.StartsWith("req_", notFound.RequestId);
            Assert.StartsWith("https://", notFound.DocUrl);
            Assert.Equal(notFound.RequestId, notFound.Headers["Request-Id"]);
            Assert.Contains("\"error\"", notFound.RawBody);

            var noPhone = await Assert.ThrowsAsync<InvalidRequestException>(() =>
                Client().Patients.CreateAsync(new PatientCreateParams { FullName = "Paciente Demo" }));
            Assert.Equal("phone", noPhone.Param);
            Assert.Equal("parameter_missing", noPhone.Code);
            Assert.IsAssignableFrom<KuidaException>(noPhone);
        }

        // 13
        [Fact]
        public async Task Retries5xx()
        {
            await Reset();
            var delays = new List<TimeSpan>();
            var patient = await Client(FlakyKey, delays: delays).Patients.CreateAsync(DemoPatient());
            Assert.StartsWith("pat_", patient.Id);
            Assert.Single(delays);
            Assert.InRange(delays[0].TotalSeconds, 0.375, 0.625); // 0.5 s ± 25 %

            var posts = (await MockRequests()).Where(r => r.GetProperty("method").GetString() == "POST").ToList();
            Assert.Equal(2, posts.Count);
            Assert.Matches(UuidV4, Header(posts[0], "idempotency-key"));
            Assert.Equal(Header(posts[0], "idempotency-key"), Header(posts[1], "idempotency-key"));
        }

        // 14
        [Fact]
        public async Task Retries429()
        {
            await Reset();
            var delays = new List<TimeSpan>();
            var account = await Client(RateLimitKey, delays: delays).Account.RetrieveAsync();
            Assert.Equal("account", account.Object);
            Assert.Equal(new[] { TimeSpan.FromSeconds(1) }, delays); // Retry-After: 1
            Assert.Equal(2, (await MockRequests()).Count);
        }

        // 15
        [Fact]
        public async Task NoRetry()
        {
            await Reset();
            var delays = new List<TimeSpan>();
            var ex = await Assert.ThrowsAsync<ApiException>(() => Client(FlakyKey, maxRetries: 0, delays: delays).Patients.CreateAsync(DemoPatient()));
            Assert.Equal(503, ex.HttpStatus);
            Assert.Equal("api_error", ex.Type);
            Assert.Empty(delays);
            Assert.Single(await MockRequests());
        }

        // 16
        [Fact]
        public async Task ConnectionError()
        {
            var ex = await Assert.ThrowsAsync<ApiConnectionException>(() =>
                Client(maxRetries: 0, baseUrl: "http://127.0.0.1:1/api").Account.RetrieveAsync());
            Assert.Null(ex.HttpStatus);
            Assert.IsAssignableFrom<KuidaException>(ex);

            // Con reintentos, reintenta los errores de conexión.
            var delays = new List<TimeSpan>();
            await Assert.ThrowsAsync<ApiConnectionException>(() =>
                Client(maxRetries: 2, delays: delays, baseUrl: "http://127.0.0.1:1/api").Account.RetrieveAsync());
            Assert.Equal(2, delays.Count);
        }

        // 17
        [Fact]
        public void WebhookSignature()
        {
            var v = WebhookVectors.Load();
            var ts = long.Parse(v.Timestamp);
            var at = DateTimeOffset.FromUnixTimeSeconds(ts);

            Assert.True(Webhook.VerifySignature(v.Payload, v.SignatureHeader, v.Timestamp, v.Secret, now: at));
            Assert.True(Webhook.VerifySignature(v.Payload, v.SignatureHeader, v.Timestamp, v.Secret, tolerance: 0));
            Assert.True(Webhook.VerifySignature(System.Text.Encoding.UTF8.GetBytes(v.Payload), v.SignatureHeader, v.Timestamp, v.Secret, tolerance: 0));

            var evt = Webhook.ConstructEvent(v.Payload, v.SignatureHeader, v.Timestamp, v.Secret, now: at);
            Assert.Equal("event", evt.Object);
            Assert.Equal("intake.ready", evt.Type);
            Assert.Equal("2026-09-28", evt.ApiVersion);
            Assert.False(evt.Livemode);
            Assert.Equal("cmockintake0001", evt.Ref);
            Assert.Equal("LISTA", evt.Data["intake"].GetProperty("status").GetString());

            Assert.Throws<SignatureVerificationException>(() => Webhook.VerifySignature(v.Payload, v.SignatureHeader, v.Timestamp, v.WrongSecret, now: at));
            Assert.Throws<SignatureVerificationException>(() => Webhook.VerifySignature(v.TamperedPayload, v.SignatureHeader, v.Timestamp, v.Secret, now: at));
            Assert.Throws<SignatureVerificationException>(() => Webhook.VerifySignature(v.Payload, v.SignatureHeader, v.Timestamp, v.Secret, now: at.AddSeconds(301)));
            Assert.Throws<SignatureVerificationException>(() => Webhook.VerifySignature(v.Payload, v.SignatureHeader, v.Timestamp, v.Secret));
            Assert.Throws<SignatureVerificationException>(() => Webhook.VerifySignature(v.Payload, null, v.Timestamp, v.Secret, tolerance: 0));
            Assert.Throws<SignatureVerificationException>(() => Webhook.VerifySignature(v.Payload, v.SignatureHeader, null, v.Secret, tolerance: 0));
            Assert.Throws<SignatureVerificationException>(() => Webhook.ConstructEvent(v.TamperedPayload, v.SignatureHeader, v.Timestamp, v.Secret, tolerance: 0));

            // Un cuerpo propio firmado ahora verifica con la tolerancia default.
            var now = DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString();
            const string body = "{\"id\":\"whd_propio\",\"object\":\"event\",\"type\":\"webhook.ping\",\"apiVersion\":\"2026-09-28\",\"livemode\":false,\"occurredAt\":\"2026-09-28T12:00:00.000Z\",\"ref\":\"ping\",\"data\":{\"message\":\"hola\"}}";
            var signature = Webhook.ComputeSignature(body, now, v.Secret);
            Assert.True(Webhook.VerifySignature(body, signature, now, v.Secret));
            Assert.Equal("webhook.ping", Webhook.ConstructEvent(body, signature, now, v.Secret).Type);
        }

        private sealed class WebhookVectors
        {
            public string Secret { get; set; } = "";
            public string Timestamp { get; set; } = "";
            public string Payload { get; set; } = "";
            public string SignatureHeader { get; set; } = "";
            public string WrongSecret { get; set; } = "";
            public string TamperedPayload { get; set; } = "";

            public static WebhookVectors Load()
            {
                var dir = new DirectoryInfo(AppContext.BaseDirectory);
                while (dir != null)
                {
                    var candidate = Path.Combine(dir.FullName, "conformance", "webhook-vectors.json");
                    if (File.Exists(candidate))
                    {
                        return JsonSerializer.Deserialize<WebhookVectors>(File.ReadAllText(candidate),
                            new JsonSerializerOptions { PropertyNameCaseInsensitive = true })!;
                    }
                    dir = dir.Parent;
                }
                throw new FileNotFoundException("No se encontró conformance/webhook-vectors.json subiendo desde " + AppContext.BaseDirectory);
            }
        }
    }
}
