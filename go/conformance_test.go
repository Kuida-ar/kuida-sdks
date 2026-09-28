package kuida_test

// Suite de conformidad (SDK_DESIGN.md §9). Corre contra el mock:
//
//	PORT=12126 node ../conformance/mock-server.mjs &
//	KUIDA_API_BASE=http://localhost:12126/api go test ./...
//
// Sin KUIDA_API_BASE los escenarios que usan el mock se saltean; el de firma
// de webhooks corre siempre.

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"os"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"

	kuida "github.com/Kuida-ar/kuida-sdks/go"
	"github.com/Kuida-ar/kuida-sdks/go/webhook"
)

const (
	keyAll       = "kd_test_000000000000_mocksecretmocksecret00"
	keyEventOnly = "kd_test_1e9ac7000000_mocksecretmocksecret00"
	keyRevoked   = "kd_test_dead00000000_mocksecretmocksecret00"
	keyFlaky     = "kd_test_fa11ed000000_mocksecretmocksecret00"
	keyRateLimit = "kd_test_42900000000a_mocksecretmocksecret00"

	demoPhone = "+54 9 342 555 0000"
)

var uuidV4 = regexp.MustCompile(`^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`)

// ─── utilidades ──────────────────────────────────────────────────────────────

func apiBase(t *testing.T) string {
	t.Helper()
	base := os.Getenv("KUIDA_API_BASE")
	if base == "" {
		t.Skip("KUIDA_API_BASE no está definida; levantar el mock de conformance/ para correr este escenario")
	}
	return strings.TrimRight(base, "/")
}

// mockRoot es la raíz del mock (sin /api), donde viven /__mock/*.
func mockRoot(t *testing.T) string {
	return strings.TrimSuffix(apiBase(t), "/api")
}

// sleeps registra las esperas pedidas por los reintentos sin esperar de verdad.
type sleeps struct {
	mu sync.Mutex
	ds []time.Duration
}

func (s *sleeps) fn(_ context.Context, d time.Duration) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.ds = append(s.ds, d)
	return nil
}

func newClient(t *testing.T, key string, opts ...kuida.ClientOption) *kuida.Client {
	t.Helper()
	all := append([]kuida.ClientOption{
		kuida.WithBaseURL(apiBase(t)),
		kuida.WithRetrySleep((&sleeps{}).fn),
	}, opts...)
	c, err := kuida.NewClient(key, all...)
	if err != nil {
		t.Fatalf("NewClient: %v", err)
	}
	return c
}

func resetMock(t *testing.T) {
	t.Helper()
	res, err := http.Post(mockRoot(t)+"/__mock/reset", "application/json", nil)
	if err != nil {
		t.Fatalf("reset del mock: %v", err)
	}
	res.Body.Close()
}

type loggedRequest struct {
	Method  string            `json:"method"`
	Path    string            `json:"path"`
	Query   map[string]any    `json:"query"`
	Body    json.RawMessage   `json:"body"`
	Headers map[string]string `json:"headers"`
}

func mockRequests(t *testing.T) []loggedRequest {
	t.Helper()
	res, err := http.Get(mockRoot(t) + "/__mock/requests")
	if err != nil {
		t.Fatalf("leyendo /__mock/requests: %v", err)
	}
	defer res.Body.Close()
	var out struct {
		Requests []loggedRequest `json:"requests"`
	}
	if err := json.NewDecoder(res.Body).Decode(&out); err != nil {
		t.Fatalf("decodificando /__mock/requests: %v", err)
	}
	return out.Requests
}

// must corta el test si la llamada devolvió error: must(c.Account.Retrieve(ctx))(t).
func must[T any](v T, err error) func(t *testing.T) T {
	return func(t *testing.T) T {
		t.Helper()
		if err != nil {
			t.Fatalf("error inesperado: %v", err)
		}
		return v
	}
}

func asKuidaError(t *testing.T, err error) *kuida.Error {
	t.Helper()
	var kerr *kuida.Error
	if !errors.As(err, &kerr) {
		t.Fatalf("se esperaba *kuida.Error, llegó %T: %v", err, err)
	}
	return kerr
}

// ─── 1. account_retrieve ─────────────────────────────────────────────────────

func TestAccountRetrieve(t *testing.T) {
	c := newClient(t, keyAll)
	acct := must(c.Account.Retrieve(context.Background()))(t)
	if acct.Object != "account" {
		t.Errorf("object = %q, se esperaba account", acct.Object)
	}
	if acct.APIKey == nil || len(acct.APIKey.Scopes) == 0 {
		t.Fatalf("apiKey.scopes vacío: %+v", acct.APIKey)
	}
	if acct.LastResponse == nil || acct.LastResponse.StatusCode != 200 || acct.LastResponse.RequestID == "" {
		t.Errorf("LastResponse incompleto: %+v", acct.LastResponse)
	}
	if len(acct.Raw()) == 0 {
		t.Error("Raw() vacío")
	}
}

// ─── 2. headers ──────────────────────────────────────────────────────────────

func TestHeaders(t *testing.T) {
	c := newClient(t, keyAll)
	resetMock(t)
	ctx := context.Background()
	must(c.Account.Retrieve(ctx))(t)
	must(c.Patients.Create(ctx, &kuida.PatientCreateParams{Phone: demoPhone}))(t)
	must(c.Patients.Create(ctx, &kuida.PatientCreateParams{Phone: "+54 9 342 555 0001"}))(t)

	reqs := mockRequests(t)
	if len(reqs) != 3 {
		t.Fatalf("se esperaban 3 pedidos, hubo %d", len(reqs))
	}
	uaRe := regexp.MustCompile(`^Kuida/v1 GoBindings/\d+\.\d+\.\d+$`)
	keys := map[string]bool{}
	for _, r := range reqs {
		h := r.Headers
		if h["authorization"] != "Bearer "+keyAll {
			t.Errorf("%s %s: Authorization = %q", r.Method, r.Path, h["authorization"])
		}
		if h["kuida-version"] != "2026-09-28" {
			t.Errorf("%s %s: Kuida-Version = %q", r.Method, r.Path, h["kuida-version"])
		}
		if !uaRe.MatchString(h["user-agent"]) {
			t.Errorf("%s %s: User-Agent = %q", r.Method, r.Path, h["user-agent"])
		}
		var cua map[string]string
		if err := json.Unmarshal([]byte(h["x-kuida-client-user-agent"]), &cua); err != nil || cua["lang"] != "go" || cua["bindings_version"] != kuida.Version {
			t.Errorf("%s %s: X-Kuida-Client-User-Agent = %q", r.Method, r.Path, h["x-kuida-client-user-agent"])
		}
		switch r.Method {
		case http.MethodPost:
			if !uuidV4.MatchString(h["idempotency-key"]) {
				t.Errorf("POST %s: Idempotency-Key no es UUID v4: %q", r.Path, h["idempotency-key"])
			}
			if h["content-type"] != "application/json" {
				t.Errorf("POST %s: Content-Type = %q", r.Path, h["content-type"])
			}
			keys[h["idempotency-key"]] = true
		case http.MethodGet:
			if h["idempotency-key"] != "" {
				t.Errorf("GET %s lleva Idempotency-Key %q", r.Path, h["idempotency-key"])
			}
		}
	}
	if len(keys) != 2 {
		t.Errorf("cada POST tiene que tener su propia Idempotency-Key; hubo %d distintas", len(keys))
	}
}

// ─── 3. patients_crud ────────────────────────────────────────────────────────

func TestPatientsCRUD(t *testing.T) {
	c := newClient(t, keyAll)
	resetMock(t)
	ctx := context.Background()

	created := must(c.Patients.Create(ctx, &kuida.PatientCreateParams{
		Phone:    demoPhone,
		FullName: kuida.String("Paciente Demo"),
		DNI:      kuida.String("11111111"),
	}))(t)
	if !strings.HasPrefix(created.ID, "pat_") || created.Object != "patient" {
		t.Fatalf("paciente creado inválido: %+v", created)
	}
	if created.FullName == nil || *created.FullName != "Paciente Demo" {
		t.Errorf("fullName = %v", created.FullName)
	}
	if created.CreatedAt.IsZero() {
		t.Error("createdAt no se parseó como time.Time")
	}

	got := must(c.Patients.Retrieve(ctx, created.ID))(t)
	if got.ID != created.ID {
		t.Errorf("retrieve devolvió %s, se esperaba %s", got.ID, created.ID)
	}

	updated := must(c.Patients.Update(ctx, created.ID, &kuida.PatientUpdateParams{Email: kuida.String("paciente.demo@example.com")}))(t)
	if updated.Email == nil || *updated.Email != "paciente.demo@example.com" {
		t.Errorf("email = %v", updated.Email)
	}

	page := must(c.Patients.ListPage(ctx, &kuida.PatientListParams{Phone: kuida.String("3425550000")}))(t)
	if len(page.Data) != 1 || page.Data[0].ID != created.ID {
		t.Fatalf("list por phone devolvió %d pacientes", len(page.Data))
	}
	reqs := mockRequests(t)
	last := reqs[len(reqs)-1]
	if last.Method != http.MethodGet || last.Path != "/v1/patients" || last.Query["phone"] != "3425550000" {
		t.Errorf("list mandó %s %s %v", last.Method, last.Path, last.Query)
	}
}

// ─── 4. idempotency_explicit ─────────────────────────────────────────────────

func TestIdempotencyExplicit(t *testing.T) {
	c := newClient(t, keyAll)
	resetMock(t)
	ctx := context.Background()
	key := fmt.Sprintf("conformance-go-%d", time.Now().UnixNano())

	a := must(c.Patients.Create(ctx, &kuida.PatientCreateParams{Phone: demoPhone}, kuida.WithIdempotencyKey(key)))(t)
	b := must(c.Patients.Create(ctx, &kuida.PatientCreateParams{Phone: demoPhone}, kuida.WithIdempotencyKey(key)))(t)
	if a.ID != b.ID {
		t.Errorf("misma clave, ids distintos: %s y %s", a.ID, b.ID)
	}
	if !b.LastResponse.IdempotentReplayed || b.LastResponse.IdempotencyKey != key {
		t.Errorf("la segunda respuesta tendría que ser un replay con la clave %q: %+v", key, b.LastResponse)
	}

	_, err := c.Patients.Create(ctx, &kuida.PatientCreateParams{Phone: "+54 9 342 555 0009"}, kuida.WithIdempotencyKey(key))
	if !errors.Is(err, kuida.ErrIdempotency) {
		t.Fatalf("se esperaba ErrIdempotency, llegó %v", err)
	}
	if kerr := asKuidaError(t, err); kerr.Type != kuida.ErrorTypeIdempotency || kerr.Code != kuida.ErrorCodeIdempotencyKeyReused {
		t.Errorf("type/code = %s/%s", kerr.Type, kerr.Code)
	}
}

// ─── 5. pagination_manual ────────────────────────────────────────────────────

func createPatients(t *testing.T, c *kuida.Client, n int) map[string]bool {
	t.Helper()
	ids := map[string]bool{}
	for i := 0; i < n; i++ {
		p := must(c.Patients.Create(context.Background(), &kuida.PatientCreateParams{
			Phone:    fmt.Sprintf("+54 9 342 555 %04d", i+10),
			FullName: kuida.String(fmt.Sprintf("Paciente Demo %d", i+1)),
		}))(t)
		ids[p.ID] = true
	}
	return ids
}

func TestPaginationManual(t *testing.T) {
	c := newClient(t, keyAll)
	resetMock(t)
	ctx := context.Background()
	ids := createPatients(t, c, 5)

	first := must(c.Patients.ListPage(ctx, &kuida.PatientListParams{ListParams: kuida.ListParams{Limit: kuida.Int64(2)}}))(t)
	if len(first.Data) != 2 || !first.HasMore || first.Object != "list" {
		t.Fatalf("primera página: %d objetos, hasMore=%v", len(first.Data), first.HasMore)
	}
	second := must(c.Patients.ListPage(ctx, &kuida.PatientListParams{ListParams: kuida.ListParams{
		Limit:         kuida.Int64(2),
		StartingAfter: kuida.String(first.Data[1].ID),
	}}))(t)
	if len(second.Data) != 2 {
		t.Fatalf("segunda página: %d objetos", len(second.Data))
	}
	seen := map[string]bool{}
	for _, p := range append(first.Data, second.Data...) {
		if seen[p.ID] {
			t.Errorf("id repetido entre páginas: %s", p.ID)
		}
		if !ids[p.ID] {
			t.Errorf("id desconocido: %s", p.ID)
		}
		seen[p.ID] = true
	}
}

// ─── 6. pagination_auto ──────────────────────────────────────────────────────

func TestPaginationAuto(t *testing.T) {
	c := newClient(t, keyAll)
	resetMock(t)
	ids := createPatients(t, c, 5)

	it := c.Patients.List(context.Background(), &kuida.PatientListParams{ListParams: kuida.ListParams{Limit: kuida.Int64(2)}})
	seen := map[string]bool{}
	for it.Next() {
		p := it.Current()
		if seen[p.ID] {
			t.Errorf("el iterador repitió %s", p.ID)
		}
		seen[p.ID] = true
	}
	if err := it.Err(); err != nil {
		t.Fatalf("iterador: %v", err)
	}
	if len(seen) != len(ids) {
		t.Fatalf("el iterador recorrió %d pacientes, se esperaban %d", len(seen), len(ids))
	}

	var gets []loggedRequest
	for _, r := range mockRequests(t) {
		if r.Method == http.MethodGet && r.Path == "/v1/patients" {
			gets = append(gets, r)
		}
	}
	if len(gets) != 3 {
		t.Fatalf("se esperaban 3 páginas de a 2, hubo %d pedidos", len(gets))
	}
	for i, r := range gets {
		if r.Query["limit"] != "2" {
			t.Errorf("página %d: limit = %v", i+1, r.Query["limit"])
		}
		if (i == 0) != (r.Query["startingAfter"] == nil) {
			t.Errorf("página %d: startingAfter = %v", i+1, r.Query["startingAfter"])
		}
	}
}

// ─── 7. appointments_flow ────────────────────────────────────────────────────

func TestAppointmentsFlow(t *testing.T) {
	c := newClient(t, keyAll)
	resetMock(t)
	ctx := context.Background()
	art := time.FixedZone("ART", -3*3600)
	start := time.Date(2026, 10, 1, 14, 30, 0, 0, art)

	apt := must(c.Appointments.Create(ctx, &kuida.AppointmentCreateParams{
		Patient: kuida.PatientByIdentity(kuida.PatientIdentity{Phone: kuida.String(demoPhone), FullName: kuida.String("Paciente Demo")}),
		Doctor:  kuida.DoctorByIdentity(kuida.DoctorIdentity{ExternalID: kuida.String("prof-demo-1"), FullName: kuida.String("Profesional Demo")}),
		StartAt: start,
		Type:    kuida.String("Control"),
	}))(t)
	if !strings.HasPrefix(apt.ID, "apt_") || !strings.HasPrefix(apt.Patient, "pat_") || apt.Doctor == nil || !strings.HasPrefix(*apt.Doctor, "doc_") {
		t.Fatalf("turno creado inválido: %+v", apt)
	}
	if !apt.StartAt.Equal(start) {
		t.Errorf("startAt = %v, se esperaba %v", apt.StartAt, start)
	}

	later := start.Add(24 * time.Hour)
	apt = must(c.Appointments.Update(ctx, apt.ID, &kuida.AppointmentUpdateParams{StartAt: kuida.Time(later)}))(t)
	if !apt.StartAt.Equal(later) {
		t.Errorf("startAt tras update = %v", apt.StartAt)
	}

	apt = must(c.Appointments.Cancel(ctx, apt.ID, nil))(t)
	if apt.Status != kuida.AppointmentStatusCancelled || apt.CancelledAt == nil {
		t.Errorf("status = %s, cancelledAt = %v", apt.Status, apt.CancelledAt)
	}
	reqs := mockRequests(t)
	if cancel := reqs[len(reqs)-1]; cancel.Path != "/v1/appointments/"+apt.ID+"/cancel" || string(cancel.Body) != "{}" {
		t.Errorf("cancel sin parámetros mandó %s con cuerpo %s", cancel.Path, cancel.Body)
	}

	_, err := c.Appointments.Update(ctx, apt.ID, &kuida.AppointmentUpdateParams{StartAt: kuida.Time(later.Add(time.Hour))})
	if !errors.Is(err, kuida.ErrInvalidRequest) {
		t.Fatalf("update de un turno cancelado: se esperaba ErrInvalidRequest, llegó %v", err)
	}
}

// ─── 8. visit_closes_appointment ─────────────────────────────────────────────

func TestVisitClosesAppointment(t *testing.T) {
	c := newClient(t, keyAll)
	resetMock(t)
	ctx := context.Background()
	start := time.Now().Add(-time.Hour).Truncate(time.Second)

	apt := must(c.Appointments.Create(ctx, &kuida.AppointmentCreateParams{
		Patient:    kuida.PatientByIdentity(kuida.PatientIdentity{Phone: kuida.String(demoPhone)}),
		StartAt:    start,
		ExternalID: kuida.String("turno-demo-1"),
	}))(t)
	visit := must(c.Visits.Create(ctx, &kuida.VisitCreateParams{
		Patient:     kuida.PatientID(apt.Patient),
		VisitedAt:   start.Add(20 * time.Minute),
		Appointment: kuida.String("turno-demo-1"),
		ExternalID:  kuida.String("consulta-demo-1"),
	}))(t)
	if visit.Appointment == nil || *visit.Appointment != apt.ID {
		t.Fatalf("visit.appointment = %v, se esperaba %s", visit.Appointment, apt.ID)
	}
	apt = must(c.Appointments.Retrieve(ctx, apt.ID))(t)
	if apt.Status != kuida.AppointmentStatusCompleted {
		t.Errorf("el turno quedó %s, se esperaba completed", apt.Status)
	}
}

// ─── 9. intake_and_treatment ─────────────────────────────────────────────────

func TestIntakeAndTreatment(t *testing.T) {
	c := newClient(t, keyAll)
	resetMock(t)
	ctx := context.Background()

	intake := must(c.IntakeRequests.Create(ctx, &kuida.IntakeRequestCreateParams{
		Patient:          &kuida.PatientIdentity{Phone: kuida.String(demoPhone), FullName: kuida.String("Paciente Demo"), DNI: kuida.String("11111111")},
		Coverage:         &kuida.IntakeCoverage{Insurer: kuida.String("Obra Social Demo")},
		RequestedService: kuida.String("Internación domiciliaria"),
	}))(t)
	if !strings.HasPrefix(intake.ID, "int_") || intake.Object != "intake_request" || intake.Patient == nil {
		t.Fatalf("solicitud inválida: %+v", intake)
	}
	if intake.Data == nil {
		t.Error("data vacío en la solicitud")
	}
	got := must(c.IntakeRequests.Retrieve(ctx, intake.ID))(t)
	if got.ID != intake.ID {
		t.Errorf("retrieve devolvió %s", got.ID)
	}

	trt := must(c.Treatments.Create(ctx, &kuida.TreatmentCreateParams{
		Patient:   kuida.PatientID(*intake.Patient),
		Name:      "Enalapril 10 mg",
		Kind:      kuida.TreatmentKindMedication,
		Frequency: kuida.String("cada 12 horas"),
	}))(t)
	if !strings.HasPrefix(trt.ID, "trt_") || trt.Patient != *intake.Patient || trt.Kind != kuida.TreatmentKindMedication {
		t.Fatalf("tratamiento inválido: %+v", trt)
	}
	reqs := mockRequests(t)
	var body map[string]any
	json.Unmarshal(reqs[len(reqs)-1].Body, &body)
	if body["patient"] != *intake.Patient {
		t.Errorf("treatments.create mandó patient = %v, se esperaba el id como texto", body["patient"])
	}
}

// ─── 10. events_batch ────────────────────────────────────────────────────────

func TestEventsBatch(t *testing.T) {
	c := newClient(t, keyAll)
	resetMock(t)
	ctx := context.Background()

	valid := &kuida.EventInput{
		ID:   "pms-evt-demo-1",
		Type: kuida.EventTypePatientUpserted,
		Data: map[string]any{"phone": demoPhone, "fullName": "Paciente Demo", "dni": "11111111"},
	}
	invalid := &kuida.EventInput{ID: "pms-evt-demo-2", Type: "no.existe", Data: map[string]any{}}

	batch := must(c.Events.CreateBatch(ctx, []*kuida.EventInput{valid, invalid}))(t)
	if batch.Object != "event_batch" || len(batch.Results) != 2 {
		t.Fatalf("lote inválido: %+v", batch)
	}
	if batch.Results[0].Status != kuida.EventResultStatusProcessed || batch.Results[1].Status != kuida.EventResultStatusInvalid {
		t.Errorf("estados = %s, %s", batch.Results[0].Status, batch.Results[1].Status)
	}

	dup := must(c.Events.Create(ctx, valid))(t)
	if len(dup.Results) != 1 || dup.Results[0].Status != kuida.EventResultStatusDuplicate {
		t.Errorf("reenvío: %+v", dup.Results)
	}

	// Si no entra ninguno, la API responde con el sobre de error normal.
	_, err := c.Events.CreateBatch(ctx, []*kuida.EventInput{invalid})
	if !errors.Is(err, kuida.ErrInvalidRequest) {
		t.Fatalf("lote todo inválido: se esperaba ErrInvalidRequest, llegó %v", err)
	}
	if kerr := asKuidaError(t, err); !strings.Contains(string(kerr.Body), `"results"`) {
		t.Errorf("el cuerpo crudo del error no trae results: %s", kerr.Body)
	}
}

// ─── 11. webhook_endpoints_flow ──────────────────────────────────────────────

func TestWebhookEndpointsFlow(t *testing.T) {
	c := newClient(t, keyAll)
	resetMock(t)
	ctx := context.Background()

	we := must(c.WebhookEndpoints.Create(ctx, &kuida.WebhookEndpointCreateParams{
		URL:           "https://sistema-demo.example.com/kuida/webhooks",
		EnabledEvents: []kuida.WebhookEventType{kuida.WebhookEventTypeIntakeReady, kuida.WebhookEventTypeConversationHandoff},
		Description:   kuida.String("Sistema de gestión demo"),
	}))(t)
	if !strings.HasPrefix(we.ID, "we_") || !strings.HasPrefix(we.Secret, "whsec_") {
		t.Fatalf("endpoint creado inválido: %+v", we)
	}

	got := must(c.WebhookEndpoints.Retrieve(ctx, we.ID))(t)
	if got.Secret != "" {
		t.Error("retrieve no tendría que traer el secreto")
	}

	upd := must(c.WebhookEndpoints.Update(ctx, we.ID, &kuida.WebhookEndpointUpdateParams{
		Description:   kuida.String("Sistema de gestión demo (producción)"),
		EnabledEvents: []kuida.WebhookEventType{kuida.WebhookEventTypeAll},
	}))(t)
	if upd.Description == nil || *upd.Description != "Sistema de gestión demo (producción)" || len(upd.EnabledEvents) != 1 {
		t.Errorf("update: %+v", upd)
	}

	delivery := must(c.WebhookEndpoints.Ping(ctx, we.ID))(t)
	if delivery.Object != "webhook_delivery" || delivery.Status != kuida.WebhookDeliveryStatusPending || delivery.EventType != "webhook.ping" {
		t.Errorf("ping: %+v", delivery)
	}
	reqs := mockRequests(t)
	if ping := reqs[len(reqs)-1]; string(ping.Body) != "{}" {
		t.Errorf("ping mandó el cuerpo %s, se esperaba {}", ping.Body)
	}

	it := c.WebhookDeliveries.List(ctx, &kuida.WebhookDeliveryListParams{WebhookEndpoint: kuida.String(we.ID)})
	found := false
	for it.Next() {
		if it.Current().ID == delivery.ID {
			found = true
		}
	}
	if it.Err() != nil || !found {
		t.Errorf("webhookDeliveries.list no trajo el ping (err=%v)", it.Err())
	}

	del := must(c.WebhookEndpoints.Delete(ctx, we.ID))(t)
	if !del.Deleted || del.ID != we.ID {
		t.Errorf("delete: %+v", del)
	}
}

// ─── 12. errors ──────────────────────────────────────────────────────────────

func TestErrors(t *testing.T) {
	base := apiBase(t)
	ctx := context.Background()

	t.Run("sin clave", func(t *testing.T) {
		t.Setenv("KUIDA_API_KEY", "")
		if _, err := kuida.NewClient("", kuida.WithBaseURL(base)); !errors.Is(err, kuida.ErrMissingAPIKey) {
			t.Fatalf("se esperaba ErrMissingAPIKey al construir, llegó %v", err)
		}
	})

	t.Run("clave inválida", func(t *testing.T) {
		_, err := newClient(t, "kd_test_invalida").Account.Retrieve(ctx)
		if !errors.Is(err, kuida.ErrAuthentication) {
			t.Fatalf("se esperaba ErrAuthentication, llegó %v", err)
		}
		if kerr := asKuidaError(t, err); kerr.HTTPStatus != 401 || kerr.Code != kuida.ErrorCodeAPIKeyInvalid {
			t.Errorf("status/code = %d/%s", kerr.HTTPStatus, kerr.Code)
		}
	})

	t.Run("clave revocada", func(t *testing.T) {
		_, err := newClient(t, keyRevoked).Account.Retrieve(ctx)
		kerr := asKuidaError(t, err)
		if kerr.Type != kuida.ErrorTypeAuthentication || kerr.Code != kuida.ErrorCodeAPIKeyRevoked {
			t.Errorf("type/code = %s/%s", kerr.Type, kerr.Code)
		}
	})

	t.Run("scope faltante", func(t *testing.T) {
		it := newClient(t, keyEventOnly).Patients.List(ctx, nil)
		if it.Next() {
			t.Fatal("el iterador no tendría que devolver pacientes")
		}
		if !errors.Is(it.Err(), kuida.ErrPermission) {
			t.Fatalf("se esperaba ErrPermission, llegó %v", it.Err())
		}
		if kerr := asKuidaError(t, it.Err()); kerr.HTTPStatus != 403 || kerr.Code != kuida.ErrorCodeScopeMissing {
			t.Errorf("status/code = %d/%s", kerr.HTTPStatus, kerr.Code)
		}
	})

	t.Run("recurso inexistente", func(t *testing.T) {
		_, err := newClient(t, keyAll).Patients.Retrieve(ctx, "pat_noexiste")
		if !errors.Is(err, kuida.ErrInvalidRequest) {
			t.Fatalf("se esperaba ErrInvalidRequest, llegó %v", err)
		}
		kerr := asKuidaError(t, err)
		if kerr.HTTPStatus != 404 || kerr.Code != kuida.ErrorCodeResourceMissing || !strings.HasPrefix(kerr.RequestID, "req_") {
			t.Errorf("status/code/requestId = %d/%s/%q", kerr.HTTPStatus, kerr.Code, kerr.RequestID)
		}
		if kerr.DocURL == "" || kerr.Message == "" || kerr.Headers.Get("Request-Id") == "" || len(kerr.Body) == 0 {
			t.Errorf("faltan datos en el error: %+v", kerr)
		}
	})

	t.Run("parámetro faltante", func(t *testing.T) {
		_, err := newClient(t, keyAll).Patients.Create(ctx, &kuida.PatientCreateParams{FullName: kuida.String("Paciente Demo")})
		kerr := asKuidaError(t, err)
		if kerr.Class() != kuida.ErrorTypeInvalidRequest || kerr.Param != "phone" {
			t.Errorf("type/param = %s/%q", kerr.Type, kerr.Param)
		}
	})
}

// ─── 13. retries_5xx ─────────────────────────────────────────────────────────

func TestRetries5xx(t *testing.T) {
	var s sleeps
	c := newClient(t, keyFlaky, kuida.WithRetrySleep(s.fn))
	resetMock(t)

	p := must(c.Patients.Create(context.Background(), &kuida.PatientCreateParams{Phone: demoPhone}))(t)
	if !strings.HasPrefix(p.ID, "pat_") {
		t.Fatalf("paciente inválido: %+v", p)
	}
	var posts []loggedRequest
	for _, r := range mockRequests(t) {
		if r.Method == http.MethodPost && r.Path == "/v1/patients" {
			posts = append(posts, r)
		}
	}
	if len(posts) != 2 {
		t.Fatalf("se esperaban 2 intentos, hubo %d", len(posts))
	}
	if k := posts[0].Headers["idempotency-key"]; k == "" || k != posts[1].Headers["idempotency-key"] {
		t.Errorf("los intentos llevan claves distintas: %q y %q", k, posts[1].Headers["idempotency-key"])
	}
	if len(s.ds) != 1 || s.ds[0] < 375*time.Millisecond || s.ds[0] > 625*time.Millisecond {
		t.Errorf("espera antes del reintento = %v, se esperaba 0,5 s ± 25 %%", s.ds)
	}
}

// ─── 14. retries_429 ─────────────────────────────────────────────────────────

func TestRetries429(t *testing.T) {
	var s sleeps
	c := newClient(t, keyRateLimit, kuida.WithRetrySleep(s.fn))
	resetMock(t)

	acct := must(c.Account.Retrieve(context.Background()))(t)
	if acct.Object != "account" {
		t.Fatalf("object = %q", acct.Object)
	}
	if len(s.ds) != 1 || s.ds[0] != time.Second {
		t.Errorf("esperas = %v, se esperaba una de 1 s por Retry-After", s.ds)
	}
	if n := len(mockRequests(t)); n != 2 {
		t.Errorf("se esperaban 2 intentos, hubo %d", n)
	}
}

// ─── 15. no_retry ────────────────────────────────────────────────────────────

func TestNoRetry(t *testing.T) {
	c := newClient(t, keyFlaky, kuida.WithMaxRetries(0))
	resetMock(t)

	_, err := c.Patients.Create(context.Background(), &kuida.PatientCreateParams{Phone: demoPhone})
	if !errors.Is(err, kuida.ErrAPI) {
		t.Fatalf("se esperaba ErrAPI, llegó %v", err)
	}
	if kerr := asKuidaError(t, err); kerr.HTTPStatus != 503 {
		t.Errorf("status = %d", kerr.HTTPStatus)
	}
	if n := len(mockRequests(t)); n != 1 {
		t.Errorf("con maxRetries 0 hubo %d intentos", n)
	}

	// La opción por pedido también desactiva los reintentos.
	c2 := newClient(t, keyFlaky)
	_, err = c2.Patients.Create(context.Background(), &kuida.PatientCreateParams{Phone: demoPhone}, kuida.WithRequestMaxRetries(0))
	if kerr := asKuidaError(t, err); kerr.HTTPStatus != 503 {
		t.Errorf("WithRequestMaxRetries(0): status = %d", kerr.HTTPStatus)
	}
}

// ─── 16. connection_error ────────────────────────────────────────────────────

func TestConnectionError(t *testing.T) {
	apiBase(t) // la suite de conformidad corre junta; este escenario no usa el mock.
	c, err := kuida.NewClient(keyAll, kuida.WithBaseURL("http://127.0.0.1:1/api"), kuida.WithMaxRetries(0))
	if err != nil {
		t.Fatal(err)
	}
	_, err = c.Account.Retrieve(context.Background())
	var cerr *kuida.ConnectionError
	if !errors.As(err, &cerr) || !errors.Is(err, kuida.ErrConnection) {
		t.Fatalf("se esperaba *kuida.ConnectionError, llegó %T: %v", err, err)
	}
	if cerr.Unwrap() == nil {
		t.Error("ConnectionError sin causa")
	}
}

// ─── 17. webhook_signature ───────────────────────────────────────────────────

func TestWebhookSignature(t *testing.T) {
	raw, err := os.ReadFile("../conformance/webhook-vectors.json")
	if err != nil {
		t.Fatalf("leyendo los vectores: %v", err)
	}
	var v struct {
		Secret, Timestamp, Payload, SignatureHeader, WrongSecret, TamperedPayload string
	}
	if err := json.Unmarshal(raw, &v); err != nil {
		t.Fatal(err)
	}
	tsSecs, err := strconv.ParseInt(v.Timestamp, 10, 64)
	if err != nil {
		t.Fatal(err)
	}
	at := time.Unix(tsSecs, 0)
	nowAt := webhook.WithNow(func() time.Time { return at })

	ev, err := webhook.ConstructEvent([]byte(v.Payload), v.SignatureHeader, v.Timestamp, v.Secret, nowAt)
	if err != nil {
		t.Fatalf("el vector fijo no verificó: %v", err)
	}
	if ev.Object != "event" || ev.Type != kuida.WebhookEventTypeIntakeReady || ev.Data["intake"] == nil {
		t.Errorf("evento parseado: %+v", ev)
	}
	if err := webhook.VerifySignature([]byte(v.Payload), v.SignatureHeader, v.Timestamp, v.Secret, webhook.WithTolerance(0)); err != nil {
		t.Errorf("con tolerancia 0 no verificó: %v", err)
	}

	bad := []struct {
		name                     string
		payload, sig, ts, secret string
		opts                     []webhook.Option
	}{
		{"secreto equivocado", v.Payload, v.SignatureHeader, v.Timestamp, v.WrongSecret, []webhook.Option{nowAt}},
		{"cuerpo alterado", v.TamperedPayload, v.SignatureHeader, v.Timestamp, v.Secret, []webhook.Option{nowAt}},
		{"fuera de tolerancia", v.Payload, v.SignatureHeader, v.Timestamp, v.Secret, nil},
		{"sin firma", v.Payload, "", v.Timestamp, v.Secret, []webhook.Option{nowAt}},
		{"sin timestamp", v.Payload, v.SignatureHeader, "", v.Secret, []webhook.Option{nowAt}},
	}
	for _, c := range bad {
		err := webhook.VerifySignature([]byte(c.payload), c.sig, c.ts, c.secret, c.opts...)
		var serr *kuida.SignatureVerificationError
		if !errors.As(err, &serr) || !errors.Is(err, kuida.ErrSignatureVerification) {
			t.Errorf("%s: se esperaba *kuida.SignatureVerificationError, llegó %v", c.name, err)
		}
		if _, err := webhook.ConstructEvent([]byte(c.payload), c.sig, c.ts, c.secret, c.opts...); !errors.Is(err, kuida.ErrSignatureVerification) {
			t.Errorf("%s: ConstructEvent no rechazó (%v)", c.name, err)
		}
	}

	// Un cuerpo propio firmado con la hora actual verifica con la tolerancia por defecto.
	payload := []byte(`{"id":"whd_demo","object":"event","type":"webhook.ping","apiVersion":"2026-09-28","livemode":false,"occurredAt":"2026-09-28T12:00:00.000Z","ref":"ping_demo","data":{"message":"Paciente Demo"}}`)
	now := fmt.Sprint(time.Now().Unix())
	sig := webhook.ComputeSignature(payload, now, v.Secret)
	ev, err = webhook.ConstructEvent(payload, sig, now, v.Secret)
	if err != nil {
		t.Fatalf("cuerpo firmado ahora: %v", err)
	}
	if ev.Type != kuida.WebhookEventTypeWebhookPing {
		t.Errorf("type = %s", ev.Type)
	}
}
