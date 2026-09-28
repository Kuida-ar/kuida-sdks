// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Una solicitud de ingreso: alguien pide el servicio para un paciente y Kuida completa lo que falta. */
public class IntakeRequest extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("status")
  private String status;

  @SerializedName("patient")
  private String patient;

  @SerializedName("summary")
  private String summary;

  @SerializedName("missing")
  private List<String> missing;

  @SerializedName("data")
  private Map<String, Object> data;

  @SerializedName("discardReason")
  private String discardReason;

  @SerializedName("readyAt")
  private OffsetDateTime readyAt;

  @SerializedName("loadedAt")
  private OffsetDateTime loadedAt;

  @SerializedName("closedAt")
  private OffsetDateTime closedAt;

  @SerializedName("createdAt")
  private OffsetDateTime createdAt;

  @SerializedName("updatedAt")
  private OffsetDateTime updatedAt;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public IntakeRequest() {}

  /** Id de la solicitud. */
  public String getId() {
    return id;
  }

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>intake_request</code>.
   */
  public String getObject() {
    return object;
  }

  /**
   * <code>new</code> recién llegada · <code>validating</code> Kuida está completando datos por WhatsApp · <code>ready</code> completa · <code>loaded</code> cargada en el sistema de la organización · <code>discarded</code> descartada.
   *
   * <p>Valores posibles: <code>new</code>, <code>validating</code>, <code>ready</code>, <code>loaded</code>, <code>discarded</code>.
   */
  public String getStatus() {
    return status;
  }

  /**
   * Paciente, cuando ya se pudo identificar.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getPatient() {
    return patient;
  }

  /**
   * Campo <code>summary</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getSummary() {
    return summary;
  }

  /** Datos que todavía faltan para que la solicitud quede lista. */
  public List<String> getMissing() {
    return missing;
  }

  /** Los datos de la solicitud tal como los tiene Kuida. */
  public Map<String, Object> getData() {
    return data;
  }

  /**
   * Campo <code>discardReason</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getDiscardReason() {
    return discardReason;
  }

  /**
   * Fecha y hora ISO 8601.
   *
   * <p>Puede ser <code>null</code>.
   */
  public OffsetDateTime getReadyAt() {
    return readyAt;
  }

  /**
   * Fecha y hora ISO 8601.
   *
   * <p>Puede ser <code>null</code>.
   */
  public OffsetDateTime getLoadedAt() {
    return loadedAt;
  }

  /**
   * Fecha y hora ISO 8601.
   *
   * <p>Puede ser <code>null</code>.
   */
  public OffsetDateTime getClosedAt() {
    return closedAt;
  }

  /** Fecha y hora ISO 8601. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /** Fecha y hora ISO 8601. */
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
