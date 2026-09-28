/**
 * Jerarquía de errores del SDK (SDK_DESIGN.md §7).
 *
 * Toda respuesta no 2xx de la API se convierte en una subclase de
 * {@link KuidaError} según `error.type`. Los problemas de red o timeout
 * terminan en {@link APIConnectionError} y la verificación de webhooks en
 * {@link SignatureVerificationError}.
 */

/** Datos con los que se construye un error del SDK. */
export interface KuidaErrorInit {
  message: string;
  httpStatus?: number | null;
  type?: string | null;
  code?: string | null;
  param?: string | null;
  requestId?: string | null;
  docUrl?: string | null;
  headers?: Record<string, string>;
  raw?: unknown;
  cause?: unknown;
}

/** Clase base de todos los errores del SDK. */
export class KuidaError extends Error {
  /** Status HTTP de la respuesta, o `null` si no hubo respuesta. */
  readonly httpStatus: number | null;
  /** `error.type` de la API (`invalid_request_error`, `authentication_error`, …). */
  readonly type: string | null;
  /** `error.code` de la API (`resource_missing`, `scope_missing`, …). */
  readonly code: string | null;
  /** Parámetro que causó el error, si aplica. */
  readonly param: string | null;
  /** Id del pedido (`req_…`), del cuerpo o del header `Request-Id`. Conviene citarlo al pedir soporte. */
  readonly requestId: string | null;
  /** Página de la documentación que explica el error. */
  readonly docUrl: string | null;
  /** Headers de la respuesta, con nombres en minúscula. */
  readonly headers: Record<string, string>;
  /** Cuerpo crudo de la respuesta: el JSON parseado o el texto si no era JSON. */
  readonly raw: unknown;

  constructor(init: KuidaErrorInit) {
    super(init.message);
    this.name = new.target.name;
    this.httpStatus = init.httpStatus ?? null;
    this.type = init.type ?? null;
    this.code = init.code ?? null;
    this.param = init.param ?? null;
    this.requestId = init.requestId ?? null;
    this.docUrl = init.docUrl ?? null;
    this.headers = init.headers ?? {};
    this.raw = init.raw;
    if (init.cause !== undefined) {
      Object.defineProperty(this, "cause", { value: init.cause, enumerable: false, writable: true, configurable: true });
    }
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** Parámetros inválidos, objeto inexistente (404) o conflicto (`invalid_request_error`). */
export class InvalidRequestError extends KuidaError {}

/** Clave faltante, inválida, revocada o vencida (`authentication_error`, 401). */
export class AuthenticationError extends KuidaError {}

/** A la clave le falta un scope o la organización no contrató la línea de servicio (`permission_error`, 403). */
export class PermissionError extends KuidaError {}

/** La misma `Idempotency-Key` se usó con otro pedido (`idempotency_error`). */
export class IdempotencyError extends KuidaError {}

/** Demasiados pedidos (`rate_limit_error`, 429). El SDK ya reintentó antes de lanzarlo. */
export class RateLimitError extends KuidaError {}

/** Error del lado de Kuida (`api_error`) o de un tipo que el SDK no conoce. */
export class APIError extends KuidaError {}

/** No hubo respuesta: error de red, DNS, TLS o timeout. */
export class APIConnectionError extends KuidaError {}

/** La firma de un webhook no es válida, falta un header o el timestamp está fuera de tolerancia. */
export class SignatureVerificationError extends KuidaError {}

const BY_TYPE: Record<string, new (init: KuidaErrorInit) => KuidaError> = {
  invalid_request_error: InvalidRequestError,
  authentication_error: AuthenticationError,
  permission_error: PermissionError,
  idempotency_error: IdempotencyError,
  rate_limit_error: RateLimitError,
  api_error: APIError,
};

// Solo para respuestas JSON sin `error` (no deberían existir según el OpenAPI).
const BY_STATUS: Record<number, new (init: KuidaErrorInit) => KuidaError> = {
  400: InvalidRequestError,
  401: AuthenticationError,
  403: PermissionError,
  404: InvalidRequestError,
  409: InvalidRequestError,
  413: InvalidRequestError,
  429: RateLimitError,
};

/**
 * Construye el error que corresponde a una respuesta no 2xx.
 * @internal
 */
export function errorFromResponse(status: number, headers: Record<string, string>, text: string): KuidaError {
  let body: unknown = text;
  let parsed = false;
  try {
    body = JSON.parse(text);
    parsed = true;
  } catch {
    // cuerpo que no es JSON → APIError con el texto
  }
  const headerRequestId = headers["request-id"] ?? null;
  const err =
    parsed && body && typeof body === "object" && "error" in body && typeof (body as { error: unknown }).error === "object"
      ? ((body as { error: Record<string, unknown> }).error ?? {})
      : null;

  if (!err) {
    const Cls = parsed ? (BY_STATUS[status] ?? APIError) : APIError;
    const message = parsed
      ? `La API respondió ${status} sin un objeto error`
      : text.trim() || `La API respondió ${status}`;
    return new Cls({ message, httpStatus: status, requestId: headerRequestId, headers, raw: body });
  }

  const str = (v: unknown) => (typeof v === "string" ? v : null);
  const type = str(err.type);
  const Cls = (type && BY_TYPE[type]) || APIError;
  return new Cls({
    message: str(err.message) ?? `La API respondió ${status}`,
    httpStatus: status,
    type,
    code: str(err.code),
    param: str(err.param),
    requestId: str(err.requestId) ?? headerRequestId,
    docUrl: str(err.docUrl),
    headers,
    raw: body,
  });
}
