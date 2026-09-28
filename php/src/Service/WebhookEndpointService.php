<?php

namespace Kuida\Service;

use Kuida\Collection;
use Kuida\Model\DeletedObject;
use Kuida\Model\WebhookDelivery;
use Kuida\Model\WebhookEndpoint;

/**
 * Endpoints de webhooks: URLs de tu sistema donde Kuida avisa lo que pasa.
 */
class WebhookEndpointService extends AbstractService
{
    /**
     * Registra un endpoint. La respuesta trae el `secret` para verificar las
     * firmas: guárdalo, no se vuelve a mostrar.
     *
     * @param array<string, mixed> $params `url` (https, obligatoria), `enabledEvents`
     *   (lista, obligatoria; `*` = todos) y `description`.
     * @param array<string, mixed> $opts Opciones por pedido (`idempotency_key`, `timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function create(array $params, array $opts = []): WebhookEndpoint
    {
        return $this->requestObject('POST', '/v1/webhook_endpoints', $params, $opts, WebhookEndpoint::class);
    }

    /**
     * Devuelve un endpoint por su id (`we_…`), sin el secreto.
     *
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function retrieve(string $id, array $opts = []): WebhookEndpoint
    {
        return $this->requestObject('GET', self::path('/v1/webhook_endpoints/%s', $id), null, $opts, WebhookEndpoint::class);
    }

    /**
     * Actualiza un endpoint.
     *
     * @param array<string, mixed> $params `url`, `enabledEvents`, `description` y
     *   `disabled` (`true` pausa los envíos sin borrar el endpoint).
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function update(string $id, array $params, array $opts = []): WebhookEndpoint
    {
        return $this->requestObject('PATCH', self::path('/v1/webhook_endpoints/%s', $id), $params, $opts, WebhookEndpoint::class);
    }

    /**
     * Borra un endpoint y sus entregas pendientes.
     *
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function delete(string $id, array $opts = []): DeletedObject
    {
        return $this->requestObject('DELETE', self::path('/v1/webhook_endpoints/%s', $id), null, $opts, DeletedObject::class);
    }

    /**
     * Lista endpoints.
     *
     * @param array<string, mixed> $params `limit`, `startingAfter`, `endingBefore`.
     * @param array<string, mixed> $opts Opciones por pedido.
     * @return Collection<WebhookEndpoint>
     * @throws \Kuida\Exception\KuidaException
     */
    public function list(array $params = [], array $opts = []): Collection
    {
        return $this->requestCollection('/v1/webhook_endpoints', $params, $opts, WebhookEndpoint::class);
    }

    /**
     * Manda un evento de prueba (`webhook.ping`) al endpoint. Devuelve la entrega creada.
     *
     * @param array<string, mixed> $opts Opciones por pedido (`idempotency_key`, `timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function ping(string $id, array $opts = []): WebhookDelivery
    {
        return $this->requestObject('POST', self::path('/v1/webhook_endpoints/%s/ping', $id), null, $opts, WebhookDelivery::class);
    }
}
