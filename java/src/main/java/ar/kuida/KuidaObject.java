package ar.kuida;

import com.google.gson.JsonObject;

/**
 * Base de todos los objetos que devuelve la API.
 *
 * <p>Los campos desconocidos se ignoran sin error, pero el JSON crudo queda accesible con
 * {@link #getRawJson()}: sirve para leer un campo que la API agregó después de esta versión del
 * SDK. Los objetos de primer nivel traen además la respuesta HTTP en {@link #getLastResponse()}.
 */
public abstract class KuidaObject {
  private transient JsonObject rawJson;
  private transient KuidaResponse lastResponse;

  /** Constructor para las subclases. */
  protected KuidaObject() {}

  /** El JSON tal como llegó. */
  public JsonObject getRawJson() {
    return rawJson;
  }

  /**
   * La respuesta HTTP que trajo este objeto (status, headers, {@code Request-Id}). Es {@code null}
   * en los objetos anidados y en los construidos por {@link Webhook}.
   */
  public KuidaResponse getLastResponse() {
    return lastResponse;
  }

  void setRawJson(JsonObject rawJson) {
    this.rawJson = rawJson;
  }

  void setLastResponse(KuidaResponse lastResponse) {
    this.lastResponse = lastResponse;
  }

  static void attach(KuidaObject object, JsonObject raw, KuidaResponse response) {
    if (raw != null) object.setRawJson(raw);
    if (response != null) object.setLastResponse(response);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + (rawJson == null ? "{}" : rawJson.toString());
  }
}
