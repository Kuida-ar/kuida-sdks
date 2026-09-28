// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Map;

/** Parámetros de <code>GET /v1/doctors</code>: listar profesionales. Viajan como query string. */
public final class DoctorListParams extends KuidaParams {
  private DoctorListParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link DoctorListParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, DoctorListParams> {
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

    /** Filtre por activos o inactivos. */
    public Builder active(Boolean active) {
      return set("active", active);
    }

    /** Asigna <code>externalId</code>. */
    public Builder externalId(String externalId) {
      return set("externalId", externalId);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public DoctorListParams build() {
      return new DoctorListParams(values());
    }
  }
}
