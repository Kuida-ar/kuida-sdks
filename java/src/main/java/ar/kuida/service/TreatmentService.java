package ar.kuida.service;

import ar.kuida.ApiRequestor;
import ar.kuida.KuidaList;
import ar.kuida.RequestOptions;
import ar.kuida.model.Treatment;
import ar.kuida.model.TreatmentCreateParams;
import ar.kuida.model.TreatmentListParams;

/** {@code /v1/treatments}: tratamientos indicados. */
public final class TreatmentService extends ApiService {
  /** Uso interno: el servicio se obtiene desde {@link ar.kuida.KuidaClient}. */
  public TreatmentService(ApiRequestor requestor) {
    super(requestor);
  }

  /** Registra un tratamiento indicado al paciente. */
  public Treatment create(TreatmentCreateParams params) {
    return create(params, null);
  }

  /** Igual que {@link #create(TreatmentCreateParams)}, con opciones por pedido. */
  public Treatment create(TreatmentCreateParams params, RequestOptions options) {
    return requestor.request("POST", "/v1/treatments", params, Treatment.class, options);
  }

  /** Obtiene un tratamiento por su id ({@code trt_…}). */
  public Treatment retrieve(String id) {
    return retrieve(id, null);
  }

  /** Igual que {@link #retrieve(String)}, con opciones por pedido. */
  public Treatment retrieve(String id, RequestOptions options) {
    return requestor.request("GET", "/v1/treatments/" + id(id), null, Treatment.class, options);
  }

  /** Lista tratamientos, del más nuevo al más viejo. */
  public KuidaList<Treatment> list() {
    return list(null, null);
  }

  /** Lista tratamientos; filtra por paciente o activos. */
  public KuidaList<Treatment> list(TreatmentListParams params) {
    return list(params, null);
  }

  /** Igual que {@link #list(TreatmentListParams)}, con opciones por pedido. */
  public KuidaList<Treatment> list(TreatmentListParams params, RequestOptions options) {
    return requestor.requestList("/v1/treatments", params, Treatment.class, options);
  }
}
