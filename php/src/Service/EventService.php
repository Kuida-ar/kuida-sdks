<?php

namespace Kuida\Service;

use Kuida\Collection;
use Kuida\Exception\InvalidArgumentException;
use Kuida\Model\Event;
use Kuida\Model\EventBatchResponse;

/**
 * Eventos: la puerta genérica para avisarle a Kuida lo que pasó en tu sistema.
 *
 * Cada evento es un sobre `{ id, type, data, occurredAt?, source?, schemaVersion? }`.
 * El `id` es tuyo y es la clave de idempotencia: reenviar el mismo id no repite nada.
 */
class EventService extends AbstractService
{
    const MAX_BATCH = 100;

    /**
     * Manda un evento.
     *
     * @param array<string, mixed> $event `id` y `type` obligatorios (`patient.upserted`,
     *   `visit.completed`, `appointment.created`, …), `data` con los datos del hecho, y
     *   opcionales `occurredAt`, `source` (`system`, `version`) y `schemaVersion`.
     * @param array<string, mixed> $opts Opciones por pedido (`idempotency_key`, `timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function create(array $event, array $opts = []): EventBatchResponse
    {
        if ($event === [] || self::isList($event)) {
            throw new InvalidArgumentException('events->create recibe un solo evento; para varios usa events->createBatch.');
        }

        return $this->requestObject('POST', '/v1/events', $event, $opts, EventBatchResponse::class);
    }

    /**
     * Manda un lote de hasta 100 eventos. Cada uno se procesa por separado y trae
     * su propio resultado (`processed`, `duplicate`, `unhandled`, `invalid`, `failed`).
     *
     * @param array<int, array<string, mixed>> $events Lista de eventos.
     * @param array<string, mixed> $opts Opciones por pedido (`idempotency_key`, `timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function createBatch(array $events, array $opts = []): EventBatchResponse
    {
        if ($events === [] || !self::isList($events)) {
            throw new InvalidArgumentException('events->createBatch recibe una lista no vacía de eventos.');
        }
        if (count($events) > self::MAX_BATCH) {
            throw new InvalidArgumentException('Un lote admite hasta ' . self::MAX_BATCH . ' eventos.');
        }

        return $this->requestObject('POST', '/v1/events', $events, $opts, EventBatchResponse::class);
    }

    /**
     * Devuelve un evento por su id de Kuida (`evt_…`).
     *
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function retrieve(string $id, array $opts = []): Event
    {
        return $this->requestObject('GET', self::path('/v1/events/%s', $id), null, $opts, Event::class);
    }

    /**
     * Lista eventos recibidos.
     *
     * @param array<string, mixed> $params `limit`, `startingAfter`, `endingBefore`, y filtros
     *   `type`, `status` y `source`.
     * @param array<string, mixed> $opts Opciones por pedido.
     * @return Collection<Event>
     * @throws \Kuida\Exception\KuidaException
     */
    public function list(array $params = [], array $opts = []): Collection
    {
        return $this->requestCollection('/v1/events', $params, $opts, Event::class);
    }

    /**
     * @param array<mixed> $value
     */
    private static function isList(array $value): bool
    {
        return array_keys($value) === range(0, count($value) - 1);
    }
}
