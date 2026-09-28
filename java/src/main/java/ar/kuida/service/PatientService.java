package ar.kuida.service;

import ar.kuida.ApiRequestor;
import ar.kuida.KuidaList;
import ar.kuida.RequestOptions;
import ar.kuida.model.Patient;
import ar.kuida.model.PatientCreateParams;
import ar.kuida.model.PatientListParams;
import ar.kuida.model.PatientUpdateParams;

/** {@code /v1/patients}: pacientes de la organización. */
public final class PatientService extends ApiService {
  /** Uso interno: el servicio se obtiene desde {@link ar.kuida.KuidaClient}. */
  public PatientService(ApiRequestor requestor) {
    super(requestor);
  }

  /**
   * Crea o identifica un paciente, sin mandarle nada. Si ya existe uno con ese teléfono (o ese DNI),
   * devuelve el existente y completa los datos que le faltaban.
   */
  public Patient create(PatientCreateParams params) {
    return create(params, null);
  }

  /** Igual que {@link #create(PatientCreateParams)}, con opciones por pedido. */
  public Patient create(PatientCreateParams params, RequestOptions options) {
    return requestor.request("POST", "/v1/patients", params, Patient.class, options);
  }

  /** Obtiene un paciente por su id ({@code pat_…}). */
  public Patient retrieve(String id) {
    return retrieve(id, null);
  }

  /** Igual que {@link #retrieve(String)}, con opciones por pedido. */
  public Patient retrieve(String id, RequestOptions options) {
    return requestor.request("GET", "/v1/patients/" + id(id), null, Patient.class, options);
  }

  /** Actualiza solo los campos que se mandan. El teléfono no se puede cambiar. */
  public Patient update(String id, PatientUpdateParams params) {
    return update(id, params, null);
  }

  /** Igual que {@link #update(String, PatientUpdateParams)}, con opciones por pedido. */
  public Patient update(String id, PatientUpdateParams params, RequestOptions options) {
    return requestor.request("PATCH", "/v1/patients/" + id(id), params, Patient.class, options);
  }

  /** Lista pacientes, del más nuevo al más viejo. */
  public KuidaList<Patient> list() {
    return list(null, null);
  }

  /** Lista pacientes; filtra por teléfono, DNI o id externo. */
  public KuidaList<Patient> list(PatientListParams params) {
    return list(params, null);
  }

  /** Igual que {@link #list(PatientListParams)}, con opciones por pedido. */
  public KuidaList<Patient> list(PatientListParams params, RequestOptions options) {
    return requestor.requestList("/v1/patients", params, Patient.class, options);
  }
}
