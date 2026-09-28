// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;

/** Un turno. Kuida le manda al paciente los recordatorios que la organización configuró. */
public class Appointment extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("patient")
  private String patient;

  @SerializedName("doctor")
  private String doctor;

  @SerializedName("startAt")
  private OffsetDateTime startAt;

  @SerializedName("status")
  private String status;

  @SerializedName("type")
  private String type;

  @SerializedName("externalId")
  private String externalId;

  @SerializedName("source")
  private String source;

  @SerializedName("cancelledAt")
  private OffsetDateTime cancelledAt;

  @SerializedName("createdAt")
  private OffsetDateTime createdAt;

  @SerializedName("updatedAt")
  private OffsetDateTime updatedAt;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public Appointment() {}

  /** Id del turno. */
  public String getId() {
    return id;
  }

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>appointment</code>.
   */
  public String getObject() {
    return object;
  }

  /** Paciente del turno. */
  public String getPatient() {
    return patient;
  }

  /**
   * Profesional.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getDoctor() {
    return doctor;
  }

  /** Fecha y hora ISO 8601. */
  public OffsetDateTime getStartAt() {
    return startAt;
  }

  /**
   * Campo <code>status</code>.
   *
   * <p>Valores posibles: <code>confirmed</code>, <code>pending</code>, <code>rescheduled</code>, <code>cancelled</code>, <code>completed</code>.
   */
  public String getStatus() {
    return status;
  }

  /**
   * Tipo de turno o práctica.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getType() {
    return type;
  }

  /**
   * Id del turno en su sistema.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getExternalId() {
    return externalId;
  }

  /**
   * Por dónde entró: su API, el conector del sistema de gestión, el agente de Kuida o el equipo.
   *
   * <p>Valores posibles: <code>api</code>, <code>pms</code>, <code>agent</code>, <code>manual</code>.
   */
  public String getSource() {
    return source;
  }

  /**
   * Fecha y hora ISO 8601.
   *
   * <p>Puede ser <code>null</code>.
   */
  public OffsetDateTime getCancelledAt() {
    return cancelledAt;
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
