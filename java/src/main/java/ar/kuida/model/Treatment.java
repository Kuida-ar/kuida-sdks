// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;

/** Un tratamiento indicado al paciente. */
public class Treatment extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("patient")
  private String patient;

  @SerializedName("kind")
  private String kind;

  @SerializedName("name")
  private String name;

  @SerializedName("dosage")
  private String dosage;

  @SerializedName("frequency")
  private String frequency;

  @SerializedName("instructions")
  private String instructions;

  @SerializedName("startDate")
  private OffsetDateTime startDate;

  @SerializedName("endDate")
  private OffsetDateTime endDate;

  @SerializedName("active")
  private Boolean active;

  @SerializedName("createdAt")
  private OffsetDateTime createdAt;

  @SerializedName("updatedAt")
  private OffsetDateTime updatedAt;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public Treatment() {}

  /** Id del tratamiento. */
  public String getId() {
    return id;
  }

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>treatment</code>.
   */
  public String getObject() {
    return object;
  }

  /** Paciente. */
  public String getPatient() {
    return patient;
  }

  /**
   * Campo <code>kind</code>.
   *
   * <p>Valores posibles: <code>medication</code>, <code>therapy</code>, <code>diet</code>, <code>study</code>, <code>labwork</code>, <code>lifestyle</code>.
   */
  public String getKind() {
    return kind;
  }

  /** Campo <code>name</code>. */
  public String getName() {
    return name;
  }

  /**
   * Campo <code>dosage</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getDosage() {
    return dosage;
  }

  /**
   * Campo <code>frequency</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getFrequency() {
    return frequency;
  }

  /**
   * Campo <code>instructions</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getInstructions() {
    return instructions;
  }

  /**
   * Fecha y hora ISO 8601.
   *
   * <p>Puede ser <code>null</code>.
   */
  public OffsetDateTime getStartDate() {
    return startDate;
  }

  /**
   * Fecha y hora ISO 8601.
   *
   * <p>Puede ser <code>null</code>.
   */
  public OffsetDateTime getEndDate() {
    return endDate;
  }

  /** Campo <code>active</code>. */
  public Boolean getActive() {
    return active;
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
