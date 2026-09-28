// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;
import java.util.List;

/** Una URL de tu sistema a la que Kuida le avisa lo que pasa. */
public class WebhookEndpoint extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("url")
  private String url;

  @SerializedName("description")
  private String description;

  @SerializedName("enabledEvents")
  private List<String> enabledEvents;

  @SerializedName("status")
  private String status;

  @SerializedName("secret")
  private String secret;

  @SerializedName("lastDeliveredAt")
  private OffsetDateTime lastDeliveredAt;

  @SerializedName("lastError")
  private LastError lastError;

  @SerializedName("createdAt")
  private OffsetDateTime createdAt;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public WebhookEndpoint() {}

  /** Id del endpoint. */
  public String getId() {
    return id;
  }

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>webhook_endpoint</code>.
   */
  public String getObject() {
    return object;
  }

  /** Campo <code>url</code>. */
  public String getUrl() {
    return url;
  }

  /**
   * Campo <code>description</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getDescription() {
    return description;
  }

  /** Tipos de evento que recibe. <code>*</code> = todos. */
  public List<String> getEnabledEvents() {
    return enabledEvents;
  }

  /**
   * Campo <code>status</code>.
   *
   * <p>Valores posibles: <code>enabled</code>, <code>disabled</code>.
   */
  public String getStatus() {
    return status;
  }

  /** Secreto para verificar la firma (<code>whsec_…</code>). Solo viene en la respuesta de creación. */
  public String getSecret() {
    return secret;
  }

  /**
   * Fecha y hora ISO 8601.
   *
   * <p>Puede ser <code>null</code>.
   */
  public OffsetDateTime getLastDeliveredAt() {
    return lastDeliveredAt;
  }

  /**
   * Campo <code>lastError</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public LastError getLastError() {
    return lastError;
  }

  /** Fecha y hora ISO 8601. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /** Objeto anidado <code>LastError</code>. */
  public static class LastError extends KuidaObject {
    @SerializedName("at")
    private OffsetDateTime at;

    @SerializedName("message")
    private String message;

    /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
    public LastError() {}

    /** Fecha y hora ISO 8601. */
    public OffsetDateTime getAt() {
      return at;
    }

    /** Campo <code>message</code>. */
    public String getMessage() {
      return message;
    }
  }
}
