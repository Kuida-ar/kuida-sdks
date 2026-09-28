// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Map;

/** Parámetros de <code>GET /v1/webhook_deliveries</code>: listar entregas de webhooks. Viajan como query string. */
public final class WebhookDeliveryListParams extends KuidaParams {
  private WebhookDeliveryListParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link WebhookDeliveryListParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, WebhookDeliveryListParams> {
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

    /** Id del endpoint (<code>we_…</code>). */
    public Builder webhookEndpoint(String webhookEndpoint) {
      return set("webhookEndpoint", webhookEndpoint);
    }

    /**
     * Asigna <code>status</code>.
     *
     * <p>Valores posibles: <code>pending</code>, <code>succeeded</code>, <code>failed</code>.
     */
    public Builder status(String status) {
      return set("status", status);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public WebhookDeliveryListParams build() {
      return new WebhookDeliveryListParams(values());
    }
  }
}
