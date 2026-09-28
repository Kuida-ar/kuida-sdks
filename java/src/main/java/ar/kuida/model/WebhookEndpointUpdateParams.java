// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Parámetros &lt;code&gt;WebhookEndpointUpdateParams&lt;/code&gt;. */
public final class WebhookEndpointUpdateParams extends KuidaParams {
  private WebhookEndpointUpdateParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link WebhookEndpointUpdateParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, WebhookEndpointUpdateParams> {
    Builder() {}

    /** Asigna <code>url</code>. */
    public Builder url(String url) {
      return set("url", url);
    }

    /**
     * Asigna <code>enabledEvents</code>.
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
     * Asigna <code>enabledEvents</code>.
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

    /** <code>true</code> pausa los envíos sin borrar el endpoint. */
    public Builder disabled(Boolean disabled) {
      return set("disabled", disabled);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public WebhookEndpointUpdateParams build() {
      return new WebhookEndpointUpdateParams(values());
    }
  }
}
