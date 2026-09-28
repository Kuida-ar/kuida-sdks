using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Capa HTTP: headers, idempotencia, reintentos y conversión de errores.</summary>
    internal sealed class ApiRequestor
    {
        internal const string SdkVersion = "0.1.0";
        internal static readonly HttpMethod Patch = new HttpMethod("PATCH");

        private static readonly Lazy<HttpClient> SharedHttpClient = new Lazy<HttpClient>(CreateDefaultHttpClient);
        private static readonly object RandomLock = new object();
        private static readonly Random Jitter = new Random();
        private static readonly string ClientUserAgent = BuildClientUserAgent();

        private readonly HttpClient _http;
        private readonly string _apiKey;
        private readonly string _baseUrl;
        private readonly TimeSpan _timeout;
        private readonly int _maxRetries;
        private readonly string _apiVersion;
        private readonly Func<TimeSpan, CancellationToken, Task> _delay;

        internal ApiRequestor(string apiKey, KuidaClientOptions options, HttpClient? httpClient)
        {
            _apiKey = apiKey;
            _http = httpClient ?? SharedHttpClient.Value;
            var baseUrl = options.BaseUrl;
            if (string.IsNullOrWhiteSpace(baseUrl)) baseUrl = Environment.GetEnvironmentVariable("KUIDA_API_BASE");
            if (string.IsNullOrWhiteSpace(baseUrl)) baseUrl = KuidaClient.DefaultBaseUrl;
            _baseUrl = baseUrl!.TrimEnd('/');
            _timeout = options.Timeout ?? KuidaClient.DefaultTimeout;
            _maxRetries = Math.Max(0, options.MaxRetries ?? KuidaClient.DefaultMaxRetries);
            _apiVersion = string.IsNullOrWhiteSpace(options.ApiVersion) ? KuidaClient.ApiVersion : options.ApiVersion!;
            _delay = options.RetryDelay ?? ((d, ct) => Task.Delay(d, ct));
        }

        internal string BaseUrl => _baseUrl;

        internal static string UserAgent => $"Kuida/v1 DotNetBindings/{SdkVersion}";

        private static HttpClient CreateDefaultHttpClient()
        {
#if NET8_0_OR_GREATER
            var handler = new SocketsHttpHandler
            {
                PooledConnectionLifetime = TimeSpan.FromMinutes(5),
                AutomaticDecompression = System.Net.DecompressionMethods.GZip | System.Net.DecompressionMethods.Deflate,
            };
#else
            var handler = new HttpClientHandler
            {
                AutomaticDecompression = System.Net.DecompressionMethods.GZip | System.Net.DecompressionMethods.Deflate,
            };
#endif
            // El timeout lo maneja el SDK por intento.
            return new HttpClient(handler) { Timeout = System.Threading.Timeout.InfiniteTimeSpan };
        }

        private static string BuildClientUserAgent()
        {
            string Safe(Func<string> f)
            {
                try { return f(); } catch { return "unknown"; }
            }
            var ua = new Dictionary<string, string>
            {
                ["bindings_version"] = SdkVersion,
                ["lang"] = "dotnet",
                ["lang_version"] = Safe(() => RuntimeInformation.FrameworkDescription),
                ["platform"] = Safe(() => RuntimeInformation.OSDescription),
            };
            return JsonSerializer.Serialize(ua, KuidaJson.Options);
        }

        internal static string EscapePath(string id)
        {
            if (string.IsNullOrEmpty(id)) throw new ArgumentException("El id no puede estar vacío.", nameof(id));
            return Uri.EscapeDataString(id);
        }

        internal async Task<T> RequestAsync<T>(
            HttpMethod method,
            string path,
            object? body,
            ListParams? query,
            RequestOptions? options,
            CancellationToken cancellationToken)
            where T : class
        {
            var url = BuildUrl(path, query);
            string? json = null;
            if (body != null) json = KuidaJson.Serialize(body);
            else if (method == HttpMethod.Post || method == Patch) json = "{}";

            string? idempotencyKey = null;
            if (method == HttpMethod.Post)
            {
                idempotencyKey = string.IsNullOrEmpty(options?.IdempotencyKey) ? Guid.NewGuid().ToString("D") : options!.IdempotencyKey;
            }

            var maxRetries = Math.Max(0, options?.MaxRetries ?? _maxRetries);
            var timeout = options?.Timeout ?? _timeout;

            for (var attempt = 0; ; attempt++)
            {
                int status;
                IReadOnlyDictionary<string, string> headers;
                string text;
                TimeSpan? retryAfter;

                using (var request = BuildRequest(method, url, json, idempotencyKey))
                using (var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken))
                {
                    if (timeout > TimeSpan.Zero && timeout != System.Threading.Timeout.InfiniteTimeSpan) cts.CancelAfter(timeout);
                    Exception? connectionError = null;
                    HttpResponseMessage? response = null;
                    try
                    {
                        response = await _http.SendAsync(request, HttpCompletionOption.ResponseContentRead, cts.Token).ConfigureAwait(false);
                    }
                    catch (OperationCanceledException e) when (!cancellationToken.IsCancellationRequested)
                    {
                        connectionError = new ApiConnectionException(
                            $"El pedido a Kuida superó el tiempo máximo ({timeout.TotalSeconds.ToString(CultureInfo.InvariantCulture)} s).", e);
                    }
                    catch (HttpRequestException e)
                    {
                        connectionError = new ApiConnectionException($"No se pudo conectar con Kuida: {e.Message}", e);
                    }

                    if (connectionError != null)
                    {
                        if (attempt < maxRetries)
                        {
                            await _delay(Backoff(attempt, null), cancellationToken).ConfigureAwait(false);
                            continue;
                        }
                        throw connectionError;
                    }

                    using (response!)
                    {
                        status = (int)response!.StatusCode;
                        headers = CollectHeaders(response);
                        text = response.Content == null ? string.Empty : await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                        retryAfter = ParseRetryAfter(response);
                    }
                }

                var kuidaResponse = new KuidaResponse(status, headers, text);
                if (status >= 200 && status < 300) return Deserialize<T>(text, kuidaResponse);

                var error = BuildException(status, headers, text);
                if (attempt < maxRetries && ShouldRetry(status, error.Code))
                {
                    await _delay(Backoff(attempt, retryAfter), cancellationToken).ConfigureAwait(false);
                    continue;
                }
                throw error;
            }
        }

        private string BuildUrl(string path, ListParams? query)
        {
            var sb = new StringBuilder(_baseUrl).Append(path);
            if (query != null)
            {
                var pairs = new List<KeyValuePair<string, string>>();
                query.AppendQuery(pairs);
                for (var i = 0; i < pairs.Count; i++)
                {
                    sb.Append(i == 0 ? '?' : '&')
                        .Append(Uri.EscapeDataString(pairs[i].Key))
                        .Append('=')
                        .Append(Uri.EscapeDataString(pairs[i].Value));
                }
            }
            return sb.ToString();
        }

        private HttpRequestMessage BuildRequest(HttpMethod method, string url, string? json, string? idempotencyKey)
        {
            var request = new HttpRequestMessage(method, url);
            request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", _apiKey);
            request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            request.Headers.TryAddWithoutValidation("Kuida-Version", _apiVersion);
            request.Headers.TryAddWithoutValidation("User-Agent", UserAgent);
            request.Headers.TryAddWithoutValidation("X-Kuida-Client-User-Agent", ClientUserAgent);
            if (idempotencyKey != null) request.Headers.TryAddWithoutValidation("Idempotency-Key", idempotencyKey);
            if (json != null)
            {
                request.Content = new StringContent(json, Encoding.UTF8);
                request.Content.Headers.ContentType = new MediaTypeHeaderValue("application/json");
            }
            return request;
        }

        private static IReadOnlyDictionary<string, string> CollectHeaders(HttpResponseMessage response)
        {
            var headers = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
            foreach (var h in response.Headers) headers[h.Key] = string.Join(", ", h.Value);
            if (response.Content != null) foreach (var h in response.Content.Headers) headers[h.Key] = string.Join(", ", h.Value);
            return headers;
        }

        private static TimeSpan? ParseRetryAfter(HttpResponseMessage response)
        {
            var ra = response.Headers.RetryAfter;
            if (ra == null) return null;
            if (ra.Delta.HasValue) return ra.Delta.Value;
            if (ra.Date.HasValue)
            {
                var d = ra.Date.Value - DateTimeOffset.UtcNow;
                return d > TimeSpan.Zero ? d : TimeSpan.Zero;
            }
            return null;
        }

        private static bool ShouldRetry(int status, string? code) =>
            status == 429 || status == 500 || status == 502 || status == 503 || status == 504
            || (status == 409 && code == "idempotency_key_in_use");

        /// <summary><c>min(0.5 s × 2^intento, 8 s)</c> con jitter de ±25 %; <c>Retry-After</c> manda, hasta 60 s.</summary>
        internal static TimeSpan Backoff(int attempt, TimeSpan? retryAfter)
        {
            if (retryAfter.HasValue)
            {
                var s = Math.Min(Math.Max(retryAfter.Value.TotalSeconds, 0), 60);
                return TimeSpan.FromSeconds(s);
            }
            var baseSeconds = Math.Min(0.5 * Math.Pow(2, attempt), 8);
            double factor;
            lock (RandomLock) factor = 0.75 + Jitter.NextDouble() * 0.5;
            return TimeSpan.FromSeconds(baseSeconds * factor);
        }

        private static bool TryParse(string text, out JsonElement root)
        {
            root = default;
            if (string.IsNullOrWhiteSpace(text)) return false;
            try
            {
                using (var doc = JsonDocument.Parse(text))
                {
                    root = doc.RootElement.Clone();
                    return true;
                }
            }
            catch (JsonException)
            {
                return false;
            }
        }

        private static T Deserialize<T>(string text, KuidaResponse response) where T : class
        {
            T? result;
            JsonElement root;
            try
            {
                result = JsonSerializer.Deserialize<T>(text, KuidaJson.Options);
                using (var doc = JsonDocument.Parse(text)) root = doc.RootElement.Clone();
            }
            catch (JsonException e)
            {
                throw new ApiException($"La respuesta de Kuida no es JSON válido: {e.Message}", e)
                {
                    HttpStatus = response.StatusCode,
                    RequestId = response.RequestId,
                    Headers = response.Headers,
                    RawBody = text,
                };
            }
            if (result == null)
            {
                throw new ApiException("La respuesta de Kuida vino vacía.")
                {
                    HttpStatus = response.StatusCode,
                    RequestId = response.RequestId,
                    Headers = response.Headers,
                    RawBody = text,
                };
            }
            if (result is KuidaObject ko)
            {
                ko.RawJson = root;
                ko.LastResponse = response;
            }
            if (result is IKuidaList list) list.AttachItems(root);
            return result;
        }

        internal static KuidaException BuildException(int status, IReadOnlyDictionary<string, string> headers, string text)
        {
            headers.TryGetValue("Request-Id", out var headerRequestId);
            string? type = null, code = null, message = null, param = null, requestId = null, docUrl = null;
            var parsed = false;
            var isJson = TryParse(text, out var root);
            if (isJson && root.ValueKind == JsonValueKind.Object
                && root.TryGetProperty("error", out var err) && err.ValueKind == JsonValueKind.Object)
            {
                parsed = true;
                type = GetString(err, "type");
                code = GetString(err, "code");
                message = GetString(err, "message");
                param = GetString(err, "param");
                requestId = GetString(err, "requestId");
                docUrl = GetString(err, "docUrl");
            }

            KuidaException ex;
            if (!parsed)
            {
                var snippet = string.IsNullOrWhiteSpace(text) ? $"HTTP {status} sin cuerpo" : text.Length > 500 ? text.Substring(0, 500) : text;
                ex = new ApiException(snippet);
            }
            else
            {
                message = string.IsNullOrEmpty(message) ? $"HTTP {status}" : message!;
                switch (type)
                {
                    case "invalid_request_error": ex = new InvalidRequestException(message); break;
                    case "authentication_error": ex = new AuthenticationException(message); break;
                    case "permission_error": ex = new PermissionException(message); break;
                    case "idempotency_error": ex = new IdempotencyException(message); break;
                    case "rate_limit_error": ex = new RateLimitException(message); break;
                    default: ex = new ApiException(message); break;
                }
            }
            ex.HttpStatus = status;
            ex.Type = type;
            ex.Code = code;
            ex.Param = param;
            ex.RequestId = requestId ?? headerRequestId;
            ex.DocUrl = docUrl;
            ex.Headers = headers;
            ex.RawBody = text;
            if (isJson) ex.RawJson = root;
            return ex;
        }

        private static string? GetString(JsonElement obj, string name) =>
            obj.TryGetProperty(name, out var v) && v.ValueKind == JsonValueKind.String ? v.GetString() : null;
    }
}
