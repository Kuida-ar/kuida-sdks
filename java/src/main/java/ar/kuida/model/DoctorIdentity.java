// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Map;

/** Cómo conoce su sistema al profesional. */
public final class DoctorIdentity extends KuidaParams {
  private DoctorIdentity(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link DoctorIdentity}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, DoctorIdentity> {
    Builder() {}

    /** Id del profesional en su sistema. */
    public Builder externalId(String externalId) {
      return set("externalId", externalId);
    }

    /** Nombre del profesional. */
    public Builder fullName(String fullName) {
      return set("fullName", fullName);
    }

    /** Especialidad. */
    public Builder specialty(String specialty) {
      return set("specialty", specialty);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public DoctorIdentity build() {
      return new DoctorIdentity(values());
    }
  }
}
