// Package kuida es el SDK oficial de Go para la API REST de Kuida
// (https://www.kuida.ar/desarrolladores).
//
// Kuida es una plataforma de seguimiento de pacientes por WhatsApp. Esta
// librería permite que el sistema de gestión de una institución de salud
// cargue pacientes, turnos, consultas, solicitudes de ingreso, tratamientos y
// eventos, y que administre sus endpoints de webhooks.
//
// Uso básico:
//
//	client, err := kuida.NewClient("kd_live_…")
//	if err != nil {
//		log.Fatal(err)
//	}
//	patient, err := client.Patients.Create(ctx, &kuida.PatientCreateParams{
//		Phone:    "+54 9 342 555 0000",
//		FullName: kuida.String("Paciente Demo"),
//	})
//
// Todos los métodos reciben un context.Context. Los POST llevan una
// Idempotency-Key automática que se reusa en los reintentos. Los errores de
// la API son *kuida.Error; los de red, *kuida.ConnectionError. La
// verificación de webhooks está en el subpaquete webhook.
package kuida

import (
	"context"
	"errors"
	"net/http"
	"os"
	"strings"
	"time"
)

// Version es la versión de este SDK.
const Version = "0.1.0"

// DefaultBaseURL es la URL base de la API de producción.
const DefaultBaseURL = "https://www.kuida.ar/api"

// Valores por defecto del cliente.
const (
	DefaultTimeout    = 30 * time.Second
	DefaultMaxRetries = 2
)

// ErrMissingAPIKey se devuelve al construir un cliente sin clave de API y sin
// la variable de entorno KUIDA_API_KEY.
var ErrMissingAPIKey = errors.New("kuida: falta la clave de API; se pasa a NewClient o se define en KUIDA_API_KEY")

// Client es el cliente de la API de Kuida. Es seguro para usar desde varias
// goroutines; lo recomendable es crear uno por proceso y reutilizarlo.
type Client struct {
	// Account es la organización dueña de la clave.
	Account *AccountService
	// Patients administra pacientes.
	Patients *PatientService
	// Doctors consulta el padrón de profesionales.
	Doctors *DoctorService
	// Appointments administra turnos.
	Appointments *AppointmentService
	// Visits registra consultas atendidas.
	Visits *VisitService
	// IntakeRequests administra solicitudes de ingreso.
	IntakeRequests *IntakeRequestService
	// Treatments administra tratamientos e indicaciones.
	Treatments *TreatmentService
	// Events envía y consulta eventos del catálogo v1.
	Events *EventService
	// WebhookEndpoints administra los endpoints que reciben webhooks.
	WebhookEndpoints *WebhookEndpointService
	// WebhookDeliveries consulta los envíos de webhooks.
	WebhookDeliveries *WebhookDeliveryService

	b *backend
}

// ClientOption configura un Client en NewClient.
type ClientOption func(*backend)

// WithBaseURL cambia la URL base de la API (por defecto
// https://www.kuida.ar/api, o KUIDA_API_BASE si está definida).
func WithBaseURL(baseURL string) ClientOption {
	return func(b *backend) { b.baseURL = strings.TrimRight(baseURL, "/") }
}

// WithHTTPClient usa un *http.Client propio (proxy, transporte, trazas).
// El timeout de cada intento lo sigue controlando WithTimeout.
func WithHTTPClient(c *http.Client) ClientOption {
	return func(b *backend) {
		if c != nil {
			b.httpClient = c
		}
	}
}

// WithMaxRetries fija cuántas veces se reintenta un pedido fallido
// (por defecto 2, o sea hasta 3 intentos). 0 desactiva los reintentos.
func WithMaxRetries(n int) ClientOption {
	return func(b *backend) {
		if n >= 0 {
			b.maxRetries = n
		}
	}
}

// WithTimeout fija el tiempo máximo de cada intento (por defecto 30 s).
func WithTimeout(d time.Duration) ClientOption {
	return func(b *backend) {
		if d > 0 {
			b.timeout = d
		}
	}
}

// WithAPIVersion fija el header Kuida-Version (por defecto APIVersion).
func WithAPIVersion(v string) ClientOption {
	return func(b *backend) {
		if v != "" {
			b.apiVersion = v
		}
	}
}

// WithRetrySleep reemplaza la espera entre reintentos. Pensado para tests:
// una función que devuelve nil sin esperar elimina las demoras. La función
// recibe la espera calculada (backoff o Retry-After).
func WithRetrySleep(sleep func(ctx context.Context, d time.Duration) error) ClientOption {
	return func(b *backend) {
		if sleep != nil {
			b.sleep = sleep
		}
	}
}

// NewClient crea un cliente con la clave de API dada. Si apiKey está vacía se
// lee KUIDA_API_KEY; si tampoco está, devuelve ErrMissingAPIKey. La URL base
// sale de WithBaseURL, o de KUIDA_API_BASE, o de DefaultBaseURL.
func NewClient(apiKey string, opts ...ClientOption) (*Client, error) {
	if apiKey == "" {
		apiKey = os.Getenv("KUIDA_API_KEY")
	}
	if apiKey == "" {
		return nil, ErrMissingAPIKey
	}
	b := &backend{
		apiKey:     apiKey,
		baseURL:    DefaultBaseURL,
		httpClient: &http.Client{},
		timeout:    DefaultTimeout,
		maxRetries: DefaultMaxRetries,
		apiVersion: APIVersion,
		sleep:      sleepContext,
	}
	if env := os.Getenv("KUIDA_API_BASE"); env != "" {
		b.baseURL = strings.TrimRight(env, "/")
	}
	for _, opt := range opts {
		opt(b)
	}
	return &Client{
		Account:           &AccountService{b},
		Patients:          &PatientService{b},
		Doctors:           &DoctorService{b},
		Appointments:      &AppointmentService{b},
		Visits:            &VisitService{b},
		IntakeRequests:    &IntakeRequestService{b},
		Treatments:        &TreatmentService{b},
		Events:            &EventService{b},
		WebhookEndpoints:  &WebhookEndpointService{b},
		WebhookDeliveries: &WebhookDeliveryService{b},
		b:                 b,
	}, nil
}
