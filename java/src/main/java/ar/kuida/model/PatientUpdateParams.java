// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.time.LocalDate;
import java.util.Map;

/** Solo cambia lo que envías. El teléfono no se cambia: es la identidad. */
public final class PatientUpdateParams extends KuidaParams {
  private PatientUpdateParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link PatientUpdateParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, PatientUpdateParams> {
    Builder() {}

    /** Asigna <code>fullName</code>. */
    public Builder fullName(String fullName) {
      return set("fullName", fullName);
    }

    /** Asigna <code>email</code>. */
    public Builder email(String email) {
      return set("email", email);
    }

    /** Asigna <code>dni</code>. */
    public Builder dni(String dni) {
      return set("dni", dni);
    }

    /** Asigna <code>externalId</code>. */
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
    public PatientUpdateParams build() {
      return new PatientUpdateParams(values());
    }
  }
}
