/**
 * Métodos de recurso, escritos a mano: uno por operación del OpenAPI
 * (`x-kuida-resource` + `x-kuida-method`), 32 en total. Cada método acepta al
 * final {@link RequestOptions} (`idempotencyKey`, `timeout`, `maxRetries`).
 */
import type { Kuida } from "./client.js";
import type {
  Account as AccountObject,
  Appointment,
  AppointmentCancelParams,
  AppointmentCreateParams,
  AppointmentListParams,
  AppointmentUpdateParams,
  DeletedObject,
  Doctor,
  DoctorListParams,
  Event,
  EventBatchResponse,
  EventInput,
  EventListParams,
  IntakeRequest,
  IntakeRequestCreateParams,
  IntakeRequestListParams,
  Patient,
  PatientCreateParams,
  PatientListParams,
  PatientUpdateParams,
  Treatment,
  TreatmentCreateParams,
  TreatmentListParams,
  Visit,
  VisitCreateParams,
  VisitListParams,
  WebhookDelivery,
  WebhookDeliveryListParams,
  WebhookEndpoint,
  WebhookEndpointCreateParams,
  WebhookEndpointListParams,
  WebhookEndpointUpdateParams,
} from "./generated/types.js";
import { type ApiList, ListPromise, type PaginationParams } from "./pagination.js";
import type { KuidaResponse, RequestOptions } from "./types.js";

/**
 * Evento con `type` libre, para tipos que el SDK todavía no conoce. Los tipos
 * del catálogo tienen su interfaz propia en {@link EventInput}.
 */
export interface GenericEventInput {
  /** Id del evento en el sistema de origen. Es la clave de idempotencia del evento. */
  id: string;
  type: string;
  data: Record<string, unknown>;
  occurredAt?: string | Date;
  source?: { system?: string; version?: string };
  schemaVersion?: 1;
}

type Response<T> = Promise<KuidaResponse<T>>;

const seg = (id: string) => {
  if (typeof id !== "string" || !id.trim()) throw new TypeError("Falta el id del objeto");
  return encodeURIComponent(id);
};

abstract class Resource {
  /** @internal */
  protected readonly client: Kuida;

  /** @internal */
  constructor(client: Kuida) {
    this.client = client;
  }

  protected get<T>(path: string, options?: RequestOptions, query?: object): Response<T> {
    return this.client.request<T>({ method: "GET", path, query, options });
  }

  protected post<T>(path: string, body: unknown, options?: RequestOptions): Response<T> {
    return this.client.request<T>({ method: "POST", path, body: body ?? {}, options });
  }

  protected patch<T>(path: string, body: unknown, options?: RequestOptions): Response<T> {
    return this.client.request<T>({ method: "PATCH", path, body: body ?? {}, options });
  }

  protected del<T>(path: string, options?: RequestOptions): Response<T> {
    return this.client.request<T>({ method: "DELETE", path, options });
  }

  protected paginate<T extends { id: string }, P extends PaginationParams>(
    path: string,
    params: P | undefined,
    options?: RequestOptions,
  ): ListPromise<T, P> {
    return new ListPromise<T, P>((p) => this.get<ApiList<T>>(path, options, p), { ...(params ?? {}) } as P);
  }
}

/** `GET /v1/account`. */
export class Account extends Resource {
  /** Devuelve la organización dueña de la clave y los scopes de la clave. */
  retrieve(options?: RequestOptions): Response<AccountObject> {
    return this.get("/v1/account", options);
  }
}

/** `/v1/patients`. */
export class Patients extends Resource {
  /**
   * Crea un paciente, o devuelve el existente con ese teléfono (o DNI) y
   * completa los datos que le faltaban. No le manda nada al paciente.
   */
  create(params: PatientCreateParams, options?: RequestOptions): Response<Patient> {
    return this.post("/v1/patients", params, options);
  }

  /** Trae un paciente por id (`pat_…`). */
  retrieve(id: string, options?: RequestOptions): Response<Patient> {
    return this.get(`/v1/patients/${seg(id)}`, options);
  }

  /** Actualiza los datos de un paciente. */
  update(id: string, params: PatientUpdateParams, options?: RequestOptions): Response<Patient> {
    return this.patch(`/v1/patients/${seg(id)}`, params, options);
  }

  /** Lista pacientes, del más nuevo al más viejo. Filtra por `phone`, `dni` o `externalId`. */
  list(params?: PatientListParams, options?: RequestOptions): ListPromise<Patient, PatientListParams> {
    return this.paginate("/v1/patients", params, options);
  }
}

/** `/v1/doctors` (solo lectura). */
export class Doctors extends Resource {
  /** Lista profesionales. Filtra por `active` y `externalId`. */
  list(params?: DoctorListParams, options?: RequestOptions): ListPromise<Doctor, DoctorListParams> {
    return this.paginate("/v1/doctors", params, options);
  }

  /** Trae un profesional por id (`doc_…`). */
  retrieve(id: string, options?: RequestOptions): Response<Doctor> {
    return this.get(`/v1/doctors/${seg(id)}`, options);
  }
}

/** `/v1/appointments`. */
export class Appointments extends Resource {
  /**
   * Crea un turno. `patient` y `doctor` aceptan un id o un objeto de identidad.
   * Si ya existe un turno con el mismo `externalId`, devuelve ese.
   */
  create(params: AppointmentCreateParams, options?: RequestOptions): Response<Appointment> {
    return this.post("/v1/appointments", params, options);
  }

  /** Trae un turno por id (`apt_…`). */
  retrieve(id: string, options?: RequestOptions): Response<Appointment> {
    return this.get(`/v1/appointments/${seg(id)}`, options);
  }

  /** Reprograma o cambia un turno. Un turno cancelado o atendido no se puede cambiar. */
  update(id: string, params: AppointmentUpdateParams, options?: RequestOptions): Response<Appointment> {
    return this.patch(`/v1/appointments/${seg(id)}`, params, options);
  }

  /** Cancela un turno. Sin parámetros manda `{}`. */
  cancel(id: string, params?: AppointmentCancelParams, options?: RequestOptions): Response<Appointment> {
    return this.post(`/v1/appointments/${seg(id)}/cancel`, params ?? {}, options);
  }

  /** Lista turnos. Filtra por `patient`, `status`, `externalId`, `startAtGte` y `startAtLt`. */
  list(params?: AppointmentListParams, options?: RequestOptions): ListPromise<Appointment, AppointmentListParams> {
    return this.paginate("/v1/appointments", params, options);
  }
}

/** `/v1/visits`. */
export class Visits extends Resource {
  /**
   * Registra una consulta atendida. Si `appointment` apunta a un turno (id o
   * `externalId`), el turno queda `completed`.
   */
  create(params: VisitCreateParams, options?: RequestOptions): Response<Visit> {
    return this.post("/v1/visits", params, options);
  }

  /** Trae una consulta por id (`vis_…`). */
  retrieve(id: string, options?: RequestOptions): Response<Visit> {
    return this.get(`/v1/visits/${seg(id)}`, options);
  }

  /** Lista consultas. Filtra por `patient` y `externalId`. */
  list(params?: VisitListParams, options?: RequestOptions): ListPromise<Visit, VisitListParams> {
    return this.paginate("/v1/visits", params, options);
  }
}

/** `/v1/intake_requests`. */
export class IntakeRequests extends Resource {
  /** Crea una solicitud de ingreso: Kuida contacta al paciente o a su referente para completar los datos. */
  create(params: IntakeRequestCreateParams, options?: RequestOptions): Response<IntakeRequest> {
    return this.post("/v1/intake_requests", params, options);
  }

  /** Trae una solicitud de ingreso por id (`int_…`). */
  retrieve(id: string, options?: RequestOptions): Response<IntakeRequest> {
    return this.get(`/v1/intake_requests/${seg(id)}`, options);
  }

  /** Lista solicitudes de ingreso. Filtra por `status` y `patient`. */
  list(params?: IntakeRequestListParams, options?: RequestOptions): ListPromise<IntakeRequest, IntakeRequestListParams> {
    return this.paginate("/v1/intake_requests", params, options);
  }
}

/** `/v1/treatments`. */
export class Treatments extends Resource {
  /** Registra un tratamiento o indicación para un paciente. */
  create(params: TreatmentCreateParams, options?: RequestOptions): Response<Treatment> {
    return this.post("/v1/treatments", params, options);
  }

  /** Trae un tratamiento por id (`trt_…`). */
  retrieve(id: string, options?: RequestOptions): Response<Treatment> {
    return this.get(`/v1/treatments/${seg(id)}`, options);
  }

  /** Lista tratamientos. Filtra por `patient` y `active`. */
  list(params?: TreatmentListParams, options?: RequestOptions): ListPromise<Treatment, TreatmentListParams> {
    return this.paginate("/v1/treatments", params, options);
  }
}

/** `/v1/events`: la puerta genérica con el sobre común. */
export class Events extends Resource {
  /** Manda un evento. El `id` del evento es su clave de idempotencia. */
  create(event: EventInput | GenericEventInput, options?: RequestOptions): Response<EventBatchResponse> {
    return this.post("/v1/events", event, options);
  }

  /** Manda un lote de hasta 100 eventos. Cada uno trae su resultado, en el mismo orden. */
  createBatch(events: ReadonlyArray<EventInput | GenericEventInput>, options?: RequestOptions): Response<EventBatchResponse> {
    return this.post("/v1/events", { events }, options);
  }

  /** Trae un evento por id (`evt_…`). */
  retrieve(id: string, options?: RequestOptions): Response<Event> {
    return this.get(`/v1/events/${seg(id)}`, options);
  }

  /** Lista eventos recibidos. Filtra por `type`, `status` y `source`. */
  list(params?: EventListParams, options?: RequestOptions): ListPromise<Event, EventListParams> {
    return this.paginate("/v1/events", params, options);
  }
}

/** `/v1/webhook_endpoints`. */
export class WebhookEndpoints extends Resource {
  /** Crea un endpoint. La respuesta trae `secret` (`whsec_…`) por única vez: hay que guardarlo. */
  create(params: WebhookEndpointCreateParams, options?: RequestOptions): Response<WebhookEndpoint> {
    return this.post("/v1/webhook_endpoints", params, options);
  }

  /** Trae un endpoint por id (`we_…`). No incluye `secret`. */
  retrieve(id: string, options?: RequestOptions): Response<WebhookEndpoint> {
    return this.get(`/v1/webhook_endpoints/${seg(id)}`, options);
  }

  /** Cambia la URL, los eventos o el estado (`disabled`) de un endpoint. */
  update(id: string, params: WebhookEndpointUpdateParams, options?: RequestOptions): Response<WebhookEndpoint> {
    return this.patch(`/v1/webhook_endpoints/${seg(id)}`, params, options);
  }

  /** Borra un endpoint y sus entregas pendientes. */
  delete(id: string, options?: RequestOptions): Response<DeletedObject> {
    return this.del(`/v1/webhook_endpoints/${seg(id)}`, options);
  }

  /** Lista endpoints. */
  list(params?: WebhookEndpointListParams, options?: RequestOptions): ListPromise<WebhookEndpoint, WebhookEndpointListParams> {
    return this.paginate("/v1/webhook_endpoints", params, options);
  }

  /** Encola un evento `webhook.ping` firmado hacia el endpoint. Devuelve la entrega pendiente. */
  ping(id: string, options?: RequestOptions): Response<WebhookDelivery> {
    return this.post(`/v1/webhook_endpoints/${seg(id)}/ping`, {}, options);
  }
}

/** `/v1/webhook_deliveries` (solo lectura). */
export class WebhookDeliveries extends Resource {
  /** Lista entregas. Filtra por `webhookEndpoint` y `status`. */
  list(params?: WebhookDeliveryListParams, options?: RequestOptions): ListPromise<WebhookDelivery, WebhookDeliveryListParams> {
    return this.paginate("/v1/webhook_deliveries", params, options);
  }

  /** Trae una entrega por id (`whd_…`). */
  retrieve(id: string, options?: RequestOptions): Response<WebhookDelivery> {
    return this.get(`/v1/webhook_deliveries/${seg(id)}`, options);
  }
}
