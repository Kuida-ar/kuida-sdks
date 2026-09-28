package ar.kuida.exception;

import java.util.List;
import java.util.Map;

/** Demasiados pedidos ({@code rate_limit_error}). El SDK ya reintentó respetando {@code Retry-After}. */
public class RateLimitException extends KuidaException {
  private static final long serialVersionUID = 1L;

  /** Crea la excepción con los datos del error de la API. */
  public RateLimitException(String message, int httpStatus, String type, String code, String param,
      String requestId, String docUrl, Map<String, List<String>> headers, String rawBody, Throwable cause) {
    super(message, httpStatus, type, code, param, requestId, docUrl, headers, rawBody, cause);
  }
}
