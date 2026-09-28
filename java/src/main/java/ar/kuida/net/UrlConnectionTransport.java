package ar.kuida.net;

import ar.kuida.KuidaResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Transporte por defecto, sobre {@link HttpURLConnection} (Java 8+, sin dependencias).
 *
 * <p>{@code HttpURLConnection} rechaza el método {@code PATCH}. Para mandarlo de verdad (la API no
 * lee {@code X-HTTP-Method-Override}) el transporte prueba, en orden:
 * <ol>
 *   <li>asignar el método por reflexión sobre {@code HttpURLConnection.method} (y sobre la conexión
 *       delegada en HTTPS). Funciona en Java 8 a 15, y en 16+ si la JVM corre con
 *       {@code --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED};</li>
 *   <li>si la reflexión no está permitida, un pedido HTTP/1.1 mínimo sobre un socket (TLS con
 *       verificación de hostname para {@code https}). Este camino no usa el proxy del sistema.</li>
 * </ol>
 */
public final class UrlConnectionTransport implements HttpTransport {
  static final Charset UTF_8 = Charset.forName("UTF-8");

  /** Cómo se manda un {@code PATCH}. */
  enum PatchStrategy {
    /** Reflexión si se puede; si no, socket. */
    AUTO,
    /** Solo reflexión; falla si no está permitida. */
    REFLECTION,
    /** Siempre socket. */
    SOCKET
  }

  private static volatile Boolean reflectionAllowed;

  private final PatchStrategy patchStrategy;

  /** Transporte con la estrategia de PATCH automática (reflexión si se puede; si no, socket). */
  public UrlConnectionTransport() {
    this(PatchStrategy.AUTO);
  }

  UrlConnectionTransport(PatchStrategy patchStrategy) {
    this.patchStrategy = patchStrategy;
  }

  @Override
  public KuidaResponse execute(HttpRequest request) throws IOException {
    String method = request.getMethod();
    if ("PATCH".equals(method)) {
      if (patchStrategy == PatchStrategy.SOCKET) return SocketHttp.execute(request);
      if (patchStrategy == PatchStrategy.AUTO && Boolean.FALSE.equals(reflectionAllowed)) {
        return SocketHttp.execute(request);
      }
      HttpURLConnection conn = open(request);
      boolean ok = setMethodReflectively(conn, method);
      reflectionAllowed = ok;
      if (!ok) {
        if (patchStrategy == PatchStrategy.REFLECTION) {
          throw new IllegalStateException("La JVM no permite asignar PATCH por reflexión");
        }
        return SocketHttp.execute(request);
      }
      return send(conn, request);
    }
    HttpURLConnection conn = open(request);
    conn.setRequestMethod(method);
    return send(conn, request);
  }

  private static HttpURLConnection open(HttpRequest request) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) new URL(request.getUrl()).openConnection();
    conn.setConnectTimeout(request.getTimeoutMillis());
    conn.setReadTimeout(request.getTimeoutMillis());
    conn.setUseCaches(false);
    conn.setInstanceFollowRedirects(false);
    for (Map.Entry<String, String> h : request.getHeaders().entrySet()) {
      conn.setRequestProperty(h.getKey(), h.getValue());
    }
    return conn;
  }

  private static KuidaResponse send(HttpURLConnection conn, HttpRequest request) throws IOException {
    byte[] body = request.getBody();
    if (body != null) {
      conn.setDoOutput(true);
      conn.setFixedLengthStreamingMode(body.length);
      OutputStream out = conn.getOutputStream();
      try {
        out.write(body);
      } finally {
        out.close();
      }
    }
    int status = conn.getResponseCode();
    InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
    String text = in == null ? "" : readAll(in);
    Map<String, List<String>> headers = conn.getHeaderFields();
    return new KuidaResponse(status, headers, text);
  }

  static String readAll(InputStream in) throws IOException {
    try {
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      byte[] chunk = new byte[8192];
      int n;
      while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
      return new String(buf.toByteArray(), UTF_8);
    } finally {
      in.close();
    }
  }

  /**
   * Asigna el método en el campo protegido {@code HttpURLConnection.method} de la conexión y de
   * su delegada (las conexiones HTTPS del JDK envuelven una conexión HTTP interna).
   */
  static boolean setMethodReflectively(HttpURLConnection conn, String method) {
    try {
      Field methodField = HttpURLConnection.class.getDeclaredField("method");
      methodField.setAccessible(true);
      for (Class<?> k = conn.getClass(); k != null && k != HttpURLConnection.class; k = k.getSuperclass()) {
        Field delegateField;
        try {
          delegateField = k.getDeclaredField("delegate");
        } catch (NoSuchFieldException e) {
          continue;
        }
        delegateField.setAccessible(true);
        Object delegate = delegateField.get(conn);
        if (delegate instanceof HttpURLConnection) methodField.set(delegate, method);
      }
      methodField.set(conn, method);
      return method.equals(conn.getRequestMethod());
    } catch (Exception e) {
      return false; // NoSuchField, IllegalAccess o InaccessibleObjectException (Java 16+)
    }
  }
}
