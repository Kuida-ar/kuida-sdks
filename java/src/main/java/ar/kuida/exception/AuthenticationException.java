package ar.kuida.exception;

import java.util.List;
import java.util.Map;

/** Clave faltante, inválida, revocada o vencida ({@code authentication_error}). */
public class AuthenticationException extends KuidaException {
  private static final long serialVersionUID = 1L;

  /** Crea la excepción con los datos del error de la API. */
  public AuthenticationException(String message, int httpStatus, String type, String code, String param,
      String requestId, String docUrl, Map<String, List<String>> headers, String rawBody, Throwable cause) {
    super(message, httpStatus, type, code, param, requestId, docUrl, headers, rawBody, cause);
  }
}
