// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.Map;

/** Parámetros de <code>GET /v1/events</code>: listar eventos. Viajan como query string. */
public final class EventListParams extends KuidaParams {
  private EventListParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link EventListParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, EventListParams> {
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

    /**
     * Asigna <code>type</code>.
     *
     * <p>Valores posibles: <code>intake.requested</code>, <code>patient.upserted</code>, <code>appointment.created</code>, <code>appointment.rescheduled</code>, <code>appointment.cancelled</code>, <code>visit.completed</code>, <code>treatment.prescribed</code>, <code>order.issued</code>.
     */
    public Builder type(String type) {
      return set("type", type);
    }

    /**
     * Asigna <code>status</code>.
     *
     * <p>Valores posibles: <code>received</code>, <code>processed</code>, <code>failed</code>, <code>duplicate</code>, <code>unhandled</code>.
     */
    public Builder status(String status) {
      return set("status", status);
    }

    /**
     * Asigna <code>source</code>.
     *
     * <p>Valores posibles: <code>api</code>, <code>pms</code>, <code>email</code>, <code>tool</code>, <code>manual</code>, <code>import</code>.
     */
    public Builder source(String source) {
      return set("source", source);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public EventListParams build() {
      return new EventListParams(values());
    }
  }
}
