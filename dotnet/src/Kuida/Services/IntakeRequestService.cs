using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Solicitudes de ingreso (<c>kuida.IntakeRequests</c>).</summary>
    public sealed class IntakeRequestService : KuidaService
    {
        internal IntakeRequestService(ApiRequestor requestor) : base(requestor) { }

        /// <summary>Crea una solicitud de ingreso. Kuida completa por WhatsApp los datos que falten.</summary>
        /// <param name="parameters">Datos del objeto.</param>
        /// <param name="options">Opciones del pedido (por ejemplo <c>IdempotencyKey</c>).</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<IntakeRequest> CreateAsync(IntakeRequestCreateParams parameters, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PostAsync<IntakeRequest>("/v1/intake_requests", Required(parameters, nameof(parameters)), options, cancellationToken);

        /// <summary>Trae una solicitud de ingreso por id.</summary>
        /// <param name="id">Id de Kuida (<c>int_…</c>).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        /// <exception cref="InvalidRequestException">Si no existe (<c>HttpStatus</c> 404, <c>Code</c> <c>resource_missing</c>).</exception>
        public Task<IntakeRequest> RetrieveAsync(string id, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<IntakeRequest>("/v1/intake_requests/" + Id(id), null, options, cancellationToken);

        /// <summary>Lista solicitudes de ingreso, del más nuevo al más viejo, de a una página.</summary>
        /// <param name="parameters">Filtros y cursores. Opcional.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<KuidaList<IntakeRequest>> ListAsync(IntakeRequestListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<KuidaList<IntakeRequest>>("/v1/intake_requests", parameters, options, cancellationToken);

        /// <summary>
        /// Recorre todas las páginas de solicitudes de ingreso pidiendo de a <c>Limit</c>. Úselo con <c>await foreach</c>.
        /// </summary>
        /// <param name="parameters">Filtros, tamaño de página y cursor inicial. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public IAsyncEnumerable<IntakeRequest> ListAutoPagingAsync(IntakeRequestListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            AutoPage<IntakeRequest, IntakeRequestListParams>(parameters, (p, ct) => ListAsync(p, options, ct), cancellationToken);

        /// <summary>
        /// Trae todas las páginas de solicitudes de ingreso en una sola lista. Alternativa a <see cref="ListAutoPagingAsync"/>
        /// para código que no puede usar <c>await foreach</c> (C# 7.3 en .NET Framework).
        /// </summary>
        /// <param name="parameters">Filtros y tamaño de página. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<List<IntakeRequest>> ListAllAsync(IntakeRequestListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            CollectAsync(ListAutoPagingAsync(parameters, options, cancellationToken), cancellationToken);
    }
}
