// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Parámetros &lt;code&gt;AppointmentCreateParams&lt;/code&gt;.
 *
 * <p>Obligatorios: <code>patient</code>, <code>startAt</code>.
 */
public final class AppointmentCreateParams extends KuidaParams {
  private AppointmentCreateParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link AppointmentCreateParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, AppointmentCreateParams> {
    Builder() {}

    /** Asigna <code>patient</code>. */
    public Builder patient(PatientReference patient) {
      return set("patient", patient);
    }

    /**
     * Asigna <code>patient</code>.
     *
     * <p>Atajo para <code>PatientReference.id(patient)</code>.
     */
    public Builder patient(String patientId) {
      return set("patient", PatientReference.id(patientId));
    }

    /**
     * Asigna <code>patient</code>.
     *
     * <p>Atajo para <code>PatientReference.identity(patient)</code>.
     */
    public Builder patient(PatientIdentity patient) {
      return set("patient", PatientReference.identity(patient));
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

    /** Inicio del turno. */
    public Builder startAt(OffsetDateTime startAt) {
      return set("startAt", startAt);
    }

    /**
     * Inicio del turno.
     *
     * <p>Texto ISO 8601 con zona.
     */
    public Builder startAt(String startAt) {
      return set("startAt", startAt);
    }

    /** Tipo de turno o práctica. */
    public Builder type(String type) {
      return set("type", type);
    }

    /** Id del turno en tu sistema. Si ya existe un turno con ese id, se devuelve ese y no se crea otro. */
    public Builder externalId(String externalId) {
      return set("externalId", externalId);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public AppointmentCreateParams build() {
      return new AppointmentCreateParams(values());
    }
  }
}
