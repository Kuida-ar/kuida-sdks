package ar.kuida.service;

import ar.kuida.ApiRequestor;
import ar.kuida.KuidaList;
import ar.kuida.RequestOptions;
import ar.kuida.model.Doctor;
import ar.kuida.model.DoctorListParams;

/** {@code /v1/doctors}: el padrón de profesionales (solo lectura). */
public final class DoctorService extends ApiService {
  /** Uso interno: el servicio se obtiene desde {@link ar.kuida.KuidaClient}. */
  public DoctorService(ApiRequestor requestor) {
    super(requestor);
  }

  /** Lista los profesionales de la organización. */
  public KuidaList<Doctor> list() {
    return list(null, null);
  }

  /** Lista profesionales; filtra por activo o id externo. */
  public KuidaList<Doctor> list(DoctorListParams params) {
    return list(params, null);
  }

  /** Igual que {@link #list(DoctorListParams)}, con opciones por pedido. */
  public KuidaList<Doctor> list(DoctorListParams params, RequestOptions options) {
    return requestor.requestList("/v1/doctors", params, Doctor.class, options);
  }

  /** Obtiene un profesional por su id ({@code doc_…}). */
  public Doctor retrieve(String id) {
    return retrieve(id, null);
  }

  /** Igual que {@link #retrieve(String)}, con opciones por pedido. */
  public Doctor retrieve(String id, RequestOptions options) {
    return requestor.request("GET", "/v1/doctors/" + id(id), null, Doctor.class, options);
  }
}
