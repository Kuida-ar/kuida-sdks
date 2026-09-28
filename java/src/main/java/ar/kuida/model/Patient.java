// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.time.OffsetDateTime;

/** Un paciente de la organización. */
public class Patient extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("fullName")
  private String fullName;

  @SerializedName("phone")
  private String phone;

  @SerializedName("email")
  private String email;

  @SerializedName("dni")
  private String dni;

  @SerializedName("externalId")
  private String externalId;

  @SerializedName("dateOfBirth")
  private String dateOfBirth;

  @SerializedName("stage")
  private String stage;

  @SerializedName("optedOut")
  private Boolean optedOut;

  @SerializedName("createdAt")
  private OffsetDateTime createdAt;

  @SerializedName("updatedAt")
  private OffsetDateTime updatedAt;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public Patient() {}

  /** Id del paciente. */
  public String getId() {
    return id;
  }

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>patient</code>.
   */
  public String getObject() {
    return object;
  }

  /**
   * Campo <code>fullName</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getFullName() {
    return fullName;
  }

  /**
   * Teléfono normalizado.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getPhone() {
    return phone;
  }

  /**
   * Campo <code>email</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Campo <code>dni</code>.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getDni() {
    return dni;
  }

  /**
   * Id del paciente en su sistema.
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getExternalId() {
    return externalId;
  }

  /**
   * Fecha de nacimiento (AAAA-MM-DD).
   *
   * <p>Puede ser <code>null</code>.
   */
  public String getDateOfBirth() {
    return dateOfBirth;
  }

  /**
   * Estado básico del paciente en Kuida.
   *
   * <p>Valores posibles: <code>pending_follow_up</code>, <code>in_conversation</code>, <code>scheduled</code>, <code>periodic_follow_up</code>, <code>discharged</code>, <code>lost</code>.
   */
  public String getStage() {
    return stage;
  }

  /** Si el paciente pidió no recibir mensajes. Kuida no le escribe. */
  public Boolean getOptedOut() {
    return optedOut;
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
