package ar.kuida.service;

import ar.kuida.ApiRequestor;
import ar.kuida.KuidaList;
import ar.kuida.RequestOptions;
import ar.kuida.model.DeletedObject;
import ar.kuida.model.WebhookDelivery;
import ar.kuida.model.WebhookEndpoint;
import ar.kuida.model.WebhookEndpointCreateParams;
import ar.kuida.model.WebhookEndpointListParams;
import ar.kuida.model.WebhookEndpointUpdateParams;

/** {@code /v1/webhook_endpoints}: URLs de tu sistema que reciben eventos de Kuida. */
public final class WebhookEndpointService extends ApiService {
  /** Uso interno: el servicio se obtiene desde {@link ar.kuida.KuidaClient}. */
  public WebhookEndpointService(ApiRequestor requestor) {
    super(requestor);
  }

  /**
   * Registra un endpoint. La respuesta trae el {@code secret} para verificar la firma: hay que guardarlo, no
   * se vuelve a mostrar.
   */
  public WebhookEndpoint create(WebhookEndpointCreateParams params) {
    return create(params, null);
  }

  /** Igual que {@link #create(WebhookEndpointCreateParams)}, con opciones por pedido. */
  public WebhookEndpoint create(WebhookEndpointCreateParams params, RequestOptions options) {
    return requestor.request("POST", "/v1/webhook_endpoints", params, WebhookEndpoint.class, options);
  }

  /** Obtiene un endpoint, sin el secreto. */
  public WebhookEndpoint retrieve(String id) {
    return retrieve(id, null);
  }

  /** Igual que {@link #retrieve(String)}, con opciones por pedido. */
  public WebhookEndpoint retrieve(String id, RequestOptions options) {
    return requestor.request("GET", "/v1/webhook_endpoints/" + id(id), null, WebhookEndpoint.class, options);
  }

  /** Cambia URL, eventos o descripción, o lo pausa con {@code disabled(true)}. */
  public WebhookEndpoint update(String id, WebhookEndpointUpdateParams params) {
    return update(id, params, null);
  }

  /** Igual que {@link #update(String, WebhookEndpointUpdateParams)}, con opciones por pedido. */
  public WebhookEndpoint update(String id, WebhookEndpointUpdateParams params, RequestOptions options) {
    return requestor.request("PATCH", "/v1/webhook_endpoints/" + id(id), params, WebhookEndpoint.class, options);
  }

  /** Borra un endpoint con su historial de entregas; las pendientes se cancelan. */
  public DeletedObject delete(String id) {
    return delete(id, null);
  }

  /** Igual que {@link #delete(String)}, con opciones por pedido. */
  public DeletedObject delete(String id, RequestOptions options) {
    return requestor.request("DELETE", "/v1/webhook_endpoints/" + id(id), null, DeletedObject.class, options);
  }

  /** Lista los endpoints, del más nuevo al más viejo. */
  public KuidaList<WebhookEndpoint> list() {
    return list(null, null);
  }

  /** Lista los endpoints con cursor. */
  public KuidaList<WebhookEndpoint> list(WebhookEndpointListParams params) {
    return list(params, null);
  }

  /** Igual que {@link #list(WebhookEndpointListParams)}, con opciones por pedido. */
  public KuidaList<WebhookEndpoint> list(WebhookEndpointListParams params, RequestOptions options) {
    return requestor.requestList("/v1/webhook_endpoints", params, WebhookEndpoint.class, options);
  }

  /**
   * Encola un evento {@code webhook.ping} firmado hacia el endpoint. La entrega se sigue con
   * {@code webhookDeliveries().retrieve(id)}.
   */
  public WebhookDelivery ping(String id) {
    return ping(id, null);
  }

  /** Igual que {@link #ping(String)}, con opciones por pedido. */
  public WebhookDelivery ping(String id, RequestOptions options) {
    return requestor.request("POST", "/v1/webhook_endpoints/" + id(id) + "/ping", null, WebhookDelivery.class, options);
  }
}
