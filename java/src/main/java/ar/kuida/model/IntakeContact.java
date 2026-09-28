// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Map;

/** Un referente del paciente (familiar, cuidador). */
public final class IntakeContact extends KuidaParams {
  private IntakeContact(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link IntakeContact}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, IntakeContact> {
    Builder() {}

    /** Asigna <code>name</code>. */
    public Builder name(String name) {
      return set("name", name);
    }

    /** Asigna <code>relationship</code>. */
    public Builder relationship(String relationship) {
      return set("relationship", relationship);
    }

    /** Asigna <code>phone</code>. */
    public Builder phone(String phone) {
      return set("phone", phone);
    }

    /** Asigna <code>email</code>. */
    public Builder email(String email) {
      return set("email", email);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public IntakeContact build() {
      return new IntakeContact(values());
    }
  }
}
