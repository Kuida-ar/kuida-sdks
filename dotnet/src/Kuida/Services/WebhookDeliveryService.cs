using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Entregas de webhook (<c>kuida.WebhookDeliveries</c>).</summary>
    public sealed class WebhookDeliveryService : KuidaService
    {
        internal WebhookDeliveryService(ApiRequestor requestor) : base(requestor) { }

        /// <summary>Lista entregas de webhook, del más nuevo al más viejo, de a una página.</summary>
        /// <param name="parameters">Filtros y cursores. Opcional.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<KuidaList<WebhookDelivery>> ListAsync(WebhookDeliveryListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<KuidaList<WebhookDelivery>>("/v1/webhook_deliveries", parameters, options, cancellationToken);

        /// <summary>
        /// Recorre todas las páginas de entregas de webhook pidiendo de a <c>Limit</c>. Úselo con <c>await foreach</c>.
        /// </summary>
        /// <param name="parameters">Filtros, tamaño de página y cursor inicial. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public IAsyncEnumerable<WebhookDelivery> ListAutoPagingAsync(WebhookDeliveryListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            AutoPage<WebhookDelivery, WebhookDeliveryListParams>(parameters, (p, ct) => ListAsync(p, options, ct), cancellationToken);

        /// <summary>
        /// Trae todas las páginas de entregas de webhook en una sola lista. Alternativa a <see cref="ListAutoPagingAsync"/>
        /// para código que no puede usar <c>await foreach</c> (C# 7.3 en .NET Framework).
        /// </summary>
        /// <param name="parameters">Filtros y tamaño de página. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<List<WebhookDelivery>> ListAllAsync(WebhookDeliveryListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            CollectAsync(ListAutoPagingAsync(parameters, options, cancellationToken), cancellationToken);

        /// <summary>Trae una entrega por id, con el cuerpo que se mandó.</summary>
        /// <param name="id">Id de Kuida (<c>whd_…</c>).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        /// <exception cref="InvalidRequestException">Si no existe (<c>HttpStatus</c> 404, <c>Code</c> <c>resource_missing</c>).</exception>
        public Task<WebhookDelivery> RetrieveAsync(string id, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<WebhookDelivery>("/v1/webhook_deliveries/" + Id(id), null, options, cancellationToken);
    }
}
