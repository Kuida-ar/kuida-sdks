package ar.kuida.exception;

import java.util.List;
import java.util.Map;

/** La clave no tiene el scope o la organización no contrató la línea de servicio ({@code permission_error}). */
public class PermissionException extends KuidaException {
  private static final long serialVersionUID = 1L;

  /** Crea la excepción con los datos del error de la API. */
  public PermissionException(String message, int httpStatus, String type, String code, String param,
      String requestId, String docUrl, Map<String, List<String>> headers, String rawBody, Throwable cause) {
    super(message, httpStatus, type, code, param, requestId, docUrl, headers, rawBody, cause);
  }
}
