// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Map;

/** Parámetros &lt;code&gt;AppointmentCancelParams&lt;/code&gt;. */
public final class AppointmentCancelParams extends KuidaParams {
  private AppointmentCancelParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link AppointmentCancelParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, AppointmentCancelParams> {
    Builder() {}

    /** Motivo, para el registro. */
    public Builder reason(String reason) {
      return set("reason", reason);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public AppointmentCancelParams build() {
      return new AppointmentCancelParams(values());
    }
  }
}
