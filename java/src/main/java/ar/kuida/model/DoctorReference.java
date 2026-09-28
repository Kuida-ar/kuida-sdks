// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.JsonEncodable;

/**
 * Un profesional: su id de Kuida (<code>doc_…</code>) o sus datos.
 *
 * <p>Se arma con {@link #id(String)} (<code>doc_…</code>) o con {@link #identity(DoctorIdentity)}.
 */
public final class DoctorReference implements JsonEncodable {
  private final String id;
  private final DoctorIdentity identity;

  private DoctorReference(String id, DoctorIdentity identity) {
    this.id = id;
    this.identity = identity;
  }

  /** Referencia por id de Kuida (<code>doc_…</code>). */
  public static DoctorReference id(String id) {
    if (id == null) throw new IllegalArgumentException("id no puede ser null");
    return new DoctorReference(id, null);
  }

  /** Referencia por identidad: Kuida busca el objeto con estos datos o lo crea. */
  public static DoctorReference identity(DoctorIdentity identity) {
    if (identity == null) throw new IllegalArgumentException("identity no puede ser null");
    return new DoctorReference(null, identity);
  }

  /** El id, o <code>null</code> si la referencia es por identidad. */
  public String getId() {
    return id;
  }

  /** La identidad, o <code>null</code> si la referencia es por id. */
  public DoctorIdentity getIdentity() {
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
