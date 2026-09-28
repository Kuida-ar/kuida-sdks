package ar.kuida.exception;

import java.util.List;
import java.util.Map;

/** Pedido inválido ({@code invalid_request_error}): falta un parámetro, sobra uno, el objeto no existe o no se puede cambiar. Ver {@link #getParam()} y {@link #getCode()}. */
public class InvalidRequestException extends KuidaException {
  private static final long serialVersionUID = 1L;

  /** Crea la excepción con los datos del error de la API. */
  public InvalidRequestException(String message, int httpStatus, String type, String code, String param,
      String requestId, String docUrl, Map<String, List<String>> headers, String rawBody, Throwable cause) {
    super(message, httpStatus, type, code, param, requestId, docUrl, headers, rawBody, cause);
  }
}
