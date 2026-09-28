// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Map;

/** Parámetros de <code>GET /v1/patients</code>: listar pacientes. Viajan como query string. */
public final class PatientListParams extends KuidaParams {
  private PatientListParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link PatientListParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, PatientListParams> {
    Builder() {}

    /** Cantidad de objetos a devolver, entre 1 y 100. Default 10. */
    public Builder limit(Integer limit) {
      return set("limit", limit);
    }

    /** Cursor: id del último objeto de la página anterior. Devuelve los siguientes. */
    public Builder startingAfter(String startingAfter) {
      return set("startingAfter", startingAfter);
    }

    /** Cursor: id del primer objeto de la página actual. Devuelve los anteriores. */
    public Builder endingBefore(String endingBefore) {
      return set("endingBefore", endingBefore);
    }

    /** Filtre por teléfono (cualquier formato). */
    public Builder phone(String phone) {
      return set("phone", phone);
    }

    /** Asigna <code>dni</code>. */
    public Builder dni(String dni) {
      return set("dni", dni);
    }

    /** Asigna <code>externalId</code>. */
    public Builder externalId(String externalId) {
      return set("externalId", externalId);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public PatientListParams build() {
      return new PatientListParams(values());
    }
  }
}
