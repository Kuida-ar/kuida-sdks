<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Un turno. Kuida le manda al paciente los recordatorios que la organización configuró.
 *
 * @property-read string $id Id del turno.
 * @property-read string $object
 * @property-read string $patient Paciente del turno.
 * @property-read string|null $doctor
 * @property-read string $startAt Fecha y hora ISO 8601.
 * @property-read string $status Valores: `confirmed`, `pending`, `rescheduled`, `cancelled`, `completed`.
 * @property-read string|null $type Tipo de turno o práctica.
 * @property-read string|null $externalId Id del turno en tu sistema.
 * @property-read string $source Por dónde entró: tu API, el conector del sistema de gestión, el agente de Kuida o el equipo. Valores: `api`, `pms`, `agent`, `manual`.
 * @property-read string|null $cancelledAt Fecha y hora ISO 8601.
 * @property-read string $createdAt Fecha y hora ISO 8601.
 * @property-read string $updatedAt Fecha y hora ISO 8601.
 */
class Appointment extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'appointment';

    const STATUS_CONFIRMED = 'confirmed';
    const STATUS_PENDING = 'pending';
    const STATUS_RESCHEDULED = 'rescheduled';
    const STATUS_CANCELLED = 'cancelled';
    const STATUS_COMPLETED = 'completed';
    const SOURCE_API = 'api';
    const SOURCE_PMS = 'pms';
    const SOURCE_AGENT = 'agent';
    const SOURCE_MANUAL = 'manual';
}
