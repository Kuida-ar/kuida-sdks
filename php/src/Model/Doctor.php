<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Un profesional del padrón de la organización.
 *
 * @property-read string $id Id del profesional.
 * @property-read string $object
 * @property-read string $fullName
 * @property-read string|null $specialty
 * @property-read string|null $licenseNumber Matrícula.
 * @property-read string|null $externalId
 * @property-read bool $active Si atiende hoy. Solo los activos reciben turnos del agente.
 * @property-read string $createdAt Fecha y hora ISO 8601.
 */
class Doctor extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'doctor';
}
