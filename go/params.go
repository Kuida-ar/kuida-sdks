package kuida

import (
	"encoding/json"
	"errors"
	"net/url"
	"strconv"
	"time"
)

// String devuelve un puntero al valor. Sirve para los campos opcionales de
// los parámetros: FullName: kuida.String("Paciente Demo").
func String(v string) *string { return &v }

// Int64 devuelve un puntero al valor.
func Int64(v int64) *int64 { return &v }

// Int devuelve un puntero al valor.
func Int(v int) *int { return &v }

// Bool devuelve un puntero al valor.
func Bool(v bool) *bool { return &v }

// Time devuelve un puntero al valor.
func Time(v time.Time) *time.Time { return &v }

// ListParams son los parámetros de paginación comunes a todos los listados.
// Se usa startingAfter o endingBefore, no los dos.
type ListParams struct {
	// Limit es la cantidad de objetos por página, entre 1 y 100 (por defecto 10).
	Limit *int64
	// StartingAfter pide los objetos que siguen a este id (página siguiente).
	StartingAfter *string
	// EndingBefore pide los objetos anteriores a este id (página anterior).
	EndingBefore *string
}

func (p *ListParams) query() url.Values {
	q := url.Values{}
	if p == nil {
		return q
	}
	if p.Limit != nil {
		q.Set("limit", strconv.FormatInt(*p.Limit, 10))
	}
	if p.StartingAfter != nil {
		q.Set("startingAfter", *p.StartingAfter)
	}
	if p.EndingBefore != nil {
		q.Set("endingBefore", *p.EndingBefore)
	}
	return q
}

func boolString(v bool) string {
	if v {
		return "true"
	}
	return "false"
}

func timeOrNil(t time.Time) *time.Time {
	if t.IsZero() {
		return nil
	}
	return &t
}

// ─── referencias ─────────────────────────────────────────────────────────────

// PatientReference identifica a un paciente de dos formas: por su id de
// Kuida (PatientID) o por los datos con que lo conoce el sistema de la
// institución (PatientByIdentity). En JSON es un texto o un objeto.
type PatientReference struct {
	id       string
	identity *PatientIdentity
}

// PatientID referencia un paciente por su id de Kuida ("pat_…").
func PatientID(id string) *PatientReference { return &PatientReference{id: id} }

// PatientByIdentity referencia un paciente por sus datos (teléfono, DNI, id
// externo, nombre). Si no existe, Kuida lo crea.
func PatientByIdentity(identity PatientIdentity) *PatientReference {
	return &PatientReference{identity: &identity}
}

// ID devuelve el id si la referencia es por id, o "".
func (r *PatientReference) ID() string { return r.id }

// Identity devuelve los datos si la referencia es por identidad, o nil.
func (r *PatientReference) Identity() *PatientIdentity { return r.identity }

// MarshalJSON serializa la referencia como texto o como objeto.
func (r PatientReference) MarshalJSON() ([]byte, error) {
	if r.identity != nil {
		return json.Marshal(r.identity)
	}
	if r.id == "" {
		return nil, errors.New("kuida: PatientReference vacía; se construye con PatientID o PatientByIdentity")
	}
	return json.Marshal(r.id)
}

// UnmarshalJSON acepta las dos formas.
func (r *PatientReference) UnmarshalJSON(data []byte) error {
	var id string
	if err := json.Unmarshal(data, &id); err == nil {
		*r = PatientReference{id: id}
		return nil
	}
	var identity PatientIdentity
	if err := json.Unmarshal(data, &identity); err != nil {
		return err
	}
	*r = PatientReference{identity: &identity}
	return nil
}

// DoctorReference identifica a un profesional por su id de Kuida (DoctorID)
// o por sus datos (DoctorByIdentity). En JSON es un texto o un objeto.
type DoctorReference struct {
	id       string
	identity *DoctorIdentity
}

// DoctorID referencia un profesional por su id de Kuida ("doc_…").
func DoctorID(id string) *DoctorReference { return &DoctorReference{id: id} }

// DoctorByIdentity referencia un profesional por su id externo o su nombre.
// Si no existe en el padrón, Kuida lo crea.
func DoctorByIdentity(identity DoctorIdentity) *DoctorReference {
	return &DoctorReference{identity: &identity}
}

// ID devuelve el id si la referencia es por id, o "".
func (r *DoctorReference) ID() string { return r.id }

// Identity devuelve los datos si la referencia es por identidad, o nil.
func (r *DoctorReference) Identity() *DoctorIdentity { return r.identity }

// MarshalJSON serializa la referencia como texto o como objeto.
func (r DoctorReference) MarshalJSON() ([]byte, error) {
	if r.identity != nil {
		return json.Marshal(r.identity)
	}
	if r.id == "" {
		return nil, errors.New("kuida: DoctorReference vacía; se construye con DoctorID o DoctorByIdentity")
	}
	return json.Marshal(r.id)
}

// UnmarshalJSON acepta las dos formas.
func (r *DoctorReference) UnmarshalJSON(data []byte) error {
	var id string
	if err := json.Unmarshal(data, &id); err == nil {
		*r = DoctorReference{id: id}
		return nil
	}
	var identity DoctorIdentity
	if err := json.Unmarshal(data, &identity); err != nil {
		return err
	}
	*r = DoctorReference{identity: &identity}
	return nil
}
