using System;
using System.Globalization;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Kuida
{
    internal static class KuidaJson
    {
        /// <summary>
        /// Nombres tal cual los atributos (camelCase del cable), sin nulls al mandar y
        /// tolerante a campos desconocidos al leer.
        /// </summary>
        internal static readonly JsonSerializerOptions Options = new JsonSerializerOptions
        {
            PropertyNamingPolicy = null,
            DictionaryKeyPolicy = null,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
            PropertyNameCaseInsensitive = false,
        };

        internal static string Serialize(object value) => JsonSerializer.Serialize(value, value.GetType(), Options);

        /// <summary>ISO 8601 con zona y milisegundos (<c>2026-10-01T14:30:00.000-03:00</c>).</summary>
        internal static string FormatDate(DateTimeOffset value) =>
            value.ToString("yyyy-MM-dd'T'HH:mm:ss.fffzzz", CultureInfo.InvariantCulture);
    }
}
