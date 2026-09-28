package ar.kuida.exception;

import java.util.List;
import java.util.Map;

/** La firma de un webhook no verifica: falta un header, la firma no coincide o el timestamp está fuera de tolerancia. */
public class SignatureVerificationException extends KuidaException {
  private static final long serialVersionUID = 1L;

  /** Crea la excepción con los datos del error de la API. */
  public SignatureVerificationException(String message, int httpStatus, String type, String code, String param,
      String requestId, String docUrl, Map<String, List<String>> headers, String rawBody, Throwable cause) {
    super(message, httpStatus, type, code, param, requestId, docUrl, headers, rawBody, cause);
  }

  /** Crea la excepción con un mensaje y la causa. */
  public SignatureVerificationException(String message, Throwable cause) {
    super(message, 0, null, null, null, null, null, null, null, cause);
  }

  /** Crea la excepción con un mensaje. */
  public SignatureVerificationException(String message) {
    this(message, null);
  }
}
