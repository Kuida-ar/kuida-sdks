// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Parámetros &lt;code&gt;TreatmentCreateParams&lt;/code&gt;.
 *
 * <p>Obligatorios: <code>patient</code>, <code>name</code>.
 */
public final class TreatmentCreateParams extends KuidaParams {
  private TreatmentCreateParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link TreatmentCreateParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, TreatmentCreateParams> {
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

    /** Asigna <code>name</code>. */
    public Builder name(String name) {
      return set("name", name);
    }

    /**
     * Default <code>medication</code>.
     *
     * <p>Valores posibles: <code>medication</code>, <code>therapy</code>, <code>diet</code>, <code>study</code>, <code>labwork</code>, <code>lifestyle</code>.
     */
    public Builder kind(String kind) {
      return set("kind", kind);
    }

    /** Asigna <code>dosage</code>. */
    public Builder dosage(String dosage) {
      return set("dosage", dosage);
    }

    /** Asigna <code>frequency</code>. */
    public Builder frequency(String frequency) {
      return set("frequency", frequency);
    }

    /** Asigna <code>instructions</code>. */
    public Builder instructions(String instructions) {
      return set("instructions", instructions);
    }

    /** Fecha y hora ISO 8601. */
    public Builder startDate(OffsetDateTime startDate) {
      return set("startDate", startDate);
    }

    /**
     * Fecha y hora ISO 8601.
     *
     * <p>Texto ISO 8601 con zona.
     */
    public Builder startDate(String startDate) {
      return set("startDate", startDate);
    }

    /** Fecha y hora ISO 8601. */
    public Builder endDate(OffsetDateTime endDate) {
      return set("endDate", endDate);
    }

    /**
     * Fecha y hora ISO 8601.
     *
     * <p>Texto ISO 8601 con zona.
     */
    public Builder endDate(String endDate) {
      return set("endDate", endDate);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public TreatmentCreateParams build() {
      return new TreatmentCreateParams(values());
    }
  }
}
