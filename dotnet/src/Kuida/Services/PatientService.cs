using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Pacientes de la organización (<c>kuida.Patients</c>).</summary>
    public sealed class PatientService : KuidaService
    {
        internal PatientService(ApiRequestor requestor) : base(requestor) { }

        /// <summary>Crea un paciente o, si ya existe uno con ese teléfono o DNI, devuelve ese y completa los datos que le falten.</summary>
        /// <param name="parameters">Datos del objeto.</param>
        /// <param name="options">Opciones del pedido (por ejemplo <c>IdempotencyKey</c>).</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<Patient> CreateAsync(PatientCreateParams parameters, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PostAsync<Patient>("/v1/patients", Required(parameters, nameof(parameters)), options, cancellationToken);

        /// <summary>Trae un paciente por id.</summary>
        /// <param name="id">Id de Kuida (<c>pat_…</c>).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        /// <exception cref="InvalidRequestException">Si no existe (<c>HttpStatus</c> 404, <c>Code</c> <c>resource_missing</c>).</exception>
        public Task<Patient> RetrieveAsync(string id, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<Patient>("/v1/patients/" + Id(id), null, options, cancellationToken);

        /// <summary>Actualiza los datos de un paciente.</summary>
        /// <param name="id">Id de Kuida.</param>
        /// <param name="parameters">Campos a cambiar; los que no se pasan quedan como están.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<Patient> UpdateAsync(string id, PatientUpdateParams parameters, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PatchAsync<Patient>("/v1/patients/" + Id(id), Required(parameters, nameof(parameters)), options, cancellationToken);

        /// <summary>Lista pacientes, del más nuevo al más viejo, de a una página.</summary>
        /// <param name="parameters">Filtros y cursores. Opcional.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<KuidaList<Patient>> ListAsync(PatientListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<KuidaList<Patient>>("/v1/patients", parameters, options, cancellationToken);

        /// <summary>
        /// Recorre todas las páginas de pacientes pidiendo de a <c>Limit</c>. Úselo con <c>await foreach</c>.
        /// </summary>
        /// <param name="parameters">Filtros, tamaño de página y cursor inicial. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public IAsyncEnumerable<Patient> ListAutoPagingAsync(PatientListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            AutoPage<Patient, PatientListParams>(parameters, (p, ct) => ListAsync(p, options, ct), cancellationToken);

        /// <summary>
        /// Trae todas las páginas de pacientes en una sola lista. Alternativa a <see cref="ListAutoPagingAsync"/>
        /// para código que no puede usar <c>await foreach</c> (C# 7.3 en .NET Framework).
        /// </summary>
        /// <param name="parameters">Filtros y tamaño de página. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<List<Patient>> ListAllAsync(PatientListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            CollectAsync(ListAutoPagingAsync(parameters, options, cancellationToken), cancellationToken);
    }
}
