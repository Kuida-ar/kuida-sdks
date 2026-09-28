package ar.kuida;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base de todos los parámetros de entrada del SDK.
 *
 * <p>Los parámetros son inmutables y se arman con un builder. Solo viajan las claves que se
 * asignaron: una clave que no se tocó no se manda, y una asignada en {@code null} tampoco.
 * {@link AbstractBuilder#putExtra(String, Object)} permite mandar campos que la API agregó
 * después de esta versión del SDK.
 */
public abstract class KuidaParams implements JsonEncodable {
  private final Map<String, Object> values;

  /** Crea los parámetros con las claves asignadas por el builder. */
  protected KuidaParams(Map<String, Object> values) {
    this.values = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(values));
  }

  /** Las claves asignadas, con los nombres del cable (camelCase). Mapa de solo lectura. */
  public Map<String, Object> toMap() {
    return values;
  }

  @Override
  public Object toJsonValue() {
    return values;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + values;
  }

  /**
   * Builder base de los parámetros.
   *
   * @param <B> el builder concreto
   * @param <P> los parámetros que arma
   */
  public abstract static class AbstractBuilder<B extends AbstractBuilder<B, P>, P extends KuidaParams> {
    private final Map<String, Object> values = new LinkedHashMap<String, Object>();

    /** Constructor para los builders concretos. */
    protected AbstractBuilder() {}

    @SuppressWarnings("unchecked")
    private B self() {
      return (B) this;
    }

    /** Asigna una clave. Un valor {@code null} la quita. */
    protected B set(String key, Object value) {
      if (value == null) {
        values.remove(key);
      } else if (value instanceof Collection) {
        values.put(key, new ArrayList<Object>((Collection<?>) value));
      } else {
        values.put(key, value);
      }
      return self();
    }

    /** Agrega un elemento a la lista de una clave. */
    @SuppressWarnings("unchecked")
    protected B add(String key, Object item) {
      Object current = values.get(key);
      List<Object> list;
      if (current instanceof List) {
        list = (List<Object>) current;
      } else {
        list = new ArrayList<Object>();
        values.put(key, list);
      }
      list.add(item);
      return self();
    }

    /** Agrega una entrada al mapa de una clave. */
    @SuppressWarnings("unchecked")
    protected B putInMap(String key, String mapKey, Object value) {
      Object current = values.get(key);
      Map<String, Object> map;
      if (current instanceof Map) {
        map = new LinkedHashMap<String, Object>((Map<String, Object>) current);
      } else {
        map = new LinkedHashMap<String, Object>();
      }
      map.put(mapKey, value);
      values.put(key, map);
      return self();
    }

    /**
     * Asigna un campo que este SDK todavía no conoce. El nombre va tal cual (camelCase del cable).
     */
    public B putExtra(String key, Object value) {
      return set(key, value);
    }

    /** Copia de las claves asignadas hasta ahora. */
    protected Map<String, Object> values() {
      Map<String, Object> copy = new LinkedHashMap<String, Object>();
      for (Map.Entry<String, Object> e : values.entrySet()) {
        Object v = e.getValue();
        if (v instanceof List) v = Collections.unmodifiableList(new ArrayList<Object>((List<?>) v));
        else if (v instanceof Map) v = Collections.unmodifiableMap(new LinkedHashMap<Object, Object>((Map<?, ?>) v));
        copy.put(e.getKey(), v);
      }
      return copy;
    }

    /** Arma los parámetros. */
    public abstract P build();
  }
}
