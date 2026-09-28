/** Tipos del cliente que no salen del OpenAPI. */

/** Configuración del cliente. */
export interface KuidaConfig {
  /** Clave de API (`kd_live_…` o `kd_test_…`). Default: `process.env.KUIDA_API_KEY`. */
  apiKey?: string;
  /** URL base de la API. Default: `process.env.KUIDA_API_BASE` o `https://www.kuida.ar/api`. */
  baseUrl?: string;
  /** Timeout por intento, en milisegundos. Default: 30000. */
  timeout?: number;
  /** Reintentos ante errores transitorios (red, 409 en curso, 429, 5xx). Default: 2. */
  maxRetries?: number;
  /** Versión de la API (header `Kuida-Version`). Default: la del SDK, `2026-09-28`. */
  apiVersion?: string;
  /** Implementación de `fetch` a usar. Default: el `fetch` global de Node 18+. */
  fetch?: typeof fetch;
  /**
   * Función de espera entre reintentos, en milisegundos. Pensada para tests
   * (por ejemplo `() => Promise.resolve()` para no esperar).
   */
  sleep?: (ms: number) => Promise<void>;
}

/** Opciones que acepta cada método al final. */
export interface RequestOptions {
  /** Clave de idempotencia para un POST. Si no se pasa, el SDK genera un UUID v4. */
  idempotencyKey?: string;
  /** Timeout de este pedido, en milisegundos. */
  timeout?: number;
  /** Reintentos para este pedido. */
  maxRetries?: number;
}

/** Datos de la respuesta HTTP que trajo un objeto. */
export interface ResponseMeta {
  /** Status HTTP. */
  statusCode: number;
  /** Headers de la respuesta, con nombres en minúscula. */
  headers: Record<string, string>;
  /** Header `Request-Id` (`req_…`). */
  requestId: string | null;
  /** Cuerpo JSON tal como llegó, sin parsear. */
  rawBody: string;
}

/**
 * Un objeto devuelto por la API. `lastResponse` no es enumerable: no aparece
 * en `JSON.stringify` ni en `Object.keys`.
 */
export type KuidaResponse<T> = T & { readonly lastResponse: ResponseMeta };
