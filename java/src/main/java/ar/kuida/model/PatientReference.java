// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.JsonEncodable;

/**
 * Un paciente: su id de Kuida (<code>pat_…</code>) o los datos con que lo conoce su sistema.
 *
 * <p>Se arma con {@link #id(String)} (<code>pat_…</code>) o con {@link #identity(PatientIdentity)}.
 */
public final class PatientReference implements JsonEncodable {
  private final String id;
  private final PatientIdentity identity;

  private PatientReference(String id, PatientIdentity identity) {
    this.id = id;
    this.identity = identity;
  }

  /** Referencia por id de Kuida (<code>pat_…</code>). */
  public static PatientReference id(String id) {
    if (id == null) throw new IllegalArgumentException("id no puede ser null");
    return new PatientReference(id, null);
  }

  /** Referencia por identidad: Kuida busca el objeto con estos datos o lo crea. */
  public static PatientReference identity(PatientIdentity identity) {
    if (identity == null) throw new IllegalArgumentException("identity no puede ser null");
    return new PatientReference(null, identity);
  }

  /** El id, o <code>null</code> si la referencia es por identidad. */
  public String getId() {
    return id;
  }

  /** La identidad, o <code>null</code> si la referencia es por id. */
  public PatientIdentity getIdentity() {
    return identity;
  }

  /** <code>true</code> si la referencia es por id. */
  public boolean isId() {
    return id != null;
  }

  @Override
  public Object toJsonValue() {
    return id != null ? id : identity;
  }

  @Override
  public String toString() {
    return id != null ? id : String.valueOf(identity);
  }
}
