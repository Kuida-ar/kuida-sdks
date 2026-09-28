package kuida

import (
	"bytes"
	"context"
	"crypto/rand"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"math"
	mrand "math/rand"
	"net/http"
	"net/url"
	"runtime"
	"strconv"
	"strings"
	"time"
)

// APIResponse son los datos HTTP de la respuesta que produjo un objeto.
type APIResponse struct {
	// StatusCode es el status HTTP.
	StatusCode int
	// Header son los headers de la respuesta.
	Header http.Header
	// RequestID es el header Request-Id; sirve para pedir soporte.
	RequestID string
	// IdempotencyKey es la clave con que se mandó el pedido (solo en POST).
	IdempotencyKey string
	// IdempotentReplayed es true cuando la API devolvió una respuesta guardada
	// para esa Idempotency-Key en lugar de ejecutar el pedido de nuevo.
	IdempotentReplayed bool
	// RawJSON es el cuerpo crudo de la respuesta.
	RawJSON []byte
}

// RequestOption configura un pedido puntual. Va al final de cada método.
type RequestOption func(*requestConfig)

type requestConfig struct {
	idempotencyKey string
	timeout        time.Duration
	maxRetries     int
	hasMaxRetries  bool
}

// WithIdempotencyKey fija la Idempotency-Key de un POST. Si no se usa, el SDK
// genera un UUID v4 por pedido. En los dos casos la clave se reusa en todos
// los reintentos del mismo pedido.
func WithIdempotencyKey(key string) RequestOption {
	return func(c *requestConfig) { c.idempotencyKey = key }
}

// WithRequestTimeout cambia el timeout de cada intento solo para este pedido.
func WithRequestTimeout(d time.Duration) RequestOption {
	return func(c *requestConfig) { c.timeout = d }
}

// WithRequestMaxRetries cambia la cantidad de reintentos solo para este pedido.
func WithRequestMaxRetries(n int) RequestOption {
	return func(c *requestConfig) {
		if n >= 0 {
			c.maxRetries, c.hasMaxRetries = n, true
		}
	}
}

type backend struct {
	apiKey     string
	baseURL    string
	httpClient *http.Client
	timeout    time.Duration
	maxRetries int
	apiVersion string
	sleep      func(ctx context.Context, d time.Duration) error
}

var userAgent = "Kuida/v1 GoBindings/" + Version

var clientUserAgent = func() string {
	b, _ := json.Marshal(map[string]string{
		"bindings_version": Version,
		"lang":             "go",
		"lang_version":     runtime.Version(),
		"platform":         runtime.GOOS + "/" + runtime.GOARCH,
	})
	return string(b)
}()

type lastResponseSetter interface {
	setLastResponse(*APIResponse)
}

// call hace el pedido y decodifica la respuesta en out.
func (b *backend) call(ctx context.Context, method, path string, query url.Values, body any, out any, opts []RequestOption) error {
	resp, err := b.do(ctx, method, path, query, body, opts)
	if err != nil {
		return err
	}
	return decodeInto(resp, out)
}

func decodeInto(resp *APIResponse, out any) error {
	if err := json.Unmarshal(resp.RawJSON, out); err != nil {
		return &Error{
			Message:    "la respuesta de la API no es JSON válido: " + err.Error(),
			HTTPStatus: resp.StatusCode,
			Type:       ErrorTypeAPI,
			RequestID:  resp.RequestID,
			Headers:    resp.Header,
			Body:       resp.RawJSON,
		}
	}
	if s, ok := out.(lastResponseSetter); ok {
		s.setLastResponse(resp)
	}
	return nil
}

// do ejecuta el pedido con reintentos. Si la API responde con error, devuelve
// también el *APIResponse de ese último intento.
func (b *backend) do(ctx context.Context, method, path string, query url.Values, body any, opts []RequestOption) (*APIResponse, error) {
	if ctx == nil {
		ctx = context.Background()
	}
	cfg := requestConfig{timeout: b.timeout, maxRetries: b.maxRetries}
	for _, o := range opts {
		o(&cfg)
	}
	if !cfg.hasMaxRetries {
		cfg.maxRetries = b.maxRetries
	}
	if cfg.timeout <= 0 {
		cfg.timeout = b.timeout
	}

	u := b.baseURL + path
	if len(query) > 0 {
		u += "?" + query.Encode()
	}

	var payload []byte
	if method == http.MethodPost || method == http.MethodPatch {
		if body == nil {
			payload = []byte("{}")
		} else {
			var err error
			if payload, err = json.Marshal(body); err != nil {
				return nil, fmt.Errorf("kuida: no se pudieron serializar los parámetros: %w", err)
			}
		}
	}

	idemKey := ""
	if method == http.MethodPost {
		idemKey = cfg.idempotencyKey
		if idemKey == "" {
			idemKey = newUUIDv4()
		}
	}

	for attempt := 0; ; attempt++ {
		resp, err := b.attempt(ctx, method, u, payload, idemKey, cfg.timeout)
		if err == nil {
			return resp, nil
		}
		if attempt >= cfg.maxRetries || !retryable(err) || ctx.Err() != nil {
			return resp, err
		}
		if serr := b.sleep(ctx, retryDelay(attempt, resp)); serr != nil {
			return resp, &ConnectionError{Message: "pedido cancelado mientras se esperaba para reintentar", Err: serr}
		}
	}
}

func (b *backend) attempt(ctx context.Context, method, u string, payload []byte, idemKey string, timeout time.Duration) (*APIResponse, error) {
	actx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()

	var rdr io.Reader
	if payload != nil {
		rdr = bytes.NewReader(payload)
	}
	req, err := http.NewRequestWithContext(actx, method, u, rdr)
	if err != nil {
		return nil, fmt.Errorf("kuida: pedido inválido: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+b.apiKey)
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Kuida-Version", b.apiVersion)
	req.Header.Set("User-Agent", userAgent)
	req.Header.Set("X-Kuida-Client-User-Agent", clientUserAgent)
	if payload != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	if idemKey != "" {
		req.Header.Set("Idempotency-Key", idemKey)
	}

	res, err := b.httpClient.Do(req)
	if err != nil {
		return nil, connectionError(ctx, err)
	}
	defer res.Body.Close()
	raw, err := io.ReadAll(res.Body)
	if err != nil {
		return nil, connectionError(ctx, err)
	}

	resp := &APIResponse{
		StatusCode:         res.StatusCode,
		Header:             res.Header,
		RequestID:          res.Header.Get("Request-Id"),
		IdempotencyKey:     idemKey,
		IdempotentReplayed: res.Header.Get("Idempotent-Replayed") == "true",
		RawJSON:            raw,
	}
	if res.StatusCode >= 200 && res.StatusCode < 300 {
		return resp, nil
	}
	return resp, newAPIError(res.StatusCode, res.Header, raw)
}

func connectionError(ctx context.Context, err error) error {
	if ctx.Err() != nil {
		// El contexto del usuario se canceló o venció: no es un error de red.
		return &ConnectionError{Message: "pedido cancelado: " + ctx.Err().Error(), Err: ctx.Err()}
	}
	msg := "no se pudo conectar con la API de Kuida"
	if errors.Is(err, context.DeadlineExceeded) {
		msg = "la API de Kuida no respondió a tiempo"
	}
	return &ConnectionError{Message: msg + ": " + err.Error(), Err: err}
}

// retryable dice si el error admite reintento según SDK_DESIGN.md §6.
func retryable(err error) bool {
	var ce *ConnectionError
	if errors.As(err, &ce) {
		return !errors.Is(ce.Err, context.Canceled)
	}
	var ae *Error
	if errors.As(err, &ae) {
		switch ae.HTTPStatus {
		case http.StatusTooManyRequests, http.StatusInternalServerError, http.StatusBadGateway,
			http.StatusServiceUnavailable, http.StatusGatewayTimeout:
			return true
		case http.StatusConflict:
			return ae.Code == ErrorCodeIdempotencyKeyInUse
		}
	}
	return false
}

const maxRetryAfter = 60 * time.Second

// retryDelay usa Retry-After (segundos, hasta 60 s) si viene; si no,
// min(0,5 s × 2^intento, 8 s) con ±25 % de jitter.
func retryDelay(attempt int, resp *APIResponse) time.Duration {
	if resp != nil {
		if ra := strings.TrimSpace(resp.Header.Get("Retry-After")); ra != "" {
			if secs, err := strconv.ParseFloat(ra, 64); err == nil && secs >= 0 {
				d := time.Duration(secs * float64(time.Second))
				if d > maxRetryAfter {
					d = maxRetryAfter
				}
				return d
			}
		}
	}
	base := math.Min(0.5*math.Pow(2, float64(attempt)), 8)
	jitter := 0.75 + mrand.Float64()*0.5
	return time.Duration(base * jitter * float64(time.Second))
}

func sleepContext(ctx context.Context, d time.Duration) error {
	if d <= 0 {
		return ctx.Err()
	}
	t := time.NewTimer(d)
	defer t.Stop()
	select {
	case <-ctx.Done():
		return ctx.Err()
	case <-t.C:
		return nil
	}
}

func newUUIDv4() string {
	var u [16]byte
	if _, err := rand.Read(u[:]); err != nil {
		// crypto/rand no falla en las plataformas soportadas; por las dudas, math/rand.
		for i := range u {
			u[i] = byte(mrand.Intn(256))
		}
	}
	u[6] = (u[6] & 0x0f) | 0x40
	u[8] = (u[8] & 0x3f) | 0x80
	return fmt.Sprintf("%x-%x-%x-%x-%x", u[0:4], u[4:6], u[6:8], u[8:10], u[10:16])
}

// ─── helpers de servicios ────────────────────────────────────────────────────

func request[T any](ctx context.Context, b *backend, method, path string, body any, opts []RequestOption) (*T, error) {
	out := new(T)
	if err := b.call(ctx, method, path, nil, body, out, opts); err != nil {
		return nil, err
	}
	return out, nil
}

func listPage[T any](ctx context.Context, b *backend, path string, q url.Values, opts []RequestOption) (*ListPage[T], error) {
	out := new(ListPage[T])
	if err := b.call(ctx, http.MethodGet, path, q, nil, out, opts); err != nil {
		return nil, err
	}
	return out, nil
}

func resourcePath(prefix, id string, suffix ...string) (string, error) {
	if strings.TrimSpace(id) == "" {
		return "", errors.New("kuida: el id no puede estar vacío")
	}
	p := prefix + "/" + url.PathEscape(id)
	for _, s := range suffix {
		p += "/" + s
	}
	return p, nil
}
