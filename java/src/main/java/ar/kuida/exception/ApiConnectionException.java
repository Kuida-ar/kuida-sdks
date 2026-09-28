package ar.kuida.exception;

import java.util.List;
import java.util.Map;

/** No hubo respuesta: error de red, DNS, TLS o timeout. El SDK ya reintentó con la misma {@code Idempotency-Key}. */
public class ApiConnectionException extends KuidaException {
  private static final long serialVersionUID = 1L;

  /** Crea la excepción con los datos del error de la API. */
  public ApiConnectionException(String message, int httpStatus, String type, String code, String param,
      String requestId, String docUrl, Map<String, List<String>> headers, String rawBody, Throwable cause) {
    super(message, httpStatus, type, code, param, requestId, docUrl, headers, rawBody, cause);
  }

  /** Crea la excepción con un mensaje y la causa. */
  public ApiConnectionException(String message, Throwable cause) {
    super(message, 0, null, null, null, null, null, null, null, cause);
  }

  /** Crea la excepción con un mensaje. */
  public ApiConnectionException(String message) {
    this(message, null);
  }
}
