// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Map;

/** Parámetros &lt;code&gt;IntakeCoverage&lt;/code&gt;. */
public final class IntakeCoverage extends KuidaParams {
  private IntakeCoverage(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link IntakeCoverage}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, IntakeCoverage> {
    Builder() {}

    /** Obra social o prepaga. */
    public Builder insurer(String insurer) {
      return set("insurer", insurer);
    }

    /** Asigna <code>plan</code>. */
    public Builder plan(String plan) {
      return set("plan", plan);
    }

    /** Número de afiliado. */
    public Builder memberId(String memberId) {
      return set("memberId", memberId);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public IntakeCoverage build() {
      return new IntakeCoverage(values());
    }
  }
}
