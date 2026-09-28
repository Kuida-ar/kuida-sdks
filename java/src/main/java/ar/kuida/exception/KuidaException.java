package ar.kuida.exception;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base de todos los errores del SDK.
 *
 * <p>Es unchecked ({@link RuntimeException}): los errores de la API casi nunca se pueden resolver
 * en el lugar donde se llama, y una excepción checked obligaría a envolverla en cada lambda y en el
 * iterador de paginación automática. Conviene atraparla donde tenga sentido decidir qué hacer.
 *
 * <p>Toda respuesta no 2xx con cuerpo {@code {"error": {...}}} se convierte en la subclase que
 * corresponde a {@code error.type}.
 */
public class KuidaException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final int httpStatus;
  private final String type;
  private final String code;
  private final String param;
  private final String requestId;
  private final String docUrl;
  private final transient Map<String, List<String>> headers;
  private final String rawBody;

  /** Crea la excepción con los datos del error de la API. */
  public KuidaException(String message, int httpStatus, String type, String code, String param,
      String requestId, String docUrl, Map<String, List<String>> headers, String rawBody, Throwable cause) {
    super(message, cause);
    this.httpStatus = httpStatus;
    this.type = type;
    this.code = code;
    this.param = param;
    this.requestId = requestId;
    this.docUrl = docUrl;
    this.headers = headers == null ? Collections.<String, List<String>>emptyMap() : headers;
    this.rawBody = rawBody;
  }

  /** Status HTTP de la respuesta, o 0 si no hubo respuesta. */
  public int getHttpStatus() {
    return httpStatus;
  }

  /** {@code error.type} de la API (por ejemplo {@code invalid_request_error}). */
  public String getType() {
    return type;
  }

  /** {@code error.code} de la API (por ejemplo {@code resource_missing}). */
  public String getCode() {
    return code;
  }

  /** Parámetro que causó el error, si la API lo informa. */
  public String getParam() {
    return param;
  }

  /** Id del pedido ({@code req_…}), del cuerpo o del header {@code Request-Id}. */
  public String getRequestId() {
    return requestId;
  }

  /** Link a la documentación del error. */
  public String getDocUrl() {
    return docUrl;
  }

  /** Headers de la respuesta; las claves no distinguen mayúsculas. */
  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  /** Cuerpo crudo de la respuesta. */
  public String getRawBody() {
    return rawBody;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append(": ").append(getMessage());
    if (httpStatus != 0) sb.append(" (status ").append(httpStatus);
    if (code != null) sb.append(httpStatus != 0 ? ", " : " (").append("code ").append(code);
    if (requestId != null) sb.append(", requestId ").append(requestId);
    if (httpStatus != 0 || code != null) sb.append(')');
    return sb.toString();
  }
}
