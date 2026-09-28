using System;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace Kuida
{
    /// <summary>
    /// Un paciente: su id de Kuida (<c>pat_…</c>) o sus datos (<see cref="PatientIdentity"/>).
    /// Se convierte solo desde los dos:
    /// <code>
    /// Patient = "pat_123"
    /// Patient = new PatientIdentity { Phone = "+54 9 342 555 0000", FullName = "Paciente Demo" }
    /// </code>
    /// </summary>
    [JsonConverter(typeof(PatientReferenceConverter))]
    public sealed class PatientReference
    {
        /// <summary>Referencia por id de Kuida (<c>pat_…</c>).</summary>
        public PatientReference(string id)
        {
            Id = id ?? throw new ArgumentNullException(nameof(id));
        }

        /// <summary>Referencia por datos del paciente. Kuida lo busca por teléfono o DNI y, si no existe, lo crea.</summary>
        public PatientReference(PatientIdentity identity)
        {
            Identity = identity ?? throw new ArgumentNullException(nameof(identity));
        }

        /// <summary>Id de Kuida, si la referencia es por id.</summary>
        public string? Id { get; }

        /// <summary>Datos del paciente, si la referencia es por identidad.</summary>
        public PatientIdentity? Identity { get; }

        /// <summary>Crea una referencia por id.</summary>
        public static implicit operator PatientReference(string id) => new PatientReference(id);

        /// <summary>Crea una referencia por identidad.</summary>
        public static implicit operator PatientReference(PatientIdentity identity) => new PatientReference(identity);

        /// <inheritdoc />
        public override string ToString() => Id ?? "PatientIdentity";
    }

    /// <summary>
    /// Un profesional: su id de Kuida (<c>doc_…</c>) o sus datos (<see cref="DoctorIdentity"/>).
    /// Se convierte solo desde los dos.
    /// </summary>
    [JsonConverter(typeof(DoctorReferenceConverter))]
    public sealed class DoctorReference
    {
        /// <summary>Referencia por id de Kuida (<c>doc_…</c>).</summary>
        public DoctorReference(string id)
        {
            Id = id ?? throw new ArgumentNullException(nameof(id));
        }

        /// <summary>Referencia por datos del profesional. Kuida lo busca por id externo o nombre y, si no existe, lo crea.</summary>
        public DoctorReference(DoctorIdentity identity)
        {
            Identity = identity ?? throw new ArgumentNullException(nameof(identity));
        }

        /// <summary>Id de Kuida, si la referencia es por id.</summary>
        public string? Id { get; }

        /// <summary>Datos del profesional, si la referencia es por identidad.</summary>
        public DoctorIdentity? Identity { get; }

        /// <summary>Crea una referencia por id.</summary>
        public static implicit operator DoctorReference(string id) => new DoctorReference(id);

        /// <summary>Crea una referencia por identidad.</summary>
        public static implicit operator DoctorReference(DoctorIdentity identity) => new DoctorReference(identity);

        /// <inheritdoc />
        public override string ToString() => Id ?? "DoctorIdentity";
    }

    internal sealed class PatientReferenceConverter : JsonConverter<PatientReference>
    {
        public override PatientReference? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            if (reader.TokenType == JsonTokenType.Null) return null;
            if (reader.TokenType == JsonTokenType.String) return new PatientReference(reader.GetString()!);
            var identity = JsonSerializer.Deserialize<PatientIdentity>(ref reader, options);
            return identity == null ? null : new PatientReference(identity);
        }

        public override void Write(Utf8JsonWriter writer, PatientReference value, JsonSerializerOptions options)
        {
            if (value.Id != null) writer.WriteStringValue(value.Id);
            else JsonSerializer.Serialize(writer, value.Identity, options);
        }
    }

    internal sealed class DoctorReferenceConverter : JsonConverter<DoctorReference>
    {
        public override DoctorReference? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
        {
            if (reader.TokenType == JsonTokenType.Null) return null;
            if (reader.TokenType == JsonTokenType.String) return new DoctorReference(reader.GetString()!);
            var identity = JsonSerializer.Deserialize<DoctorIdentity>(ref reader, options);
            return identity == null ? null : new DoctorReference(identity);
        }

        public override void Write(Utf8JsonWriter writer, DoctorReference value, JsonSerializerOptions options)
        {
            if (value.Id != null) writer.WriteStringValue(value.Id);
            else JsonSerializer.Serialize(writer, value.Identity, options);
        }
    }
}
