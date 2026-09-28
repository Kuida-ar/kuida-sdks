package ar.kuida;

import ar.kuida.exception.ApiConnectionException;
import ar.kuida.exception.ApiException;
import ar.kuida.exception.AuthenticationException;
import ar.kuida.exception.IdempotencyException;
import ar.kuida.exception.InvalidRequestException;
import ar.kuida.exception.KuidaException;
import ar.kuida.exception.PermissionException;
import ar.kuida.exception.RateLimitException;
import ar.kuida.net.HttpRequest;
import ar.kuida.net.HttpTransport;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Arma, manda y reintenta los pedidos a la API. Uso interno de los servicios: puede cambiar sin
 * aviso entre versiones menores.
 */
public final class ApiRequestor {
  private static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final Random JITTER = new Random();

  private final String apiKey;
  private final String baseUrl;
  private final String apiVersion;
  private final Duration timeout;
  private final int maxRetries;
  private final HttpTransport transport;
  private final Sleeper sleeper;

  ApiRequestor(String apiKey, String baseUrl, String apiVersion, Duration timeout, int maxRetries,
      HttpTransport transport, Sleeper sleeper) {
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
    this.apiVersion = apiVersion;
    this.timeout = timeout;
    this.maxRetries = maxRetries;
    this.transport = transport;
    this.sleeper = sleeper;
  }

  /** Pedido que devuelve un objeto. {@code params} va como cuerpo JSON (POST/PATCH). */
  public <T> T request(String method, String path, Object params, Class<T> type, RequestOptions options) {
    KuidaResponse response = send(method, path, params, null, options);
    return parse(response, type);
  }

  /** {@code GET} de una lista con cursor; la página devuelta sabe pedir las siguientes. */
  public <T> KuidaList<T> requestList(final String path, KuidaParams params, final Class<T> itemType,
      final RequestOptions options) {
    Map<String, Object> query = params == null ? new LinkedHashMap<String, Object>() : params.toMap();
    return fetchPage(path, query, itemType, options);
  }

  private <T> KuidaList<T> fetchPage(final String path, Map<String, Object> query, final Class<T> itemType,
      final RequestOptions options) {
    KuidaResponse response = send("GET", path, null, query, options);
    Type listType = TypeToken.getParameterized(KuidaList.class, itemType).getType();
    KuidaList<T> page = parse(response, listType);
    page.setPaging(new KuidaList.PageFetcher<T>() {
      @Override
      public KuidaList<T> fetch(Map<String, Object> next) {
        return fetchPage(path, next, itemType, options);
      }
    }, query);
    return page;
  }

  // ─── envío y reintentos ─────────────────────────────────────────────────────

  private KuidaResponse send(String method, String path, Object params, Map<String, Object> query,
      RequestOptions options) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    int retries = opts.getMaxRetries() != null ? opts.getMaxRetries() : maxRetries;
    Duration t = opts.getTimeout() != null ? opts.getTimeout() : timeout;
    int timeoutMillis = (int) Math.max(1, Math.min(Integer.MAX_VALUE, t.toMillis()));

    Map<String, String> headers = new LinkedHashMap<String, String>();
    headers.put("Authorization", "Bearer " + apiKey);
    headers.put("Accept", "application/json");
    headers.put("Kuida-Version", apiVersion);
    headers.put("User-Agent", KuidaClient.USER_AGENT);
    headers.put("X-Kuida-Client-User-Agent", KuidaClient.clientUserAgent());
    byte[] body = null;
    if ("POST".equals(method) || "PATCH".equals(method)) {
      headers.put("Content-Type", "application/json");
      JsonElement json = params == null ? new JsonObject() : KuidaJson.encode(params);
      body = KuidaJson.gson().toJson(json).getBytes(UTF_8);
    }
    if ("POST".equals(method)) {
      String key = opts.getIdempotencyKey() != null ? opts.getIdempotencyKey() : UUID.randomUUID().toString();
      headers.put("Idempotency-Key", key);
    }
    HttpRequest request = new HttpRequest(method, baseUrl + path + queryString(query), headers, body, timeoutMillis);

    for (int attempt = 0; ; attempt++) {
      KuidaResponse response;
      try {
        response = transport.execute(request);
      } catch (IOException e) {
        if (attempt < retries) {
          pause(delayMillis(attempt, null));
          continue;
        }
        throw new ApiConnectionException("No se pudo conectar con Kuida (" + method + " " + path + "): "
            + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage()), e);
      }
      int status = response.getStatusCode();
      if (status >= 200 && status < 300) return response;
      if (attempt < retries && shouldRetry(response)) {
        pause(delayMillis(attempt, response));
        continue;
      }
      throw toException(response);
    }
  }

  private void pause(long millis) {
    try {
      sleeper.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiConnectionException("Reintento interrumpido", e);
    }
  }

  private static boolean shouldRetry(KuidaResponse response) {
    int status = response.getStatusCode();
    if (status == 429 || status == 500 || status == 502 || status == 503 || status == 504) return true;
    if (status == 409) {
      JsonObject error = errorObject(response);
      return error != null && "idempotency_key_in_use".equals(str(error, "code"));
    }
    return false;
  }

  /** {@code min(0.5 s × 2^intento, 8 s)} con jitter de ±25 %, o {@code Retry-After} (hasta 60 s). */
  static long delayMillis(int attempt, KuidaResponse response) {
    if (response != null) {
      String retryAfter = response.getHeader("Retry-After");
      if (retryAfter != null) {
        try {
          double seconds = Double.parseDouble(retryAfter.trim());
          if (seconds >= 0) return (long) (Math.min(seconds, 60.0) * 1000);
        } catch (NumberFormatException ignored) {
          // formato fecha HTTP: se usa el backoff
        }
      }
    }
    double base = Math.min(0.5 * Math.pow(2, attempt), 8.0);
    double jitter = 1 + (JITTER.nextDouble() * 0.5 - 0.25);
    return (long) (base * jitter * 1000);
  }

  // ─── respuestas y errores ───────────────────────────────────────────────────

  private static <T> T parse(KuidaResponse response, Type type) {
    JsonElement tree;
    try {
      tree = JsonParser.parseString(response.getBody());
    } catch (JsonParseException e) {
      throw new ApiException("La respuesta no es JSON válido: " + abbreviate(response.getBody()), response.getStatusCode(),
          null, null, null, response.getRequestId(), null, response.getHeaders(), response.getBody(), e);
    }
    if (tree == null || tree.isJsonNull()) {
      throw new ApiException("La respuesta vino vacía", response.getStatusCode(), null, null, null,
          response.getRequestId(), null, response.getHeaders(), response.getBody(), null);
    }
    return KuidaJson.parse(tree, type, response);
  }

  private static JsonObject errorObject(KuidaResponse response) {
    try {
      JsonElement tree = JsonParser.parseString(response.getBody());
      if (tree != null && tree.isJsonObject()) {
        JsonElement error = tree.getAsJsonObject().get("error");
        if (error != null && error.isJsonObject()) return error.getAsJsonObject();
      }
    } catch (RuntimeException ignored) {
      // cuerpo no JSON
    }
    return null;
  }

  static KuidaException toException(KuidaResponse response) {
    int status = response.getStatusCode();
    String raw = response.getBody();
    JsonObject error = errorObject(response);
    if (error == null) {
      String text = raw == null || raw.trim().isEmpty() ? "Respuesta HTTP " + status + " sin cuerpo" : raw;
      return new ApiException(text, status, null, null, null, response.getRequestId(), null, response.getHeaders(), raw, null);
    }
    String type = str(error, "type");
    String code = str(error, "code");
    String message = str(error, "message");
    if (message == null) message = "Error " + status + (code == null ? "" : " (" + code + ")");
    String param = str(error, "param");
    String requestId = str(error, "requestId");
    if (requestId == null) requestId = response.getRequestId();
    String docUrl = str(error, "docUrl");
    Map<String, java.util.List<String>> h = response.getHeaders();
    if ("invalid_request_error".equals(type)) return new InvalidRequestException(message, status, type, code, param, requestId, docUrl, h, raw, null);
    if ("authentication_error".equals(type)) return new AuthenticationException(message, status, type, code, param, requestId, docUrl, h, raw, null);
    if ("permission_error".equals(type)) return new PermissionException(message, status, type, code, param, requestId, docUrl, h, raw, null);
    if ("idempotency_error".equals(type)) return new IdempotencyException(message, status, type, code, param, requestId, docUrl, h, raw, null);
    if ("rate_limit_error".equals(type)) return new RateLimitException(message, status, type, code, param, requestId, docUrl, h, raw, null);
    return new ApiException(message, status, type, code, param, requestId, docUrl, h, raw, null);
  }

  private static String str(JsonObject obj, String key) {
    JsonElement e = obj.get(key);
    return e == null || e.isJsonNull() || !e.isJsonPrimitive() ? null : e.getAsString();
  }

  private static String abbreviate(String s) {
    if (s == null) return "";
    return s.length() > 200 ? s.substring(0, 200) + "…" : s;
  }

  // ─── query string ───────────────────────────────────────────────────────────

  static String queryString(Map<String, Object> query) {
    if (query == null || query.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Object> e : query.entrySet()) {
      Object v = e.getValue();
      if (v == null) continue;
      if (v instanceof Iterable) {
        for (Object item : (Iterable<?>) v) appendParam(sb, e.getKey(), item);
      } else {
        appendParam(sb, e.getKey(), v);
      }
    }
    return sb.length() == 0 ? "" : "?" + sb.substring(1);
  }

  private static void appendParam(StringBuilder sb, String key, Object value) {
    if (value == null) return;
    sb.append('&').append(urlEncode(key)).append('=').append(urlEncode(KuidaJson.queryValue(value)));
  }

  private static String urlEncode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }
}
