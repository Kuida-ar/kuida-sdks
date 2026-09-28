using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Consultas atendidas (<c>kuida.Visits</c>).</summary>
    public sealed class VisitService : KuidaService
    {
        internal VisitService(ApiRequestor requestor) : base(requestor) { }

        /// <summary>Registra una consulta atendida. Si se pasa <c>Appointment</c> (id <c>apt_…</c> o <c>externalId</c> del turno), el turno queda <c>completed</c>.</summary>
        /// <param name="parameters">Datos del objeto.</param>
        /// <param name="options">Opciones del pedido (por ejemplo <c>IdempotencyKey</c>).</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<Visit> CreateAsync(VisitCreateParams parameters, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PostAsync<Visit>("/v1/visits", Required(parameters, nameof(parameters)), options, cancellationToken);

        /// <summary>Trae una consulta por id.</summary>
        /// <param name="id">Id de Kuida (<c>vis_…</c>).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        /// <exception cref="InvalidRequestException">Si no existe (<c>HttpStatus</c> 404, <c>Code</c> <c>resource_missing</c>).</exception>
        public Task<Visit> RetrieveAsync(string id, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<Visit>("/v1/visits/" + Id(id), null, options, cancellationToken);

        /// <summary>Lista consultas, del más nuevo al más viejo, de a una página.</summary>
        /// <param name="parameters">Filtros y cursores. Opcional.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<KuidaList<Visit>> ListAsync(VisitListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<KuidaList<Visit>>("/v1/visits", parameters, options, cancellationToken);

        /// <summary>
        /// Recorre todas las páginas de consultas pidiendo de a <c>Limit</c>. Úselo con <c>await foreach</c>.
        /// </summary>
        /// <param name="parameters">Filtros, tamaño de página y cursor inicial. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public IAsyncEnumerable<Visit> ListAutoPagingAsync(VisitListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            AutoPage<Visit, VisitListParams>(parameters, (p, ct) => ListAsync(p, options, ct), cancellationToken);

        /// <summary>
        /// Trae todas las páginas de consultas en una sola lista. Alternativa a <see cref="ListAutoPagingAsync"/>
        /// para código que no puede usar <c>await foreach</c> (C# 7.3 en .NET Framework).
        /// </summary>
        /// <param name="parameters">Filtros y tamaño de página. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<List<Visit>> ListAllAsync(VisitListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            CollectAsync(ListAutoPagingAsync(parameters, options, cancellationToken), cancellationToken);
    }
}
