// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.time.OffsetDateTime;
import java.util.Map;

/** Parámetros &lt;code&gt;AppointmentUpdateParams&lt;/code&gt;. */
public final class AppointmentUpdateParams extends KuidaParams {
  private AppointmentUpdateParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link AppointmentUpdateParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, AppointmentUpdateParams> {
    Builder() {}

    /** Nuevo horario. Reprograma los recordatorios. */
    public Builder startAt(OffsetDateTime startAt) {
      return set("startAt", startAt);
    }

    /**
     * Nuevo horario. Reprograma los recordatorios.
     *
     * <p>Texto ISO 8601 con zona.
     */
    public Builder startAt(String startAt) {
      return set("startAt", startAt);
    }

    /** Asigna <code>doctor</code>. */
    public Builder doctor(DoctorReference doctor) {
      return set("doctor", doctor);
    }

    /**
     * Asigna <code>doctor</code>.
     *
     * <p>Atajo para <code>DoctorReference.id(doctor)</code>.
     */
    public Builder doctor(String doctorId) {
      return set("doctor", DoctorReference.id(doctorId));
    }

    /**
     * Asigna <code>doctor</code>.
     *
     * <p>Atajo para <code>DoctorReference.identity(doctor)</code>.
     */
    public Builder doctor(DoctorIdentity doctor) {
      return set("doctor", DoctorReference.identity(doctor));
    }

    /** Asigna <code>type</code>. */
    public Builder type(String type) {
      return set("type", type);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public AppointmentUpdateParams build() {
      return new AppointmentUpdateParams(values());
    }
  }
}
