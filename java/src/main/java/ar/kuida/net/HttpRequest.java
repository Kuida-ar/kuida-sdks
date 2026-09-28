package ar.kuida.net;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Un pedido HTTP listo para mandar. */
public final class HttpRequest {
  private final String method;
  private final String url;
  private final Map<String, String> headers;
  private final byte[] body;
  private final int timeoutMillis;

  /** Crea un pedido; lo arma el SDK. */
  public HttpRequest(String method, String url, Map<String, String> headers, byte[] body, int timeoutMillis) {
    this.method = method;
    this.url = url;
    this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, String>(headers));
    this.body = body;
    this.timeoutMillis = timeoutMillis;
  }

  /** {@code GET}, {@code POST}, {@code PATCH} o {@code DELETE}. */
  public String getMethod() {
    return method;
  }

  /** URL completa, con query string. */
  public String getUrl() {
    return url;
  }

  /** Headers a mandar. */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /** Cuerpo en UTF-8, o {@code null} si el pedido no lleva cuerpo. */
  public byte[] getBody() {
    return body;
  }

  /** Timeout de conexión y de lectura, en milisegundos. */
  public int getTimeoutMillis() {
    return timeoutMillis;
  }
}
