using System;
using System.Collections;
using System.Collections.Generic;
using System.Globalization;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Kuida
{
    /// <summary>
    /// Base de todos los objetos que devuelve la API. Conserva el JSON crudo y los
    /// datos de la respuesta HTTP de la que salió.
    /// </summary>
    public abstract class KuidaObject
    {
        /// <summary>
        /// JSON crudo del objeto tal como llegó por el cable, incluidos los campos que
        /// este SDK todavía no conoce.
        /// </summary>
        [JsonIgnore]
        public JsonElement RawJson { get; internal set; }

        /// <summary>
        /// Respuesta HTTP de la que salió el objeto (status, headers, <c>Request-Id</c>).
        /// Es <c>null</c> en los objetos anidados dentro de una lista y en los eventos de webhook.
        /// </summary>
        [JsonIgnore]
        public KuidaResponse? LastResponse { get; internal set; }
    }

    /// <summary>Objeto con id de Kuida (<c>pat_…</c>, <c>apt_…</c>, …).</summary>
    public interface IHasId
    {
        /// <summary>Id del objeto.</summary>
        string Id { get; }
    }

    /// <summary>Datos de la respuesta HTTP de un pedido a la API.</summary>
    public sealed class KuidaResponse
    {
        internal KuidaResponse(int statusCode, IReadOnlyDictionary<string, string> headers, string body)
        {
            StatusCode = statusCode;
            Headers = headers;
            Body = body;
            headers.TryGetValue("Request-Id", out var requestId);
            RequestId = requestId;
            IdempotentReplayed = headers.TryGetValue("Idempotent-Replayed", out var replayed)
                && string.Equals(replayed, "true", StringComparison.OrdinalIgnoreCase);
        }

        /// <summary>Código de estado HTTP.</summary>
        public int StatusCode { get; }

        /// <summary>Headers de la respuesta (sin distinguir mayúsculas).</summary>
        public IReadOnlyDictionary<string, string> Headers { get; }

        /// <summary>Id del pedido (header <c>Request-Id</c>). Inclúyalo al escribir a soporte.</summary>
        public string? RequestId { get; }

        /// <summary><c>true</c> si la API devolvió una respuesta guardada por la <c>Idempotency-Key</c>.</summary>
        public bool IdempotentReplayed { get; }

        /// <summary>Cuerpo crudo de la respuesta.</summary>
        public string Body { get; }
    }

    internal interface IKuidaList
    {
        void AttachItems(JsonElement root);
    }

    /// <summary>
    /// Una página de resultados. Para la siguiente, pase <c>StartingAfter</c> = id del
    /// último elemento, o use <c>ListAutoPagingAsync</c> del recurso.
    /// </summary>
    /// <typeparam name="T">Tipo de los objetos de la página.</typeparam>
    [JsonConverter(typeof(KuidaListConverterFactory))]
    public sealed class KuidaList<T> : KuidaObject, IKuidaList, IEnumerable<T>
    {
        /// <summary>Siempre <c>list</c>.</summary>
        [JsonPropertyName("object")]
        public string Object { get; set; } = "list";

        /// <summary>Los objetos de esta página.</summary>
        [JsonPropertyName("data")]
        public List<T> Data { get; set; } = new List<T>();

        /// <summary>Si hay más objetos después de esta página.</summary>
        [JsonPropertyName("hasMore")]
        public bool HasMore { get; set; }

        /// <summary>Ruta de la lista (<c>/v1/patients</c>).</summary>
        [JsonPropertyName("url")]
        public string? Url { get; set; }

        /// <inheritdoc />
        public IEnumerator<T> GetEnumerator() => Data.GetEnumerator();

        IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();

        void IKuidaList.AttachItems(JsonElement root)
        {
            if (root.ValueKind != JsonValueKind.Object || !root.TryGetProperty("data", out var data) || data.ValueKind != JsonValueKind.Array) return;
            var i = 0;
            foreach (var el in data.EnumerateArray())
            {
                if (i >= Data.Count) break;
                if (Data[i] is KuidaObject ko) ko.RawJson = el;
                i++;
            }
        }
    }

    /// <summary>
    /// <see cref="KuidaList{T}"/> es enumerable; sin este conversor System.Text.Json lo leería
    /// como un array en vez de como el objeto <c>{ object, data, hasMore, url }</c>.
    /// </summary>
    internal sealed class KuidaListConverterFactory : JsonConverterFactory
    {
        public override bool CanConvert(Type typeToConvert) =>
            typeToConvert.IsGenericType && typeToConvert.GetGenericTypeDefinition() == typeof(KuidaList<>);

        public override JsonConverter CreateConverter(Type typeToConvert, JsonSerializerOptions options) =>
            (JsonConverter)Activator.CreateInstance(typeof(Converter<>).MakeGenericType(typeToConvert.GetGenericArguments()[0]))!;

        private sealed class Wire<T>
        {
            [JsonPropertyName("object")] public string Object { get; set; } = "list";
            [JsonPropertyName("data")] public List<T>? Data { get; set; }
            [JsonPropertyName("hasMore")] public bool HasMore { get; set; }
            [JsonPropertyName("url")] public string? Url { get; set; }
        }

        private sealed class Converter<T> : JsonConverter<KuidaList<T>>
        {
            public override KuidaList<T>? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
            {
                var wire = JsonSerializer.Deserialize<Wire<T>>(ref reader, options);
                if (wire == null) return null;
                return new KuidaList<T> { Object = wire.Object, Data = wire.Data ?? new List<T>(), HasMore = wire.HasMore, Url = wire.Url };
            }

            public override void Write(Utf8JsonWriter writer, KuidaList<T> value, JsonSerializerOptions options) =>
                JsonSerializer.Serialize(writer, new Wire<T> { Object = value.Object, Data = value.Data, HasMore = value.HasMore, Url = value.Url }, options);
        }
    }

    /// <summary>
    /// Parámetros comunes de toda lista: tamaño de página y cursores.
    /// </summary>
    public abstract class ListParams
    {
        /// <summary>Cantidad por página, de 1 a 100. Default de la API: 10.</summary>
        public int? Limit { get; set; }

        /// <summary>Cursor: trae los objetos que siguen a este id.</summary>
        public string? StartingAfter { get; set; }

        /// <summary>Cursor: trae los objetos anteriores a este id.</summary>
        public string? EndingBefore { get; set; }

        internal virtual void AppendQuery(IList<KeyValuePair<string, string>> query)
        {
            Add(query, "limit", Limit?.ToString(CultureInfo.InvariantCulture));
            Add(query, "startingAfter", StartingAfter);
            Add(query, "endingBefore", EndingBefore);
        }

        internal static void Add(IList<KeyValuePair<string, string>> query, string name, string? value)
        {
            if (value != null) query.Add(new KeyValuePair<string, string>(name, value));
        }

        internal ListParams Copy() => (ListParams)MemberwiseClone();
    }
}
