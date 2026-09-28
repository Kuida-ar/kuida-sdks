<?php

namespace Kuida\Service;

use Kuida\Collection;
use Kuida\Model\Treatment;

/**
 * Tratamientos e indicaciones. Kuida acompaña al paciente con recordatorios.
 */
class TreatmentService extends AbstractService
{
    /**
     * Crea un tratamiento.
     *
     * @param array<string, mixed> $params `patient` (obligatorio), `name` (obligatorio), `kind`
     *   (`medication` por defecto, `therapy`, `diet`, `study`, `labwork`, `lifestyle`),
     *   `dosage`, `frequency`, `instructions`, `startDate`, `endDate`.
     * @param array<string, mixed> $opts Opciones por pedido (`idempotency_key`, `timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function create(array $params, array $opts = []): Treatment
    {
        return $this->requestObject('POST', '/v1/treatments', $params, $opts, Treatment::class);
    }

    /**
     * Devuelve un tratamiento por su id (`trt_…`).
     *
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function retrieve(string $id, array $opts = []): Treatment
    {
        return $this->requestObject('GET', self::path('/v1/treatments/%s', $id), null, $opts, Treatment::class);
    }

    /**
     * Lista tratamientos.
     *
     * @param array<string, mixed> $params `limit`, `startingAfter`, `endingBefore`, y filtros
     *   `patient` y `active` (bool).
     * @param array<string, mixed> $opts Opciones por pedido.
     * @return Collection<Treatment>
     * @throws \Kuida\Exception\KuidaException
     */
    public function list(array $params = [], array $opts = []): Collection
    {
        return $this->requestCollection('/v1/treatments', $params, $opts, Treatment::class);
    }
}
