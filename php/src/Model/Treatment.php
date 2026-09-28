<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Un tratamiento indicado al paciente.
 *
 * @property-read string $id Id del tratamiento.
 * @property-read string $object
 * @property-read string $patient Paciente.
 * @property-read string $kind Valores: `medication`, `therapy`, `diet`, `study`, `labwork`, `lifestyle`.
 * @property-read string $name
 * @property-read string|null $dosage
 * @property-read string|null $frequency
 * @property-read string|null $instructions
 * @property-read string|null $startDate Fecha y hora ISO 8601.
 * @property-read string|null $endDate Fecha y hora ISO 8601.
 * @property-read bool $active
 * @property-read string $createdAt Fecha y hora ISO 8601.
 * @property-read string $updatedAt Fecha y hora ISO 8601.
 */
class Treatment extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'treatment';

    const KIND_MEDICATION = 'medication';
    const KIND_THERAPY = 'therapy';
    const KIND_DIET = 'diet';
    const KIND_STUDY = 'study';
    const KIND_LABWORK = 'labwork';
    const KIND_LIFESTYLE = 'lifestyle';
}
