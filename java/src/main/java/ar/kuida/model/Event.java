// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;
import java.util.Map;

/** Un evento que entró a Kuida, con su resultado. */
public class Event extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("type")
  private String type;

  @SerializedName("source")
  private String source;

  @SerializedName("sourceRef")
  private String sourceRef;

  @SerializedName("status")
  private String status;

  @SerializedName("error")
  private String error;

  @SerializedName("result")
  private Result result;

  @SerializedName("data")
  private Map<String, Object> data;

  @SerializedName("occurredAt")
  private OffsetDateTime occurredAt;

  @SerializedName("receivedAt")
  private OffsetDateTime receivedAt;

  @SerializedName("processedAt")
  private OffsetDateTime processedAt;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public Event() {}

  /** Id del evento en Kuida. */
  public String getId() {
    return id;
  }

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>event</code>.
   */
  public String getObject() {
    return object;
  }

  /**
   * Campo <code>type</code>.
   *
   * <p>Valores posibles: <code>intake.requested</code>, <code>patient.upserted</code>, <code>appointment.created</code>, <code>appointment.rescheduled</code>, <code>appointment.cancelled</code>, <code>visit.completed</code>, <code>treatment.prescribed</code>, <code>order.issued</code>.
   */
  public String getType() {
    return type;
  }

  /**
   * Por dónde entró.
   *
   * <p>Valores posibles: <code>api</code>, <code>pms</code>, <code>email</code>, <code>tool</code>, <code>manual</code>, <code>import</code>.
   */
  public String getSource() {
    return source;
  }

  /** Identidad del evento en su fuente. Para la API, el <code>id</code> que envió. */
  public String getSourceRef() {
    return sourceRef;
  }

  /**
   * <code>processed</code> Kuida hizo lo suyo · <code>unhandled</code> guardado, sin efecto todavía · <code>failed</code> falló, reintente con el mismo id · <code>duplicate</code> · <code>received</code> en curso.
   *
   * <p>Valores posibles: <code>received</code>, <code>processed</code>, <code>failed</code>, <code>duplicate</code>, <code>unhandled</code>.
   */
  public String getStatus() {
    return status;
  }

  /**
   * Campo <code>error</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getError() {
    return error;
  }

  /**
   * Lo que produjo el evento (<code>{ object: "visit", id: "vis_…" }</code>).
   *
   * <p>Puede ser <code>null</code>.
   */
  public Result getResult() {
    return result;
  }

  /** Campo <code>data</code>. */
  public Map<String, Object> getData() {
    return data;
  }

  /** Fecha y hora ISO 8601. */
  public OffsetDateTime getOccurredAt() {
    return occurredAt;
  }

  /** Fecha y hora ISO 8601. */
  public OffsetDateTime getReceivedAt() {
    return receivedAt;
  }

  /**
   * Fecha y hora ISO 8601.
   *
   * <p>Puede ser <code>null</code>.
   */
  public OffsetDateTime getProcessedAt() {
    return processedAt;
  }

  /** Lo que produjo el evento (<code>{ object: "visit", id: "vis_…" }</code>). */
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
