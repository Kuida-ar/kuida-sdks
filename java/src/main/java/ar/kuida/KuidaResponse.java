package ar.kuida;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Una respuesta HTTP de la API: status, headers y cuerpo crudo. */
public final class KuidaResponse {
  private final int statusCode;
  private final Map<String, List<String>> headers;
  private final String body;

  /** Crea una respuesta; lo usan los transportes HTTP. */
  public KuidaResponse(int statusCode, Map<String, List<String>> headers, String body) {
    this.statusCode = statusCode;
    Map<String, List<String>> h = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
    if (headers != null) {
      for (Map.Entry<String, List<String>> e : headers.entrySet()) {
        if (e.getKey() != null) h.put(e.getKey(), e.getValue());
      }
    }
    this.headers = Collections.unmodifiableMap(h);
    this.body = body;
  }

  /** Código de estado HTTP. */
  public int getStatusCode() {
    return statusCode;
  }

  /** Headers de la respuesta; las claves no distinguen mayúsculas. */
  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  /** Primer valor de un header, o {@code null}. */
  public String getHeader(String name) {
    List<String> values = headers.get(name);
    return values == null || values.isEmpty() ? null : values.get(0);
  }

  /** Header {@code Request-Id}: conviene citarlo al pedir soporte. */
  public String getRequestId() {
    return getHeader("Request-Id");
  }

  /** {@code true} si la respuesta es la repetición de un pedido idempotente anterior. */
  public boolean isIdempotentReplayed() {
    return "true".equalsIgnoreCase(getHeader("Idempotent-Replayed"));
  }

  /** El cuerpo crudo. */
  public String getBody() {
    return body;
  }
}
