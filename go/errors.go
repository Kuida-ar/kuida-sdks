package kuida

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
)

// Error es un error devuelto por la API de Kuida (toda respuesta no 2xx).
//
// Para distinguir la clase se compara Type con las constantes ErrorType* o se
// usa errors.Is con los sentinelas:
//
//	var kerr *kuida.Error
//	if errors.As(err, &kerr) && kerr.Code == kuida.ErrorCodeResourceMissing { … }
//	if errors.Is(err, kuida.ErrAuthentication) { … }
type Error struct {
	// Message es la descripción legible del error.
	Message string
	// HTTPStatus es el status HTTP de la respuesta.
	HTTPStatus int
	// Type es la clase de error (error.type). Si la API no mandó un cuerpo de
	// error reconocible, vale ErrorTypeAPI.
	Type ErrorType
	// Code es el código puntual (error.code), por ejemplo resource_missing.
	Code ErrorCode
	// Param es el parámetro que causó el error, si corresponde.
	Param string
	// RequestID identifica el pedido (del cuerpo o del header Request-Id).
	RequestID string
	// DocURL apunta a la documentación del código de error.
	DocURL string
	// Headers son los headers de la respuesta.
	Headers http.Header
	// Body es el cuerpo crudo de la respuesta.
	Body []byte
}

// Error implementa la interfaz error.
func (e *Error) Error() string {
	var b strings.Builder
	b.WriteString("kuida: ")
	b.WriteString(e.Message)
	fmt.Fprintf(&b, " (status %d, type %s", e.HTTPStatus, e.Type)
	if e.Code != "" {
		fmt.Fprintf(&b, ", code %s", e.Code)
	}
	if e.Param != "" {
		fmt.Fprintf(&b, ", param %s", e.Param)
	}
	if e.RequestID != "" {
		fmt.Fprintf(&b, ", request %s", e.RequestID)
	}
	b.WriteString(")")
	return b.String()
}

// Class devuelve la clase del error según SDK_DESIGN.md §7: el Type recibido
// si es uno conocido, o ErrorTypeAPI para cualquier otro.
func (e *Error) Class() ErrorType {
	switch e.Type {
	case ErrorTypeInvalidRequest, ErrorTypeAuthentication, ErrorTypePermission,
		ErrorTypeIdempotency, ErrorTypeRateLimit:
		return e.Type
	}
	return ErrorTypeAPI
}

// Is permite errors.Is(err, kuida.ErrInvalidRequest) y el resto de los
// sentinelas de clase, además de errors.Is(err, kuida.ErrKuida).
func (e *Error) Is(target error) bool {
	if target == ErrKuida {
		return true
	}
	c, ok := target.(*classError)
	return ok && c.typ != "" && c.typ == e.Class()
}

// classError es el tipo de los sentinelas de clase.
type classError struct {
	typ  ErrorType
	name string
}

func (c *classError) Error() string { return "kuida: " + c.name }

// Sentinelas para errors.Is. Cada uno corresponde a una clase de la tabla de
// SDK_DESIGN.md §7; ErrKuida coincide con cualquier error del SDK.
var (
	// ErrKuida coincide con cualquier *Error, *ConnectionError o *SignatureVerificationError.
	ErrKuida error = &classError{name: "error de Kuida"}
	// ErrInvalidRequest: parámetros inválidos, faltantes o recurso inexistente (400, 404, 409, 413).
	ErrInvalidRequest error = &classError{typ: ErrorTypeInvalidRequest, name: "pedido inválido"}
	// ErrAuthentication: clave faltante, inválida, revocada o vencida (401).
	ErrAuthentication error = &classError{typ: ErrorTypeAuthentication, name: "error de autenticación"}
	// ErrPermission: a la clave le falta el scope o la línea de servicio (403).
	ErrPermission error = &classError{typ: ErrorTypePermission, name: "permiso denegado"}
	// ErrIdempotency: la Idempotency-Key ya se usó con otro pedido.
	ErrIdempotency error = &classError{typ: ErrorTypeIdempotency, name: "error de idempotencia"}
	// ErrRateLimit: se superó el límite de pedidos (429).
	ErrRateLimit error = &classError{typ: ErrorTypeRateLimit, name: "límite de pedidos superado"}
	// ErrAPI: error de Kuida (5xx) o respuesta no reconocida.
	ErrAPI error = &classError{typ: ErrorTypeAPI, name: "error de la API"}
	// ErrConnection: no hubo respuesta (red, timeout, DNS, TLS). Ver *ConnectionError.
	ErrConnection error = &classError{name: "error de conexión"}
	// ErrSignatureVerification: la firma de un webhook no es válida. Ver *SignatureVerificationError.
	ErrSignatureVerification error = &classError{name: "firma de webhook inválida"}
)

// ConnectionError indica que el pedido no obtuvo respuesta: error de red,
// timeout, DNS o TLS. Err tiene la causa original.
type ConnectionError struct {
	Message string
	Err     error
}

func (e *ConnectionError) Error() string { return "kuida: " + e.Message }

// Unwrap devuelve la causa original.
func (e *ConnectionError) Unwrap() error { return e.Err }

// Is permite errors.Is(err, kuida.ErrConnection).
func (e *ConnectionError) Is(target error) bool {
	return target == ErrConnection || target == ErrKuida
}

// SignatureVerificationError indica que un webhook no pasó la verificación de
// firma: falta un header, la firma no coincide o el timestamp está fuera de
// la tolerancia.
type SignatureVerificationError struct {
	Message string
	// SignatureHeader es el valor recibido en x-kuida-signature.
	SignatureHeader string
}

func (e *SignatureVerificationError) Error() string { return "kuida: " + e.Message }

// Is permite errors.Is(err, kuida.ErrSignatureVerification).
func (e *SignatureVerificationError) Is(target error) bool {
	return target == ErrSignatureVerification || target == ErrKuida
}

// newAPIError arma el *Error de una respuesta no 2xx.
func newAPIError(status int, header http.Header, body []byte) *Error {
	e := &Error{HTTPStatus: status, Headers: header, Body: body, RequestID: header.Get("Request-Id")}
	var env struct {
		Error *struct {
			Type      ErrorType `json:"type"`
			Code      ErrorCode `json:"code"`
			Message   string    `json:"message"`
			Param     *string   `json:"param"`
			RequestID string    `json:"requestId"`
			DocURL    string    `json:"docUrl"`
		} `json:"error"`
	}
	if err := json.Unmarshal(body, &env); err != nil || env.Error == nil {
		e.Type = ErrorTypeAPI
		e.Message = strings.TrimSpace(string(body))
		if e.Message == "" {
			e.Message = http.StatusText(status)
		}
		return e
	}
	ee := env.Error
	e.Type, e.Code, e.Message, e.DocURL = ee.Type, ee.Code, ee.Message, ee.DocURL
	if e.Type == "" {
		e.Type = ErrorTypeAPI
	}
	if ee.Param != nil {
		e.Param = *ee.Param
	}
	if ee.RequestID != "" {
		e.RequestID = ee.RequestID
	}
	return e
}
