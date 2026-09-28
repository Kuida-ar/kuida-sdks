<?php

namespace Kuida\Service;

use Kuida\Collection;
use Kuida\Model\WebhookDelivery;

/**
 * Entregas de webhooks: cada intento de Kuida de avisarle algo a un endpoint.
 */
class WebhookDeliveryService extends AbstractService
{
    /**
     * Lista entregas.
     *
     * @param array<string, mixed> $params `limit`, `startingAfter`, `endingBefore`, y filtros
     *   `webhookEndpoint` y `status` (`pending`, `succeeded`, `failed`).
     * @param array<string, mixed> $opts Opciones por pedido.
     * @return Collection<WebhookDelivery>
     * @throws \Kuida\Exception\KuidaException
     */
    public function list(array $params = [], array $opts = []): Collection
    {
        return $this->requestCollection('/v1/webhook_deliveries', $params, $opts, WebhookDelivery::class);
    }

    /**
     * Devuelve una entrega por su id (`whd_…`), con el cuerpo exacto que se envió.
     *
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function retrieve(string $id, array $opts = []): WebhookDelivery
    {
        return $this->requestObject('GET', self::path('/v1/webhook_deliveries/%s', $id), null, $opts, WebhookDelivery::class);
    }
}
