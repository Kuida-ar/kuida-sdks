using System;
using System.Collections.Generic;

namespace Kuida
{
    /// <summary>
    /// Base de todos los errores del SDK. Toda respuesta no 2xx de la API se convierte
    /// en una subclase según <c>error.type</c>.
    /// </summary>
    public class KuidaException : Exception
    {
        private static readonly IReadOnlyDictionary<string, string> NoHeaders =
            new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);

        /// <summary>Crea el error con un mensaje.</summary>
        public KuidaException(string message) : base(message) { }

        /// <summary>Crea el error con un mensaje y la excepción que lo causó.</summary>
        public KuidaException(string message, Exception? innerException) : base(message, innerException) { }

        /// <summary>Código de estado HTTP, si hubo respuesta.</summary>
        public int? HttpStatus { get; internal set; }

        /// <summary>Categoría del error (<c>invalid_request_error</c>, <c>authentication_error</c>, …).</summary>
        public string? Type { get; internal set; }

        /// <summary>Código estable para programar contra él (<c>resource_missing</c>, <c>scope_missing</c>, …).</summary>
        public string? Code { get; internal set; }

        /// <summary>Parámetro que causó el error, si aplica (<c>patient.phone</c>).</summary>
        public string? Param { get; internal set; }

        /// <summary>Id del pedido (del cuerpo o del header <c>Request-Id</c>). Inclúyalo al escribir a soporte.</summary>
        public string? RequestId { get; internal set; }

        /// <summary>Link a la explicación del código de error.</summary>
        public string? DocUrl { get; internal set; }

        /// <summary>Headers de la respuesta (vacío si no hubo respuesta).</summary>
        public IReadOnlyDictionary<string, string> Headers { get; internal set; } = NoHeaders;

        /// <summary>Cuerpo crudo de la respuesta, si hubo.</summary>
        public string? RawBody { get; internal set; }

        /// <summary>
        /// Cuerpo de la respuesta parseado, si era JSON. Además de <c>error</c> puede traer datos
        /// extra, como <c>results</c> en un lote de eventos que no entró.
        /// </summary>
        public System.Text.Json.JsonElement? RawJson { get; internal set; }
    }

    /// <summary>Parámetros inválidos, faltantes o recurso inexistente (<c>invalid_request_error</c>, 400/404/409/413).</summary>
    public class InvalidRequestException : KuidaException
    {
        /// <summary>Crea el error.</summary>
        public InvalidRequestException(string message) : base(message) { }
    }

    /// <summary>Clave de API faltante, inválida, revocada o vencida (<c>authentication_error</c>, 401).</summary>
    public class AuthenticationException : KuidaException
    {
        /// <summary>Crea el error.</summary>
        public AuthenticationException(string message) : base(message) { }
    }

    /// <summary>A la clave le falta el scope o la organización no tiene la línea de servicio (<c>permission_error</c>, 403).</summary>
    public class PermissionException : KuidaException
    {
        /// <summary>Crea el error.</summary>
        public PermissionException(string message) : base(message) { }
    }

    /// <summary>La <c>Idempotency-Key</c> ya se usó con otro pedido o hay otro en curso con la misma (<c>idempotency_error</c>).</summary>
    public class IdempotencyException : KuidaException
    {
        /// <summary>Crea el error.</summary>
        public IdempotencyException(string message) : base(message) { }
    }

    /// <summary>Demasiados pedidos (<c>rate_limit_error</c>, 429). El SDK ya reintentó respetando <c>Retry-After</c>.</summary>
    public class RateLimitException : KuidaException
    {
        /// <summary>Crea el error.</summary>
        public RateLimitException(string message) : base(message) { }
    }

    /// <summary>Error de Kuida (<c>api_error</c>, 5xx) o respuesta que no se pudo interpretar.</summary>
    public class ApiException : KuidaException
    {
        /// <summary>Crea el error.</summary>
        public ApiException(string message) : base(message) { }

        /// <summary>Crea el error con la excepción que lo causó.</summary>
        public ApiException(string message, Exception? innerException) : base(message, innerException) { }
    }

    /// <summary>No hubo respuesta: error de red, DNS, TLS o timeout.</summary>
    public class ApiConnectionException : KuidaException
    {
        /// <summary>Crea el error con la excepción que lo causó.</summary>
        public ApiConnectionException(string message, Exception? innerException) : base(message, innerException) { }
    }

    /// <summary>La firma de un webhook no se pudo verificar (header faltante, firma distinta o timestamp fuera de tolerancia).</summary>
    public class SignatureVerificationException : KuidaException
    {
        /// <summary>Crea el error.</summary>
        public SignatureVerificationException(string message, string? signatureHeader = null) : base(message)
        {
            SignatureHeader = signatureHeader;
        }

        /// <summary>Valor recibido en <c>x-kuida-signature</c>.</summary>
        public string? SignatureHeader { get; }
    }
}
