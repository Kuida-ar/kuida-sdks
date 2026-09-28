package kuida

import (
	"context"
	"errors"
	"net/http"
)

// ─── account ─────────────────────────────────────────────────────────────────

// AccountService expone la organización dueña de la clave de API.
type AccountService struct{ b *backend }

// Retrieve devuelve la organización, sus líneas de servicio y los scopes de la clave.
func (s *AccountService) Retrieve(ctx context.Context, opts ...RequestOption) (*Account, error) {
	return request[Account](ctx, s.b, http.MethodGet, "/v1/account", nil, opts)
}

// ─── patients ────────────────────────────────────────────────────────────────

// PatientService administra los pacientes de la organización.
type PatientService struct{ b *backend }

// Create crea un paciente o, si ya existe uno con ese teléfono o DNI, lo
// identifica y completa los datos que le falten.
func (s *PatientService) Create(ctx context.Context, params *PatientCreateParams, opts ...RequestOption) (*Patient, error) {
	return request[Patient](ctx, s.b, http.MethodPost, "/v1/patients", params, opts)
}

// Retrieve devuelve un paciente por su id ("pat_…").
func (s *PatientService) Retrieve(ctx context.Context, id string, opts ...RequestOption) (*Patient, error) {
	path, err := resourcePath("/v1/patients", id)
	if err != nil {
		return nil, err
	}
	return request[Patient](ctx, s.b, http.MethodGet, path, nil, opts)
}

// Update modifica los datos de un paciente. Solo se envían los campos no nil.
func (s *PatientService) Update(ctx context.Context, id string, params *PatientUpdateParams, opts ...RequestOption) (*Patient, error) {
	path, err := resourcePath("/v1/patients", id)
	if err != nil {
		return nil, err
	}
	return request[Patient](ctx, s.b, http.MethodPatch, path, params, opts)
}

// List recorre los pacientes, del más nuevo al más viejo, pidiendo páginas a
// medida que hacen falta.
func (s *PatientService) List(ctx context.Context, params *PatientListParams, opts ...RequestOption) *Iter[*Patient] {
	return newIter(ctx, s.b, "/v1/patients", params.query(), opts, func(p *Patient) string { return p.ID })
}

// ListPage pide una sola página de pacientes.
func (s *PatientService) ListPage(ctx context.Context, params *PatientListParams, opts ...RequestOption) (*PatientList, error) {
	return listPage[*Patient](ctx, s.b, "/v1/patients", params.query(), opts)
}

// ─── doctors ─────────────────────────────────────────────────────────────────

// DoctorService consulta el padrón de profesionales de la organización.
type DoctorService struct{ b *backend }

// List recorre los profesionales.
func (s *DoctorService) List(ctx context.Context, params *DoctorListParams, opts ...RequestOption) *Iter[*Doctor] {
	return newIter(ctx, s.b, "/v1/doctors", params.query(), opts, func(d *Doctor) string { return d.ID })
}

// ListPage pide una sola página de profesionales.
func (s *DoctorService) ListPage(ctx context.Context, params *DoctorListParams, opts ...RequestOption) (*DoctorList, error) {
	return listPage[*Doctor](ctx, s.b, "/v1/doctors", params.query(), opts)
}

// Retrieve devuelve un profesional por su id ("doc_…").
func (s *DoctorService) Retrieve(ctx context.Context, id string, opts ...RequestOption) (*Doctor, error) {
	path, err := resourcePath("/v1/doctors", id)
	if err != nil {
		return nil, err
	}
	return request[Doctor](ctx, s.b, http.MethodGet, path, nil, opts)
}

// ─── appointments ────────────────────────────────────────────────────────────

// AppointmentService administra los turnos. Kuida le manda al paciente los
// recordatorios que la organización configuró.
type AppointmentService struct{ b *backend }

// Create crea un turno. Si ya existe uno con el mismo ExternalID, devuelve ese.
func (s *AppointmentService) Create(ctx context.Context, params *AppointmentCreateParams, opts ...RequestOption) (*Appointment, error) {
	return request[Appointment](ctx, s.b, http.MethodPost, "/v1/appointments", params, opts)
}

// Retrieve devuelve un turno por su id ("apt_…").
func (s *AppointmentService) Retrieve(ctx context.Context, id string, opts ...RequestOption) (*Appointment, error) {
	path, err := resourcePath("/v1/appointments", id)
	if err != nil {
		return nil, err
	}
	return request[Appointment](ctx, s.b, http.MethodGet, path, nil, opts)
}

// Update reprograma o modifica un turno. Un turno cancelado o atendido no se
// puede cambiar.
func (s *AppointmentService) Update(ctx context.Context, id string, params *AppointmentUpdateParams, opts ...RequestOption) (*Appointment, error) {
	path, err := resourcePath("/v1/appointments", id)
	if err != nil {
		return nil, err
	}
	return request[Appointment](ctx, s.b, http.MethodPatch, path, params, opts)
}

// Cancel cancela un turno. params puede ser nil.
func (s *AppointmentService) Cancel(ctx context.Context, id string, params *AppointmentCancelParams, opts ...RequestOption) (*Appointment, error) {
	path, err := resourcePath("/v1/appointments", id, "cancel")
	if err != nil {
		return nil, err
	}
	var body any
	if params != nil {
		body = params
	}
	return request[Appointment](ctx, s.b, http.MethodPost, path, body, opts)
}

// List recorre los turnos.
func (s *AppointmentService) List(ctx context.Context, params *AppointmentListParams, opts ...RequestOption) *Iter[*Appointment] {
	return newIter(ctx, s.b, "/v1/appointments", params.query(), opts, func(a *Appointment) string { return a.ID })
}

// ListPage pide una sola página de turnos.
func (s *AppointmentService) ListPage(ctx context.Context, params *AppointmentListParams, opts ...RequestOption) (*AppointmentList, error) {
	return listPage[*Appointment](ctx, s.b, "/v1/appointments", params.query(), opts)
}

// ─── visits ──────────────────────────────────────────────────────────────────

// VisitService registra las consultas atendidas.
type VisitService struct{ b *backend }

// Create registra una consulta. Si Appointment apunta a un turno (por id o
// por su externalId), el turno queda como atendido.
func (s *VisitService) Create(ctx context.Context, params *VisitCreateParams, opts ...RequestOption) (*Visit, error) {
	return request[Visit](ctx, s.b, http.MethodPost, "/v1/visits", params, opts)
}

// Retrieve devuelve una consulta por su id ("vis_…").
func (s *VisitService) Retrieve(ctx context.Context, id string, opts ...RequestOption) (*Visit, error) {
	path, err := resourcePath("/v1/visits", id)
	if err != nil {
		return nil, err
	}
	return request[Visit](ctx, s.b, http.MethodGet, path, nil, opts)
}

// List recorre las consultas.
func (s *VisitService) List(ctx context.Context, params *VisitListParams, opts ...RequestOption) *Iter[*Visit] {
	return newIter(ctx, s.b, "/v1/visits", params.query(), opts, func(v *Visit) string { return v.ID })
}

// ListPage pide una sola página de consultas.
func (s *VisitService) ListPage(ctx context.Context, params *VisitListParams, opts ...RequestOption) (*VisitList, error) {
	return listPage[*Visit](ctx, s.b, "/v1/visits", params.query(), opts)
}

// ─── intake requests ─────────────────────────────────────────────────────────

// IntakeRequestService administra las solicitudes de ingreso: alguien pide el
// servicio para un paciente y Kuida completa lo que falta por WhatsApp.
type IntakeRequestService struct{ b *backend }

// Create crea una solicitud de ingreso. Hace falta al menos un dato del
// paciente o de un referente.
func (s *IntakeRequestService) Create(ctx context.Context, params *IntakeRequestCreateParams, opts ...RequestOption) (*IntakeRequest, error) {
	return request[IntakeRequest](ctx, s.b, http.MethodPost, "/v1/intake_requests", params, opts)
}

// Retrieve devuelve una solicitud por su id ("int_…").
func (s *IntakeRequestService) Retrieve(ctx context.Context, id string, opts ...RequestOption) (*IntakeRequest, error) {
	path, err := resourcePath("/v1/intake_requests", id)
	if err != nil {
		return nil, err
	}
	return request[IntakeRequest](ctx, s.b, http.MethodGet, path, nil, opts)
}

// List recorre las solicitudes de ingreso.
func (s *IntakeRequestService) List(ctx context.Context, params *IntakeRequestListParams, opts ...RequestOption) *Iter[*IntakeRequest] {
	return newIter(ctx, s.b, "/v1/intake_requests", params.query(), opts, func(r *IntakeRequest) string { return r.ID })
}

// ListPage pide una sola página de solicitudes de ingreso.
func (s *IntakeRequestService) ListPage(ctx context.Context, params *IntakeRequestListParams, opts ...RequestOption) (*IntakeRequestList, error) {
	return listPage[*IntakeRequest](ctx, s.b, "/v1/intake_requests", params.query(), opts)
}

// ─── treatments ──────────────────────────────────────────────────────────────

// TreatmentService administra tratamientos e indicaciones de los pacientes.
type TreatmentService struct{ b *backend }

// Create registra un tratamiento para un paciente.
func (s *TreatmentService) Create(ctx context.Context, params *TreatmentCreateParams, opts ...RequestOption) (*Treatment, error) {
	return request[Treatment](ctx, s.b, http.MethodPost, "/v1/treatments", params, opts)
}

// Retrieve devuelve un tratamiento por su id ("trt_…").
func (s *TreatmentService) Retrieve(ctx context.Context, id string, opts ...RequestOption) (*Treatment, error) {
	path, err := resourcePath("/v1/treatments", id)
	if err != nil {
		return nil, err
	}
	return request[Treatment](ctx, s.b, http.MethodGet, path, nil, opts)
}

// List recorre los tratamientos.
func (s *TreatmentService) List(ctx context.Context, params *TreatmentListParams, opts ...RequestOption) *Iter[*Treatment] {
	return newIter(ctx, s.b, "/v1/treatments", params.query(), opts, func(t *Treatment) string { return t.ID })
}

// ListPage pide una sola página de tratamientos.
func (s *TreatmentService) ListPage(ctx context.Context, params *TreatmentListParams, opts ...RequestOption) (*TreatmentList, error) {
	return listPage[*Treatment](ctx, s.b, "/v1/treatments", params.query(), opts)
}

// ─── events ──────────────────────────────────────────────────────────────────

// EventService envía eventos del catálogo v1 (la puerta genérica) y consulta
// los que entraron.
type EventService struct{ b *backend }

// Create envía un solo evento. Devuelve el resultado en un EventBatchResponse.
//
// Si el evento no entra, la API responde con error (400 invalid_request_error
// si no pasa la validación de esquema) y el SDK devuelve un *Error; el
// detalle por evento queda en Error.Body (campo "results").
func (s *EventService) Create(ctx context.Context, event *EventInput, opts ...RequestOption) (*EventBatchResponse, error) {
	if event == nil {
		return nil, errors.New("kuida: el evento no puede ser nil")
	}
	return s.post(ctx, event, opts)
}

// CreateBatch envía un lote de hasta 100 eventos. Cada evento se procesa por
// separado y trae su propio resultado, en el mismo orden.
//
// Si al menos uno entra, la respuesta es 200 y los rechazados vienen con
// Status "invalid". Si no entra ninguno, la API responde con error (400, o
// 500 si fallaron al procesarse) y el SDK devuelve un *Error; los resultados
// por evento quedan en Error.Body (campo "results").
func (s *EventService) CreateBatch(ctx context.Context, events []*EventInput, opts ...RequestOption) (*EventBatchResponse, error) {
	if len(events) == 0 {
		return nil, errors.New("kuida: el lote de eventos está vacío")
	}
	return s.post(ctx, struct {
		Events []*EventInput `json:"events"`
	}{events}, opts)
}

func (s *EventService) post(ctx context.Context, body any, opts []RequestOption) (*EventBatchResponse, error) {
	return request[EventBatchResponse](ctx, s.b, http.MethodPost, "/v1/events", body, opts)
}

// Retrieve devuelve un evento por su id ("evt_…").
func (s *EventService) Retrieve(ctx context.Context, id string, opts ...RequestOption) (*Event, error) {
	path, err := resourcePath("/v1/events", id)
	if err != nil {
		return nil, err
	}
	return request[Event](ctx, s.b, http.MethodGet, path, nil, opts)
}

// List recorre los eventos.
func (s *EventService) List(ctx context.Context, params *EventListParams, opts ...RequestOption) *Iter[*Event] {
	return newIter(ctx, s.b, "/v1/events", params.query(), opts, func(e *Event) string { return e.ID })
}

// ListPage pide una sola página de eventos.
func (s *EventService) ListPage(ctx context.Context, params *EventListParams, opts ...RequestOption) (*EventList, error) {
	return listPage[*Event](ctx, s.b, "/v1/events", params.query(), opts)
}

// ─── webhook endpoints ───────────────────────────────────────────────────────

// WebhookEndpointService administra los endpoints que reciben webhooks.
type WebhookEndpointService struct{ b *backend }

// Create registra un endpoint. La respuesta trae Secret, el secreto de firma,
// que no se vuelve a mostrar: hay que guardarlo.
func (s *WebhookEndpointService) Create(ctx context.Context, params *WebhookEndpointCreateParams, opts ...RequestOption) (*WebhookEndpoint, error) {
	return request[WebhookEndpoint](ctx, s.b, http.MethodPost, "/v1/webhook_endpoints", params, opts)
}

// Retrieve devuelve un endpoint por su id ("we_…"), sin el secreto.
func (s *WebhookEndpointService) Retrieve(ctx context.Context, id string, opts ...RequestOption) (*WebhookEndpoint, error) {
	path, err := resourcePath("/v1/webhook_endpoints", id)
	if err != nil {
		return nil, err
	}
	return request[WebhookEndpoint](ctx, s.b, http.MethodGet, path, nil, opts)
}

// Update modifica un endpoint (URL, eventos, descripción o pausa).
func (s *WebhookEndpointService) Update(ctx context.Context, id string, params *WebhookEndpointUpdateParams, opts ...RequestOption) (*WebhookEndpoint, error) {
	path, err := resourcePath("/v1/webhook_endpoints", id)
	if err != nil {
		return nil, err
	}
	return request[WebhookEndpoint](ctx, s.b, http.MethodPatch, path, params, opts)
}

// Delete borra un endpoint y sus envíos pendientes.
func (s *WebhookEndpointService) Delete(ctx context.Context, id string, opts ...RequestOption) (*DeletedObject, error) {
	path, err := resourcePath("/v1/webhook_endpoints", id)
	if err != nil {
		return nil, err
	}
	return request[DeletedObject](ctx, s.b, http.MethodDelete, path, nil, opts)
}

// List recorre los endpoints.
func (s *WebhookEndpointService) List(ctx context.Context, params *ListParams, opts ...RequestOption) *Iter[*WebhookEndpoint] {
	return newIter(ctx, s.b, "/v1/webhook_endpoints", params.query(), opts, func(w *WebhookEndpoint) string { return w.ID })
}

// ListPage pide una sola página de endpoints.
func (s *WebhookEndpointService) ListPage(ctx context.Context, params *ListParams, opts ...RequestOption) (*WebhookEndpointList, error) {
	return listPage[*WebhookEndpoint](ctx, s.b, "/v1/webhook_endpoints", params.query(), opts)
}

// Ping encola un evento webhook.ping hacia el endpoint y devuelve el envío.
func (s *WebhookEndpointService) Ping(ctx context.Context, id string, opts ...RequestOption) (*WebhookDelivery, error) {
	path, err := resourcePath("/v1/webhook_endpoints", id, "ping")
	if err != nil {
		return nil, err
	}
	return request[WebhookDelivery](ctx, s.b, http.MethodPost, path, nil, opts)
}

// ─── webhook deliveries ──────────────────────────────────────────────────────

// WebhookDeliveryService consulta los envíos de webhooks y sus reintentos.
type WebhookDeliveryService struct{ b *backend }

// List recorre los envíos.
func (s *WebhookDeliveryService) List(ctx context.Context, params *WebhookDeliveryListParams, opts ...RequestOption) *Iter[*WebhookDelivery] {
	return newIter(ctx, s.b, "/v1/webhook_deliveries", params.query(), opts, func(d *WebhookDelivery) string { return d.ID })
}

// ListPage pide una sola página de envíos.
func (s *WebhookDeliveryService) ListPage(ctx context.Context, params *WebhookDeliveryListParams, opts ...RequestOption) (*WebhookDeliveryList, error) {
	return listPage[*WebhookDelivery](ctx, s.b, "/v1/webhook_deliveries", params.query(), opts)
}

// Retrieve devuelve un envío por su id ("whd_…").
func (s *WebhookDeliveryService) Retrieve(ctx context.Context, id string, opts ...RequestOption) (*WebhookDelivery, error) {
	path, err := resourcePath("/v1/webhook_deliveries", id)
	if err != nil {
		return nil, err
	}
	return request[WebhookDelivery](ctx, s.b, http.MethodGet, path, nil, opts)
}
