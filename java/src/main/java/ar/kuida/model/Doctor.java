// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;

/** Un profesional del padrón de la organización. */
public class Doctor extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("fullName")
  private String fullName;

  @SerializedName("specialty")
  private String specialty;

  @SerializedName("licenseNumber")
  private String licenseNumber;

  @SerializedName("externalId")
  private String externalId;

  @SerializedName("active")
  private Boolean active;

  @SerializedName("createdAt")
  private OffsetDateTime createdAt;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public Doctor() {}

  /** Id del profesional. */
  public String getId() {
    return id;
  }

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>doctor</code>.
   */
  public String getObject() {
    return object;
  }

  /** Campo <code>fullName</code>. */
  public String getFullName() {
    return fullName;
  }

  /**
   * Campo <code>specialty</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getSpecialty() {
    return specialty;
  }

  /**
   * Matrícula.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getLicenseNumber() {
    return licenseNumber;
  }

  /**
   * Campo <code>externalId</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getExternalId() {
    return externalId;
  }

  /** Si atiende hoy. Solo los activos reciben turnos del agente. */
  public Boolean getActive() {
    return active;
  }

  /** Fecha y hora ISO 8601. */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
