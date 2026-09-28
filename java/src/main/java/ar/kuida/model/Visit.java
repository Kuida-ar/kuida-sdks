// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;

/** Una consulta atendida. Crearla arranca el seguimiento post-consulta del paciente. */
public class Visit extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("patient")
  private String patient;

  @SerializedName("doctor")
  private String doctor;

  @SerializedName("appointment")
  private String appointment;

  @SerializedName("visitedAt")
  private OffsetDateTime visitedAt;

  @SerializedName("type")
  private String type;

  @SerializedName("diagnosis")
  private String diagnosis;

  @SerializedName("notes")
  private String notes;

  @SerializedName("externalId")
  private String externalId;

  @SerializedName("source")
  private String source;

  @SerializedName("createdAt")
  private OffsetDateTime createdAt;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public Visit() {}

  /** Id de la consulta. */
  public String getId() {
    return id;
  }

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>visit</code>.
   */
  public String getObject() {
    return object;
  }

  /** Paciente atendido. */
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

  /**
   * Turno del que salió la consulta.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getAppointment() {
    return appointment;
  }

  /** Fecha y hora ISO 8601. */
  public OffsetDateTime getVisitedAt() {
    return visitedAt;
  }

  /**
   * Campo <code>type</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getType() {
    return type;
  }

  /**
   * Campo <code>diagnosis</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getDiagnosis() {
    return diagnosis;
  }

  /**
   * Campo <code>notes</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getNotes() {
    return notes;
  }

  /**
   * Campo <code>externalId</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getExternalId() {
    return externalId;
  }

  /**
   * Campo <code>source</code>.
   *
   * <p>Valores posibles: <code>pms</code>, <code>manual</code>.
   */
  public String getSource() {
    return source;
  }

  /** Fecha y hora ISO 8601. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
