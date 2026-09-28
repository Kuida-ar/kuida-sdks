<?php

namespace Kuida\Service;

use Kuida\Collection;
use Kuida\Model\Visit;

/**
 * Consultas atendidas. Registrar una consulta dispara el seguimiento del paciente.
 */
class VisitService extends AbstractService
{
    /**
     * Registra una consulta. Si viene de un turno (`appointment`, id `apt_…` o el
     * `externalId` del turno), el turno queda `completed`.
     *
     * @param array<string, mixed> $params `patient` (obligatorio), `visitedAt` (obligatorio),
     *   `doctor`, `type`, `externalId`, `appointment`, `diagnosis`, `notes`.
     * @param array<string, mixed> $opts Opciones por pedido (`idempotency_key`, `timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function create(array $params, array $opts = []): Visit
    {
        return $this->requestObject('POST', '/v1/visits', $params, $opts, Visit::class);
    }

    /**
     * Devuelve una consulta por su id (`vis_…`).
     *
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function retrieve(string $id, array $opts = []): Visit
    {
        return $this->requestObject('GET', self::path('/v1/visits/%s', $id), null, $opts, Visit::class);
    }

    /**
     * Lista consultas.
     *
     * @param array<string, mixed> $params `limit`, `startingAfter`, `endingBefore`, y filtros
     *   `patient` y `externalId`.
     * @param array<string, mixed> $opts Opciones por pedido.
     * @return Collection<Visit>
     * @throws \Kuida\Exception\KuidaException
     */
    public function list(array $params = [], array $opts = []): Collection
    {
        return $this->requestCollection('/v1/visits', $params, $opts, Visit::class);
    }
}
