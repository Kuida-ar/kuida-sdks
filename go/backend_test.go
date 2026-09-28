package kuida

import (
	"encoding/json"
	"errors"
	"net/http"
	"regexp"
	"testing"
	"time"
)

func TestRetryDelay(t *testing.T) {
	for attempt, base := range []float64{0.5, 1, 2, 4, 8, 8} {
		for i := 0; i < 50; i++ {
			d := retryDelay(attempt, nil).Seconds()
			if d < base*0.75 || d > base*1.25 {
				t.Fatalf("intento %d: espera %.3f s fuera de %.2f ± 25 %%", attempt, d, base)
			}
		}
	}
	h := http.Header{}
	h.Set("Retry-After", "3")
	if d := retryDelay(0, &APIResponse{Header: h}); d != 3*time.Second {
		t.Errorf("Retry-After 3 → %v", d)
	}
	h.Set("Retry-After", "600")
	if d := retryDelay(0, &APIResponse{Header: h}); d != 60*time.Second {
		t.Errorf("Retry-After 600 → %v, se esperaba el tope de 60 s", d)
	}
}

func TestRetryable(t *testing.T) {
	cases := []struct {
		err  error
		want bool
	}{
		{&ConnectionError{Err: errors.New("dial")}, true},
		{&Error{HTTPStatus: 429}, true},
		{&Error{HTTPStatus: 500}, true},
		{&Error{HTTPStatus: 502}, true},
		{&Error{HTTPStatus: 503}, true},
		{&Error{HTTPStatus: 504}, true},
		{&Error{HTTPStatus: 409, Code: ErrorCodeIdempotencyKeyInUse}, true},
		{&Error{HTTPStatus: 409, Code: ErrorCodeResourceConflict}, false},
		{&Error{HTTPStatus: 400}, false},
		{&Error{HTTPStatus: 404}, false},
		{&Error{HTTPStatus: 501}, false},
	}
	for _, c := range cases {
		if got := retryable(c.err); got != c.want {
			t.Errorf("retryable(%v) = %v", c.err, got)
		}
	}
}

func TestUUIDv4(t *testing.T) {
	re := regexp.MustCompile(`^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`)
	seen := map[string]bool{}
	for i := 0; i < 100; i++ {
		u := newUUIDv4()
		if !re.MatchString(u) || seen[u] {
			t.Fatalf("UUID inválido o repetido: %s", u)
		}
		seen[u] = true
	}
}

func TestNewAPIErrorNonJSON(t *testing.T) {
	h := http.Header{}
	h.Set("Request-Id", "req_demo")
	e := newAPIError(502, h, []byte("<html>Bad Gateway</html>"))
	if e.Type != ErrorTypeAPI || e.Message != "<html>Bad Gateway</html>" || e.RequestID != "req_demo" || !errors.Is(e, ErrAPI) {
		t.Errorf("error no JSON: %+v", e)
	}
	e = newAPIError(418, h, []byte(`{"error":{"type":"teapot_error","code":"x","message":"m","param":null,"requestId":"req_body","docUrl":"u"}}`))
	if e.Class() != ErrorTypeAPI || !errors.Is(e, ErrAPI) || e.RequestID != "req_body" || errors.Is(e, ErrInvalidRequest) {
		t.Errorf("tipo desconocido: %+v", e)
	}
	if !errors.Is(e, ErrKuida) {
		t.Error("todo *Error tiene que coincidir con ErrKuida")
	}
}

func TestReferencesMarshal(t *testing.T) {
	b, _ := json.Marshal(TreatmentCreateParams{Patient: PatientID("pat_demo"), Name: "Control"})
	if string(b) != `{"patient":"pat_demo","name":"Control"}` {
		t.Errorf("por id: %s", b)
	}
	b, _ = json.Marshal(AppointmentCreateParams{
		Patient: PatientByIdentity(PatientIdentity{Phone: String("+54 9 342 555 0000"), DNI: String("11111111")}),
		Doctor:  DoctorID("doc_demo"),
	})
	if string(b) != `{"patient":{"phone":"+54 9 342 555 0000","dni":"11111111"},"doctor":"doc_demo"}` {
		t.Errorf("por identidad, sin startAt: %s", b)
	}
	var r PatientReference
	if err := json.Unmarshal([]byte(`"pat_x"`), &r); err != nil || r.ID() != "pat_x" {
		t.Errorf("unmarshal id: %v %+v", err, r)
	}
	if err := json.Unmarshal([]byte(`{"dni":"11111111"}`), &r); err != nil || r.Identity() == nil || *r.Identity().DNI != "11111111" {
		t.Errorf("unmarshal identidad: %v %+v", err, r)
	}
}

func TestListQuery(t *testing.T) {
	at := time.Date(2026, 10, 1, 14, 30, 0, 0, time.FixedZone("ART", -3*3600))
	q := (&AppointmentListParams{
		ListParams: ListParams{Limit: Int64(5), StartingAfter: String("apt_x")},
		Status:     AppointmentStatusConfirmed,
		StartAtGte: &at,
	}).query()
	if q.Encode() != "limit=5&startAtGte=2026-10-01T14%3A30%3A00-03%3A00&startingAfter=apt_x&status=confirmed" {
		t.Errorf("query = %s", q.Encode())
	}
	q = (&DoctorListParams{Active: Bool(false)}).query()
	if q.Get("active") != "false" {
		t.Errorf("active = %q", q.Get("active"))
	}
	if (*PatientListParams)(nil).query().Encode() != "" {
		t.Error("params nil tendría que dar query vacía")
	}
}

func TestUnknownFieldsAndRaw(t *testing.T) {
	var p Patient
	body := `{"id":"pat_x","object":"patient","phone":"+5493425550000","campoNuevo":42,"stage":"scheduled","optedOut":false,"createdAt":"2026-09-28T12:00:00.000Z","updatedAt":"2026-09-28T12:00:00.000Z"}`
	if err := json.Unmarshal([]byte(body), &p); err != nil {
		t.Fatal(err)
	}
	if p.ID != "pat_x" || p.Stage != PatientStageScheduled || string(p.Raw()) != body || p.CreatedAt.IsZero() {
		t.Errorf("patient: %+v", p)
	}
}
