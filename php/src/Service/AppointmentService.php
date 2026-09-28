<?php

namespace Kuida\Service;

use Kuida\Collection;
use Kuida\Model\Appointment;

/**
 * Turnos. Kuida manda los recordatorios por WhatsApp.
 */
class AppointmentService extends AbstractService
{
    /**
     * Crea un turno. Si mandas un `externalId` que ya existe, devuelve ese turno sin duplicarlo.
     *
     * @param array<string, mixed> $params `patient` (id `pat_…` o array de identidad con `phone`,
     *   obligatorio), `startAt` (ISO 8601 o `DateTimeInterface`, obligatorio), `doctor`
     *   (id `doc_…` o array con `externalId`/`fullName`/`specialty`), `type`, `externalId`.
     * @param array<string, mixed> $opts Opciones por pedido (`idempotency_key`, `timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function create(array $params, array $opts = []): Appointment
    {
        return $this->requestObject('POST', '/v1/appointments', $params, $opts, Appointment::class);
    }

    /**
     * Devuelve un turno por su id (`apt_…`).
     *
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function retrieve(string $id, array $opts = []): Appointment
    {
        return $this->requestObject('GET', self::path('/v1/appointments/%s', $id), null, $opts, Appointment::class);
    }

    /**
     * Reprograma o cambia un turno. Un turno cancelado o atendido no se puede cambiar.
     *
     * @param array<string, mixed> $params `startAt`, `doctor`, `type`.
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws \Kuida\Exception\KuidaException
     */
    public function update(string $id, array $params, array $opts = []): Appointment
    {
        return $this->requestObject('PATCH', self::path('/v1/appointments/%s', $id), $params, $opts, Appointment::class);
    }

    /**
     * Cancela un turno. Cancelar uno ya cancelado no hace nada.
     *
     * @param array<string, mixed> $params `reason` (opcional, para el registro).
     * @param array<string, mixed> $opts Opciones por pedido (`idempotency_key`, `timeout`, `max_retries`).
     * @throws \Kuida\Exception\KuidaException
     */
    public function cancel(string $id, array $params = [], array $opts = []): Appointment
    {
        return $this->requestObject('POST', self::path('/v1/appointments/%s/cancel', $id), $params, $opts, Appointment::class);
    }

    /**
     * Lista turnos.
     *
     * @param array<string, mixed> $params `limit`, `startingAfter`, `endingBefore`, y filtros
     *   `patient`, `status`, `externalId`, `startAtGte`, `startAtLt`.
     * @param array<string, mixed> $opts Opciones por pedido.
     * @return Collection<Appointment>
     * @throws \Kuida\Exception\KuidaException
     */
    public function list(array $params = [], array $opts = []): Collection
    {
        return $this->requestCollection('/v1/appointments', $params, $opts, Appointment::class);
    }
}
