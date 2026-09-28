package ar.kuida.service;

import ar.kuida.ApiRequestor;
import ar.kuida.KuidaList;
import ar.kuida.RequestOptions;
import ar.kuida.model.WebhookDelivery;
import ar.kuida.model.WebhookDeliveryListParams;

/** {@code /v1/webhook_deliveries}: cada envío a tus endpoints, con sus intentos. */
public final class WebhookDeliveryService extends ApiService {
  /** Uso interno: el servicio se obtiene desde {@link ar.kuida.KuidaClient}. */
  public WebhookDeliveryService(ApiRequestor requestor) {
    super(requestor);
  }

  /** Lista entregas, de la más nueva a la más vieja. */
  public KuidaList<WebhookDelivery> list() {
    return list(null, null);
  }

  /** Lista entregas; filtra por endpoint o por {@code status} (por ejemplo {@code failed}). */
  public KuidaList<WebhookDelivery> list(WebhookDeliveryListParams params) {
    return list(params, null);
  }

  /** Igual que {@link #list(WebhookDeliveryListParams)}, con opciones por pedido. */
  public KuidaList<WebhookDelivery> list(WebhookDeliveryListParams params, RequestOptions options) {
    return requestor.requestList("/v1/webhook_deliveries", params, WebhookDelivery.class, options);
  }

  /** Obtiene una entrega con su estado, intentos y el cuerpo enviado. */
  public WebhookDelivery retrieve(String id) {
    return retrieve(id, null);
  }

  /** Igual que {@link #retrieve(String)}, con opciones por pedido. */
  public WebhookDelivery retrieve(String id, RequestOptions options) {
    return requestor.request("GET", "/v1/webhook_deliveries/" + id(id), null, WebhookDelivery.class, options);
  }
}
