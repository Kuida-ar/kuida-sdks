using System;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Configuración del cliente. Todas las opciones tienen default.</summary>
    public sealed class KuidaClientOptions
    {
        /// <summary>
        /// URL base de la API. Default: la variable de entorno <c>KUIDA_API_BASE</c> o
        /// <c>https://www.kuida.ar/api</c>.
        /// </summary>
        public string? BaseUrl { get; set; }

        /// <summary>Tiempo máximo de cada intento. Default: 30 segundos.</summary>
        public TimeSpan? Timeout { get; set; }

        /// <summary>
        /// Reintentos ante errores de red, timeouts, 409 <c>idempotency_key_in_use</c>, 429 y
        /// 500/502/503/504. Default: 2 (tres intentos en total). 0 los desactiva.
        /// </summary>
        public int? MaxRetries { get; set; }

        /// <summary>Versión fechada de la API (header <c>Kuida-Version</c>). Default: la del SDK.</summary>
        public string? ApiVersion { get; set; }

        /// <summary>
        /// Espera entre reintentos. Solo para tests: inyecte una espera nula
        /// (<c>(d, ct) =&gt; Task.CompletedTask</c>) para no dormir. Default: <c>Task.Delay</c>.
        /// </summary>
        public Func<TimeSpan, CancellationToken, Task>? RetryDelay { get; set; }
    }

    /// <summary>Opciones de un pedido puntual. Pisan las del cliente.</summary>
    public sealed class RequestOptions
    {
        /// <summary>
        /// Clave de idempotencia del POST. Si no se pasa, el SDK genera un UUID v4 y lo
        /// reusa en todos los reintentos del mismo pedido.
        /// </summary>
        public string? IdempotencyKey { get; set; }

        /// <summary>Tiempo máximo de cada intento de este pedido.</summary>
        public TimeSpan? Timeout { get; set; }

        /// <summary>Reintentos de este pedido.</summary>
        public int? MaxRetries { get; set; }
    }
}
