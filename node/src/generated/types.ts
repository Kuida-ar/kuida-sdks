// Generado por scripts/generate.mjs desde openapi/kuida-v1.json (API 2026-09-28).
// No editar a mano: regenerar con `npm run generate`.
/* eslint-disable */

/** Versión de la API contra la que se generaron estos tipos. */
export const API_VERSION = "2026-09-28";

/** La organización dueña de la clave de API. */
export interface Account {
  /**
   * Id de la organización.
   * Patrón: `^org_.*`.
   */
  id: string;
  object: "account";
  name: string;
  /** Tipo de institución (clinic, pharmacy, lab, …). */
  type: string;
  timeZone: string;
  /** Líneas de servicio contratadas: seguimiento de pacientes y red de derivaciones. */
  serviceLines: Array<"follow_up" | "network">;
  /** `true` en producción, `false` en el entorno de pruebas. */
  livemode: boolean;
  apiKey: {
    name: string;
    prefix: string;
    scopes: string[];
  };
}

/** Un turno. Kuida le manda al paciente los recordatorios que la organización configuró. */
export interface Appointment {
  /**
   * Id del turno.
   * Patrón: `^apt_.*`.
   */
  id: string;
  object: "appointment";
  /**
   * Paciente del turno.
   * Patrón: `^pat_.*`.
   */
  patient: string;
  doctor: string | null;
  /** Fecha y hora ISO 8601. */
  startAt: string;
  status: "confirmed" | "pending" | "rescheduled" | "cancelled" | "completed";
  /** Tipo de turno o práctica. */
  type: string | null;
  /** Id del turno en su sistema. */
  externalId: string | null;
  /** Por dónde entró: su API, el conector del sistema de gestión, el agente de Kuida o el equipo. */
  source: "api" | "pms" | "agent" | "manual";
  cancelledAt: string | null;
  /** Fecha y hora ISO 8601. */
  createdAt: string;
  /** Fecha y hora ISO 8601. */
  updatedAt: string;
}

export interface AppointmentCancelParams {
  /** Motivo, para el registro. */
  reason?: string;
}

export interface AppointmentCreateParams {
  patient: PatientReference;
  doctor?: DoctorReference;
  /** Inicio del turno. */
  startAt: string | Date;
  /** Tipo de turno o práctica. */
  type?: string;
  /** Id del turno en su sistema. Si ya existe un turno con ese id, se devuelve ese y no se crea otro. */
  externalId?: string;
}

/** Página de resultados. Siga con `startingAfter` = id del último. */
export interface AppointmentList {
  object: "list";
  data: Appointment[];
  /** Si hay más objetos después de esta página. */
  hasMore: boolean;
  url: string;
}

export interface AppointmentUpdateParams {
  /** Nuevo horario. Reprograma los recordatorios. */
  startAt?: string | Date;
  doctor?: DoctorReference;
  type?: string;
}

/** Confirmación de un borrado. */
export interface DeletedObject {
  id: string;
  object: string;
  deleted: true;
}

/** Un profesional del padrón de la organización. */
export interface Doctor {
  /**
   * Id del profesional.
   * Patrón: `^doc_.*`.
   */
  id: string;
  object: "doctor";
  fullName: string;
  specialty: string | null;
  /** Matrícula. */
  licenseNumber: string | null;
  externalId: string | null;
  /** Si atiende hoy. Solo los activos reciben turnos del agente. */
  active: boolean;
  /** Fecha y hora ISO 8601. */
  createdAt: string;
}

/** Cómo conoce su sistema al profesional. */
export interface DoctorIdentity {
  /** Id del profesional en su sistema. */
  externalId?: string;
  /** Nombre del profesional. */
  fullName?: string;
  /** Especialidad. */
  specialty?: string;
}

/** Página de resultados. Siga con `startingAfter` = id del último. */
export interface DoctorList {
  object: "list";
  data: Doctor[];
  /** Si hay más objetos después de esta página. */
  hasMore: boolean;
  url: string;
}

/** Un profesional: su id de Kuida (`doc_…`) o sus datos. */
export type DoctorReference = string | DoctorIdentity;

/** Cuerpo de toda respuesta de error. */
export interface Error {
  error: {
    /** Categoría del error. Los SDKs la mapean a una clase de excepción. */
    type: "invalid_request_error" | "authentication_error" | "permission_error" | "idempotency_error" | "rate_limit_error" | "api_error";
    /** Qué pasó, en forma estable para programar contra ella. */
    code: "parameter_missing" | "parameter_invalid" | "body_invalid_json" | "body_too_large" | "batch_too_large" | "resource_missing" | "resource_conflict" | "api_key_missing" | "api_key_invalid" | "api_key_revoked" | "api_key_expired" | "scope_missing" | "service_not_contracted" | "api_version_invalid" | "idempotency_key_in_use" | "idempotency_key_reused" | "rate_limited" | "processing_failed" | "internal_error";
    /** Explicación para una persona. Puede cambiar de redacción. */
    message: string;
    /** El parámetro que causó el error, si aplica (`patient.phone`). */
    param: string | null;
    /** Id del pedido. Envíelo si escribe a soporte. */
    requestId: string;
    /** Link a la explicación del código de error. */
    docUrl: string;
  };
}

/** Un evento que entró a Kuida, con su resultado. */
export interface Event {
  /**
   * Id del evento en Kuida.
   * Patrón: `^evt_.*`.
   */
  id: string;
  object: "event";
  type: "intake.requested" | "patient.upserted" | "appointment.created" | "appointment.rescheduled" | "appointment.cancelled" | "visit.completed" | "treatment.prescribed" | "order.issued";
  /** Por dónde entró. */
  source: "api" | "pms" | "email" | "tool" | "manual" | "import";
  /** Identidad del evento en su fuente. Para la API, el `id` que envió. */
  sourceRef: string;
  /** `processed` Kuida hizo lo suyo · `unhandled` guardado, sin efecto todavía · `failed` falló, reintente con el mismo id · `duplicate` · `received` en curso. */
  status: "received" | "processed" | "failed" | "duplicate" | "unhandled";
  error: string | null;
  /** Lo que produjo el evento (`{ object: "visit", id: "vis_…" }`). */
  result: {
    object: string;
    id: string;
  } | null;
  data: Record<string, unknown>;
  /** Fecha y hora ISO 8601. */
  occurredAt: string;
  /** Fecha y hora ISO 8601. */
  receivedAt: string;
  processedAt: string | null;
}

/** Un evento, un array de hasta 100 o `{ events: [...] }`. */
export type EventBatchInput = EventInput | EventInput[] | {
  events: EventInput[];
};

/** Un resultado por evento, en el mismo orden. */
export interface EventBatchResponse {
  object: "event_batch";
  results: EventResult[];
}

/** Evento `intake.requested` para `events.create` / `events.createBatch`. */
export interface IntakeRequestedEventInput {
  /** Su id del evento. Es la clave de idempotencia: reenviar el mismo id no repite nada. */
  id: string;
  /**
   * Versión del esquema del evento. Siempre 1 en v1.
   * Default: `1`.
   */
  schemaVersion?: 1;
  /** Cuándo pasó. Default: ahora. */
  occurredAt?: string | Date;
  /** Qué sistema lo emite, para el registro. */
  source?: {
    system?: string;
    version?: string;
  };
  type: "intake.requested";
  data: {
    patient?: {
      phone?: string | null;
      dni?: string | null;
      externalId?: string | null;
      fullName?: string | null;
      email?: string | null;
      dateOfBirth?: string | Date | null;
    };
    /** Default: `[]`. */
    contacts?: Array<{
      name?: string | null;
      relationship?: string | null;
      phone?: string | null;
      email?: string | null;
    }>;
    coverage?: {
      insurer?: string | null;
      plan?: string | null;
      memberId?: string | null;
    };
    address?: string | null;
    requestedService?: string | null;
    sender?: {
      name?: string | null;
      email?: string | null;
      organization?: string | null;
    };
    /** Default: `[]`. */
    attachments?: Array<{
      name: string;
      mimeType?: string | null;
      kind?: "PEDIDO_MEDICO" | "HISTORIA_CLINICA" | "COBERTURA" | "OTRO";
      ref?: string | null;
    }>;
    summary?: string | null;
    flags?: {
      hospitalized?: boolean;
    };
    origin?: {
      channel?: "EMAIL" | "FORM" | "MANUAL" | "API";
      threadId?: string | null;
      messageId?: string | null;
      from?: string | null;
      subject?: string | null;
      receivedAt?: string | Date | null;
    };
  };
}

/** Evento `patient.upserted` para `events.create` / `events.createBatch`. */
export interface PatientUpsertedEventInput {
  /** Su id del evento. Es la clave de idempotencia: reenviar el mismo id no repite nada. */
  id: string;
  /**
   * Versión del esquema del evento. Siempre 1 en v1.
   * Default: `1`.
   */
  schemaVersion?: 1;
  /** Cuándo pasó. Default: ahora. */
  occurredAt?: string | Date;
  /** Qué sistema lo emite, para el registro. */
  source?: {
    system?: string;
    version?: string;
  };
  type: "patient.upserted";
  data: {
    phone?: string | null;
    dni?: string | null;
    externalId?: string | null;
    fullName?: string | null;
    email?: string | null;
    dateOfBirth?: string | Date | null;
  };
}

/** Evento `appointment.created` para `events.create` / `events.createBatch`. */
export interface AppointmentCreatedEventInput {
  /** Su id del evento. Es la clave de idempotencia: reenviar el mismo id no repite nada. */
  id: string;
  /**
   * Versión del esquema del evento. Siempre 1 en v1.
   * Default: `1`.
   */
  schemaVersion?: 1;
  /** Cuándo pasó. Default: ahora. */
  occurredAt?: string | Date;
  /** Qué sistema lo emite, para el registro. */
  source?: {
    system?: string;
    version?: string;
  };
  type: "appointment.created";
  data: {
    externalId?: string | null;
    bookingExternalId?: string | null;
    startAt: string | Date;
    visitType?: string | null;
    patient: {
      phone?: string | null;
      dni?: string | null;
      externalId?: string | null;
      fullName?: string | null;
      email?: string | null;
      dateOfBirth?: string | Date | null;
    };
    doctor?: {
      fullName?: string | null;
      specialty?: string | null;
      externalId?: string | null;
    };
  };
}

/** Evento `appointment.rescheduled` para `events.create` / `events.createBatch`. */
export interface AppointmentRescheduledEventInput {
  /** Su id del evento. Es la clave de idempotencia: reenviar el mismo id no repite nada. */
  id: string;
  /**
   * Versión del esquema del evento. Siempre 1 en v1.
   * Default: `1`.
   */
  schemaVersion?: 1;
  /** Cuándo pasó. Default: ahora. */
  occurredAt?: string | Date;
  /** Qué sistema lo emite, para el registro. */
  source?: {
    system?: string;
    version?: string;
  };
  type: "appointment.rescheduled";
  data: {
    externalId: string;
    bookingExternalId?: string | null;
    startAt: string | Date;
    visitType?: string | null;
    patient: {
      phone?: string | null;
      dni?: string | null;
      externalId?: string | null;
      fullName?: string | null;
      email?: string | null;
      dateOfBirth?: string | Date | null;
    };
    doctor?: {
      fullName?: string | null;
      specialty?: string | null;
      externalId?: string | null;
    };
    reason?: string | null;
  };
}

/** Evento `appointment.cancelled` para `events.create` / `events.createBatch`. */
export interface AppointmentCancelledEventInput {
  /** Su id del evento. Es la clave de idempotencia: reenviar el mismo id no repite nada. */
  id: string;
  /**
   * Versión del esquema del evento. Siempre 1 en v1.
   * Default: `1`.
   */
  schemaVersion?: 1;
  /** Cuándo pasó. Default: ahora. */
  occurredAt?: string | Date;
  /** Qué sistema lo emite, para el registro. */
  source?: {
    system?: string;
    version?: string;
  };
  type: "appointment.cancelled";
  data: {
    externalId: string;
    bookingExternalId?: string | null;
    startAt: string | Date;
    visitType?: string | null;
    patient: {
      phone?: string | null;
      dni?: string | null;
      externalId?: string | null;
      fullName?: string | null;
      email?: string | null;
      dateOfBirth?: string | Date | null;
    };
    doctor?: {
      fullName?: string | null;
      specialty?: string | null;
      externalId?: string | null;
    };
    reason?: string | null;
  };
}

/** Evento `visit.completed` para `events.create` / `events.createBatch`. */
export interface VisitCompletedEventInput {
  /** Su id del evento. Es la clave de idempotencia: reenviar el mismo id no repite nada. */
  id: string;
  /**
   * Versión del esquema del evento. Siempre 1 en v1.
   * Default: `1`.
   */
  schemaVersion?: 1;
  /** Cuándo pasó. Default: ahora. */
  occurredAt?: string | Date;
  /** Qué sistema lo emite, para el registro. */
  source?: {
    system?: string;
    version?: string;
  };
  type: "visit.completed";
  data: {
    patient: {
      phone?: string | null;
      dni?: string | null;
      externalId?: string | null;
      fullName?: string | null;
      email?: string | null;
      dateOfBirth?: string | Date | null;
    };
    doctor?: {
      fullName?: string | null;
      specialty?: string | null;
      externalId?: string | null;
    };
    visit: {
      visitedAt: string | Date;
      type?: string | null;
      diagnosis?: string | null;
      notes?: string | null;
      recommendedFollowUps?: unknown[] | null;
      externalId?: string | null;
      bookingExternalId?: string | null;
    };
    raw?: unknown;
  };
}

/** Evento `treatment.prescribed` para `events.create` / `events.createBatch`. */
export interface TreatmentPrescribedEventInput {
  /** Su id del evento. Es la clave de idempotencia: reenviar el mismo id no repite nada. */
  id: string;
  /**
   * Versión del esquema del evento. Siempre 1 en v1.
   * Default: `1`.
   */
  schemaVersion?: 1;
  /** Cuándo pasó. Default: ahora. */
  occurredAt?: string | Date;
  /** Qué sistema lo emite, para el registro. */
  source?: {
    system?: string;
    version?: string;
  };
  type: "treatment.prescribed";
  data: {
    patient: {
      phone?: string | null;
      dni?: string | null;
      externalId?: string | null;
      fullName?: string | null;
      email?: string | null;
      dateOfBirth?: string | Date | null;
    };
    name: string;
    kind?: "MEDICATION" | "THERAPY" | "DIET" | "STUDY" | "LABWORK" | "LIFESTYLE";
    dosage?: string | null;
    instructions?: string | null;
    frequency?: string | null;
    startAt?: string | Date | null;
    endAt?: string | Date | null;
    prescribedBy?: {
      fullName?: string | null;
      specialty?: string | null;
      externalId?: string | null;
    };
  };
}

/** Evento `order.issued` para `events.create` / `events.createBatch`. */
export interface OrderIssuedEventInput {
  /** Su id del evento. Es la clave de idempotencia: reenviar el mismo id no repite nada. */
  id: string;
  /**
   * Versión del esquema del evento. Siempre 1 en v1.
   * Default: `1`.
   */
  schemaVersion?: 1;
  /** Cuándo pasó. Default: ahora. */
  occurredAt?: string | Date;
  /** Qué sistema lo emite, para el registro. */
  source?: {
    system?: string;
    version?: string;
  };
  type: "order.issued";
  data: {
    patient: {
      phone?: string | null;
      dni?: string | null;
      externalId?: string | null;
      fullName?: string | null;
      email?: string | null;
      dateOfBirth?: string | Date | null;
    };
    kind: "LAB" | "IMAGING" | "CONSULT" | "OTHER";
    description: string;
    issuedAt?: string | Date | null;
    issuedBy?: {
      fullName?: string | null;
      specialty?: string | null;
      externalId?: string | null;
    };
  };
}

/** Un evento del catálogo v1. El `type` define la forma de `data`. */
export type EventInput =
  | IntakeRequestedEventInput
  | PatientUpsertedEventInput
  | AppointmentCreatedEventInput
  | AppointmentRescheduledEventInput
  | AppointmentCancelledEventInput
  | VisitCompletedEventInput
  | TreatmentPrescribedEventInput
  | OrderIssuedEventInput;

/** Tipos de evento que acepta `POST /v1/events`. */
export type EventInputType = EventInput["type"];

/** Mapa tipo de evento → forma de `data`. */
export interface EventInputDataMap {
  "intake.requested": IntakeRequestedEventInput["data"];
  "patient.upserted": PatientUpsertedEventInput["data"];
  "appointment.created": AppointmentCreatedEventInput["data"];
  "appointment.rescheduled": AppointmentRescheduledEventInput["data"];
  "appointment.cancelled": AppointmentCancelledEventInput["data"];
  "visit.completed": VisitCompletedEventInput["data"];
  "treatment.prescribed": TreatmentPrescribedEventInput["data"];
  "order.issued": OrderIssuedEventInput["data"];
}

/** Página de resultados. Siga con `startingAfter` = id del último. */
export interface EventList {
  object: "list";
  data: Event[];
  /** Si hay más objetos después de esta página. */
  hasMore: boolean;
  url: string;
}

export interface EventResult {
  /** El id que envió. */
  id: string;
  accepted: boolean;
  status: "processed" | "duplicate" | "unhandled" | "invalid" | "failed";
  /** Id del evento en Kuida (`evt_…`). */
  event: string | null;
  result: {
    object: string;
    id: string;
  } | null;
  error: string | null;
}

/** Un referente del paciente (familiar, cuidador). */
export interface IntakeContact {
  name?: string;
  relationship?: string;
  phone?: string;
  /** Formato: email. */
  email?: string;
}

export interface IntakeCoverage {
  /** Obra social o prepaga. */
  insurer?: string;
  plan?: string;
  /** Número de afiliado. */
  memberId?: string;
}

/** Una solicitud de ingreso: alguien pide el servicio para un paciente y Kuida completa lo que falta. */
export interface IntakeRequest {
  /**
   * Id de la solicitud.
   * Patrón: `^int_.*`.
   */
  id: string;
  object: "intake_request";
  /** `new` recién llegada · `validating` Kuida está completando datos por WhatsApp · `ready` completa · `loaded` cargada en el sistema de la organización · `discarded` descartada. */
  status: "new" | "validating" | "ready" | "loaded" | "discarded";
  patient: string | null;
  summary: string | null;
  /** Datos que todavía faltan para que la solicitud quede lista. */
  missing: string[];
  /** Los datos de la solicitud tal como los tiene Kuida. */
  data: Record<string, unknown>;
  discardReason: string | null;
  readyAt: string | null;
  loadedAt: string | null;
  closedAt: string | null;
  /** Fecha y hora ISO 8601. */
  createdAt: string;
  /** Fecha y hora ISO 8601. */
  updatedAt: string;
}

/** Hace falta al menos un dato del paciente o de un referente (nombre, documento o teléfono). */
export interface IntakeRequestCreateParams {
  patient?: PatientIdentity;
  contacts?: IntakeContact[];
  coverage?: IntakeCoverage;
  address?: string;
  requestedService?: string;
  /** Resumen administrativo del pedido, sin contenido clínico. */
  summary?: string;
  /** Si el paciente está internado hoy. */
  hospitalized?: boolean;
  /** Quién pide el servicio. */
  sender?: {
    name?: string;
    /** Formato: email. */
    email?: string;
    organization?: string;
  };
}

/** Página de resultados. Siga con `startingAfter` = id del último. */
export interface IntakeRequestList {
  object: "list";
  data: IntakeRequest[];
  /** Si hay más objetos después de esta página. */
  hasMore: boolean;
  url: string;
}

/** Un paciente de la organización. */
export interface Patient {
  /**
   * Id del paciente.
   * Patrón: `^pat_.*`.
   */
  id: string;
  object: "patient";
  fullName: string | null;
  /** Teléfono normalizado. */
  phone: string | null;
  email: string | null;
  dni: string | null;
  /** Id del paciente en su sistema. */
  externalId: string | null;
  /** Fecha de nacimiento (AAAA-MM-DD). */
  dateOfBirth: string | null;
  /** Estado básico del paciente en Kuida. */
  stage: "pending_follow_up" | "in_conversation" | "scheduled" | "periodic_follow_up" | "discharged" | "lost";
  /** Si el paciente pidió no recibir mensajes. Kuida no le escribe. */
  optedOut: boolean;
  /** Fecha y hora ISO 8601. */
  createdAt: string;
  /** Fecha y hora ISO 8601. */
  updatedAt: string;
}

export interface PatientCreateParams {
  /** Celular con WhatsApp, en cualquier formato; Kuida lo normaliza. */
  phone: string;
  /** Nombre y apellido. */
  fullName?: string;
  /** Documento, sin puntos. */
  dni?: string;
  /**
   * Correo electrónico.
   * Formato: email.
   */
  email?: string;
  /** Id del paciente en su sistema. */
  externalId?: string;
  /**
   * Fecha ISO 8601 (AAAA-MM-DD).
   * Formato: date.
   */
  dateOfBirth?: string;
}

/** Cómo conoce su sistema al paciente. Kuida lo busca por teléfono y después por DNI; si no existe, lo crea. */
export interface PatientIdentity {
  /** Celular con WhatsApp, en cualquier formato; Kuida lo normaliza. Es la identidad principal del paciente. */
  phone?: string;
  /** Nombre y apellido. */
  fullName?: string;
  /** Documento, sin puntos. */
  dni?: string;
  /**
   * Correo electrónico.
   * Formato: email.
   */
  email?: string;
  /** Id del paciente en su sistema. */
  externalId?: string;
  /**
   * Fecha ISO 8601 (AAAA-MM-DD).
   * Formato: date.
   */
  dateOfBirth?: string;
}

/** Página de resultados. Siga con `startingAfter` = id del último. */
export interface PatientList {
  object: "list";
  data: Patient[];
  /** Si hay más objetos después de esta página. */
  hasMore: boolean;
  url: string;
}

/** Un paciente: su id de Kuida (`pat_…`) o los datos con que lo conoce su sistema. */
export type PatientReference = string | PatientIdentity;

/** Solo cambia lo que envía. El teléfono no se cambia: es la identidad. */
export interface PatientUpdateParams {
  fullName?: string;
  /** Formato: email. */
  email?: string;
  dni?: string;
  externalId?: string;
  /**
   * Fecha ISO 8601 (AAAA-MM-DD).
   * Formato: date.
   */
  dateOfBirth?: string;
}

/** Un tratamiento indicado al paciente. */
export interface Treatment {
  /**
   * Id del tratamiento.
   * Patrón: `^trt_.*`.
   */
  id: string;
  object: "treatment";
  /**
   * Paciente.
   * Patrón: `^pat_.*`.
   */
  patient: string;
  kind: "medication" | "therapy" | "diet" | "study" | "labwork" | "lifestyle";
  name: string;
  dosage: string | null;
  frequency: string | null;
  instructions: string | null;
  startDate: string | null;
  endDate: string | null;
  active: boolean;
  /** Fecha y hora ISO 8601. */
  createdAt: string;
  /** Fecha y hora ISO 8601. */
  updatedAt: string;
}

export interface TreatmentCreateParams {
  patient: PatientReference;
  name: string;
  /** Default `medication`. */
  kind?: "medication" | "therapy" | "diet" | "study" | "labwork" | "lifestyle";
  dosage?: string;
  frequency?: string;
  instructions?: string;
  /** Fecha y hora ISO 8601. */
  startDate?: string | Date;
  /** Fecha y hora ISO 8601. */
  endDate?: string | Date;
}

/** Página de resultados. Siga con `startingAfter` = id del último. */
export interface TreatmentList {
  object: "list";
  data: Treatment[];
  /** Si hay más objetos después de esta página. */
  hasMore: boolean;
  url: string;
}

/** Una consulta atendida. Crearla arranca el seguimiento post-consulta del paciente. */
export interface Visit {
  /**
   * Id de la consulta.
   * Patrón: `^vis_.*`.
   */
  id: string;
  object: "visit";
  /**
   * Paciente atendido.
   * Patrón: `^pat_.*`.
   */
  patient: string;
  doctor: string | null;
  appointment: string | null;
  /** Fecha y hora ISO 8601. */
  visitedAt: string;
  type: string | null;
  diagnosis: string | null;
  notes: string | null;
  externalId: string | null;
  source: "pms" | "manual";
  /** Fecha y hora ISO 8601. */
  createdAt: string;
}

export interface VisitCreateParams {
  patient: PatientReference;
  doctor?: DoctorReference;
  /** Cuándo se atendió. */
  visitedAt: string | Date;
  type?: string;
  /** Id de la consulta en su sistema. Si ya existe, se devuelve esa y no se crea otra. */
  externalId?: string;
  /** Turno del que sale la consulta: id de Kuida (`apt_…`) o el `externalId` del turno. Lo marca como atendido. */
  appointment?: string;
  diagnosis?: string;
  notes?: string;
}

/** Página de resultados. Siga con `startingAfter` = id del último. */
export interface VisitList {
  object: "list";
  data: Visit[];
  /** Si hay más objetos después de esta página. */
  hasMore: boolean;
  url: string;
}

/** Un envío de webhook con sus reintentos. */
export interface WebhookDelivery {
  /**
   * Id de la entrega.
   * Patrón: `^whd_.*`.
   */
  id: string;
  object: "webhook_delivery";
  /**
   * Endpoint destino.
   * Patrón: `^we_.*`.
   */
  webhookEndpoint: string;
  eventType: string;
  /** `pending` en curso o esperando reintento · `succeeded` su endpoint respondió 2xx · `failed` se agotaron los reintentos. */
  status: "pending" | "succeeded" | "failed";
  attempts: number;
  lastStatusCode: number | null;
  lastError: string | null;
  nextAttemptAt: string | null;
  deliveredAt: string | null;
  /** El cuerpo exacto que se envió. */
  payload: Record<string, unknown>;
  /** Fecha y hora ISO 8601. */
  createdAt: string;
}

/** Página de resultados. Siga con `startingAfter` = id del último. */
export interface WebhookDeliveryList {
  object: "list";
  data: WebhookDelivery[];
  /** Si hay más objetos después de esta página. */
  hasMore: boolean;
  url: string;
}

/** Una URL de su sistema a la que Kuida le avisa lo que pasa. */
export interface WebhookEndpoint {
  /**
   * Id del endpoint.
   * Patrón: `^we_.*`.
   */
  id: string;
  object: "webhook_endpoint";
  url: string;
  description: string | null;
  /** Tipos de evento que recibe. `*` = todos. */
  enabledEvents: string[];
  status: "enabled" | "disabled";
  /** Secreto para verificar la firma (`whsec_…`). Solo viene en la respuesta de creación. */
  secret?: string;
  lastDeliveredAt: string | null;
  lastError: {
    /** Fecha y hora ISO 8601. */
    at: string;
    message: string;
  } | null;
  /** Fecha y hora ISO 8601. */
  createdAt: string;
}

export interface WebhookEndpointCreateParams {
  /**
   * URL https de su sistema.
   * Formato: uri.
   */
  url: string;
  /** Qué eventos quiere recibir. `*` = todos. */
  enabledEvents: Array<"intake.requested" | "patient.upserted" | "appointment.created" | "appointment.rescheduled" | "appointment.cancelled" | "visit.completed" | "treatment.prescribed" | "order.issued" | "intake.created" | "intake.ready" | "conversation.handoff" | "patient.silent" | "webhook.ping" | "*">;
  description?: string;
}

/** Página de resultados. Siga con `startingAfter` = id del último. */
export interface WebhookEndpointList {
  object: "list";
  data: WebhookEndpoint[];
  /** Si hay más objetos después de esta página. */
  hasMore: boolean;
  url: string;
}

export interface WebhookEndpointUpdateParams {
  /** Formato: uri. */
  url?: string;
  enabledEvents?: Array<"intake.requested" | "patient.upserted" | "appointment.created" | "appointment.rescheduled" | "appointment.cancelled" | "visit.completed" | "treatment.prescribed" | "order.issued" | "intake.created" | "intake.ready" | "conversation.handoff" | "patient.silent" | "webhook.ping" | "*">;
  description?: string;
  /** `true` pausa los envíos sin borrar el endpoint. */
  disabled?: boolean;
}

/** Lo que Kuida le manda a su endpoint. */
export interface WebhookEvent {
  /** Id de la entrega (`whd_…`). Estable entre reintentos: deduplique por este campo. */
  id: string;
  object: "event";
  type: "intake.requested" | "patient.upserted" | "appointment.created" | "appointment.rescheduled" | "appointment.cancelled" | "visit.completed" | "treatment.prescribed" | "order.issued" | "intake.created" | "intake.ready" | "conversation.handoff" | "patient.silent" | "webhook.ping";
  apiVersion: string;
  livemode: boolean;
  /** Cuándo pasó, ISO 8601. */
  occurredAt: string;
  /** Id interno de lo que disparó el evento. */
  ref: string;
  /** Los datos del hecho. */
  data: Record<string, unknown>;
}

/** Parámetros de `patients.list` (GET /v1/patients). */
export interface PatientListParams {
  /**
   * Cantidad de objetos a devolver, entre 1 y 100. Default 10.
   * Default: `10`.
   */
  limit?: number;
  /** Cursor: id del último objeto de la página anterior. Devuelve los siguientes. */
  startingAfter?: string;
  /** Cursor: id del primer objeto de la página actual. Devuelve los anteriores. */
  endingBefore?: string;
  /** Filtre por teléfono (cualquier formato). */
  phone?: string;
  dni?: string;
  externalId?: string;
}

/** Parámetros de `doctors.list` (GET /v1/doctors). */
export interface DoctorListParams {
  /**
   * Cantidad de objetos a devolver, entre 1 y 100. Default 10.
   * Default: `10`.
   */
  limit?: number;
  /** Cursor: id del último objeto de la página anterior. Devuelve los siguientes. */
  startingAfter?: string;
  /** Cursor: id del primer objeto de la página actual. Devuelve los anteriores. */
  endingBefore?: string;
  /** Filtre por activos o inactivos. */
  active?: boolean | "true" | "false";
  externalId?: string;
}

/** Parámetros de `appointments.list` (GET /v1/appointments). */
export interface AppointmentListParams {
  /**
   * Cantidad de objetos a devolver, entre 1 y 100. Default 10.
   * Default: `10`.
   */
  limit?: number;
  /** Cursor: id del último objeto de la página anterior. Devuelve los siguientes. */
  startingAfter?: string;
  /** Cursor: id del primer objeto de la página actual. Devuelve los anteriores. */
  endingBefore?: string;
  /** Id del paciente (`pat_…`). */
  patient?: string;
  status?: "confirmed" | "pending" | "rescheduled" | "cancelled" | "completed";
  externalId?: string;
  /** Turnos desde esta fecha y hora. */
  startAtGte?: string | Date;
  /** Turnos antes de esta fecha y hora. */
  startAtLt?: string | Date;
}

/** Parámetros de `visits.list` (GET /v1/visits). */
export interface VisitListParams {
  /**
   * Cantidad de objetos a devolver, entre 1 y 100. Default 10.
   * Default: `10`.
   */
  limit?: number;
  /** Cursor: id del último objeto de la página anterior. Devuelve los siguientes. */
  startingAfter?: string;
  /** Cursor: id del primer objeto de la página actual. Devuelve los anteriores. */
  endingBefore?: string;
  /** Id del paciente (`pat_…`). */
  patient?: string;
  externalId?: string;
}

/** Parámetros de `intakeRequests.list` (GET /v1/intake_requests). */
export interface IntakeRequestListParams {
  /**
   * Cantidad de objetos a devolver, entre 1 y 100. Default 10.
   * Default: `10`.
   */
  limit?: number;
  /** Cursor: id del último objeto de la página anterior. Devuelve los siguientes. */
  startingAfter?: string;
  /** Cursor: id del primer objeto de la página actual. Devuelve los anteriores. */
  endingBefore?: string;
  status?: "new" | "validating" | "ready" | "loaded" | "discarded";
  patient?: string;
}

/** Parámetros de `treatments.list` (GET /v1/treatments). */
export interface TreatmentListParams {
  /**
   * Cantidad de objetos a devolver, entre 1 y 100. Default 10.
   * Default: `10`.
   */
  limit?: number;
  /** Cursor: id del último objeto de la página anterior. Devuelve los siguientes. */
  startingAfter?: string;
  /** Cursor: id del primer objeto de la página actual. Devuelve los anteriores. */
  endingBefore?: string;
  patient?: string;
  active?: boolean | "true" | "false";
}

/** Parámetros de `events.list` (GET /v1/events). */
export interface EventListParams {
  /**
   * Cantidad de objetos a devolver, entre 1 y 100. Default 10.
   * Default: `10`.
   */
  limit?: number;
  /** Cursor: id del último objeto de la página anterior. Devuelve los siguientes. */
  startingAfter?: string;
  /** Cursor: id del primer objeto de la página actual. Devuelve los anteriores. */
  endingBefore?: string;
  type?: "intake.requested" | "patient.upserted" | "appointment.created" | "appointment.rescheduled" | "appointment.cancelled" | "visit.completed" | "treatment.prescribed" | "order.issued";
  status?: "received" | "processed" | "failed" | "duplicate" | "unhandled";
  source?: "api" | "pms" | "email" | "tool" | "manual" | "import";
}

/** Parámetros de `webhookEndpoints.list` (GET /v1/webhook_endpoints). */
export interface WebhookEndpointListParams {
  /** Default: `10`. */
  limit?: number;
  startingAfter?: string;
  endingBefore?: string;
}

/** Parámetros de `webhookDeliveries.list` (GET /v1/webhook_deliveries). */
export interface WebhookDeliveryListParams {
  /**
   * Cantidad de objetos a devolver, entre 1 y 100. Default 10.
   * Default: `10`.
   */
  limit?: number;
  /** Cursor: id del último objeto de la página anterior. Devuelve los siguientes. */
  startingAfter?: string;
  /** Cursor: id del primer objeto de la página actual. Devuelve los anteriores. */
  endingBefore?: string;
  /** Id del endpoint (`we_…`). */
  webhookEndpoint?: string;
  status?: "pending" | "succeeded" | "failed";
}

/** Una operación de la API: recurso y método del SDK, verbo y ruta. */
export interface OperationSpec {
  resource: string;
  method: string;
  httpMethod: "GET" | "POST" | "PATCH" | "DELETE";
  path: string;
}

/** Las 32 operaciones del OpenAPI (`x-kuida-resource` + `x-kuida-method`). */
export const OPERATIONS: readonly OperationSpec[] = [
  { resource: "account", method: "retrieve", httpMethod: "GET", path: "/v1/account" },
  { resource: "patients", method: "create", httpMethod: "POST", path: "/v1/patients" },
  { resource: "patients", method: "list", httpMethod: "GET", path: "/v1/patients" },
  { resource: "patients", method: "retrieve", httpMethod: "GET", path: "/v1/patients/{id}" },
  { resource: "patients", method: "update", httpMethod: "PATCH", path: "/v1/patients/{id}" },
  { resource: "doctors", method: "list", httpMethod: "GET", path: "/v1/doctors" },
  { resource: "doctors", method: "retrieve", httpMethod: "GET", path: "/v1/doctors/{id}" },
  { resource: "appointments", method: "create", httpMethod: "POST", path: "/v1/appointments" },
  { resource: "appointments", method: "list", httpMethod: "GET", path: "/v1/appointments" },
  { resource: "appointments", method: "retrieve", httpMethod: "GET", path: "/v1/appointments/{id}" },
  { resource: "appointments", method: "update", httpMethod: "PATCH", path: "/v1/appointments/{id}" },
  { resource: "appointments", method: "cancel", httpMethod: "POST", path: "/v1/appointments/{id}/cancel" },
  { resource: "visits", method: "create", httpMethod: "POST", path: "/v1/visits" },
  { resource: "visits", method: "list", httpMethod: "GET", path: "/v1/visits" },
  { resource: "visits", method: "retrieve", httpMethod: "GET", path: "/v1/visits/{id}" },
  { resource: "intakeRequests", method: "create", httpMethod: "POST", path: "/v1/intake_requests" },
  { resource: "intakeRequests", method: "list", httpMethod: "GET", path: "/v1/intake_requests" },
  { resource: "intakeRequests", method: "retrieve", httpMethod: "GET", path: "/v1/intake_requests/{id}" },
  { resource: "treatments", method: "create", httpMethod: "POST", path: "/v1/treatments" },
  { resource: "treatments", method: "list", httpMethod: "GET", path: "/v1/treatments" },
  { resource: "treatments", method: "retrieve", httpMethod: "GET", path: "/v1/treatments/{id}" },
  { resource: "events", method: "create", httpMethod: "POST", path: "/v1/events" },
  { resource: "events", method: "list", httpMethod: "GET", path: "/v1/events" },
  { resource: "events", method: "retrieve", httpMethod: "GET", path: "/v1/events/{id}" },
  { resource: "webhookEndpoints", method: "create", httpMethod: "POST", path: "/v1/webhook_endpoints" },
  { resource: "webhookEndpoints", method: "list", httpMethod: "GET", path: "/v1/webhook_endpoints" },
  { resource: "webhookEndpoints", method: "retrieve", httpMethod: "GET", path: "/v1/webhook_endpoints/{id}" },
  { resource: "webhookEndpoints", method: "update", httpMethod: "PATCH", path: "/v1/webhook_endpoints/{id}" },
  { resource: "webhookEndpoints", method: "delete", httpMethod: "DELETE", path: "/v1/webhook_endpoints/{id}" },
  { resource: "webhookEndpoints", method: "ping", httpMethod: "POST", path: "/v1/webhook_endpoints/{id}/ping" },
  { resource: "webhookDeliveries", method: "list", httpMethod: "GET", path: "/v1/webhook_deliveries" },
  { resource: "webhookDeliveries", method: "retrieve", httpMethod: "GET", path: "/v1/webhook_deliveries/{id}" },
];
