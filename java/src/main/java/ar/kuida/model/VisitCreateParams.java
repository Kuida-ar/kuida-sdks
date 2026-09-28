// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Parámetros &lt;code&gt;VisitCreateParams&lt;/code&gt;.
 *
 * <p>Obligatorios: <code>patient</code>, <code>visitedAt</code>.
 */
public final class VisitCreateParams extends KuidaParams {
  private VisitCreateParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link VisitCreateParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, VisitCreateParams> {
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

    /** Cuándo se atendió. */
    public Builder visitedAt(OffsetDateTime visitedAt) {
      return set("visitedAt", visitedAt);
    }

    /**
     * Cuándo se atendió.
     *
     * <p>Texto ISO 8601 con zona.
     */
    public Builder visitedAt(String visitedAt) {
      return set("visitedAt", visitedAt);
    }

    /** Asigna <code>type</code>. */
    public Builder type(String type) {
      return set("type", type);
    }

    /** Id de la consulta en su sistema. Si ya existe, se devuelve esa y no se crea otra. */
    public Builder externalId(String externalId) {
      return set("externalId", externalId);
    }

    /** Turno del que sale la consulta: id de Kuida (<code>apt_…</code>) o el <code>externalId</code> del turno. Lo marca como atendido. */
    public Builder appointment(String appointment) {
      return set("appointment", appointment);
    }

    /** Asigna <code>diagnosis</code>. */
    public Builder diagnosis(String diagnosis) {
      return set("diagnosis", diagnosis);
    }

    /** Asigna <code>notes</code>. */
    public Builder notes(String notes) {
      return set("notes", notes);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public VisitCreateParams build() {
      return new VisitCreateParams(values());
    }
  }
}
