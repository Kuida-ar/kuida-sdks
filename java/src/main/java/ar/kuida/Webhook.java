package ar.kuida;

import ar.kuida.exception.SignatureVerificationException;
import ar.kuida.model.WebhookEvent;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verificación de los webhooks que manda Kuida. No necesita cliente ni clave de API.
 *
 * <p>Kuida firma cada envío con el {@code secret} del endpoint: el header {@code x-kuida-signature}
 * trae {@code sha256=} + el hex de HMAC-SHA256(secret, {@code "<timestamp>.<cuerpo>"}) y
 * {@code x-kuida-timestamp} los segundos Unix del envío.
 *
 * <pre>{@code
 * WebhookEvent event = Webhook.constructEvent(
 *     rawBody,                                  // el cuerpo crudo, sin re-serializar
 *     request.getHeader("x-kuida-signature"),
 *     request.getHeader("x-kuida-timestamp"),
 *     System.getenv("KUIDA_WEBHOOK_SECRET"));
 * }</pre>
 */
public final class Webhook {
  /** Tolerancia por defecto entre el timestamp del envío y la hora local, en segundos. */
  public static final long DEFAULT_TOLERANCE = 300;

  /** Header con la firma. */
  public static final String SIGNATURE_HEADER = "x-kuida-signature";
  /** Header con el timestamp. */
  public static final String TIMESTAMP_HEADER = "x-kuida-timestamp";
  /** Header con el tipo de evento. */
  public static final String EVENT_TYPE_HEADER = "x-kuida-event-type";
  /** Header con el número de intento. */
  public static final String DELIVERY_ATTEMPT_HEADER = "x-kuida-delivery-attempt";

  private static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final String PREFIX = "sha256=";

  private Webhook() {}

  /**
   * Verifica la firma y devuelve el evento parseado, con la tolerancia por defecto (300 s).
   *
   * @param payload el cuerpo crudo del pedido, tal como llegó
   * @param signatureHeader valor de {@code x-kuida-signature}
   * @param timestampHeader valor de {@code x-kuida-timestamp}
   * @param secret el {@code secret} del endpoint ({@code whsec_…})
   * @throws SignatureVerificationException si la firma no verifica o el cuerpo no es un evento
   */
  public static WebhookEvent constructEvent(String payload, String signatureHeader, String timestampHeader, String secret) {
    return constructEvent(payload, signatureHeader, timestampHeader, secret, DEFAULT_TOLERANCE);
  }

  /**
   * Igual que {@link #constructEvent(String, String, String, String)} con otra tolerancia en
   * segundos; {@code 0} desactiva el chequeo de tiempo.
   */
  public static WebhookEvent constructEvent(String payload, String signatureHeader, String timestampHeader, String secret,
      long tolerance) {
    return constructEvent(payload, signatureHeader, timestampHeader, secret, tolerance, nowSeconds());
  }

  /** Igual que {@link #constructEvent(String, String, String, String, long)} con el "ahora" dado (tests). */
  public static WebhookEvent constructEvent(String payload, String signatureHeader, String timestampHeader, String secret,
      long tolerance, long nowEpochSeconds) {
    verifySignature(payload, signatureHeader, timestampHeader, secret, tolerance, nowEpochSeconds);
    try {
      JsonElement tree = JsonParser.parseString(payload);
      if (tree == null || !tree.isJsonObject()) throw new SignatureVerificationException("El cuerpo no es un evento JSON");
      return KuidaJson.parse(tree, WebhookEvent.class, null);
    } catch (JsonParseException e) {
      throw new SignatureVerificationException("El cuerpo no es JSON válido", e);
    }
  }

  /** Igual que {@link #constructEvent(String, String, String, String)} con el cuerpo en bytes (UTF-8). */
  public static WebhookEvent constructEvent(byte[] payload, String signatureHeader, String timestampHeader, String secret) {
    return constructEvent(toText(payload), signatureHeader, timestampHeader, secret);
  }

  /** Igual que {@link #constructEvent(String, String, String, String, long)} con el cuerpo en bytes. */
  public static WebhookEvent constructEvent(byte[] payload, String signatureHeader, String timestampHeader, String secret,
      long tolerance) {
    return constructEvent(toText(payload), signatureHeader, timestampHeader, secret, tolerance);
  }

  /**
   * Verifica la firma con la tolerancia por defecto (300 s).
   *
   * @return {@code true} si verifica
   * @throws SignatureVerificationException si falta un header, la firma no coincide o el
   *     timestamp está fuera de tolerancia
   */
  public static boolean verifySignature(String payload, String signatureHeader, String timestampHeader, String secret) {
    return verifySignature(payload, signatureHeader, timestampHeader, secret, DEFAULT_TOLERANCE);
  }

  /** Igual que {@link #verifySignature(String, String, String, String)}; {@code tolerance = 0} no chequea el tiempo. */
  public static boolean verifySignature(String payload, String signatureHeader, String timestampHeader, String secret,
      long tolerance) {
    return verifySignature(payload, signatureHeader, timestampHeader, secret, tolerance, nowSeconds());
  }

  /** Igual que {@link #verifySignature(String, String, String, String, long)} con el "ahora" dado (tests). */
  public static boolean verifySignature(String payload, String signatureHeader, String timestampHeader, String secret,
      long tolerance, long nowEpochSeconds) {
    if (payload == null) throw new SignatureVerificationException("Falta el cuerpo del webhook");
    if (isBlank(signatureHeader)) throw new SignatureVerificationException("Falta el header " + SIGNATURE_HEADER);
    if (isBlank(timestampHeader)) throw new SignatureVerificationException("Falta el header " + TIMESTAMP_HEADER);
    if (isBlank(secret)) throw new SignatureVerificationException("Falta el secreto del endpoint");
    String timestamp = timestampHeader.trim();
    long ts;
    try {
      ts = Long.parseLong(timestamp);
    } catch (NumberFormatException e) {
      throw new SignatureVerificationException("El header " + TIMESTAMP_HEADER + " no es un número: " + timestamp);
    }
    byte[] expected = hmac(secret, timestamp + "." + payload).getBytes(UTF_8);
    boolean match = false;
    for (String part : signatureHeader.split(",")) {
      String candidate = part.trim();
      if (!candidate.startsWith(PREFIX)) continue;
      String hex = candidate.substring(PREFIX.length()).toLowerCase(java.util.Locale.ROOT);
      if (MessageDigest.isEqual(expected, hex.getBytes(UTF_8))) match = true;
    }
    if (!match) throw new SignatureVerificationException("La firma del webhook no coincide");
    if (tolerance > 0 && Math.abs(nowEpochSeconds - ts) > tolerance) {
      throw new SignatureVerificationException("El timestamp del webhook está fuera de la tolerancia de " + tolerance + " s");
    }
    return true;
  }

  /** Igual que {@link #verifySignature(String, String, String, String)} con el cuerpo en bytes (UTF-8). */
  public static boolean verifySignature(byte[] payload, String signatureHeader, String timestampHeader, String secret) {
    return verifySignature(toText(payload), signatureHeader, timestampHeader, secret);
  }

  /**
   * Calcula el header de firma para un cuerpo y un timestamp: {@code sha256=<hex>}. Sirve para
   * probar tu endpoint localmente.
   */
  public static String computeSignature(String payload, String timestamp, String secret) {
    return PREFIX + hmac(secret, timestamp + "." + payload);
  }

  private static String hmac(String secret, String message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(message.getBytes(UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    } catch (InvalidKeyException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String toText(byte[] payload) {
    return payload == null ? null : new String(payload, UTF_8);
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static long nowSeconds() {
    return System.currentTimeMillis() / 1000L;
  }
}
