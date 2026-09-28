// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Parámetros &lt;code&gt;WebhookEndpointCreateParams&lt;/code&gt;.
 *
 * <p>Obligatorios: <code>url</code>, <code>enabledEvents</code>.
 */
public final class WebhookEndpointCreateParams extends KuidaParams {
  private WebhookEndpointCreateParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link WebhookEndpointCreateParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, WebhookEndpointCreateParams> {
    Builder() {}

    /** URL https de su sistema. */
    public Builder url(String url) {
      return set("url", url);
    }

    /**
     * Qué eventos quiere recibir. <code>*</code> = todos.
     *
     * <p>Valores posibles: <code>intake.requested</code>, <code>patient.upserted</code>, <code>appointment.created</code>, <code>appointment.rescheduled</code>, <code>appointment.cancelled</code>, <code>visit.completed</code>, <code>treatment.prescribed</code>, <code>order.issued</code>, <code>intake.created</code>, <code>intake.ready</code>, <code>conversation.handoff</code>, <code>patient.silent</code>, <code>webhook.ping</code>, <code>*</code>.
     */
    public Builder enabledEvents(List<String> enabledEvents) {
      return set("enabledEvents", enabledEvents);
    }

    /** Agrega un elemento a <code>enabledEvents</code>. */
    public Builder addEnabledEvent(String enabledEvent) {
      return add("enabledEvents", enabledEvent);
    }

    /**
     * Qué eventos quiere recibir. <code>*</code> = todos.
     *
     * <p>Valores posibles: <code>intake.requested</code>, <code>patient.upserted</code>, <code>appointment.created</code>, <code>appointment.rescheduled</code>, <code>appointment.cancelled</code>, <code>visit.completed</code>, <code>treatment.prescribed</code>, <code>order.issued</code>, <code>intake.created</code>, <code>intake.ready</code>, <code>conversation.handoff</code>, <code>patient.silent</code>, <code>webhook.ping</code>, <code>*</code>.
     */
    public Builder enabledEvents(String... enabledEvents) {
      return set("enabledEvents", Arrays.asList(enabledEvents));
    }

    /** Asigna <code>description</code>. */
    public Builder description(String description) {
      return set("description", description);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public WebhookEndpointCreateParams build() {
      return new WebhookEndpointCreateParams(values());
    }
  }
}
