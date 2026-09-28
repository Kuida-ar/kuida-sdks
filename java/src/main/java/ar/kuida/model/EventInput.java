// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Un evento con el sobre común de Kuida: <code>id</code>, <code>type</code>, <code>data</code> y opcionales.
 *
 * <p>Obligatorios: <code>id</code>, <code>type</code>, <code>data</code>.
 *
 * <p>Los esquemas de <code>data</code> por tipo están en la referencia de la API.
 */
public final class EventInput extends KuidaParams {
  /** Tipo <code>intake.requested</code>. */
  public static final String TYPE_INTAKE_REQUESTED = "intake.requested";
  /** Tipo <code>patient.upserted</code>. */
  public static final String TYPE_PATIENT_UPSERTED = "patient.upserted";
  /** Tipo <code>appointment.created</code>. */
  public static final String TYPE_APPOINTMENT_CREATED = "appointment.created";
  /** Tipo <code>appointment.rescheduled</code>. */
  public static final String TYPE_APPOINTMENT_RESCHEDULED = "appointment.rescheduled";
  /** Tipo <code>appointment.cancelled</code>. */
  public static final String TYPE_APPOINTMENT_CANCELLED = "appointment.cancelled";
  /** Tipo <code>visit.completed</code>. */
  public static final String TYPE_VISIT_COMPLETED = "visit.completed";
  /** Tipo <code>treatment.prescribed</code>. */
  public static final String TYPE_TREATMENT_PRESCRIBED = "treatment.prescribed";
  /** Tipo <code>order.issued</code>. */
  public static final String TYPE_ORDER_ISSUED = "order.issued";

  private EventInput(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link EventInput}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, EventInput> {
    Builder() {}

    /** Tu id del evento. Es la clave de idempotencia: reenviar el mismo id no repite nada. */
    public Builder id(String id) {
      return set("id", id);
    }

    /** Versión del esquema del evento. Siempre 1 en v1. */
    public Builder schemaVersion(Integer schemaVersion) {
      return set("schemaVersion", schemaVersion);
    }

    /** Cuándo pasó. Default: ahora. */
    public Builder occurredAt(OffsetDateTime occurredAt) {
      return set("occurredAt", occurredAt);
    }

    /**
     * Cuándo pasó. Default: ahora.
     *
     * <p>Texto ISO 8601 con zona.
     */
    public Builder occurredAt(String occurredAt) {
      return set("occurredAt", occurredAt);
    }

    /** Qué sistema lo emite, para el registro. */
    public Builder source(Source source) {
      return set("source", source);
    }

    /**
     * Tipo de evento.
     *
     * <p>Valores posibles: <code>intake.requested</code>, <code>patient.upserted</code>, <code>appointment.created</code>, <code>appointment.rescheduled</code>, <code>appointment.cancelled</code>, <code>visit.completed</code>, <code>treatment.prescribed</code>, <code>order.issued</code>.
     */
    public Builder type(String type) {
      return set("type", type);
    }

    /** Los datos del evento. Viajan tal cual, sin conversión de claves. */
    public Builder data(Map<String, Object> data) {
      return set("data", data);
    }

    /** Agrega una clave a <code>data</code>. */
    public Builder putData(String key, Object value) {
      return putInMap("data", key, value);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public EventInput build() {
      return new EventInput(values());
    }
  }

  /** Qué sistema lo emite, para el registro. */
  public static final class Source extends KuidaParams {
    private Source(Map<String, Object> values) {
      super(values);
    }

    /** Crea un builder vacío. */
    public static Builder builder() {
      return new Builder();
    }

    /** Builder de {@link Source}. */
    public static final class Builder extends KuidaParams.AbstractBuilder<Builder, Source> {
      Builder() {}

      /** Asigna <code>system</code>. */
      public Builder system(String system) {
        return set("system", system);
      }

      /** Asigna <code>version</code>. */
      public Builder version(String version) {
        return set("version", version);
      }

      /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
      @Override
      public Source build() {
        return new Source(values());
      }
    }
  }
}
