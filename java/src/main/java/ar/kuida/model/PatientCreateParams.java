// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.time.LocalDate;
import java.util.Map;

/**
 * Parámetros &lt;code&gt;PatientCreateParams&lt;/code&gt;.
 *
 * <p>Obligatorios: <code>phone</code>.
 */
public final class PatientCreateParams extends KuidaParams {
  private PatientCreateParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link PatientCreateParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, PatientCreateParams> {
    Builder() {}

    /** Celular con WhatsApp, en cualquier formato; Kuida lo normaliza. */
    public Builder phone(String phone) {
      return set("phone", phone);
    }

    /** Nombre y apellido. */
    public Builder fullName(String fullName) {
      return set("fullName", fullName);
    }

    /** Documento, sin puntos. */
    public Builder dni(String dni) {
      return set("dni", dni);
    }

    /** Correo electrónico. */
    public Builder email(String email) {
      return set("email", email);
    }

    /** Id del paciente en su sistema. */
    public Builder externalId(String externalId) {
      return set("externalId", externalId);
    }

    /** Fecha ISO 8601 (AAAA-MM-DD). */
    public Builder dateOfBirth(LocalDate dateOfBirth) {
      return set("dateOfBirth", dateOfBirth == null ? null : dateOfBirth.toString());
    }

    /**
     * Fecha ISO 8601 (AAAA-MM-DD).
     *
     * <p>Texto AAAA-MM-DD.
     */
    public Builder dateOfBirth(String dateOfBirth) {
      return set("dateOfBirth", dateOfBirth);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public PatientCreateParams build() {
      return new PatientCreateParams(values());
    }
  }
}
