using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Endpoints de webhook (<c>kuida.WebhookEndpoints</c>).</summary>
    public sealed class WebhookEndpointService : KuidaService
    {
        internal WebhookEndpointService(ApiRequestor requestor) : base(requestor) { }

        /// <summary>
        /// Crea un endpoint de webhook. La respuesta trae el <c>Secret</c> (<c>whsec_…</c>) una sola vez:
        /// guárdelo para verificar las firmas con <see cref="Webhook"/>.
        /// </summary>
        /// <param name="parameters">URL https y eventos a recibir.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<WebhookEndpoint> CreateAsync(WebhookEndpointCreateParams parameters, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PostAsync<WebhookEndpoint>("/v1/webhook_endpoints", Required(parameters, nameof(parameters)), options, cancellationToken);

        /// <summary>Trae un endpoint por id (sin el secreto).</summary>
        /// <param name="id">Id de Kuida (<c>we_…</c>).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        /// <exception cref="InvalidRequestException">Si no existe (<c>HttpStatus</c> 404, <c>Code</c> <c>resource_missing</c>).</exception>
        public Task<WebhookEndpoint> RetrieveAsync(string id, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<WebhookEndpoint>("/v1/webhook_endpoints/" + Id(id), null, options, cancellationToken);

        /// <summary>Cambia la URL, los eventos o lo habilita/deshabilita.</summary>
        /// <param name="id">Id de Kuida.</param>
        /// <param name="parameters">Campos a cambiar; los que no se pasan quedan como están.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<WebhookEndpoint> UpdateAsync(string id, WebhookEndpointUpdateParams parameters, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PatchAsync<WebhookEndpoint>("/v1/webhook_endpoints/" + Id(id), Required(parameters, nameof(parameters)), options, cancellationToken);

        /// <summary>Borra un endpoint y sus entregas pendientes.</summary>
        /// <param name="id">Id del endpoint (<c>we_…</c>).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<DeletedObject> DeleteAsync(string id, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            DeleteAsync<DeletedObject>("/v1/webhook_endpoints/" + Id(id), options, cancellationToken);

        /// <summary>Encola un evento de prueba <c>webhook.ping</c> al endpoint y devuelve la entrega pendiente.</summary>
        /// <param name="id">Id del endpoint (<c>we_…</c>).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<WebhookDelivery> PingAsync(string id, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PostAsync<WebhookDelivery>("/v1/webhook_endpoints/" + Id(id) + "/ping", null, options, cancellationToken);

        /// <summary>Lista endpoints de webhook, del más nuevo al más viejo, de a una página.</summary>
        /// <param name="parameters">Filtros y cursores. Opcional.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<KuidaList<WebhookEndpoint>> ListAsync(WebhookEndpointListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<KuidaList<WebhookEndpoint>>("/v1/webhook_endpoints", parameters, options, cancellationToken);

        /// <summary>
        /// Recorre todas las páginas de endpoints de webhook pidiendo de a <c>Limit</c>. Úselo con <c>await foreach</c>.
        /// </summary>
        /// <param name="parameters">Filtros, tamaño de página y cursor inicial. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public IAsyncEnumerable<WebhookEndpoint> ListAutoPagingAsync(WebhookEndpointListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            AutoPage<WebhookEndpoint, WebhookEndpointListParams>(parameters, (p, ct) => ListAsync(p, options, ct), cancellationToken);

        /// <summary>
        /// Trae todas las páginas de endpoints de webhook en una sola lista. Alternativa a <see cref="ListAutoPagingAsync"/>
        /// para código que no puede usar <c>await foreach</c> (C# 7.3 en .NET Framework).
        /// </summary>
        /// <param name="parameters">Filtros y tamaño de página. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<List<WebhookEndpoint>> ListAllAsync(WebhookEndpointListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            CollectAsync(ListAutoPagingAsync(parameters, options, cancellationToken), cancellationToken);
    }
}
