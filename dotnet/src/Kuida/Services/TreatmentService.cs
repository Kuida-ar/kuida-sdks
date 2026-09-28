using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Tratamientos indicados (<c>kuida.Treatments</c>).</summary>
    public sealed class TreatmentService : KuidaService
    {
        internal TreatmentService(ApiRequestor requestor) : base(requestor) { }

        /// <summary>Registra un tratamiento indicado a un paciente.</summary>
        /// <param name="parameters">Datos del objeto.</param>
        /// <param name="options">Opciones del pedido (por ejemplo <c>IdempotencyKey</c>).</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<Treatment> CreateAsync(TreatmentCreateParams parameters, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PostAsync<Treatment>("/v1/treatments", Required(parameters, nameof(parameters)), options, cancellationToken);

        /// <summary>Trae un tratamiento por id.</summary>
        /// <param name="id">Id de Kuida (<c>trt_…</c>).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        /// <exception cref="InvalidRequestException">Si no existe (<c>HttpStatus</c> 404, <c>Code</c> <c>resource_missing</c>).</exception>
        public Task<Treatment> RetrieveAsync(string id, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<Treatment>("/v1/treatments/" + Id(id), null, options, cancellationToken);

        /// <summary>Lista tratamientos, del más nuevo al más viejo, de a una página.</summary>
        /// <param name="parameters">Filtros y cursores. Opcional.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<KuidaList<Treatment>> ListAsync(TreatmentListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<KuidaList<Treatment>>("/v1/treatments", parameters, options, cancellationToken);

        /// <summary>
        /// Recorre todas las páginas de tratamientos pidiendo de a <c>Limit</c>. Úselo con <c>await foreach</c>.
        /// </summary>
        /// <param name="parameters">Filtros, tamaño de página y cursor inicial. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public IAsyncEnumerable<Treatment> ListAutoPagingAsync(TreatmentListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            AutoPage<Treatment, TreatmentListParams>(parameters, (p, ct) => ListAsync(p, options, ct), cancellationToken);

        /// <summary>
        /// Trae todas las páginas de tratamientos en una sola lista. Alternativa a <see cref="ListAutoPagingAsync"/>
        /// para código que no puede usar <c>await foreach</c> (C# 7.3 en .NET Framework).
        /// </summary>
        /// <param name="parameters">Filtros y tamaño de página. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<List<Treatment>> ListAllAsync(TreatmentListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            CollectAsync(ListAutoPagingAsync(parameters, options, cancellationToken), cancellationToken);
    }
}
