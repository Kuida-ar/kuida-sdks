package ar.kuida.exception;

import java.util.List;
import java.util.Map;

/** Error de Kuida ({@code api_error}), un {@code type} desconocido o una respuesta que no es JSON. */
public class ApiException extends KuidaException {
  private static final long serialVersionUID = 1L;

  /** Crea la excepción con los datos del error de la API. */
  public ApiException(String message, int httpStatus, String type, String code, String param,
      String requestId, String docUrl, Map<String, List<String>> headers, String rawBody, Throwable cause) {
    super(message, httpStatus, type, code, param, requestId, docUrl, headers, rawBody, cause);
  }
}
