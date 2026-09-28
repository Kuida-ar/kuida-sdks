// Generado por scripts/generate.mjs a partir de openapi/kuida-v1.json. No editar a mano.
package ar.kuida.model;

import ar.kuida.KuidaObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Un resultado por evento, en el mismo orden. */
public class EventBatchResponse extends KuidaObject {
  @SerializedName("object")
  private String object;

  @SerializedName("results")
  private List<EventResult> results;

  /** Constructor vacío: los objetos los arma el SDK a partir de las respuestas. */
  public EventBatchResponse() {}

  /**
   * Campo <code>object</code>.
   *
   * <p>Siempre <code>event_batch</code>.
   */
  public String getObject() {
    return object;
  }

  /** Campo <code>results</code>. */
  public List<EventResult> getResults() {
    return results;
  }
}
