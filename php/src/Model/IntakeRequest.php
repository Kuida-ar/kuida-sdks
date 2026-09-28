<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Una solicitud de ingreso: alguien pide el servicio para un paciente y Kuida completa lo que falta.
 *
 * @property-read string $id Id de la solicitud.
 * @property-read string $object
 * @property-read string $status `new` recién llegada · `validating` Kuida está completando datos por WhatsApp · `ready` completa · `loaded` cargada en el sistema de la organización · `discarded` descartada. Valores: `new`, `validating`, `ready`, `loaded`, `discarded`.
 * @property-read string|null $patient
 * @property-read string|null $summary
 * @property-read string[] $missing Datos que todavía faltan para que la solicitud quede lista.
 * @property-read \Kuida\KuidaObject $data Los datos de la solicitud tal como los tiene Kuida.
 * @property-read string|null $discardReason
 * @property-read string|null $readyAt Fecha y hora ISO 8601.
 * @property-read string|null $loadedAt Fecha y hora ISO 8601.
 * @property-read string|null $closedAt Fecha y hora ISO 8601.
 * @property-read string $createdAt Fecha y hora ISO 8601.
 * @property-read string $updatedAt Fecha y hora ISO 8601.
 */
class IntakeRequest extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'intake_request';

    const STATUS_NEW = 'new';
    const STATUS_VALIDATING = 'validating';
    const STATUS_READY = 'ready';
    const STATUS_LOADED = 'loaded';
    const STATUS_DISCARDED = 'discarded';
}
