// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;

/** Objeto <code>EventResult</code> de la API de Kuida. */
public class EventResult extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("accepted")
  private Boolean accepted;

  @SerializedName("status")
  private String status;

  @SerializedName("event")
  private String event;

  @SerializedName("result")
  private Result result;

  @SerializedName("error")
  private String error;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public EventResult() {}

  /** El id que enviaste. */
  public String getId() {
    return id;
  }

  /** Campo <code>accepted</code>. */
  public Boolean getAccepted() {
    return accepted;
  }

  /**
   * Campo <code>status</code>.
   *
   * <p>Valores posibles: <code>processed</code>, <code>duplicate</code>, <code>unhandled</code>, <code>invalid</code>, <code>failed</code>.
   */
  public String getStatus() {
    return status;
  }

  /**
   * Id del evento en Kuida (<code>evt_…</code>).
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getEvent() {
    return event;
  }

  /**
   * Campo <code>result</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public Result getResult() {
    return result;
  }

  /**
   * Campo <code>error</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getError() {
    return error;
  }

  /** Objeto anidado <code>Result</code>. */
  public static class Result extends KuidaObject {
    @SerializedName("object")
    private String object;

    @SerializedName("id")
    private String id;

    /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
    public Result() {}

    /** Campo <code>object</code>. */
    public String getObject() {
      return object;
    }

    /** Campo <code>id</code>. */
    public String getId() {
      return id;
    }
  }
}
