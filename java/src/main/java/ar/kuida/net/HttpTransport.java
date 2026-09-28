package ar.kuida.net;

import ar.kuida.KuidaResponse;
import java.io.IOException;

/**
 * Transporte HTTP del SDK. El default es {@link UrlConnectionTransport} (solo JDK, Java 8+).
 *
 * <p>Se puede reemplazar con {@code KuidaClient.builder().httpTransport(...)}, por ejemplo para
 * pasar por un cliente HTTP propio con proxy corporativo. Una implementación tiene que lanzar
 * {@link IOException} cuando no hay respuesta (red, DNS, TLS, timeout) y devolver cualquier
 * respuesta HTTP, sea cual sea su status; los reintentos y los errores los maneja el SDK.
 */
public interface HttpTransport {
  /** Manda un pedido y devuelve la respuesta tal como llegó. */
  KuidaResponse execute(HttpRequest request) throws IOException;
}
