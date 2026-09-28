// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaParams;
import java.util.List;
import java.util.Map;

/** Hace falta al menos un dato del paciente o de un referente (nombre, documento o teléfono). */
public final class IntakeRequestCreateParams extends KuidaParams {
  private IntakeRequestCreateParams(Map<String, Object> values) {
    super(values);
  }

  /** Crea un builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder de {@link IntakeRequestCreateParams}. */
  public static final class Builder extends KuidaParams.AbstractBuilder<Builder, IntakeRequestCreateParams> {
    Builder() {}

    /** Asigna <code>patient</code>. */
    public Builder patient(PatientIdentity patient) {
      return set("patient", patient);
    }

    /** Asigna <code>contacts</code>. */
    public Builder contacts(List<IntakeContact> contacts) {
      return set("contacts", contacts);
    }

    /** Agrega un elemento a <code>contacts</code>. */
    public Builder addContact(IntakeContact contact) {
      return add("contacts", contact);
    }

    /** Asigna <code>coverage</code>. */
    public Builder coverage(IntakeCoverage coverage) {
      return set("coverage", coverage);
    }

    /** Asigna <code>address</code>. */
    public Builder address(String address) {
      return set("address", address);
    }

    /** Asigna <code>requestedService</code>. */
    public Builder requestedService(String requestedService) {
      return set("requestedService", requestedService);
    }

    /** Resumen administrativo del pedido, sin contenido clínico. */
    public Builder summary(String summary) {
      return set("summary", summary);
    }

    /** Si el paciente está internado hoy. */
    public Builder hospitalized(Boolean hospitalized) {
      return set("hospitalized", hospitalized);
    }

    /** Quién pide el servicio. */
    public Builder sender(Sender sender) {
      return set("sender", sender);
    }

    /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
    @Override
    public IntakeRequestCreateParams build() {
      return new IntakeRequestCreateParams(values());
    }
  }

  /** Quién pide el servicio. */
  public static final class Sender extends KuidaParams {
    private Sender(Map<String, Object> values) {
      super(values);
    }

    /** Crea un builder vacío. */
    public static Builder builder() {
      return new Builder();
    }

    /** Builder de {@link Sender}. */
    public static final class Builder extends KuidaParams.AbstractBuilder<Builder, Sender> {
      Builder() {}

      /** Asigna <code>name</code>. */
      public Builder name(String name) {
        return set("name", name);
      }

      /** Asigna <code>email</code>. */
      public Builder email(String email) {
        return set("email", email);
      }

      /** Asigna <code>organization</code>. */
      public Builder organization(String organization) {
        return set("organization", organization);
      }

      /** Arma los parámetros. No valida: la API responde con el campo que falte o sea inválido. */
      @Override
      public Sender build() {
        return new Sender(values());
      }
    }
  }
}
