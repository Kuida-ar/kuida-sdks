package ar.kuida.service;

import ar.kuida.ApiRequestor;
import ar.kuida.KuidaList;
import ar.kuida.RequestOptions;
import ar.kuida.model.Visit;
import ar.kuida.model.VisitCreateParams;
import ar.kuida.model.VisitListParams;

/** {@code /v1/visits}: consultas atendidas. */
public final class VisitService extends ApiService {
  /** Uso interno: el servicio se obtiene desde {@link ar.kuida.KuidaClient}. */
  public VisitService(ApiRequestor requestor) {
    super(requestor);
  }

  /**
   * Registra una consulta atendida y arranca el seguimiento post-consulta. Si {@code appointment}
   * apunta a un turno (por id o por su {@code externalId}), el turno queda {@code completed}.
   */
  public Visit create(VisitCreateParams params) {
    return create(params, null);
  }

  /** Igual que {@link #create(VisitCreateParams)}, con opciones por pedido. */
  public Visit create(VisitCreateParams params, RequestOptions options) {
    return requestor.request("POST", "/v1/visits", params, Visit.class, options);
  }

  /** Obtiene una consulta por su id ({@code vis_…}). */
  public Visit retrieve(String id) {
    return retrieve(id, null);
  }

  /** Igual que {@link #retrieve(String)}, con opciones por pedido. */
  public Visit retrieve(String id, RequestOptions options) {
    return requestor.request("GET", "/v1/visits/" + id(id), null, Visit.class, options);
  }

  /** Lista consultas, de la más nueva a la más vieja. */
  public KuidaList<Visit> list() {
    return list(null, null);
  }

  /** Lista consultas; filtra por paciente o id externo. */
  public KuidaList<Visit> list(VisitListParams params) {
    return list(params, null);
  }

  /** Igual que {@link #list(VisitListParams)}, con opciones por pedido. */
  public KuidaList<Visit> list(VisitListParams params, RequestOptions options) {
    return requestor.requestList("/v1/visits", params, Visit.class, options);
  }
}
