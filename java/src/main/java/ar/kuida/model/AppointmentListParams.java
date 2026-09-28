// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.time.OffsetDateTime;
import java.util.Map;

/** Parámetros de <code>GET /v1/appointments</code>: listar turnos. Viajan como query string. */
public final class AppointmentListParams extends KuidaParams {
  private AppointmentListParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link AppointmentListParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, AppointmentListParams> {
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

    /** Id del paciente (<code>pat_…</code>). */
    public Builder patient(String patient) {
      return set("patient", patient);
    }

    /**
     * Asigna <code>status</code>.
     *
     * <p>Valores posibles: <code>confirmed</code>, <code>pending</code>, <code>rescheduled</code>, <code>cancelled</code>, <code>completed</code>.
     */
    public Builder status(String status) {
      return set("status", status);
    }

    /** Asigna <code>externalId</code>. */
    public Builder externalId(String externalId) {
      return set("externalId", externalId);
    }

    /** Turnos desde esta fecha y hora. */
    public Builder startAtGte(OffsetDateTime startAtGte) {
      return set("startAtGte", startAtGte);
    }

    /**
     * Turnos desde esta fecha y hora.
     *
     * <p>Texto ISO 8601 con zona.
     */
    public Builder startAtGte(String startAtGte) {
      return set("startAtGte", startAtGte);
    }

    /** Turnos antes de esta fecha y hora. */
    public Builder startAtLt(OffsetDateTime startAtLt) {
      return set("startAtLt", startAtLt);
    }

    /**
     * Turnos antes de esta fecha y hora.
     *
     * <p>Texto ISO 8601 con zona.
     */
    public Builder startAtLt(String startAtLt) {
      return set("startAtLt", startAtLt);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public AppointmentListParams build() {
      return new AppointmentListParams(values());
    }
  }
}
