import { createHmac, timingSafeEqual } from "node:crypto";
import { SignatureVerificationError } from "./errors.js";
import type { WebhookEvent } from "./generated/types.js";

/** Tolerancia default entre el timestamp del webhook y la hora local, en segundos. */
export const DEFAULT_TOLERANCE = 300;

type Payload = string | Uint8Array;
type HeaderValue = string | string[] | null | undefined;

function headerValue(v: HeaderValue): string | null {
  if (Array.isArray(v)) v = v[0];
  if (typeof v !== "string") return null;
  const t = v.trim();
  return t.length ? t : null;
}

function payloadString(payload: Payload): string {
  if (typeof payload === "string") return payload;
  if (payload instanceof Uint8Array) return Buffer.from(payload.buffer, payload.byteOffset, payload.byteLength).toString("utf8");
  throw new SignatureVerificationError({
    message: "El payload tiene que ser el cuerpo crudo del pedido (string o Buffer), no un objeto ya parseado",
  });
}

/**
 * Verificación de los webhooks que manda Kuida (SDK_DESIGN.md §8). No
 * necesita cliente ni clave de API: solo el secreto del endpoint (`whsec_…`).
 *
 * ```ts
 * const evento = Webhook.constructEvent(
 *   req.body,                          // cuerpo crudo: string o Buffer
 *   req.headers["x-kuida-signature"],
 *   req.headers["x-kuida-timestamp"],
 *   process.env.KUIDA_WEBHOOK_SECRET!,
 * );
 * ```
 */
export class Webhook {
  /** Headers que manda Kuida en cada entrega. */
  static readonly SIGNATURE_HEADER = "x-kuida-signature";
  static readonly TIMESTAMP_HEADER = "x-kuida-timestamp";
  static readonly EVENT_TYPE_HEADER = "x-kuida-event-type";
  static readonly DELIVERY_ATTEMPT_HEADER = "x-kuida-delivery-attempt";

  /**
   * Calcula la firma esperada: `sha256=` + hex de HMAC-SHA256(secret, `${timestamp}.${payload}`).
   * Útil para armar pedidos de prueba.
   */
  static computeSignature(payload: Payload, timestamp: string | number, secret: string): string {
    const body = payloadString(payload);
    return `sha256=${createHmac("sha256", secret).update(`${timestamp}.${body}`).digest("hex")}`;
  }

  /**
   * Verifica la firma de un webhook. Devuelve `true` o lanza
   * {@link SignatureVerificationError}.
   *
   * @param payload Cuerpo crudo del pedido (string o Buffer), nunca un JSON re-serializado.
   * @param signatureHeader Header `x-kuida-signature`.
   * @param timestampHeader Header `x-kuida-timestamp` (segundos Unix).
   * @param secret Secreto del endpoint (`whsec_…`).
   * @param tolerance Diferencia máxima con la hora local, en segundos. `0` desactiva el chequeo. Default 300.
   * @param now Hora actual en segundos Unix. Solo para tests.
   */
  static verifySignature(
    payload: Payload,
    signatureHeader: HeaderValue,
    timestampHeader: HeaderValue,
    secret: string,
    tolerance: number = DEFAULT_TOLERANCE,
    now?: number,
  ): true {
    const signature = headerValue(signatureHeader);
    const timestamp = headerValue(timestampHeader);
    if (!signature) throw new SignatureVerificationError({ message: "Falta el header x-kuida-signature" });
    if (!timestamp) throw new SignatureVerificationError({ message: "Falta el header x-kuida-timestamp" });
    if (!secret) throw new SignatureVerificationError({ message: "Falta el secreto del endpoint" });
    if (!/^\d+$/.test(timestamp)) {
      throw new SignatureVerificationError({ message: "El header x-kuida-timestamp no es un timestamp Unix" });
    }

    const expected = Buffer.from(Webhook.computeSignature(payload, timestamp, secret), "utf8");
    const actual = Buffer.from(signature, "utf8");
    if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) {
      throw new SignatureVerificationError({ message: "La firma del webhook no coincide con el cuerpo recibido" });
    }

    if (tolerance > 0) {
      const current = now ?? Math.floor(Date.now() / 1000);
      if (Math.abs(current - Number(timestamp)) > tolerance) {
        throw new SignatureVerificationError({
          message: `El timestamp del webhook está fuera de la tolerancia de ${tolerance} s`,
        });
      }
    }
    return true;
  }

  /**
   * Verifica la firma y devuelve el evento parseado.
   * Mismos parámetros que {@link Webhook.verifySignature}.
   */
  static constructEvent(
    payload: Payload,
    signatureHeader: HeaderValue,
    timestampHeader: HeaderValue,
    secret: string,
    tolerance: number = DEFAULT_TOLERANCE,
    now?: number,
  ): WebhookEvent {
    Webhook.verifySignature(payload, signatureHeader, timestampHeader, secret, tolerance, now);
    try {
      return JSON.parse(payloadString(payload)) as WebhookEvent;
    } catch (cause) {
      throw new SignatureVerificationError({ message: "El cuerpo del webhook no es JSON válido", cause });
    }
  }
}
