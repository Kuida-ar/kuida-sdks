// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;
import java.util.Map;

/** Un envío de webhook con sus reintentos. */
public class WebhookDelivery extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("webhookEndpoint")
  private String webhookEndpoint;

  @SerializedName("eventType")
  private String eventType;

  @SerializedName("status")
  private String status;

  @SerializedName("attempts")
  private Long attempts;

  @SerializedName("lastStatusCode")
  private Long lastStatusCode;

  @SerializedName("lastError")
  private String lastError;

  @SerializedName("nextAttemptAt")
  private OffsetDateTime nextAttemptAt;

  @SerializedName("deliveredAt")
  private OffsetDateTime deliveredAt;

  @SerializedName("payload")
  private Map<String, Object> payload;

  @SerializedName("createdAt")
  private OffsetDateTime createdAt;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public WebhookDelivery() {}

  /** Id de la entrega. */
  public String getId() {
    return id;
  }

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>webhook_delivery</code>.
   */
  public String getObject() {
    return object;
  }

  /** Endpoint destino. */
  public String getWebhookEndpoint() {
    return webhookEndpoint;
  }

  /** Campo <code>eventType</code>. */
  public String getEventType() {
    return eventType;
  }

  /**
   * <code>pending</code> en curso o esperando reintento · <code>succeeded</code> su endpoint respondió 2xx · <code>failed</code> se agotaron los reintentos.
   *
   * <p>Valores posibles: <code>pending</code>, <code>succeeded</code>, <code>failed</code>.
   */
  public String getStatus() {
    return status;
  }

  /** Campo <code>attempts</code>. */
  public Long getAttempts() {
    return attempts;
  }

  /**
   * Campo <code>lastStatusCode</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public Long getLastStatusCode() {
    return lastStatusCode;
  }

  /**
   * Campo <code>lastError</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getLastError() {
    return lastError;
  }

  /**
   * Fecha y hora ISO 8601.
   *
   * <p>Puede ser <code>null</code>.
   */
  public OffsetDateTime getNextAttemptAt() {
    return nextAttemptAt;
  }

  /**
   * Fecha y hora ISO 8601.
   *
   * <p>Puede ser <code>null</code>.
   */
  public OffsetDateTime getDeliveredAt() {
    return deliveredAt;
  }

  /** El cuerpo exacto que se envió. */
  public Map<String, Object> getPayload() {
    return payload;
  }

  /** Fecha y hora ISO 8601. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
