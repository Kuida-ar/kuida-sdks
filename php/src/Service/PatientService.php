<?php

namespace Kuida\Service;

use Kuida\Collection;
use Kuida\Model\Patient;

/**
 * Pacientes de tu institución.
 */
class PatientService extends AbstractService
{
    /**
     * Crea un paciente, o devuelve el existente si ya hay uno con ese teléfono o DNI.
     *
     * @param array<string, mixed> $params `phone` (obligatorio), `fullName`, `dni`,
     *   `email`, `externalId`, `dateOfBirth` (`AAAA-MM-DD` o `DateTimeInterface`).
     * @param array<string, mixed> $opts Opciones por pedido (`idempotency_key`, `timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function create(array $params, array $opts = []): Patient
    {
        return $this->requestObject('POST', '/v1/patients', $params, $opts, Patient::class);
    }

    /**
     * Devuelve un paciente por su id (`pat_…`).
     *
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function retrieve(string $id, array $opts = []): Patient
    {
        return $this->requestObject('GET', self::path('/v1/patients/%s', $id), null, $opts, Patient::class);
    }

    /**
     * Actualiza un paciente. Solo cambian los campos que mandes.
     *
     * @param array<string, mixed> $params `fullName`, `email`, `dni`, `externalId`, `dateOfBirth`.
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function update(string $id, array $params, array $opts = []): Patient
    {
        return $this->requestObject('PATCH', self::path('/v1/patients/%s', $id), $params, $opts, Patient::class);
    }

    /**
     * Lista pacientes, del más nuevo al más viejo.
     *
     * @param array<string, mixed> $params `limit` (1-100), `startingAfter`, `endingBefore`,
     *   y filtros `phone`, `dni`, `externalId`.
     * @param array<string, mixed> $opts Opciones por pedido.
     * @return Collection<Patient>
     * @throws \Kuida\Exception\KuidaException
     */
    public function list(array $params = [], array $opts = []): Collection
    {
        return $this->requestCollection('/v1/patients', $params, $opts, Patient::class);
    }
}
