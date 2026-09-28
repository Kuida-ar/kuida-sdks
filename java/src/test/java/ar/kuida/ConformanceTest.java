package ar.kuida;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.kuida.exception.ApiConnectionException;
import ar.kuida.exception.ApiException;
import ar.kuida.exception.AuthenticationException;
import ar.kuida.exception.IdempotencyException;
import ar.kuida.exception.InvalidRequestException;
import ar.kuida.exception.PermissionException;
import ar.kuida.exception.SignatureVerificationException;
import ar.kuida.model.Account;
import ar.kuida.model.Appointment;
import ar.kuida.model.AppointmentCreateParams;
import ar.kuida.model.AppointmentUpdateParams;
import ar.kuida.model.DeletedObject;
import ar.kuida.model.DoctorIdentity;
import ar.kuida.model.DoctorReference;
import ar.kuida.model.EventBatchResponse;
import ar.kuida.model.EventInput;
import ar.kuida.model.IntakeCoverage;
import ar.kuida.model.IntakeRequest;
import ar.kuida.model.IntakeRequestCreateParams;
import ar.kuida.model.Patient;
import ar.kuida.model.PatientCreateParams;
import ar.kuida.model.PatientIdentity;
import ar.kuida.model.PatientListParams;
import ar.kuida.model.PatientReference;
import ar.kuida.model.PatientUpdateParams;
import ar.kuida.model.Treatment;
import ar.kuida.model.TreatmentCreateParams;
import ar.kuida.model.Visit;
import ar.kuida.model.VisitCreateParams;
import ar.kuida.model.WebhookDelivery;
import ar.kuida.model.WebhookDeliveryListParams;
import ar.kuida.model.WebhookEndpoint;
import ar.kuida.model.WebhookEndpointCreateParams;
import ar.kuida.model.WebhookEndpointUpdateParams;
import ar.kuida.model.WebhookEvent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Suite de conformidad (SDK_DESIGN.md §9) contra conformance/mock-server.mjs.
 *
 * <p>Correr con el mock levantado: {@code KUIDA_API_BASE=http://localhost:12111/api mvn test}.
 */
class ConformanceTest {
  static final Charset UTF_8 = Charset.forName("UTF-8");
  static final String BASE = System.getenv("KUIDA_API_BASE") != null ? System.getenv("KUIDA_API_BASE") : "http://localhost:12111/api";
  static final String KEY = "kd_test_000000000000_mocksecretmocksecret00";
  static final String KEY_EVENTS_ONLY = "kd_test_1e9ac7000000_mocksecretmocksecret00";
  static final String KEY_REVOKED = "kd_test_dead00000000_mocksecretmocksecret00";
  static final String KEY_FLAKY = "kd_test_fa11ed000000_mocksecretmocksecret00";
  static final String KEY_RATE_LIMITED = "kd_test_42900000000a_mocksecretmocksecret00";
  static final Pattern UUID_V4 = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

  static KuidaClient client(String key) {
    return KuidaClient.builder().apiKey(key).baseUrl(BASE).sleeper(Sleeper.NONE).build();
  }

  static String mockRoot() {
    return BASE.replaceAll("/api/?$", "");
  }

  static JsonElement mock(String method, String path) throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(mockRoot() + path).openConnection();
    c.setRequestMethod(method);
    if ("POST".equals(method)) {
      c.setDoOutput(true);
      OutputStream out = c.getOutputStream();
      out.write("{}".getBytes(UTF_8));
      out.close();
    }
    InputStream in = c.getInputStream();
    java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    int n;
    while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
    in.close();
    return JsonParser.parseString(new String(buf.toByteArray(), UTF_8));
  }

  static List<JsonObject> requests() throws IOException {
    List<JsonObject> out = new ArrayList<JsonObject>();
    for (JsonElement e : mock("GET", "/__mock/requests").getAsJsonObject().getAsJsonArray("requests")) out.add(e.getAsJsonObject());
    return out;
  }

  static String header(JsonObject request, String name) {
    JsonElement v = request.getAsJsonObject("headers").get(name);
    return v == null || v.isJsonNull() ? null : v.getAsString();
  }

  KuidaClient kuida;

  @BeforeEach
  void reset() throws IOException {
    mock("POST", "/__mock/reset");
    kuida = client(KEY);
  }

  // 1
  @Test
  void accountRetrieve() {
    Account account = kuida.account().retrieve();
    assertEquals("account", account.getObject());
    assertNotNull(account.getApiKey());
    assertTrue(account.getApiKey().getScopes().contains("patients:write"));
    assertNotNull(account.getLastResponse().getRequestId());
  }

  // 2
  @Test
  void headers() throws IOException {
    kuida.account().retrieve();
    kuida.patients().create(PatientCreateParams.builder().phone("+54 9 342 555 0000").fullName("Paciente Demo").build());
    List<JsonObject> log = requests();
    assertEquals(2, log.size());
    for (JsonObject r : log) {
      assertEquals("Bearer " + KEY, header(r, "authorization"));
      assertEquals("2026-09-28", header(r, "kuida-version"));
      assertTrue(Pattern.matches("^Kuida/v1 JavaBindings/\\d+\\.\\d+\\.\\d+$", header(r, "user-agent")), header(r, "user-agent"));
      JsonObject ua = JsonParser.parseString(header(r, "x-kuida-client-user-agent")).getAsJsonObject();
      assertEquals("java", ua.get("lang").getAsString());
      assertEquals(KuidaClient.VERSION, ua.get("bindings_version").getAsString());
      assertNotNull(ua.get("lang_version"));
      assertNotNull(ua.get("platform"));
      if ("GET".equals(r.get("method").getAsString())) {
        assertNull(header(r, "idempotency-key"));
      } else {
        assertTrue(UUID_V4.matcher(header(r, "idempotency-key")).matches(), header(r, "idempotency-key"));
        assertEquals("application/json", header(r, "content-type"));
      }
    }
  }

  // 3
  @Test
  void patientsCrud() throws IOException {
    Patient created = kuida.patients().create(PatientCreateParams.builder()
        .phone("+54 9 342 555 0000").fullName("Paciente Demo").dni("11111111").build());
    assertTrue(created.getId().startsWith("pat_"));
    assertEquals("Paciente Demo", created.getFullName());
    assertNotNull(created.getCreatedAt());

    Patient retrieved = kuida.patients().retrieve(created.getId());
    assertEquals(created.getId(), retrieved.getId());

    Patient updated = kuida.patients().update(created.getId(), PatientUpdateParams.builder().email("paciente.demo@example.com").build());
    assertEquals("paciente.demo@example.com", updated.getEmail());

    KuidaList<Patient> found = kuida.patients().list(PatientListParams.builder().phone("+54 9 342 555 0000").build());
    assertEquals(1, found.getData().size());
    assertEquals(created.getId(), found.getData().get(0).getId());

    // El PATCH llega como PATCH, sin Idempotency-Key.
    JsonObject patch = null;
    for (JsonObject r : requests()) if (r.get("path").getAsString().equals("/v1/patients/" + created.getId()) && !"GET".equals(r.get("method").getAsString())) patch = r;
    assertNotNull(patch);
    assertEquals("PATCH", patch.get("method").getAsString());
    assertNull(header(patch, "idempotency-key"));
    assertEquals("paciente.demo@example.com", patch.getAsJsonObject("body").get("email").getAsString());
  }

  // 4
  @Test
  void idempotencyExplicit() {
    RequestOptions opts = RequestOptions.builder().idempotencyKey("alta-HC-1234").build();
    Patient a = kuida.patients().create(PatientCreateParams.builder().phone("+54 9 342 555 0000").build(), opts);
    Patient b = kuida.patients().create(PatientCreateParams.builder().phone("+54 9 342 555 0000").build(), opts);
    assertEquals(a.getId(), b.getId());
    assertTrue(b.getLastResponse().isIdempotentReplayed());
    IdempotencyException e = assertThrows(IdempotencyException.class, () ->
        kuida.patients().create(PatientCreateParams.builder().phone("+54 9 342 555 0001").build(), opts));
    assertEquals("idempotency_error", e.getType());
    assertEquals(400, e.getHttpStatus());
  }

  static List<String> createPatients(KuidaClient kuida, int n) {
    List<String> ids = new ArrayList<String>();
    for (int i = 0; i < n; i++) {
      ids.add(kuida.patients().create(PatientCreateParams.builder().phone("+54 9 342 555 000" + i).fullName("Paciente Demo " + i).build()).getId());
    }
    return ids;
  }

  // 5
  @Test
  void paginationManual() {
    createPatients(kuida, 5);
    KuidaList<Patient> first = kuida.patients().list(PatientListParams.builder().limit(2).build());
    assertEquals(2, first.getData().size());
    assertTrue(first.hasMore());
    String last = first.getData().get(1).getId();
    KuidaList<Patient> second = kuida.patients().list(PatientListParams.builder().limit(2).startingAfter(last).build());
    assertEquals(2, second.getData().size());
    Set<String> seen = new HashSet<String>();
    for (Patient p : first.getData()) seen.add(p.getId());
    for (Patient p : second.getData()) assertTrue(seen.add(p.getId()), "repetido: " + p.getId());
  }

  // 6
  @Test
  void paginationAuto() throws IOException {
    List<String> created = createPatients(kuida, 5);
    Set<String> seen = new HashSet<String>();
    int count = 0;
    for (Patient p : kuida.patients().list(PatientListParams.builder().limit(2).build()).autoPagingIterable()) {
      assertTrue(seen.add(p.getId()), "repetido: " + p.getId());
      count++;
    }
    assertEquals(5, count);
    assertEquals(new HashSet<String>(created), seen);
    int gets = 0;
    for (JsonObject r : requests()) {
      if ("GET".equals(r.get("method").getAsString())) {
        gets++;
        assertEquals("2", r.getAsJsonObject("query").get("limit").getAsString());
      }
    }
    assertEquals(3, gets);
  }

  // 7
  @Test
  void appointmentsFlow() {
    OffsetDateTime start = OffsetDateTime.of(2026, 10, 1, 9, 30, 0, 0, ZoneOffset.ofHours(-3));
    Appointment apt = kuida.appointments().create(AppointmentCreateParams.builder()
        .patient(PatientReference.identity(PatientIdentity.builder().phone("+54 9 342 555 0000").fullName("Paciente Demo").build()))
        .doctor(DoctorReference.identity(DoctorIdentity.builder().externalId("MED-1").fullName("Dra. Demo").build()))
        .startAt(start)
        .type("Control")
        .build());
    assertTrue(apt.getId().startsWith("apt_"));
    assertTrue(apt.getPatient().startsWith("pat_"));
    assertTrue(apt.getDoctor().startsWith("doc_"));
    assertEquals("confirmed", apt.getStatus());

    OffsetDateTime moved = start.plusDays(1);
    Appointment updated = kuida.appointments().update(apt.getId(), AppointmentUpdateParams.builder().startAt(moved).build());
    assertEquals(moved.toInstant(), updated.getStartAt().toInstant());

    Appointment cancelled = kuida.appointments().cancel(apt.getId());
    assertEquals("cancelled", cancelled.getStatus());
    assertNotNull(cancelled.getCancelledAt());

    InvalidRequestException e = assertThrows(InvalidRequestException.class, () ->
        kuida.appointments().update(apt.getId(), AppointmentUpdateParams.builder().type("Otro").build()));
    assertEquals(400, e.getHttpStatus());
  }

  // 8
  @Test
  void visitClosesAppointment() {
    Appointment apt = kuida.appointments().create(AppointmentCreateParams.builder()
        .patient(PatientIdentity.builder().phone("+54 9 342 555 0000").build())
        .startAt("2026-10-01T09:30:00-03:00")
        .externalId("turno-123")
        .build());
    Visit visit = kuida.visits().create(VisitCreateParams.builder()
        .patient(apt.getPatient())
        .visitedAt(OffsetDateTime.of(2026, 10, 1, 10, 0, 0, 0, ZoneOffset.ofHours(-3)))
        .appointment("turno-123")
        .externalId("consulta-123")
        .build());
    assertTrue(visit.getId().startsWith("vis_"));
    assertEquals(apt.getId(), visit.getAppointment());
    assertEquals("completed", kuida.appointments().retrieve(apt.getId()).getStatus());
  }

  // 9
  @Test
  void intakeAndTreatment() {
    IntakeRequest intake = kuida.intakeRequests().create(IntakeRequestCreateParams.builder()
        .patient(PatientIdentity.builder().phone("+54 9 342 555 0000").fullName("Paciente Demo").dni("11111111").build())
        .coverage(IntakeCoverage.builder().insurer("Obra Social Demo").build())
        .summary("Pedido de internación domiciliaria")
        .build());
    assertTrue(intake.getId().startsWith("int_"));
    assertEquals("new", intake.getStatus());
    assertTrue(intake.getPatient().startsWith("pat_"));
    assertNotNull(intake.getData());

    Treatment treatment = kuida.treatments().create(TreatmentCreateParams.builder()
        .patient(intake.getPatient())
        .name("Enalapril 10 mg")
        .kind("medication")
        .dosage("1 comprimido")
        .frequency("cada 12 horas")
        .build());
    assertTrue(treatment.getId().startsWith("trt_"));
    assertEquals(intake.getPatient(), treatment.getPatient());
    assertEquals("medication", treatment.getKind());
  }

  // 10
  @Test
  void eventsBatch() {
    EventInput valid = EventInput.builder()
        .id("paciente-HC-1234")
        .type(EventInput.TYPE_PATIENT_UPSERTED)
        .putData("phone", "+54 9 342 555 0000")
        .putData("fullName", "Paciente Demo")
        .build();
    EventInput invalid = EventInput.builder().id("evento-invalido").type("no.existe").data(new HashMap<String, Object>()).build();
    EventBatchResponse batch = kuida.events().createBatch(Arrays.asList(valid, invalid));
    assertEquals("event_batch", batch.getObject());
    assertEquals(2, batch.getResults().size());
    assertEquals("processed", batch.getResults().get(0).getStatus());
    assertTrue(batch.getResults().get(0).getAccepted());
    assertEquals("invalid", batch.getResults().get(1).getStatus());
    assertFalse(batch.getResults().get(1).getAccepted());

    EventBatchResponse again = kuida.events().create(valid);
    assertEquals("duplicate", again.getResults().get(0).getStatus());
    assertEquals(batch.getResults().get(0).getEvent(), again.getResults().get(0).getEvent());

    InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> kuida.events().create(invalid));
    JsonObject raw = JsonParser.parseString(e.getRawBody()).getAsJsonObject();
    assertEquals("invalid", raw.getAsJsonArray("results").get(0).getAsJsonObject().get("status").getAsString());
  }

  // 11
  @Test
  void webhookEndpointsFlow() {
    WebhookEndpoint created = kuida.webhookEndpoints().create(WebhookEndpointCreateParams.builder()
        .url("https://example.com/webhooks/kuida")
        .enabledEvents("intake.ready", "webhook.ping")
        .description("Sistema de gestión demo")
        .build());
    assertTrue(created.getId().startsWith("we_"));
    assertNotNull(created.getSecret());
    assertTrue(created.getSecret().startsWith("whsec_"));

    WebhookEndpoint retrieved = kuida.webhookEndpoints().retrieve(created.getId());
    assertNull(retrieved.getSecret());
    assertEquals(Arrays.asList("intake.ready", "webhook.ping"), retrieved.getEnabledEvents());

    WebhookEndpoint updated = kuida.webhookEndpoints().update(created.getId(),
        WebhookEndpointUpdateParams.builder().description("Otro sistema demo").addEnabledEvent("intake.ready").build());
    assertEquals("Otro sistema demo", updated.getDescription());

    WebhookDelivery delivery = kuida.webhookEndpoints().ping(created.getId());
    assertEquals("webhook_delivery", delivery.getObject());
    assertEquals("pending", delivery.getStatus());
    assertEquals("webhook.ping", delivery.getEventType());

    KuidaList<WebhookDelivery> deliveries = kuida.webhookDeliveries().list(
        WebhookDeliveryListParams.builder().webhookEndpoint(created.getId()).build());
    assertEquals(1, deliveries.getData().size());
    assertEquals(delivery.getId(), deliveries.getData().get(0).getId());

    DeletedObject deleted = kuida.webhookEndpoints().delete(created.getId());
    assertTrue(deleted.getDeleted());
    assertEquals(created.getId(), deleted.getId());
  }

  // 12
  @Test
  void errors() {
    AuthenticationException missing = assertThrows(AuthenticationException.class, () ->
        KuidaClient.builder().apiKey("").baseUrl(BASE).build());
    assertEquals("api_key_missing", missing.getCode());

    AuthenticationException bad = assertThrows(AuthenticationException.class, () ->
        client("kd_test_000000000000_otrosecretootrosecreto0").account().retrieve());
    assertEquals(401, bad.getHttpStatus());
    assertEquals("api_key_invalid", bad.getCode());

    AuthenticationException revoked = assertThrows(AuthenticationException.class, () -> client(KEY_REVOKED).account().retrieve());
    assertEquals("api_key_revoked", revoked.getCode());

    PermissionException perm = assertThrows(PermissionException.class, () -> client(KEY_EVENTS_ONLY).patients().list());
    assertEquals(403, perm.getHttpStatus());
    assertEquals("scope_missing", perm.getCode());

    InvalidRequestException notFound = assertThrows(InvalidRequestException.class, () -> kuida.patients().retrieve("pat_noexiste"));
    assertEquals(404, notFound.getHttpStatus());
    assertEquals("resource_missing", notFound.getCode());
    assertNotNull(notFound.getRequestId());
    assertTrue(notFound.getRequestId().startsWith("req_"));
    assertNotNull(notFound.getDocUrl());

    InvalidRequestException noPhone = assertThrows(InvalidRequestException.class, () ->
        kuida.patients().create(PatientCreateParams.builder().fullName("Paciente Demo").build()));
    assertEquals("phone", noPhone.getParam());
    assertEquals("parameter_missing", noPhone.getCode());
  }

  // 13
  @Test
  void retries5xx() throws IOException {
    Patient p = client(KEY_FLAKY).patients().create(PatientCreateParams.builder().phone("+54 9 342 555 0000").build());
    assertTrue(p.getId().startsWith("pat_"));
    List<JsonObject> posts = new ArrayList<JsonObject>();
    for (JsonObject r : requests()) if ("POST".equals(r.get("method").getAsString())) posts.add(r);
    assertEquals(2, posts.size());
    String key = header(posts.get(0), "idempotency-key");
    assertTrue(UUID_V4.matcher(key).matches());
    assertEquals(key, header(posts.get(1), "idempotency-key"));
  }

  // 14
  @Test
  void retries429() throws IOException {
    final List<Long> waits = new ArrayList<Long>();
    KuidaClient c = KuidaClient.builder().apiKey(KEY_RATE_LIMITED).baseUrl(BASE).sleeper(new Sleeper() {
      @Override
      public void sleep(long millis) {
        waits.add(millis);
      }
    }).build();
    Account account = c.account().retrieve();
    assertEquals("account", account.getObject());
    assertEquals(Arrays.asList(1000L), waits);
    assertEquals(2, requests().size());
  }

  // 15
  @Test
  void noRetry() throws IOException {
    KuidaClient c = KuidaClient.builder().apiKey(KEY_FLAKY).baseUrl(BASE).maxRetries(0).sleeper(Sleeper.NONE).build();
    ApiException e = assertThrows(ApiException.class, () ->
        c.patients().create(PatientCreateParams.builder().phone("+54 9 342 555 0000").build()));
    assertEquals(503, e.getHttpStatus());
    assertEquals("api_error", e.getType());
    assertEquals(1, requests().size());
  }

  // 16
  @Test
  void connectionError() {
    KuidaClient c = KuidaClient.builder().apiKey(KEY).baseUrl("http://127.0.0.1:1/api").maxRetries(0).build();
    ApiConnectionException e = assertThrows(ApiConnectionException.class, () -> c.account().retrieve());
    assertEquals(0, e.getHttpStatus());
    assertNotNull(e.getCause());
  }

  // 17
  @Test
  void webhookSignature() throws IOException {
    JsonObject v = JsonParser.parseString(new String(Files.readAllBytes(Paths.get("..", "conformance", "webhook-vectors.json")), UTF_8)).getAsJsonObject();
    String secret = v.get("secret").getAsString();
    String timestamp = v.get("timestamp").getAsString();
    String payload = v.get("payload").getAsString();
    String signature = v.get("signatureHeader").getAsString();
    long now = Long.parseLong(timestamp);

    assertTrue(Webhook.verifySignature(payload, signature, timestamp, secret, 300, now));
    assertTrue(Webhook.verifySignature(payload, signature, timestamp, secret, 0));
    WebhookEvent event = Webhook.constructEvent(payload, signature, timestamp, secret, 0);
    assertEquals("intake.ready", event.getType());
    assertEquals("event", event.getObject());
    assertEquals("Paciente Demo", ((Map<?, ?>) event.getData().get("patient")).get("fullName"));

    assertThrows(SignatureVerificationException.class, () ->
        Webhook.verifySignature(payload, signature, timestamp, v.get("wrongSecret").getAsString(), 300, now));
    assertThrows(SignatureVerificationException.class, () ->
        Webhook.verifySignature(v.get("tamperedPayload").getAsString(), signature, timestamp, secret, 300, now));
    assertThrows(SignatureVerificationException.class, () -> Webhook.verifySignature(payload, signature, timestamp, secret));
    assertThrows(SignatureVerificationException.class, () -> Webhook.verifySignature(payload, null, timestamp, secret, 0));
    assertThrows(SignatureVerificationException.class, () -> Webhook.verifySignature(payload, signature, null, secret, 0));

    String body = "{\"id\":\"whd_demo\",\"object\":\"event\",\"type\":\"webhook.ping\",\"apiVersion\":\"2026-09-28\","
        + "\"livemode\":false,\"occurredAt\":\"2026-09-28T12:00:00.000Z\",\"ref\":\"ping\",\"data\":{\"message\":\"hola\"}}";
    String ts = String.valueOf(System.currentTimeMillis() / 1000);
    String sig = Webhook.computeSignature(body, ts, secret);
    WebhookEvent fresh = Webhook.constructEvent(body.getBytes(UTF_8), sig, ts, secret);
    assertEquals("webhook.ping", fresh.getType());
    assertNotEquals(signature, sig);
  }
}
