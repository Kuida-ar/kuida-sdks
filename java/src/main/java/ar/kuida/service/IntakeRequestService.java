package ar.kuida.service;

import ar.kuida.ApiRequestor;
import ar.kuida.KuidaList;
import ar.kuida.RequestOptions;
import ar.kuida.model.IntakeRequest;
import ar.kuida.model.IntakeRequestCreateParams;
import ar.kuida.model.IntakeRequestListParams;

/** {@code /v1/intake_requests}: solicitudes de ingreso. */
public final class IntakeRequestService extends ApiService {
  /** Uso interno: el servicio se obtiene desde {@link ar.kuida.KuidaClient}. */
  public IntakeRequestService(ApiRequestor requestor) {
    super(requestor);
  }

  /**
   * Crea una solicitud de ingreso: alguien pide el servicio para un paciente y Kuida completa lo que
   * falta.
   */
  public IntakeRequest create(IntakeRequestCreateParams params) {
    return create(params, null);
  }

  /** Igual que {@link #create(IntakeRequestCreateParams)}, con opciones por pedido. */
  public IntakeRequest create(IntakeRequestCreateParams params, RequestOptions options) {
    return requestor.request("POST", "/v1/intake_requests", params, IntakeRequest.class, options);
  }

  /** Obtiene una solicitud con su estado y lo que todavía falta. */
  public IntakeRequest retrieve(String id) {
    return retrieve(id, null);
  }

  /** Igual que {@link #retrieve(String)}, con opciones por pedido. */
  public IntakeRequest retrieve(String id, RequestOptions options) {
    return requestor.request("GET", "/v1/intake_requests/" + id(id), null, IntakeRequest.class, options);
  }

  /** Lista solicitudes, de la más nueva a la más vieja. */
  public KuidaList<IntakeRequest> list() {
    return list(null, null);
  }

  /** Lista solicitudes; filtra por estado o paciente. */
  public KuidaList<IntakeRequest> list(IntakeRequestListParams params) {
    return list(params, null);
  }

  /** Igual que {@link #list(IntakeRequestListParams)}, con opciones por pedido. */
  public KuidaList<IntakeRequest> list(IntakeRequestListParams params, RequestOptions options) {
    return requestor.requestList("/v1/intake_requests", params, IntakeRequest.class, options);
  }
}
