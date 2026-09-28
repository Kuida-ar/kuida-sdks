<?php

namespace Kuida\Service;

use Kuida\Collection;
use Kuida\Model\IntakeRequest;

/**
 * Solicitudes de ingreso: pedidos de servicio que Kuida completa por WhatsApp
 * hasta que quedan listos para cargar.
 */
class IntakeRequestService extends AbstractService
{
    /**
     * Crea una solicitud de ingreso. Hace falta al menos un dato del paciente o de un referente.
     *
     * @param array<string, mixed> $params `patient` (identidad: `phone`, `fullName`, `dni`, …),
     *   `contacts` (lista de `name`, `relationship`, `phone`, `email`), `coverage`
     *   (`insurer`, `plan`, `memberId`), `address`, `requestedService`, `summary`,
     *   `hospitalized`, `sender` (`name`, `email`, `organization`).
     * @param array<string, mixed> $opts Opciones por pedido (`idempotency_key`, `timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function create(array $params, array $opts = []): IntakeRequest
    {
        return $this->requestObject('POST', '/v1/intake_requests', $params, $opts, IntakeRequest::class);
    }

    /**
     * Devuelve una solicitud de ingreso por su id.
     *
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function retrieve(string $id, array $opts = []): IntakeRequest
    {
        return $this->requestObject('GET', self::path('/v1/intake_requests/%s', $id), null, $opts, IntakeRequest::class);
    }

    /**
     * Lista solicitudes de ingreso.
     *
     * @param array<string, mixed> $params `limit`, `startingAfter`, `endingBefore`, y filtros
     *   `status` y `patient`.
     * @param array<string, mixed> $opts Opciones por pedido.
     * @return Collection<IntakeRequest>
     * @throws \Kuida\Exception\KuidaException
     */
    public function list(array $params = [], array $opts = []): Collection
    {
        return $this->requestCollection('/v1/intake_requests', $params, $opts, IntakeRequest::class);
    }
}
