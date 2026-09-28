package ar.kuida;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Una página de una lista con cursor ({@code object: "list"}).
 *
 * <p>Para la página siguiente se pasa {@code startingAfter} con el id del último elemento, o se deja que
 * el SDK lo haga con {@link #autoPagingIterable()}:
 *
 * <pre>{@code
 * for (Patient p : kuida.patients().list(PatientListParams.builder().limit(100).build()).autoPagingIterable()) {
 *   System.out.println(p.getFullName());
 * }
 * }</pre>
 *
 * @param <T> tipo de los elementos
 */
public class KuidaList<T> extends KuidaObject {
  /** Pide una página con los query params dados. */
  interface PageFetcher<T> {
    KuidaList<T> fetch(Map<String, Object> query);
  }

  @SerializedName("object")
  private String object;

  @SerializedName("data")
  private List<T> data;

  @SerializedName("hasMore")
  private Boolean hasMore;

  @SerializedName("url")
  private String url;

  private transient PageFetcher<T> fetcher;
  private transient Map<String, Object> query;

  /** Constructor vacío: las listas las arma el SDK a partir de las respuestas. */
  public KuidaList() {}

  /** Siempre {@code list}. */
  public String getObject() {
    return object;
  }

  /** Los elementos de esta página. */
  public List<T> getData() {
    return data == null ? Collections.<T>emptyList() : data;
  }

  /** {@code true} si hay más elementos después de esta página. */
  public boolean hasMore() {
    return hasMore != null && hasMore;
  }

  /** Ruta de la lista (por ejemplo {@code /v1/patients}). */
  public String getUrl() {
    return url;
  }

  void setPaging(PageFetcher<T> fetcher, Map<String, Object> query) {
    this.fetcher = fetcher;
    this.query = query == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(query);
  }

  /**
   * Recorre esta página y las siguientes, pidiéndolas a medida que hacen falta, con el mismo
   * {@code limit} y los mismos filtros. Si la lista se pidió con {@code endingBefore}, recorre
   * hacia atrás. Un error de la API en una página se lanza desde {@code next()}.
   */
  public Iterable<T> autoPagingIterable() {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return new PagingIterator<T>(KuidaList.this);
      }
    };
  }

  /** Igual que {@link #autoPagingIterable()}, como {@link Stream} secuencial y perezoso. */
  public Stream<T> autoPagingStream() {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(autoPagingIterable().iterator(), Spliterator.ORDERED | Spliterator.NONNULL), false);
  }

  private KuidaList<T> nextPage() {
    List<T> items = getData();
    if (fetcher == null || !hasMore() || items.isEmpty()) return null;
    Map<String, Object> next = new LinkedHashMap<String, Object>(query);
    if (query.containsKey("endingBefore")) {
      next.put("endingBefore", idOf(items.get(0)));
    } else {
      next.put("startingAfter", idOf(items.get(items.size() - 1)));
    }
    return fetcher.fetch(next);
  }

  private static String idOf(Object item) {
    if (item instanceof KuidaObject) {
      JsonObject raw = ((KuidaObject) item).getRawJson();
      JsonElement id = raw == null ? null : raw.get("id");
      if (id != null && id.isJsonPrimitive()) return id.getAsString();
    }
    throw new IllegalStateException("El elemento no tiene id para paginar: " + item);
  }

  private static final class PagingIterator<T> implements Iterator<T> {
    private KuidaList<T> page;
    private List<T> items;
    private int index;
    private boolean backwards;

    PagingIterator(KuidaList<T> first) {
      this.page = first;
      this.backwards = first.query != null && first.query.containsKey("endingBefore");
      this.items = ordered(first);
    }

    private List<T> ordered(KuidaList<T> p) {
      List<T> list = new ArrayList<T>(p.getData());
      if (backwards) Collections.reverse(list);
      return list;
    }

    @Override
    public boolean hasNext() {
      while (index >= items.size()) {
        KuidaList<T> next = page.nextPage();
        if (next == null) return false;
        page = next;
        items = ordered(next);
        index = 0;
      }
      return true;
    }

    @Override
    public T next() {
      if (!hasNext()) throw new NoSuchElementException();
      return items.get(index++);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
