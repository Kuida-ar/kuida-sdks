import { randomUUID } from "node:crypto";
import { APIConnectionError, AuthenticationError, KuidaError, errorFromResponse } from "./errors.js";
import { API_VERSION } from "./generated/types.js";
import {
  Account,
  Appointments,
  Doctors,
  Events,
  IntakeRequests,
  Patients,
  Treatments,
  Visits,
  WebhookDeliveries,
  WebhookEndpoints,
} from "./resources.js";
import type { KuidaConfig, KuidaResponse, RequestOptions, ResponseMeta } from "./types.js";
import { SDK_VERSION } from "./version.js";
import { Webhook } from "./webhook.js";

const DEFAULT_BASE_URL = "https://www.kuida.ar/api";
const DEFAULT_TIMEOUT_MS = 30_000;
const DEFAULT_MAX_RETRIES = 2;
const MAX_RETRY_AFTER_MS = 60_000;
const RETRYABLE_STATUS = new Set([429, 500, 502, 503, 504]);

type HttpMethod = "GET" | "POST" | "PATCH" | "DELETE";
type QueryValue = string | number | boolean | Date | null | undefined | ReadonlyArray<string | number | boolean | Date>;

/** @internal */
export interface RequestSpec {
  method: HttpMethod;
  path: string;
  query?: object;
  body?: unknown;
  options?: RequestOptions;
}

const env = (name: string): string | undefined => {
  const v = typeof process !== "undefined" ? process.env?.[name] : undefined;
  return v && v.trim() ? v.trim() : undefined;
};

const defaultSleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms));

function queryString(query: object | undefined): string {
  if (!query) return "";
  const qs = new URLSearchParams();
  const str = (v: string | number | boolean | Date) => (v instanceof Date ? v.toISOString() : String(v));
  for (const [k, raw] of Object.entries(query as Record<string, QueryValue>)) {
    if (raw === undefined || raw === null) continue;
    if (Array.isArray(raw)) for (const v of raw) qs.append(k, str(v));
    else qs.append(k, str(raw as string | number | boolean | Date));
  }
  const s = qs.toString();
  return s ? `?${s}` : "";
}

function headersToObject(h: Headers): Record<string, string> {
  const out: Record<string, string> = {};
  h.forEach((v, k) => {
    out[k.toLowerCase()] = v;
  });
  return out;
}

/** Espera antes del reintento `attempt` (0 = primer reintento): 0,5 s × 2^n, tope 8 s, jitter ±25 %. */
function backoffMs(attempt: number): number {
  const base = Math.min(500 * 2 ** attempt, 8_000);
  return Math.round(base * (0.75 + Math.random() * 0.5));
}

function retryAfterMs(headers: Record<string, string>): number | null {
  const v = headers["retry-after"];
  if (!v) return null;
  const secs = Number(v);
  let ms: number;
  if (Number.isFinite(secs)) ms = secs * 1000;
  else {
    const at = Date.parse(v);
    if (Number.isNaN(at)) return null;
    ms = at - Date.now();
  }
  return Math.min(Math.max(ms, 0), MAX_RETRY_AFTER_MS);
}

function attachMeta<T>(value: T, meta: ResponseMeta): KuidaResponse<T> {
  if (value !== null && typeof value === "object") {
    Object.defineProperty(value, "lastResponse", { value: meta, enumerable: false, writable: false, configurable: true });
  }
  return value as KuidaResponse<T>;
}

/**
 * Cliente de la API de Kuida.
 *
 * ```ts
 * import { Kuida } from "@kuida/sdk";
 * const kuida = new Kuida("kd_live_…");
 * const paciente = await kuida.patients.create({ phone: "+54 9 342 555 0000", fullName: "Paciente Demo" });
 * ```
 *
 * Conviene reutilizar una sola instancia por proceso.
 */
export class Kuida {
  /** Versión de la API que usa el SDK por default. */
  static readonly API_VERSION = API_VERSION;
  /** Versión del SDK. */
  static readonly VERSION = SDK_VERSION;
  /** Verificación de webhooks (también exportada como `Webhook`). */
  static readonly Webhook = Webhook;

  /** La organización dueña de la clave. */
  readonly account: Account;
  /** Pacientes. */
  readonly patients: Patients;
  /** Profesionales (solo lectura). */
  readonly doctors: Doctors;
  /** Turnos. */
  readonly appointments: Appointments;
  /** Consultas atendidas. */
  readonly visits: Visits;
  /** Solicitudes de ingreso. */
  readonly intakeRequests: IntakeRequests;
  /** Tratamientos e indicaciones. */
  readonly treatments: Treatments;
  /** Eventos del catálogo v1. */
  readonly events: Events;
  /** Endpoints de webhook. */
  readonly webhookEndpoints: WebhookEndpoints;
  /** Entregas de webhook. */
  readonly webhookDeliveries: WebhookDeliveries;
  /** Atajo a {@link Webhook} para verificar firmas. */
  readonly webhooks = Webhook;

  /** URL base efectiva, sin barra final. */
  readonly baseUrl: string;
  /** Timeout por intento, en milisegundos. */
  readonly timeout: number;
  /** Reintentos por default. */
  readonly maxRetries: number;
  /** Valor del header `Kuida-Version`. */
  readonly apiVersion: string;

  readonly #apiKey: string;
  readonly #fetch: typeof fetch;
  readonly #sleep: (ms: number) => Promise<void>;
  readonly #clientUserAgent: string;

  /**
   * @param apiKey Clave de API. Si no se pasa, se lee `KUIDA_API_KEY`.
   * @param config Opciones del cliente.
   * @throws {AuthenticationError} si no hay clave.
   */
  constructor(apiKey?: string | null, config?: KuidaConfig);
  /** @param config Opciones del cliente, incluida `apiKey`. */
  constructor(config: KuidaConfig);
  constructor(apiKeyOrConfig?: string | null | KuidaConfig, maybeConfig: KuidaConfig = {}) {
    const config: KuidaConfig =
      apiKeyOrConfig && typeof apiKeyOrConfig === "object" ? apiKeyOrConfig : { ...maybeConfig, apiKey: apiKeyOrConfig ?? maybeConfig.apiKey };
    const apiKey = (config.apiKey ?? env("KUIDA_API_KEY") ?? "").trim();
    if (!apiKey) {
      throw new AuthenticationError({
        message: "Falta la clave de API: se pasa al construir el cliente (new Kuida(\"kd_live_…\")) o en la variable de entorno KUIDA_API_KEY",
        type: "authentication_error",
        code: "api_key_missing",
      });
    }
    const fetchImpl = config.fetch ?? (typeof fetch === "function" ? fetch : undefined);
    if (!fetchImpl) throw new KuidaError({ message: "No hay fetch disponible: se requiere Node 18 o superior, o una implementación en config.fetch" });

    this.#apiKey = apiKey;
    this.#fetch = fetchImpl;
    this.#sleep = config.sleep ?? defaultSleep;
    this.baseUrl = (config.baseUrl ?? env("KUIDA_API_BASE") ?? DEFAULT_BASE_URL).replace(/\/+$/, "");
    this.timeout = config.timeout ?? DEFAULT_TIMEOUT_MS;
    this.maxRetries = config.maxRetries ?? DEFAULT_MAX_RETRIES;
    this.apiVersion = config.apiVersion ?? API_VERSION;
    this.#clientUserAgent = JSON.stringify({
      bindings_version: SDK_VERSION,
      lang: "node",
      lang_version: typeof process !== "undefined" ? process.version : "unknown",
      platform: typeof process !== "undefined" ? `${process.platform} ${process.arch}` : "unknown",
    });

    this.account = new Account(this);
    this.patients = new Patients(this);
    this.doctors = new Doctors(this);
    this.appointments = new Appointments(this);
    this.visits = new Visits(this);
    this.intakeRequests = new IntakeRequests(this);
    this.treatments = new Treatments(this);
    this.events = new Events(this);
    this.webhookEndpoints = new WebhookEndpoints(this);
    this.webhookDeliveries = new WebhookDeliveries(this);
  }

  /**
   * Hace un pedido a la API con idempotencia automática y reintentos.
   * Los métodos de recurso lo usan; sirve también para rutas que el SDK todavía no cubre.
   */
  async request<T>(spec: RequestSpec): Promise<KuidaResponse<T>> {
    const { method, path, query, body, options = {} } = spec;
    const url = `${this.baseUrl}${path}${queryString(query)}`;
    const maxRetries = Math.max(0, options.maxRetries ?? this.maxRetries);
    const timeout = options.timeout ?? this.timeout;

    const headers: Record<string, string> = {
      Authorization: `Bearer ${this.#apiKey}`,
      Accept: "application/json",
      "Kuida-Version": this.apiVersion,
      "User-Agent": `Kuida/v1 NodeBindings/${SDK_VERSION}`,
      "X-Kuida-Client-User-Agent": this.#clientUserAgent,
    };
    let payload: string | undefined;
    if (method === "POST" || method === "PATCH") {
      headers["Content-Type"] = "application/json";
      payload = JSON.stringify(body ?? {});
    }
    // La misma clave viaja en todos los intentos del mismo pedido.
    if (method === "POST") headers["Idempotency-Key"] = options.idempotencyKey ?? randomUUID();

    for (let attempt = 0; ; attempt++) {
      let status: number;
      let resHeaders: Record<string, string>;
      let text: string;
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), timeout);
      try {
        const res = await this.#fetch(url, { method, headers, body: payload, signal: controller.signal });
        status = res.status;
        resHeaders = headersToObject(res.headers);
        text = await res.text();
      } catch (cause) {
        if (attempt < maxRetries) {
          await this.#sleep(backoffMs(attempt));
          continue;
        }
        const timedOut = controller.signal.aborted;
        throw new APIConnectionError({
          message: timedOut
            ? `La API no respondió en ${timeout} ms (${method} ${path})`
            : `No se pudo conectar con la API de Kuida (${method} ${path}): ${(cause as Error)?.message ?? cause}`,
          cause,
        });
      } finally {
        clearTimeout(timer);
      }

      if (status >= 200 && status < 300) {
        const meta: ResponseMeta = { statusCode: status, headers: resHeaders, requestId: resHeaders["request-id"] ?? null, rawBody: text };
        let parsed: unknown = {};
        if (text) {
          try {
            parsed = JSON.parse(text);
          } catch {
            throw errorFromResponse(status, resHeaders, text);
          }
        }
        return attachMeta(parsed as T, meta);
      }

      const error = errorFromResponse(status, resHeaders, text);
      const retryable =
        RETRYABLE_STATUS.has(status) || (status === 409 && error.code === "idempotency_key_in_use");
      if (retryable && attempt < maxRetries) {
        await this.#sleep(retryAfterMs(resHeaders) ?? backoffMs(attempt));
        continue;
      }
      throw error;
    }
  }
}
