using System;
using System.Globalization;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace Kuida
{
    /// <summary>
    /// Verificación de los webhooks que manda Kuida. No necesita cliente ni clave de API,
    /// solo el <c>secret</c> del endpoint (<c>whsec_…</c>).
    /// <code>
    /// var evento = Webhook.ConstructEvent(cuerpoCrudo,
    ///     Request.Headers["x-kuida-signature"], Request.Headers["x-kuida-timestamp"], secreto);
    /// </code>
    /// </summary>
    public static class Webhook
    {
        /// <summary>Tolerancia por defecto entre la hora del servidor y el timestamp del webhook: 300 segundos.</summary>
        public const int DefaultTolerance = 300;

        /// <summary>Header con la firma (<c>sha256=&lt;hex&gt;</c>).</summary>
        public const string SignatureHeaderName = "x-kuida-signature";

        /// <summary>Header con el timestamp (epoch en segundos).</summary>
        public const string TimestampHeaderName = "x-kuida-timestamp";

        /// <summary>Header con el tipo de evento.</summary>
        public const string EventTypeHeaderName = "x-kuida-event-type";

        /// <summary>Header con el número de intento de entrega.</summary>
        public const string DeliveryAttemptHeaderName = "x-kuida-delivery-attempt";

        /// <summary>
        /// Verifica la firma y devuelve el evento parseado.
        /// </summary>
        /// <param name="payload">Cuerpo <b>crudo</b> del pedido, tal cual llegó. Nunca un JSON re-serializado.</param>
        /// <param name="signatureHeader">Valor de <c>x-kuida-signature</c>.</param>
        /// <param name="timestampHeader">Valor de <c>x-kuida-timestamp</c>.</param>
        /// <param name="secret">Secreto del endpoint (<c>whsec_…</c>).</param>
        /// <param name="tolerance">Diferencia máxima en segundos con la hora actual. 0 desactiva el chequeo.</param>
        /// <param name="now">Hora actual; solo para tests. Default: <see cref="DateTimeOffset.UtcNow"/>.</param>
        /// <exception cref="SignatureVerificationException">Si falta un header, la firma no coincide o el timestamp está fuera de tolerancia.</exception>
        public static WebhookEvent ConstructEvent(string payload, string? signatureHeader, string? timestampHeader, string secret,
            int tolerance = DefaultTolerance, DateTimeOffset? now = null)
        {
            if (payload == null) throw new ArgumentNullException(nameof(payload));
            return ConstructEvent(Encoding.UTF8.GetBytes(payload), signatureHeader, timestampHeader, secret, tolerance, now);
        }

        /// <summary>Igual que <see cref="ConstructEvent(string, string, string, string, int, DateTimeOffset?)"/>, con el cuerpo en bytes.</summary>
        /// <exception cref="SignatureVerificationException">Si la firma no se puede verificar.</exception>
        public static WebhookEvent ConstructEvent(byte[] payload, string? signatureHeader, string? timestampHeader, string secret,
            int tolerance = DefaultTolerance, DateTimeOffset? now = null)
        {
            VerifySignature(payload, signatureHeader, timestampHeader, secret, tolerance, now);
            WebhookEvent? evt;
            JsonElement root;
            try
            {
                evt = JsonSerializer.Deserialize<WebhookEvent>(payload, KuidaJson.Options);
                using (var doc = JsonDocument.Parse(payload)) root = doc.RootElement.Clone();
            }
            catch (JsonException e)
            {
                throw new ApiException($"El cuerpo del webhook no es JSON válido: {e.Message}", e);
            }
            if (evt == null) throw new ApiException("El cuerpo del webhook vino vacío.");
            evt.RawJson = root;
            return evt;
        }

        /// <summary>
        /// Verifica la firma de un webhook. Devuelve <c>true</c> o lanza.
        /// </summary>
        /// <param name="payload">Cuerpo <b>crudo</b> del pedido.</param>
        /// <param name="signatureHeader">Valor de <c>x-kuida-signature</c>.</param>
        /// <param name="timestampHeader">Valor de <c>x-kuida-timestamp</c>.</param>
        /// <param name="secret">Secreto del endpoint (<c>whsec_…</c>).</param>
        /// <param name="tolerance">Diferencia máxima en segundos con la hora actual. 0 desactiva el chequeo.</param>
        /// <param name="now">Hora actual; solo para tests.</param>
        /// <exception cref="SignatureVerificationException">Si la firma no se puede verificar.</exception>
        public static bool VerifySignature(string payload, string? signatureHeader, string? timestampHeader, string secret,
            int tolerance = DefaultTolerance, DateTimeOffset? now = null)
        {
            if (payload == null) throw new ArgumentNullException(nameof(payload));
            return VerifySignature(Encoding.UTF8.GetBytes(payload), signatureHeader, timestampHeader, secret, tolerance, now);
        }

        /// <summary>Igual que <see cref="VerifySignature(string, string, string, string, int, DateTimeOffset?)"/>, con el cuerpo en bytes.</summary>
        /// <exception cref="SignatureVerificationException">Si la firma no se puede verificar.</exception>
        public static bool VerifySignature(byte[] payload, string? signatureHeader, string? timestampHeader, string secret,
            int tolerance = DefaultTolerance, DateTimeOffset? now = null)
        {
            if (payload == null) throw new ArgumentNullException(nameof(payload));
            if (string.IsNullOrEmpty(secret)) throw new ArgumentException("Falta el secreto del endpoint.", nameof(secret));
            if (string.IsNullOrWhiteSpace(signatureHeader))
                throw new SignatureVerificationException($"Falta el header {SignatureHeaderName}.", signatureHeader);
            if (string.IsNullOrWhiteSpace(timestampHeader))
                throw new SignatureVerificationException($"Falta el header {TimestampHeaderName}.", signatureHeader);

            var ts = timestampHeader!.Trim();
            if (!long.TryParse(ts, NumberStyles.None, CultureInfo.InvariantCulture, out var timestamp))
                throw new SignatureVerificationException($"El header {TimestampHeaderName} no es un número de segundos.", signatureHeader);

            var expected = ComputeSignature(payload, ts, secret);
            if (!FixedTimeEquals(expected, signatureHeader!.Trim()))
                throw new SignatureVerificationException("La firma no coincide con el cuerpo recibido.", signatureHeader);

            if (tolerance > 0)
            {
                var current = (now ?? DateTimeOffset.UtcNow).ToUnixTimeSeconds();
                if (Math.Abs(current - timestamp) > tolerance)
                    throw new SignatureVerificationException("El timestamp del webhook está fuera de la tolerancia.", signatureHeader);
            }
            return true;
        }

        /// <summary>
        /// Calcula el header de firma que manda Kuida: <c>sha256=</c> + hex de
        /// HMAC-SHA256(secret, <c>"{timestamp}.{payload}"</c>). Útil para probar su endpoint.
        /// </summary>
        public static string ComputeSignature(string payload, string timestamp, string secret) =>
            ComputeSignature(Encoding.UTF8.GetBytes(payload ?? throw new ArgumentNullException(nameof(payload))), timestamp, secret);

        /// <summary>Igual que <see cref="ComputeSignature(string, string, string)"/>, con el cuerpo en bytes.</summary>
        public static string ComputeSignature(byte[] payload, string timestamp, string secret)
        {
            if (payload == null) throw new ArgumentNullException(nameof(payload));
            var prefix = Encoding.UTF8.GetBytes(timestamp + ".");
            var message = new byte[prefix.Length + payload.Length];
            Buffer.BlockCopy(prefix, 0, message, 0, prefix.Length);
            Buffer.BlockCopy(payload, 0, message, prefix.Length, payload.Length);
            byte[] hash;
            using (var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(secret))) hash = hmac.ComputeHash(message);
            var sb = new StringBuilder("sha256=", 7 + hash.Length * 2);
            foreach (var b in hash) sb.Append(b.ToString("x2", CultureInfo.InvariantCulture));
            return sb.ToString();
        }

        /// <summary>Comparación en tiempo constante (no corta en el primer carácter distinto).</summary>
        internal static bool FixedTimeEquals(string a, string b)
        {
            var x = Encoding.UTF8.GetBytes(a);
            var y = Encoding.UTF8.GetBytes(b);
#if NET8_0_OR_GREATER
            return CryptographicOperations.FixedTimeEquals(x, y);
#else
            var diff = x.Length ^ y.Length;
            var n = Math.Min(x.Length, y.Length);
            for (var i = 0; i < n; i++) diff |= x[i] ^ y[i];
            return diff == 0;
#endif
        }
    }
}
