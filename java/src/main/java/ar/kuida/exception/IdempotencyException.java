package ar.kuida.exception;

import java.util.List;
import java.util.Map;

/** La {@code Idempotency-Key} ya se usó con otro pedido ({@code idempotency_error}). */
public class IdempotencyException extends KuidaException {
  private static final long serialVersionUID = 1L;

  /** Crea la excepción con los datos del error de la API. */
  public IdempotencyException(String message, int httpStatus, String type, String code, String param,
      String requestId, String docUrl, Map<String, List<String>> headers, String rawBody, Throwable cause) {
    super(message, httpStatus, type, code, param, requestId, docUrl, headers, rawBody, cause);
  }
}
