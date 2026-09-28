package ar.kuida.service;

import ar.kuida.ApiRequestor;
import ar.kuida.KuidaList;
import ar.kuida.RequestOptions;
import ar.kuida.model.Event;
import ar.kuida.model.EventBatchResponse;
import ar.kuida.model.EventInput;
import ar.kuida.model.EventListParams;
import java.util.List;

/**
 * {@code /v1/events}: la puerta genérica. Tu sistema avisa lo que pasó con el sobre común y Kuida
 * reacciona. Cada evento se procesa por separado y trae su propio resultado.
 */
public final class EventService extends ApiService {
  /** Uso interno: el servicio se obtiene desde {@link ar.kuida.KuidaClient}. */
  public EventService(ApiRequestor requestor) {
    super(requestor);
  }

  /**
   * Manda un evento. Reenviar el mismo {@code id} no repite nada: el resultado viene como
   * {@code duplicate}.
   */
  public EventBatchResponse create(EventInput event) {
    return create(event, null);
  }

  /** Igual que {@link #create(EventInput)}, con opciones por pedido. */
  public EventBatchResponse create(EventInput event, RequestOptions options) {
    if (event == null) throw new IllegalArgumentException("event no puede ser null");
    return requestor.request("POST", "/v1/events", event, EventBatchResponse.class, options);
  }

  /**
   * Manda un lote de hasta 100 eventos. Devuelve un resultado por evento, en el mismo orden. Si
   * ninguno entra, lanza {@code InvalidRequestException} (o {@code ApiException}) y el detalle de
   * cada evento queda en {@code results} del cuerpo crudo ({@code getRawBody()}).
   */
  public EventBatchResponse createBatch(List<EventInput> events) {
    return createBatch(events, null);
  }

  /** Igual que {@link #createBatch(List)}, con opciones por pedido. */
  public EventBatchResponse createBatch(List<EventInput> events, RequestOptions options) {
    if (events == null) throw new IllegalArgumentException("events no puede ser null");
    return requestor.request("POST", "/v1/events", events, EventBatchResponse.class, options);
  }

  /** Obtiene un evento ({@code evt_…}) con su estado y lo que produjo. */
  public Event retrieve(String id) {
    return retrieve(id, null);
  }

  /** Igual que {@link #retrieve(String)}, con opciones por pedido. */
  public Event retrieve(String id, RequestOptions options) {
    return requestor.request("GET", "/v1/events/" + id(id), null, Event.class, options);
  }

  /** Lista todo lo que entró a Kuida, del más nuevo al más viejo. */
  public KuidaList<Event> list() {
    return list(null, null);
  }

  /** Lista eventos; filtra por tipo, estado o fuente. */
  public KuidaList<Event> list(EventListParams params) {
    return list(params, null);
  }

  /** Igual que {@link #list(EventListParams)}, con opciones por pedido. */
  public KuidaList<Event> list(EventListParams params, RequestOptions options) {
    return requestor.requestList("/v1/events", params, Event.class, options);
  }
}
