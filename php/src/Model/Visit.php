<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Una consulta atendida. Crearla arranca el seguimiento post-consulta del paciente.
 *
 * @property-read string $id Id de la consulta.
 * @property-read string $object
 * @property-read string $patient Paciente atendido.
 * @property-read string|null $doctor
 * @property-read string|null $appointment
 * @property-read string $visitedAt Fecha y hora ISO 8601.
 * @property-read string|null $type
 * @property-read string|null $diagnosis
 * @property-read string|null $notes
 * @property-read string|null $externalId
 * @property-read string $source Valores: `pms`, `manual`.
 * @property-read string $createdAt Fecha y hora ISO 8601.
 */
class Visit extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'visit';

    const SOURCE_PMS = 'pms';
    const SOURCE_MANUAL = 'manual';
}
