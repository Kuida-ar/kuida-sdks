package ar.kuida.service;

import ar.kuida.ApiRequestor;
import ar.kuida.KuidaList;
import ar.kuida.RequestOptions;
import ar.kuida.model.Appointment;
import ar.kuida.model.AppointmentCancelParams;
import ar.kuida.model.AppointmentCreateParams;
import ar.kuida.model.AppointmentListParams;
import ar.kuida.model.AppointmentUpdateParams;

/** {@code /v1/appointments}: turnos. */
public final class AppointmentService extends ApiService {
  /** Uso interno: el servicio se obtiene desde {@link ar.kuida.KuidaClient}. */
  public AppointmentService(ApiRequestor requestor) {
    super(requestor);
  }

  /**
   * Crea un turno y agenda sus recordatorios por WhatsApp. Si se manda {@code externalId} y ya
   * existe un turno con ese id, devuelve el existente.
   */
  public Appointment create(AppointmentCreateParams params) {
    return create(params, null);
  }

  /** Igual que {@link #create(AppointmentCreateParams)}, con opciones por pedido. */
  public Appointment create(AppointmentCreateParams params, RequestOptions options) {
    return requestor.request("POST", "/v1/appointments", params, Appointment.class, options);
  }

  /** Obtiene un turno por su id ({@code apt_…}). */
  public Appointment retrieve(String id) {
    return retrieve(id, null);
  }

  /** Igual que {@link #retrieve(String)}, con opciones por pedido. */
  public Appointment retrieve(String id, RequestOptions options) {
    return requestor.request("GET", "/v1/appointments/" + id(id), null, Appointment.class, options);
  }

  /**
   * Reprograma o modifica un turno (horario, profesional o tipo). Un turno cancelado o atendido no
   * se puede modificar: lanza {@code InvalidRequestException}.
   */
  public Appointment update(String id, AppointmentUpdateParams params) {
    return update(id, params, null);
  }

  /** Igual que {@link #update(String, AppointmentUpdateParams)}, con opciones por pedido. */
  public Appointment update(String id, AppointmentUpdateParams params, RequestOptions options) {
    return requestor.request("PATCH", "/v1/appointments/" + id(id), params, Appointment.class, options);
  }

  /** Cancela un turno y da de baja sus recordatorios. Cancelar uno ya cancelado no hace nada. */
  public Appointment cancel(String id) {
    return cancel(id, null, null);
  }

  /** Cancela un turno indicando el motivo. */
  public Appointment cancel(String id, AppointmentCancelParams params) {
    return cancel(id, params, null);
  }

  /** Igual que {@link #cancel(String, AppointmentCancelParams)}, con opciones por pedido. */
  public Appointment cancel(String id, AppointmentCancelParams params, RequestOptions options) {
    return requestor.request("POST", "/v1/appointments/" + id(id) + "/cancel", params, Appointment.class, options);
  }

  /** Lista turnos, del más nuevo al más viejo según cuándo se cargaron. */
  public KuidaList<Appointment> list() {
    return list(null, null);
  }

  /** Lista turnos; filtra por paciente, estado, id externo o rango de horario. */
  public KuidaList<Appointment> list(AppointmentListParams params) {
    return list(params, null);
  }

  /** Igual que {@link #list(AppointmentListParams)}, con opciones por pedido. */
  public KuidaList<Appointment> list(AppointmentListParams params, RequestOptions options) {
    return requestor.requestList("/v1/appointments", params, Appointment.class, options);
  }
}
