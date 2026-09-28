// Package webhook verifica y parsea los webhooks que Kuida manda a los
// endpoints registrados. No necesita cliente ni clave de API: solo el secreto
// del endpoint (whsec_…).
//
// En un handler HTTP:
//
//	payload, _ := io.ReadAll(r.Body)
//	event, err := webhook.ConstructEvent(payload,
//		r.Header.Get("x-kuida-signature"),
//		r.Header.Get("x-kuida-timestamp"),
//		os.Getenv("KUIDA_WEBHOOK_SECRET"))
//	if err != nil {
//		http.Error(w, "firma inválida", http.StatusBadRequest)
//		return
//	}
//	switch event.Type {
//	case kuida.WebhookEventTypeIntakeReady:
//		…
//	}
package webhook

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"time"

	kuida "github.com/Kuida-ar/kuida-sdks/go"
)

// Headers que manda Kuida en cada webhook.
const (
	SignatureHeader       = "x-kuida-signature"
	TimestampHeader       = "x-kuida-timestamp"
	EventTypeHeader       = "x-kuida-event-type"
	DeliveryAttemptHeader = "x-kuida-delivery-attempt"
)

// DefaultTolerance es la diferencia máxima aceptada entre el timestamp del
// webhook y la hora actual.
const DefaultTolerance = 300 * time.Second

// Option ajusta la verificación.
type Option func(*config)

type config struct {
	tolerance time.Duration
	now       func() time.Time
}

// WithTolerance cambia la tolerancia de tiempo (por defecto 300 s). Con 0 no
// se chequea el timestamp.
func WithTolerance(d time.Duration) Option {
	return func(c *config) {
		if d >= 0 {
			c.tolerance = d
		}
	}
}

// WithNow reemplaza el reloj usado para chequear el timestamp (para tests).
func WithNow(now func() time.Time) Option {
	return func(c *config) {
		if now != nil {
			c.now = now
		}
	}
}

// ComputeSignature devuelve la firma esperada, "sha256=" + hex de
// HMAC-SHA256(secret, "<timestamp>.<payload>"). Sirve para armar pruebas.
func ComputeSignature(payload []byte, timestamp, secret string) string {
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(timestamp))
	mac.Write([]byte("."))
	mac.Write(payload)
	return "sha256=" + hex.EncodeToString(mac.Sum(nil))
}

// VerifySignature comprueba la firma de un webhook. payload es el cuerpo
// crudo tal como llegó (nunca un JSON re-serializado); signatureHeader y
// timestampHeader son los valores de x-kuida-signature y x-kuida-timestamp.
// Devuelve nil si la firma es válida o un *kuida.SignatureVerificationError.
func VerifySignature(payload []byte, signatureHeader, timestampHeader, secret string, opts ...Option) error {
	cfg := config{tolerance: DefaultTolerance, now: time.Now}
	for _, o := range opts {
		o(&cfg)
	}
	fail := func(format string, args ...any) error {
		return &kuida.SignatureVerificationError{Message: fmt.Sprintf(format, args...), SignatureHeader: signatureHeader}
	}
	signatureHeader = strings.TrimSpace(signatureHeader)
	timestampHeader = strings.TrimSpace(timestampHeader)
	if signatureHeader == "" {
		return fail("falta el header %s", SignatureHeader)
	}
	if timestampHeader == "" {
		return fail("falta el header %s", TimestampHeader)
	}
	if secret == "" {
		return fail("falta el secreto del endpoint")
	}
	ts, err := strconv.ParseInt(timestampHeader, 10, 64)
	if err != nil {
		return fail("el header %s no es un timestamp válido", TimestampHeader)
	}

	expected := ComputeSignature(payload, timestampHeader, secret)
	if !hmac.Equal([]byte(strings.ToLower(signatureHeader)), []byte(expected)) {
		return fail("la firma no coincide con el cuerpo recibido")
	}

	if cfg.tolerance > 0 {
		diff := cfg.now().Sub(time.Unix(ts, 0))
		if diff < 0 {
			diff = -diff
		}
		if diff > cfg.tolerance {
			return fail("el timestamp está fuera de la tolerancia de %s", cfg.tolerance)
		}
	}
	return nil
}

// ConstructEvent verifica la firma y devuelve el evento parseado.
func ConstructEvent(payload []byte, signatureHeader, timestampHeader, secret string, opts ...Option) (*kuida.WebhookEvent, error) {
	if err := VerifySignature(payload, signatureHeader, timestampHeader, secret, opts...); err != nil {
		return nil, err
	}
	var ev kuida.WebhookEvent
	if err := json.Unmarshal(payload, &ev); err != nil {
		return nil, fmt.Errorf("kuida: el cuerpo del webhook no es JSON válido: %w", err)
	}
	return &ev, nil
}
