/**
 * @kuida/sdk — SDK oficial de la API de Kuida para Node.js y TypeScript.
 *
 * Contrato común de los SDKs: SDK_DESIGN.md en github.com/Kuida-ar/kuida-sdks.
 * Documentación de la API: https://www.kuida.ar/desarrolladores
 */
import { Kuida } from "./client.js";

export { Kuida };
export type { RequestSpec } from "./client.js";
export {
  KuidaError,
  InvalidRequestError,
  AuthenticationError,
  PermissionError,
  IdempotencyError,
  RateLimitError,
  APIError,
  APIConnectionError,
  SignatureVerificationError,
} from "./errors.js";
export type { KuidaErrorInit } from "./errors.js";
export { ListPromise } from "./pagination.js";
export type { ApiList, PaginationParams, AutoPagingToArrayOptions } from "./pagination.js";
export {
  Account as AccountResource,
  Patients,
  Doctors,
  Appointments,
  Visits,
  IntakeRequests,
  Treatments,
  Events,
  WebhookEndpoints,
  WebhookDeliveries,
} from "./resources.js";
export type { GenericEventInput } from "./resources.js";
export type { KuidaConfig, RequestOptions, ResponseMeta, KuidaResponse } from "./types.js";
export { Webhook, DEFAULT_TOLERANCE } from "./webhook.js";
export { SDK_VERSION } from "./version.js";
export * from "./generated/types.js";

export default Kuida;
