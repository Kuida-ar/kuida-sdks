using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Eventos: la puerta genérica para que el sistema de gestión avise lo que pasó (<c>kuida.Events</c>).</summary>
    public sealed class EventService : KuidaService
    {
        internal EventService(ApiRequestor requestor) : base(requestor) { }

        /// <summary>
        /// Manda un evento. El <c>Id</c> del evento es su clave de idempotencia: reenviarlo devuelve
        /// <c>duplicate</c> sin repetir nada.
        /// </summary>
        /// <param name="evt">El sobre del evento.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        /// <exception cref="InvalidRequestException">Si el evento no entra; el detalle está en <c>RawJson</c> (<c>results</c>).</exception>
        /// <returns>Un resultado por evento (<c>processed</c>, <c>duplicate</c>, <c>unhandled</c>, <c>invalid</c> o <c>failed</c>).</returns>
        public Task<EventBatchResponse> CreateAsync(EventInput evt, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            Requestor.RequestAsync<EventBatchResponse>(System.Net.Http.HttpMethod.Post, "/v1/events", Required(evt, nameof(evt)), null, options, cancellationToken);

        /// <summary>
        /// Manda un lote de hasta 100 eventos. Cada uno se procesa por separado y trae su resultado,
        /// en el mismo orden. Si ninguno entra, la API responde el error normal (400
        /// <c>invalid_request_error</c> o 500 <c>api_error</c>) y el SDK lanza la excepción; el detalle
        /// por evento queda en <c>RawJson</c> de la excepción (propiedad <c>results</c>).
        /// </summary>
        /// <param name="events">Los eventos (1 a 100).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<EventBatchResponse> CreateBatchAsync(IEnumerable<EventInput> events, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            Requestor.RequestAsync<EventBatchResponse>(System.Net.Http.HttpMethod.Post, "/v1/events",
                new Dictionary<string, object> { ["events"] = new List<EventInput>(Required(events, nameof(events))) },
                null, options, cancellationToken);

        /// <summary>Trae un evento por id, con su estado y lo que produjo.</summary>
        /// <param name="id">Id de Kuida (<c>evt_…</c>).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        /// <exception cref="InvalidRequestException">Si no existe (<c>HttpStatus</c> 404, <c>Code</c> <c>resource_missing</c>).</exception>
        public Task<Event> RetrieveAsync(string id, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<Event>("/v1/events/" + Id(id), null, options, cancellationToken);

        /// <summary>Lista eventos, del más nuevo al más viejo, de a una página.</summary>
        /// <param name="parameters">Filtros y cursores. Opcional.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<KuidaList<Event>> ListAsync(EventListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<KuidaList<Event>>("/v1/events", parameters, options, cancellationToken);

        /// <summary>
        /// Recorre todas las páginas de eventos pidiendo de a <c>Limit</c>. Úselo con <c>await foreach</c>.
        /// </summary>
        /// <param name="parameters">Filtros, tamaño de página y cursor inicial. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public IAsyncEnumerable<Event> ListAutoPagingAsync(EventListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            AutoPage<Event, EventListParams>(parameters, (p, ct) => ListAsync(p, options, ct), cancellationToken);

        /// <summary>
        /// Trae todas las páginas de eventos en una sola lista. Alternativa a <see cref="ListAutoPagingAsync"/>
        /// para código que no puede usar <c>await foreach</c> (C# 7.3 en .NET Framework).
        /// </summary>
        /// <param name="parameters">Filtros y tamaño de página. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<List<Event>> ListAllAsync(EventListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            CollectAsync(ListAutoPagingAsync(parameters, options, cancellationToken), cancellationToken);
    }
}
