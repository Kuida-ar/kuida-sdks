// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;
import java.util.Map;

/** Lo que Kuida le manda a su endpoint. */
public class WebhookEvent extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("type")
  private String type;

  @SerializedName("apiVersion")
  private String apiVersion;

  @SerializedName("livemode")
  private Boolean livemode;

  @SerializedName("occurredAt")
  private OffsetDateTime occurredAt;

  @SerializedName("ref")
  private String ref;

  @SerializedName("data")
  private Map<String, Object> data;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public WebhookEvent() {}

  /** Id de la entrega (<code>whd_…</code>). Estable entre reintentos: deduplique por este campo. */
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
   * <p>Valores posibles: <code>intake.requested</code>, <code>patient.upserted</code>, <code>appointment.created</code>, <code>appointment.rescheduled</code>, <code>appointment.cancelled</code>, <code>visit.completed</code>, <code>treatment.prescribed</code>, <code>order.issued</code>, <code>intake.created</code>, <code>intake.ready</code>, <code>conversation.handoff</code>, <code>patient.silent</code>, <code>webhook.ping</code>.
   */
  public String getType() {
    return type;
  }

  /** Campo <code>apiVersion</code>. */
  public String getApiVersion() {
    return apiVersion;
  }

  /** Campo <code>livemode</code>. */
  public Boolean getLivemode() {
    return livemode;
  }

  /** Cuándo pasó, ISO 8601. */
  public OffsetDateTime getOccurredAt() {
    return occurredAt;
  }

  /** Id interno de lo que disparó el evento. */
  public String getRef() {
    return ref;
  }

  /** Los datos del hecho. */
  public Map<String, Object> getData() {
    return data;
  }
}
