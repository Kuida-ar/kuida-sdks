<?php

namespace Kuida\Service;

use Kuida\Collection;
use Kuida\Model\Doctor;

/**
 * Profesionales de tu institución.
 */
class DoctorService extends AbstractService
{
    /**
     * Lista profesionales.
     *
     * @param array<string, mixed> $params `limit`, `startingAfter`, `endingBefore`,
     *   y filtros `active` (bool) y `externalId`.
     * @param array<string, mixed> $opts Opciones por pedido.
     * @return Collection<Doctor>
     * @throws \Kuida\Exception\KuidaException
     */
    public function list(array $params = [], array $opts = []): Collection
    {
        return $this->requestCollection('/v1/doctors', $params, $opts, Doctor::class);
    }

    /**
     * Devuelve un profesional por su id (`doc_…`).
     *
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function retrieve(string $id, array $opts = []): Doctor
    {
        return $this->requestObject('GET', self::path('/v1/doctors/%s', $id), null, $opts, Doctor::class);
    }
}
