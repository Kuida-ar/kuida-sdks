package ar.kuida.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.kuida.KuidaClient;
import ar.kuida.Sleeper;
import ar.kuida.exception.InvalidRequestException;
import ar.kuida.model.Patient;
import ar.kuida.model.PatientCreateParams;
import ar.kuida.model.PatientUpdateParams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** Verifica que un PATCH llegue como PATCH al mock por los dos caminos del transporte. */
class PatchTransportTest {
  static final Charset UTF_8 = Charset.forName("UTF-8");
  static final String BASE = System.getenv("KUIDA_API_BASE") != null ? System.getenv("KUIDA_API_BASE") : "http://localhost:12111/api";
  static final String KEY = "kd_test_000000000000_mocksecretmocksecret00";

  @Test
  void patchViaReflection() throws IOException {
    HttpURLConnection probe = (HttpURLConnection) new URL(BASE).openConnection();
    Assumptions.assumeTrue(UrlConnectionTransport.setMethodReflectively(probe, "PATCH"),
        "La JVM no permite reflexión sobre java.net (falta --add-opens)");
    assertPatch(UrlConnectionTransport.PatchStrategy.REFLECTION);
  }

  @Test
  void patchViaSocket() throws IOException {
    assertPatch(UrlConnectionTransport.PatchStrategy.SOCKET);
  }

  @Test
  void patchErrorViaSocket() {
    final KuidaClient kuida = client(UrlConnectionTransport.PatchStrategy.SOCKET);
    InvalidRequestException e = org.junit.jupiter.api.Assertions.assertThrows(InvalidRequestException.class, new Executable() {
      @Override
      public void execute() {
        kuida.patients().update("pat_noexiste", PatientUpdateParams.builder().fullName("Paciente Demo").build());
      }
    });
    assertEquals(404, e.getHttpStatus());
    assertNotNull(e.getRequestId());
  }

  private static KuidaClient client(UrlConnectionTransport.PatchStrategy strategy) {
    return KuidaClient.builder().apiKey(KEY).baseUrl(BASE).sleeper(Sleeper.NONE)
        .httpTransport(new UrlConnectionTransport(strategy)).build();
  }

  private static void assertPatch(UrlConnectionTransport.PatchStrategy strategy) throws IOException {
    call("POST", "/__mock/reset");
    KuidaClient kuida = client(strategy);
    Patient p = kuida.patients().create(PatientCreateParams.builder().phone("+54 9 342 555 0000").build());
    Patient updated = kuida.patients().update(p.getId(), PatientUpdateParams.builder().fullName("Paciente Demo Ñandú").build());
    assertEquals("Paciente Demo Ñandú", updated.getFullName());
    assertEquals(200, updated.getLastResponse().getStatusCode());
    JsonObject last = null;
    for (JsonElement e : call("GET", "/__mock/requests").getAsJsonObject().getAsJsonArray("requests")) last = e.getAsJsonObject();
    assertNotNull(last);
    assertEquals("PATCH", last.get("method").getAsString());
    assertEquals("/v1/patients/" + p.getId(), last.get("path").getAsString());
    assertTrue(last.getAsJsonObject("headers").get("idempotency-key").isJsonNull());
    assertEquals("application/json", last.getAsJsonObject("headers").get("content-type").getAsString());
    assertEquals("Paciente Demo Ñandú", last.getAsJsonObject("body").get("fullName").getAsString());
  }

  private static JsonElement call(String method, String path) throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(BASE.replaceAll("/api/?$", "") + path).openConnection();
    c.setRequestMethod(method);
    if ("POST".equals(method)) {
      c.setDoOutput(true);
      OutputStream out = c.getOutputStream();
      out.write("{}".getBytes(UTF_8));
      out.close();
    }
    InputStream in = c.getInputStream();
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    byte[] chunk = new byte[8192];
    int n;
    while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
    in.close();
    return JsonParser.parseString(new String(buf.toByteArray(), UTF_8));
  }
}
