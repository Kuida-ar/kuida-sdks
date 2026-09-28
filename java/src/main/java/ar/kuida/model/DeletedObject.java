// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;

/** Confirmación de un borrado. */
public class DeletedObject extends KuidaObject {
  @SerializedName("id")
  private String id;

  @SerializedName("object")
  private String object;

  @SerializedName("deleted")
  private Boolean deleted;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public DeletedObject() {}

  /** Campo <code>id</code>. */
  public String getId() {
    return id;
  }

  /** Campo <code>object</code>. */
  public String getObject() {
    return object;
  }

  /**
   * Campo <code>deleted</code>.
   *
   * <p>Siempre <code>true</code>.
   */
  public Boolean getDeleted() {
    return deleted;
  }
}
