// Código generado por internal/cmd/generate a partir de openapi/kuida-v1.json. NO EDITAR.

package kuida

import (
	"encoding/json"
	"net/url"
	"time"
)

// APIVersion es la versión de la API de Kuida contra la que se generó este SDK.
// Se manda en el header Kuida-Version de cada pedido.
const APIVersion = "2026-09-28"

// ErrorType enumera el tipo de error que devuelve la API en `error.type`.
type ErrorType string

// Valores de ErrorType.
const (
	ErrorTypeInvalidRequest ErrorType = "invalid_request_error"
	ErrorTypeAuthentication ErrorType = "authentication_error"
	ErrorTypePermission     ErrorType = "permission_error"
	ErrorTypeIdempotency    ErrorType = "idempotency_error"
	ErrorTypeRateLimit      ErrorType = "rate_limit_error"
	ErrorTypeAPI            ErrorType = "api_error"
)

// ErrorCode enumera el código de error que devuelve la API en `error.code`.
type ErrorCode string

// Valores de ErrorCode.
const (
	ErrorCodeParameterMissing     ErrorCode = "parameter_missing"
	ErrorCodeParameterInvalid     ErrorCode = "parameter_invalid"
	ErrorCodeBodyInvalidJSON      ErrorCode = "body_invalid_json"
	ErrorCodeBodyTooLarge         ErrorCode = "body_too_large"
	ErrorCodeBatchTooLarge        ErrorCode = "batch_too_large"
	ErrorCodeResourceMissing      ErrorCode = "resource_missing"
	ErrorCodeResourceConflict     ErrorCode = "resource_conflict"
	ErrorCodeAPIKeyMissing        ErrorCode = "api_key_missing"
	ErrorCodeAPIKeyInvalid        ErrorCode = "api_key_invalid"
	ErrorCodeAPIKeyRevoked        ErrorCode = "api_key_revoked"
	ErrorCodeAPIKeyExpired        ErrorCode = "api_key_expired"
	ErrorCodeScopeMissing         ErrorCode = "scope_missing"
	ErrorCodeServiceNotContracted ErrorCode = "service_not_contracted"
	ErrorCodeAPIVersionInvalid    ErrorCode = "api_version_invalid"
	ErrorCodeIdempotencyKeyInUse  ErrorCode = "idempotency_key_in_use"
	ErrorCodeIdempotencyKeyReused ErrorCode = "idempotency_key_reused"
	ErrorCodeRateLimited          ErrorCode = "rate_limited"
	ErrorCodeProcessingFailed     ErrorCode = "processing_failed"
	ErrorCodeInternalError        ErrorCode = "internal_error"
)

// ServiceLine enumera valores posibles.
type ServiceLine string

// Valores de ServiceLine.
const (
	ServiceLineFollowUp ServiceLine = "follow_up"
	ServiceLineNetwork  ServiceLine = "network"
)

// AppointmentStatus enumera valores posibles.
type AppointmentStatus string

// Valores de AppointmentStatus.
const (
	AppointmentStatusConfirmed   AppointmentStatus = "confirmed"
	AppointmentStatusPending     AppointmentStatus = "pending"
	AppointmentStatusRescheduled AppointmentStatus = "rescheduled"
	AppointmentStatusCancelled   AppointmentStatus = "cancelled"
	AppointmentStatusCompleted   AppointmentStatus = "completed"
)

// AppointmentSource enumera por dónde entró: tu API, el conector del sistema de
// gestión, el agente de Kuida o el equipo.
type AppointmentSource string

// Valores de AppointmentSource.
const (
	AppointmentSourceAPI    AppointmentSource = "api"
	AppointmentSourcePMS    AppointmentSource = "pms"
	AppointmentSourceAgent  AppointmentSource = "agent"
	AppointmentSourceManual AppointmentSource = "manual"
)

// EventType enumera valores posibles.
type EventType string

// Valores de EventType.
const (
	EventTypeIntakeRequested        EventType = "intake.requested"
	EventTypePatientUpserted        EventType = "patient.upserted"
	EventTypeAppointmentCreated     EventType = "appointment.created"
	EventTypeAppointmentRescheduled EventType = "appointment.rescheduled"
	EventTypeAppointmentCancelled   EventType = "appointment.cancelled"
	EventTypeVisitCompleted         EventType = "visit.completed"
	EventTypeTreatmentPrescribed    EventType = "treatment.prescribed"
	EventTypeOrderIssued            EventType = "order.issued"
)

// EventSource enumera por dónde entró.
type EventSource string

// Valores de EventSource.
const (
	EventSourceAPI    EventSource = "api"
	EventSourcePMS    EventSource = "pms"
	EventSourceEmail  EventSource = "email"
	EventSourceTool   EventSource = "tool"
	EventSourceManual EventSource = "manual"
	EventSourceImport EventSource = "import"
)

// EventStatus enumera `processed` Kuida hizo lo suyo · `unhandled` guardado, sin efecto
// todavía · `failed` falló, reintenta con el mismo id · `duplicate` · `received` en
// curso.
type EventStatus string

// Valores de EventStatus.
const (
	EventStatusReceived  EventStatus = "received"
	EventStatusProcessed EventStatus = "processed"
	EventStatusFailed    EventStatus = "failed"
	EventStatusDuplicate EventStatus = "duplicate"
	EventStatusUnhandled EventStatus = "unhandled"
)

// EventResultStatus enumera valores posibles.
type EventResultStatus string

// Valores de EventResultStatus.
const (
	EventResultStatusProcessed EventResultStatus = "processed"
	EventResultStatusDuplicate EventResultStatus = "duplicate"
	EventResultStatusUnhandled EventResultStatus = "unhandled"
	EventResultStatusInvalid   EventResultStatus = "invalid"
	EventResultStatusFailed    EventResultStatus = "failed"
)

// IntakeRequestStatus enumera `new` recién llegada · `validating` Kuida está
// completando datos por WhatsApp · `ready` completa · `loaded` cargada en el sistema de
// la organización · `discarded` descartada.
type IntakeRequestStatus string

// Valores de IntakeRequestStatus.
const (
	IntakeRequestStatusNew        IntakeRequestStatus = "new"
	IntakeRequestStatusValidating IntakeRequestStatus = "validating"
	IntakeRequestStatusReady      IntakeRequestStatus = "ready"
	IntakeRequestStatusLoaded     IntakeRequestStatus = "loaded"
	IntakeRequestStatusDiscarded  IntakeRequestStatus = "discarded"
)

// PatientStage enumera estado básico del paciente en Kuida.
type PatientStage string

// Valores de PatientStage.
const (
	PatientStagePendingFollowUp  PatientStage = "pending_follow_up"
	PatientStageInConversation   PatientStage = "in_conversation"
	PatientStageScheduled        PatientStage = "scheduled"
	PatientStagePeriodicFollowUp PatientStage = "periodic_follow_up"
	PatientStageDischarged       PatientStage = "discharged"
	PatientStageLost             PatientStage = "lost"
)

// TreatmentKind enumera valores posibles.
type TreatmentKind string

// Valores de TreatmentKind.
const (
	TreatmentKindMedication TreatmentKind = "medication"
	TreatmentKindTherapy    TreatmentKind = "therapy"
	TreatmentKindDiet       TreatmentKind = "diet"
	TreatmentKindStudy      TreatmentKind = "study"
	TreatmentKindLabwork    TreatmentKind = "labwork"
	TreatmentKindLifestyle  TreatmentKind = "lifestyle"
)

// VisitSource enumera valores posibles.
type VisitSource string

// Valores de VisitSource.
const (
	VisitSourcePMS    VisitSource = "pms"
	VisitSourceManual VisitSource = "manual"
)

// WebhookDeliveryStatus enumera `pending` en curso o esperando reintento · `succeeded`
// tu endpoint respondió 2xx · `failed` se agotaron los reintentos.
type WebhookDeliveryStatus string

// Valores de WebhookDeliveryStatus.
const (
	WebhookDeliveryStatusPending   WebhookDeliveryStatus = "pending"
	WebhookDeliveryStatusSucceeded WebhookDeliveryStatus = "succeeded"
	WebhookDeliveryStatusFailed    WebhookDeliveryStatus = "failed"
)

// WebhookEventType enumera valores posibles.
type WebhookEventType string

// Valores de WebhookEventType.
const (
	WebhookEventTypeIntakeRequested        WebhookEventType = "intake.requested"
	WebhookEventTypePatientUpserted        WebhookEventType = "patient.upserted"
	WebhookEventTypeAppointmentCreated     WebhookEventType = "appointment.created"
	WebhookEventTypeAppointmentRescheduled WebhookEventType = "appointment.rescheduled"
	WebhookEventTypeAppointmentCancelled   WebhookEventType = "appointment.cancelled"
	WebhookEventTypeVisitCompleted         WebhookEventType = "visit.completed"
	WebhookEventTypeTreatmentPrescribed    WebhookEventType = "treatment.prescribed"
	WebhookEventTypeOrderIssued            WebhookEventType = "order.issued"
	WebhookEventTypeIntakeCreated          WebhookEventType = "intake.created"
	WebhookEventTypeIntakeReady            WebhookEventType = "intake.ready"
	WebhookEventTypeConversationHandoff    WebhookEventType = "conversation.handoff"
	WebhookEventTypePatientSilent          WebhookEventType = "patient.silent"
	WebhookEventTypeWebhookPing            WebhookEventType = "webhook.ping"
	WebhookEventTypeAll                    WebhookEventType = "*"
)

// WebhookEndpointStatus enumera valores posibles.
type WebhookEndpointStatus string

// Valores de WebhookEndpointStatus.
const (
	WebhookEndpointStatusEnabled  WebhookEndpointStatus = "enabled"
	WebhookEndpointStatusDisabled WebhookEndpointStatus = "disabled"
)

// Account: La organización dueña de la clave de API.
type Account struct {
	// ID: Id de la organización.
	ID     string `json:"id"`
	Object string `json:"object"`
	Name   string `json:"name"`
	// Type: Tipo de institución (clinic, pharmacy, lab, …).
	Type     string `json:"type"`
	TimeZone string `json:"timeZone"`
	// ServiceLines: Líneas de servicio contratadas: seguimiento de pacientes y red de
	// derivaciones.
	ServiceLines []ServiceLine `json:"serviceLines"`
	// Livemode: `true` en producción, `false` en el entorno de pruebas.
	Livemode bool           `json:"livemode"`
	APIKey   *AccountAPIKey `json:"apiKey"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *Account) UnmarshalJSON(data []byte) error {
	type alias Account
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = Account(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *Account) Raw() json.RawMessage { return m.raw }

func (m *Account) setLastResponse(r *APIResponse) { m.LastResponse = r }

// AccountAPIKey: objeto de la API.
type AccountAPIKey struct {
	Name   string   `json:"name"`
	Prefix string   `json:"prefix"`
	Scopes []string `json:"scopes"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *AccountAPIKey) UnmarshalJSON(data []byte) error {
	type alias AccountAPIKey
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = AccountAPIKey(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *AccountAPIKey) Raw() json.RawMessage { return m.raw }

// Appointment: Un turno. Kuida le manda al paciente los recordatorios que la
// organización configuró.
type Appointment struct {
	// ID: Id del turno.
	ID     string `json:"id"`
	Object string `json:"object"`
	// Patient: Paciente del turno.
	Patient string `json:"patient"`
	// Doctor: Profesional.
	Doctor *string `json:"doctor"`
	// StartAt: Fecha y hora ISO 8601.
	StartAt time.Time         `json:"startAt"`
	Status  AppointmentStatus `json:"status"`
	// Type: Tipo de turno o práctica.
	Type *string `json:"type"`
	// ExternalID: Id del turno en tu sistema.
	ExternalID *string `json:"externalId"`
	// Source: Por dónde entró: tu API, el conector del sistema de gestión, el agente de
	// Kuida o el equipo.
	Source AppointmentSource `json:"source"`
	// CancelledAt: Fecha y hora ISO 8601.
	CancelledAt *time.Time `json:"cancelledAt"`
	// CreatedAt: Fecha y hora ISO 8601.
	CreatedAt time.Time `json:"createdAt"`
	// UpdatedAt: Fecha y hora ISO 8601.
	UpdatedAt time.Time `json:"updatedAt"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *Appointment) UnmarshalJSON(data []byte) error {
	type alias Appointment
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = Appointment(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *Appointment) Raw() json.RawMessage { return m.raw }

func (m *Appointment) setLastResponse(r *APIResponse) { m.LastResponse = r }

// DeletedObject: Confirmación de un borrado.
type DeletedObject struct {
	ID      string `json:"id"`
	Object  string `json:"object"`
	Deleted bool   `json:"deleted"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *DeletedObject) UnmarshalJSON(data []byte) error {
	type alias DeletedObject
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = DeletedObject(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *DeletedObject) Raw() json.RawMessage { return m.raw }

func (m *DeletedObject) setLastResponse(r *APIResponse) { m.LastResponse = r }

// Doctor: Un profesional del padrón de la organización.
type Doctor struct {
	// ID: Id del profesional.
	ID        string  `json:"id"`
	Object    string  `json:"object"`
	FullName  string  `json:"fullName"`
	Specialty *string `json:"specialty"`
	// LicenseNumber: Matrícula.
	LicenseNumber *string `json:"licenseNumber"`
	ExternalID    *string `json:"externalId"`
	// Active: Si atiende hoy. Solo los activos reciben turnos del agente.
	Active bool `json:"active"`
	// CreatedAt: Fecha y hora ISO 8601.
	CreatedAt time.Time `json:"createdAt"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *Doctor) UnmarshalJSON(data []byte) error {
	type alias Doctor
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = Doctor(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *Doctor) Raw() json.RawMessage { return m.raw }

func (m *Doctor) setLastResponse(r *APIResponse) { m.LastResponse = r }

// Event: Un evento que entró a Kuida, con su resultado.
type Event struct {
	// ID: Id del evento en Kuida.
	ID     string    `json:"id"`
	Object string    `json:"object"`
	Type   EventType `json:"type"`
	// Source: Por dónde entró.
	Source EventSource `json:"source"`
	// SourceRef: Identidad del evento en su fuente. Para la API, el `id` que enviaste.
	SourceRef string `json:"sourceRef"`
	// Status: `processed` Kuida hizo lo suyo · `unhandled` guardado, sin efecto todavía ·
	// `failed` falló, reintenta con el mismo id · `duplicate` · `received` en curso.
	Status EventStatus `json:"status"`
	Error  *string     `json:"error"`
	// Result: Lo que produjo el evento (`{ object: "visit", id: "vis_…" }`).
	Result *ResultRef     `json:"result"`
	Data   map[string]any `json:"data"`
	// OccurredAt: Fecha y hora ISO 8601.
	OccurredAt time.Time `json:"occurredAt"`
	// ReceivedAt: Fecha y hora ISO 8601.
	ReceivedAt time.Time `json:"receivedAt"`
	// ProcessedAt: Fecha y hora ISO 8601.
	ProcessedAt *time.Time `json:"processedAt"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *Event) UnmarshalJSON(data []byte) error {
	type alias Event
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = Event(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *Event) Raw() json.RawMessage { return m.raw }

func (m *Event) setLastResponse(r *APIResponse) { m.LastResponse = r }

// ResultRef: objeto de la API.
type ResultRef struct {
	Object string `json:"object"`
	ID     string `json:"id"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *ResultRef) UnmarshalJSON(data []byte) error {
	type alias ResultRef
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = ResultRef(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *ResultRef) Raw() json.RawMessage { return m.raw }

// EventBatchResponse: Un resultado por evento, en el mismo orden.
type EventBatchResponse struct {
	Object  string         `json:"object"`
	Results []*EventResult `json:"results"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *EventBatchResponse) UnmarshalJSON(data []byte) error {
	type alias EventBatchResponse
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = EventBatchResponse(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *EventBatchResponse) Raw() json.RawMessage { return m.raw }

func (m *EventBatchResponse) setLastResponse(r *APIResponse) { m.LastResponse = r }

// EventResult: objeto de la API.
type EventResult struct {
	// ID: El id que enviaste.
	ID       string            `json:"id"`
	Accepted bool              `json:"accepted"`
	Status   EventResultStatus `json:"status"`
	// Event: Id del evento en Kuida (`evt_…`).
	Event  *string    `json:"event"`
	Result *ResultRef `json:"result"`
	Error  *string    `json:"error"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *EventResult) UnmarshalJSON(data []byte) error {
	type alias EventResult
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = EventResult(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *EventResult) Raw() json.RawMessage { return m.raw }

func (m *EventResult) setLastResponse(r *APIResponse) { m.LastResponse = r }

// IntakeRequest: Una solicitud de ingreso: alguien pide el servicio para un paciente y
// Kuida completa lo que falta.
type IntakeRequest struct {
	// ID: Id de la solicitud.
	ID     string `json:"id"`
	Object string `json:"object"`
	// Status: `new` recién llegada · `validating` Kuida está completando datos por
	// WhatsApp · `ready` completa · `loaded` cargada en el sistema de la organización ·
	// `discarded` descartada.
	Status IntakeRequestStatus `json:"status"`
	// Patient: Paciente, cuando ya se pudo identificar.
	Patient *string `json:"patient"`
	Summary *string `json:"summary"`
	// Missing: Datos que todavía faltan para que la solicitud quede lista.
	Missing []string `json:"missing"`
	// Data: Los datos de la solicitud tal como los tiene Kuida.
	Data          map[string]any `json:"data"`
	DiscardReason *string        `json:"discardReason"`
	// ReadyAt: Fecha y hora ISO 8601.
	ReadyAt *time.Time `json:"readyAt"`
	// LoadedAt: Fecha y hora ISO 8601.
	LoadedAt *time.Time `json:"loadedAt"`
	// ClosedAt: Fecha y hora ISO 8601.
	ClosedAt *time.Time `json:"closedAt"`
	// CreatedAt: Fecha y hora ISO 8601.
	CreatedAt time.Time `json:"createdAt"`
	// UpdatedAt: Fecha y hora ISO 8601.
	UpdatedAt time.Time `json:"updatedAt"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *IntakeRequest) UnmarshalJSON(data []byte) error {
	type alias IntakeRequest
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = IntakeRequest(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *IntakeRequest) Raw() json.RawMessage { return m.raw }

func (m *IntakeRequest) setLastResponse(r *APIResponse) { m.LastResponse = r }

// Patient: Un paciente de la organización.
type Patient struct {
	// ID: Id del paciente.
	ID       string  `json:"id"`
	Object   string  `json:"object"`
	FullName *string `json:"fullName"`
	// Phone: Teléfono normalizado.
	Phone *string `json:"phone"`
	Email *string `json:"email"`
	DNI   *string `json:"dni"`
	// ExternalID: Id del paciente en tu sistema.
	ExternalID *string `json:"externalId"`
	// DateOfBirth: Fecha de nacimiento (AAAA-MM-DD).
	DateOfBirth *string `json:"dateOfBirth"`
	// Stage: Estado básico del paciente en Kuida.
	Stage PatientStage `json:"stage"`
	// OptedOut: Si el paciente pidió no recibir mensajes. Kuida no le escribe.
	OptedOut bool `json:"optedOut"`
	// CreatedAt: Fecha y hora ISO 8601.
	CreatedAt time.Time `json:"createdAt"`
	// UpdatedAt: Fecha y hora ISO 8601.
	UpdatedAt time.Time `json:"updatedAt"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *Patient) UnmarshalJSON(data []byte) error {
	type alias Patient
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = Patient(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *Patient) Raw() json.RawMessage { return m.raw }

func (m *Patient) setLastResponse(r *APIResponse) { m.LastResponse = r }

// Treatment: Un tratamiento indicado al paciente.
type Treatment struct {
	// ID: Id del tratamiento.
	ID     string `json:"id"`
	Object string `json:"object"`
	// Patient: Paciente.
	Patient      string        `json:"patient"`
	Kind         TreatmentKind `json:"kind"`
	Name         string        `json:"name"`
	Dosage       *string       `json:"dosage"`
	Frequency    *string       `json:"frequency"`
	Instructions *string       `json:"instructions"`
	// StartDate: Fecha y hora ISO 8601.
	StartDate *time.Time `json:"startDate"`
	// EndDate: Fecha y hora ISO 8601.
	EndDate *time.Time `json:"endDate"`
	Active  bool       `json:"active"`
	// CreatedAt: Fecha y hora ISO 8601.
	CreatedAt time.Time `json:"createdAt"`
	// UpdatedAt: Fecha y hora ISO 8601.
	UpdatedAt time.Time `json:"updatedAt"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *Treatment) UnmarshalJSON(data []byte) error {
	type alias Treatment
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = Treatment(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *Treatment) Raw() json.RawMessage { return m.raw }

func (m *Treatment) setLastResponse(r *APIResponse) { m.LastResponse = r }

// Visit: Una consulta atendida. Crearla arranca el seguimiento post-consulta del
// paciente.
type Visit struct {
	// ID: Id de la consulta.
	ID     string `json:"id"`
	Object string `json:"object"`
	// Patient: Paciente atendido.
	Patient string `json:"patient"`
	// Doctor: Profesional.
	Doctor *string `json:"doctor"`
	// Appointment: Turno del que salió la consulta.
	Appointment *string `json:"appointment"`
	// VisitedAt: Fecha y hora ISO 8601.
	VisitedAt  time.Time   `json:"visitedAt"`
	Type       *string     `json:"type"`
	Diagnosis  *string     `json:"diagnosis"`
	Notes      *string     `json:"notes"`
	ExternalID *string     `json:"externalId"`
	Source     VisitSource `json:"source"`
	// CreatedAt: Fecha y hora ISO 8601.
	CreatedAt time.Time `json:"createdAt"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *Visit) UnmarshalJSON(data []byte) error {
	type alias Visit
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = Visit(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *Visit) Raw() json.RawMessage { return m.raw }

func (m *Visit) setLastResponse(r *APIResponse) { m.LastResponse = r }

// WebhookDelivery: Un envío de webhook con sus reintentos.
type WebhookDelivery struct {
	// ID: Id de la entrega.
	ID     string `json:"id"`
	Object string `json:"object"`
	// WebhookEndpoint: Endpoint destino.
	WebhookEndpoint string `json:"webhookEndpoint"`
	EventType       string `json:"eventType"`
	// Status: `pending` en curso o esperando reintento · `succeeded` tu endpoint respondió
	// 2xx · `failed` se agotaron los reintentos.
	Status         WebhookDeliveryStatus `json:"status"`
	Attempts       int64                 `json:"attempts"`
	LastStatusCode *int64                `json:"lastStatusCode"`
	LastError      *string               `json:"lastError"`
	// NextAttemptAt: Fecha y hora ISO 8601.
	NextAttemptAt *time.Time `json:"nextAttemptAt"`
	// DeliveredAt: Fecha y hora ISO 8601.
	DeliveredAt *time.Time `json:"deliveredAt"`
	// Payload: El cuerpo exacto que se envió.
	Payload map[string]any `json:"payload"`
	// CreatedAt: Fecha y hora ISO 8601.
	CreatedAt time.Time `json:"createdAt"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *WebhookDelivery) UnmarshalJSON(data []byte) error {
	type alias WebhookDelivery
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = WebhookDelivery(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *WebhookDelivery) Raw() json.RawMessage { return m.raw }

func (m *WebhookDelivery) setLastResponse(r *APIResponse) { m.LastResponse = r }

// WebhookEndpoint: Una URL de tu sistema a la que Kuida le avisa lo que pasa.
type WebhookEndpoint struct {
	// ID: Id del endpoint.
	ID          string  `json:"id"`
	Object      string  `json:"object"`
	URL         string  `json:"url"`
	Description *string `json:"description"`
	// EnabledEvents: Tipos de evento que recibe. `*` = todos.
	EnabledEvents []WebhookEventType    `json:"enabledEvents"`
	Status        WebhookEndpointStatus `json:"status"`
	// Secret: Secreto para verificar la firma (`whsec_…`). Solo viene en la respuesta de
	// creación.
	Secret string `json:"secret,omitempty"`
	// LastDeliveredAt: Fecha y hora ISO 8601.
	LastDeliveredAt *time.Time                `json:"lastDeliveredAt"`
	LastError       *WebhookEndpointLastError `json:"lastError"`
	// CreatedAt: Fecha y hora ISO 8601.
	CreatedAt time.Time `json:"createdAt"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *WebhookEndpoint) UnmarshalJSON(data []byte) error {
	type alias WebhookEndpoint
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = WebhookEndpoint(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *WebhookEndpoint) Raw() json.RawMessage { return m.raw }

func (m *WebhookEndpoint) setLastResponse(r *APIResponse) { m.LastResponse = r }

// WebhookEndpointLastError: objeto de la API.
type WebhookEndpointLastError struct {
	// At: Fecha y hora ISO 8601.
	At      time.Time `json:"at"`
	Message string    `json:"message"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *WebhookEndpointLastError) UnmarshalJSON(data []byte) error {
	type alias WebhookEndpointLastError
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = WebhookEndpointLastError(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *WebhookEndpointLastError) Raw() json.RawMessage { return m.raw }

// WebhookEvent: Lo que Kuida le manda a tu endpoint.
type WebhookEvent struct {
	// ID: Id de la entrega (`whd_…`). Estable entre reintentos: deduplica por este campo.
	ID         string           `json:"id"`
	Object     string           `json:"object"`
	Type       WebhookEventType `json:"type"`
	APIVersion string           `json:"apiVersion"`
	Livemode   bool             `json:"livemode"`
	// OccurredAt: Cuándo pasó, ISO 8601.
	OccurredAt time.Time `json:"occurredAt"`
	// Ref: Id interno de lo que disparó el evento.
	Ref string `json:"ref"`
	// Data: Los datos del hecho.
	Data map[string]any `json:"data"`

	// LastResponse tiene los datos HTTP del pedido que devolvió este objeto
	// (status, headers, Request-Id y cuerpo crudo). Es nil en objetos anidados.
	LastResponse *APIResponse `json:"-"`

	raw json.RawMessage
}

// UnmarshalJSON decodifica el objeto y conserva el JSON crudo, accesible con Raw.
func (m *WebhookEvent) UnmarshalJSON(data []byte) error {
	type alias WebhookEvent
	var a alias
	if err := json.Unmarshal(data, &a); err != nil {
		return err
	}
	*m = WebhookEvent(a)
	m.raw = append(json.RawMessage(nil), data...)
	return nil
}

// Raw devuelve el JSON crudo con que llegó el objeto, incluidos los campos
// que este SDK todavía no conoce.
func (m *WebhookEvent) Raw() json.RawMessage { return m.raw }

func (m *WebhookEvent) setLastResponse(r *APIResponse) { m.LastResponse = r }

// AppointmentCancelParams: parámetros de entrada.
type AppointmentCancelParams struct {
	// Reason: Motivo, para el registro.
	Reason *string `json:"reason,omitempty"`
}

// AppointmentCreateParams: parámetros de entrada.
type AppointmentCreateParams struct {
	Patient *PatientReference `json:"patient,omitempty"`
	Doctor  *DoctorReference  `json:"doctor,omitempty"`
	// StartAt: Inicio del turno.
	StartAt time.Time `json:"startAt,omitempty"`
	// Type: Tipo de turno o práctica.
	Type *string `json:"type,omitempty"`
	// ExternalID: Id del turno en tu sistema. Si ya existe un turno con ese id, se devuelve
	// ese y no se crea otro.
	ExternalID *string `json:"externalId,omitempty"`
}

// MarshalJSON serializa los parámetros y omite las fechas obligatorias sin
// completar, para que la API responda parameter_missing en lugar de recibir el año 1.
func (p AppointmentCreateParams) MarshalJSON() ([]byte, error) {
	type alias AppointmentCreateParams
	return json.Marshal(struct {
		alias
		StartAt *time.Time `json:"startAt,omitempty"`
	}{alias(p), timeOrNil(p.StartAt)})
}

// AppointmentUpdateParams: parámetros de entrada.
type AppointmentUpdateParams struct {
	// StartAt: Nuevo horario. Reprograma los recordatorios.
	StartAt *time.Time       `json:"startAt,omitempty"`
	Doctor  *DoctorReference `json:"doctor,omitempty"`
	Type    *string          `json:"type,omitempty"`
}

// DoctorIdentity: Cómo conoce tu sistema al profesional.
type DoctorIdentity struct {
	// ExternalID: Id del profesional en tu sistema.
	ExternalID *string `json:"externalId,omitempty"`
	// FullName: Nombre del profesional.
	FullName *string `json:"fullName,omitempty"`
	// Specialty: Especialidad.
	Specialty *string `json:"specialty,omitempty"`
}

// EventInput: Un evento del catálogo v1. El `type` define la forma de `data`.
type EventInput struct {
	// ID: Tu id del evento. Es la clave de idempotencia: reenviar el mismo id no repite
	// nada.
	ID string `json:"id,omitempty"`
	// SchemaVersion: Versión del esquema del evento. Siempre 1 en v1.
	SchemaVersion *int `json:"schemaVersion,omitempty"`
	// OccurredAt: Cuándo pasó. Por defecto: ahora.
	OccurredAt *time.Time `json:"occurredAt,omitempty"`
	// Source: Qué sistema lo emite, para el registro.
	Source *EventInputSource `json:"source,omitempty"`
	Type   EventType         `json:"type,omitempty"`
	// Data: Datos del evento; su forma depende de `type` (ver la referencia de la API).
	Data map[string]any `json:"data,omitempty"`
}

// EventInputSource: Qué sistema lo emite, para el registro.
type EventInputSource struct {
	System  *string `json:"system,omitempty"`
	Version *string `json:"version,omitempty"`
}

// IntakeContact: Un referente del paciente (familiar, cuidador).
type IntakeContact struct {
	Name         *string `json:"name,omitempty"`
	Relationship *string `json:"relationship,omitempty"`
	Phone        *string `json:"phone,omitempty"`
	Email        *string `json:"email,omitempty"`
}

// IntakeCoverage: parámetros de entrada.
type IntakeCoverage struct {
	// Insurer: Obra social o prepaga.
	Insurer *string `json:"insurer,omitempty"`
	Plan    *string `json:"plan,omitempty"`
	// MemberID: Número de afiliado.
	MemberID *string `json:"memberId,omitempty"`
}

// IntakeRequestCreateParams: Hace falta al menos un dato del paciente o de un referente
// (nombre, documento o teléfono).
type IntakeRequestCreateParams struct {
	Patient          *PatientIdentity `json:"patient,omitempty"`
	Contacts         []*IntakeContact `json:"contacts,omitempty"`
	Coverage         *IntakeCoverage  `json:"coverage,omitempty"`
	Address          *string          `json:"address,omitempty"`
	RequestedService *string          `json:"requestedService,omitempty"`
	// Summary: Resumen administrativo del pedido, sin contenido clínico.
	Summary *string `json:"summary,omitempty"`
	// Hospitalized: Si el paciente está internado hoy.
	Hospitalized *bool `json:"hospitalized,omitempty"`
	// Sender: Quién pide el servicio.
	Sender *IntakeSender `json:"sender,omitempty"`
}

// PatientIdentity: Cómo conoce tu sistema al paciente. Kuida lo busca por teléfono y
// después por DNI; si no existe, lo crea.
type PatientIdentity struct {
	// Phone: Celular con WhatsApp, en cualquier formato; Kuida lo normaliza. Es la identidad
	// principal del paciente.
	Phone *string `json:"phone,omitempty"`
	// FullName: Nombre y apellido.
	FullName *string `json:"fullName,omitempty"`
	// DNI: Documento, sin puntos.
	DNI *string `json:"dni,omitempty"`
	// Email: Correo electrónico.
	Email *string `json:"email,omitempty"`
	// ExternalID: Id del paciente en tu sistema.
	ExternalID *string `json:"externalId,omitempty"`
	// DateOfBirth: Fecha ISO 8601 (AAAA-MM-DD).
	DateOfBirth *string `json:"dateOfBirth,omitempty"`
}

// IntakeSender: Quién pide el servicio.
type IntakeSender struct {
	Name         *string `json:"name,omitempty"`
	Email        *string `json:"email,omitempty"`
	Organization *string `json:"organization,omitempty"`
}

// PatientCreateParams: parámetros de entrada.
type PatientCreateParams struct {
	// Phone: Celular con WhatsApp, en cualquier formato; Kuida lo normaliza.
	Phone string `json:"phone,omitempty"`
	// FullName: Nombre y apellido.
	FullName *string `json:"fullName,omitempty"`
	// DNI: Documento, sin puntos.
	DNI *string `json:"dni,omitempty"`
	// Email: Correo electrónico.
	Email *string `json:"email,omitempty"`
	// ExternalID: Id del paciente en tu sistema.
	ExternalID *string `json:"externalId,omitempty"`
	// DateOfBirth: Fecha ISO 8601 (AAAA-MM-DD).
	DateOfBirth *string `json:"dateOfBirth,omitempty"`
}

// PatientUpdateParams: Solo cambia lo que envías. El teléfono no se cambia: es la
// identidad.
type PatientUpdateParams struct {
	FullName   *string `json:"fullName,omitempty"`
	Email      *string `json:"email,omitempty"`
	DNI        *string `json:"dni,omitempty"`
	ExternalID *string `json:"externalId,omitempty"`
	// DateOfBirth: Fecha ISO 8601 (AAAA-MM-DD).
	DateOfBirth *string `json:"dateOfBirth,omitempty"`
}

// TreatmentCreateParams: parámetros de entrada.
type TreatmentCreateParams struct {
	Patient *PatientReference `json:"patient,omitempty"`
	Name    string            `json:"name,omitempty"`
	// Kind: Por defecto: `medication`.
	Kind         TreatmentKind `json:"kind,omitempty"`
	Dosage       *string       `json:"dosage,omitempty"`
	Frequency    *string       `json:"frequency,omitempty"`
	Instructions *string       `json:"instructions,omitempty"`
	// StartDate: Fecha y hora ISO 8601.
	StartDate *time.Time `json:"startDate,omitempty"`
	// EndDate: Fecha y hora ISO 8601.
	EndDate *time.Time `json:"endDate,omitempty"`
}

// VisitCreateParams: parámetros de entrada.
type VisitCreateParams struct {
	Patient *PatientReference `json:"patient,omitempty"`
	Doctor  *DoctorReference  `json:"doctor,omitempty"`
	// VisitedAt: Cuándo se atendió.
	VisitedAt time.Time `json:"visitedAt,omitempty"`
	Type      *string   `json:"type,omitempty"`
	// ExternalID: Id de la consulta en tu sistema. Si ya existe, se devuelve esa y no se
	// crea otra.
	ExternalID *string `json:"externalId,omitempty"`
	// Appointment: Turno del que sale la consulta: id de Kuida (`apt_…`) o el `externalId`
	// del turno. Lo marca como atendido.
	Appointment *string `json:"appointment,omitempty"`
	Diagnosis   *string `json:"diagnosis,omitempty"`
	Notes       *string `json:"notes,omitempty"`
}

// MarshalJSON serializa los parámetros y omite las fechas obligatorias sin
// completar, para que la API responda parameter_missing en lugar de recibir el año 1.
func (p VisitCreateParams) MarshalJSON() ([]byte, error) {
	type alias VisitCreateParams
	return json.Marshal(struct {
		alias
		VisitedAt *time.Time `json:"visitedAt,omitempty"`
	}{alias(p), timeOrNil(p.VisitedAt)})
}

// WebhookEndpointCreateParams: parámetros de entrada.
type WebhookEndpointCreateParams struct {
	// URL: URL https de tu sistema.
	URL string `json:"url,omitempty"`
	// EnabledEvents: Qué eventos quieres recibir. `*` = todos.
	EnabledEvents []WebhookEventType `json:"enabledEvents,omitempty"`
	Description   *string            `json:"description,omitempty"`
}

// WebhookEndpointUpdateParams: parámetros de entrada.
type WebhookEndpointUpdateParams struct {
	URL           *string            `json:"url,omitempty"`
	EnabledEvents []WebhookEventType `json:"enabledEvents,omitempty"`
	Description   *string            `json:"description,omitempty"`
	// Disabled: `true` pausa los envíos sin borrar el endpoint.
	Disabled *bool `json:"disabled,omitempty"`
}

// AppointmentList es una página de objetos Appointment.
type AppointmentList = ListPage[*Appointment]

// DoctorList es una página de objetos Doctor.
type DoctorList = ListPage[*Doctor]

// EventList es una página de objetos Event.
type EventList = ListPage[*Event]

// IntakeRequestList es una página de objetos IntakeRequest.
type IntakeRequestList = ListPage[*IntakeRequest]

// PatientList es una página de objetos Patient.
type PatientList = ListPage[*Patient]

// TreatmentList es una página de objetos Treatment.
type TreatmentList = ListPage[*Treatment]

// VisitList es una página de objetos Visit.
type VisitList = ListPage[*Visit]

// WebhookDeliveryList es una página de objetos WebhookDelivery.
type WebhookDeliveryList = ListPage[*WebhookDelivery]

// WebhookEndpointList es una página de objetos WebhookEndpoint.
type WebhookEndpointList = ListPage[*WebhookEndpoint]

// AppointmentListParams son los filtros de Appointment.List. Los campos en nil no se envían.
type AppointmentListParams struct {
	ListParams
	// Patient: Id del paciente (`pat_…`).
	Patient    *string
	Status     AppointmentStatus
	ExternalID *string
	// StartAtGte: Turnos desde esta fecha y hora.
	StartAtGte *time.Time
	// StartAtLt: Turnos antes de esta fecha y hora.
	StartAtLt *time.Time
}

func (p *AppointmentListParams) query() url.Values {
	if p == nil {
		return url.Values{}
	}
	q := p.ListParams.query()
	if p.Patient != nil {
		q.Set("patient", *p.Patient)
	}
	if p.Status != "" {
		q.Set("status", string(p.Status))
	}
	if p.ExternalID != nil {
		q.Set("externalId", *p.ExternalID)
	}
	if p.StartAtGte != nil {
		q.Set("startAtGte", p.StartAtGte.Format(time.RFC3339Nano))
	}
	if p.StartAtLt != nil {
		q.Set("startAtLt", p.StartAtLt.Format(time.RFC3339Nano))
	}
	return q
}

// DoctorListParams son los filtros de Doctor.List. Los campos en nil no se envían.
type DoctorListParams struct {
	ListParams
	// Active: Filtra por activos o inactivos.
	Active     *bool
	ExternalID *string
}

func (p *DoctorListParams) query() url.Values {
	if p == nil {
		return url.Values{}
	}
	q := p.ListParams.query()
	if p.Active != nil {
		q.Set("active", boolString(*p.Active))
	}
	if p.ExternalID != nil {
		q.Set("externalId", *p.ExternalID)
	}
	return q
}

// EventListParams son los filtros de Event.List. Los campos en nil no se envían.
type EventListParams struct {
	ListParams
	Type   EventType
	Status EventStatus
	Source EventSource
}

func (p *EventListParams) query() url.Values {
	if p == nil {
		return url.Values{}
	}
	q := p.ListParams.query()
	if p.Type != "" {
		q.Set("type", string(p.Type))
	}
	if p.Status != "" {
		q.Set("status", string(p.Status))
	}
	if p.Source != "" {
		q.Set("source", string(p.Source))
	}
	return q
}

// IntakeRequestListParams son los filtros de IntakeRequest.List. Los campos en nil no se envían.
type IntakeRequestListParams struct {
	ListParams
	Status  IntakeRequestStatus
	Patient *string
}

func (p *IntakeRequestListParams) query() url.Values {
	if p == nil {
		return url.Values{}
	}
	q := p.ListParams.query()
	if p.Status != "" {
		q.Set("status", string(p.Status))
	}
	if p.Patient != nil {
		q.Set("patient", *p.Patient)
	}
	return q
}

// PatientListParams son los filtros de Patient.List. Los campos en nil no se envían.
type PatientListParams struct {
	ListParams
	// Phone: Filtra por teléfono (cualquier formato).
	Phone      *string
	DNI        *string
	ExternalID *string
}

func (p *PatientListParams) query() url.Values {
	if p == nil {
		return url.Values{}
	}
	q := p.ListParams.query()
	if p.Phone != nil {
		q.Set("phone", *p.Phone)
	}
	if p.DNI != nil {
		q.Set("dni", *p.DNI)
	}
	if p.ExternalID != nil {
		q.Set("externalId", *p.ExternalID)
	}
	return q
}

// TreatmentListParams son los filtros de Treatment.List. Los campos en nil no se envían.
type TreatmentListParams struct {
	ListParams
	Patient *string
	Active  *bool
}

func (p *TreatmentListParams) query() url.Values {
	if p == nil {
		return url.Values{}
	}
	q := p.ListParams.query()
	if p.Patient != nil {
		q.Set("patient", *p.Patient)
	}
	if p.Active != nil {
		q.Set("active", boolString(*p.Active))
	}
	return q
}

// VisitListParams son los filtros de Visit.List. Los campos en nil no se envían.
type VisitListParams struct {
	ListParams
	// Patient: Id del paciente (`pat_…`).
	Patient    *string
	ExternalID *string
}

func (p *VisitListParams) query() url.Values {
	if p == nil {
		return url.Values{}
	}
	q := p.ListParams.query()
	if p.Patient != nil {
		q.Set("patient", *p.Patient)
	}
	if p.ExternalID != nil {
		q.Set("externalId", *p.ExternalID)
	}
	return q
}

// WebhookDeliveryListParams son los filtros de WebhookDelivery.List. Los campos en nil no se envían.
type WebhookDeliveryListParams struct {
	ListParams
	// WebhookEndpoint: Id del endpoint (`we_…`).
	WebhookEndpoint *string
	Status          WebhookDeliveryStatus
}

func (p *WebhookDeliveryListParams) query() url.Values {
	if p == nil {
		return url.Values{}
	}
	q := p.ListParams.query()
	if p.WebhookEndpoint != nil {
		q.Set("webhookEndpoint", *p.WebhookEndpoint)
	}
	if p.Status != "" {
		q.Set("status", string(p.Status))
	}
	return q
}

// WebhookEndpointListParams son los filtros de WebhookEndpoint.List. Los campos en nil no se envían.
type WebhookEndpointListParams struct {
	ListParams
}

func (p *WebhookEndpointListParams) query() url.Values {
	if p == nil {
		return url.Values{}
	}
	q := p.ListParams.query()
	return q
}
