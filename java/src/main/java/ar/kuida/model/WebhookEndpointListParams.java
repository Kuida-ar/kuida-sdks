// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Map;

/** Parámetros de <code>GET /v1/webhook_endpoints</code>: listar endpoints de webhooks. Viajan como query string. */
public final class WebhookEndpointListParams extends KuidaParams {
  private WebhookEndpointListParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link WebhookEndpointListParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, WebhookEndpointListParams> {
    Builder() {}

    /** Asigna <code>limit</code>. */
    public Builder limit(Integer limit) {
      return set("limit", limit);
    }

    /** Asigna <code>startingAfter</code>. */
    public Builder startingAfter(String startingAfter) {
      return set("startingAfter", startingAfter);
    }

    /** Asigna <code>endingBefore</code>. */
    public Builder endingBefore(String endingBefore) {
      return set("endingBefore", endingBefore);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public WebhookEndpointListParams build() {
      return new WebhookEndpointListParams(values());
    }
  }
}
