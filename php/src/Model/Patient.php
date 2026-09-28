<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Un paciente de la organización.
 *
 * @property-read string $id Id del paciente.
 * @property-read string $object
 * @property-read string|null $fullName
 * @property-read string|null $phone Teléfono normalizado.
 * @property-read string|null $email
 * @property-read string|null $dni
 * @property-read string|null $externalId Id del paciente en tu sistema.
 * @property-read string|null $dateOfBirth Fecha de nacimiento (AAAA-MM-DD).
 * @property-read string $stage Estado básico del paciente en Kuida. Valores: `pending_follow_up`, `in_conversation`, `scheduled`, `periodic_follow_up`, `discharged`, `lost`.
 * @property-read bool $optedOut Si el paciente pidió no recibir mensajes. Kuida no le escribe.
 * @property-read string $createdAt Fecha y hora ISO 8601.
 * @property-read string $updatedAt Fecha y hora ISO 8601.
 */
class Patient extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'patient';

    const STAGE_PENDING_FOLLOW_UP = 'pending_follow_up';
    const STAGE_IN_CONVERSATION = 'in_conversation';
    const STAGE_SCHEDULED = 'scheduled';
    const STAGE_PERIODIC_FOLLOW_UP = 'periodic_follow_up';
    const STAGE_DISCHARGED = 'discharged';
    const STAGE_LOST = 'lost';
}
