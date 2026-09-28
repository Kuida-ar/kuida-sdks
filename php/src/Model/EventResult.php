<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Objeto `EventResult` de la API de Kuida.
 *
 * @property-read string $id El id que envió.
 * @property-read bool $accepted
 * @property-read string $status Valores: `processed`, `duplicate`, `unhandled`, `invalid`, `failed`.
 * @property-read string|null $event Id del evento en Kuida (`evt_…`).
 * @property-read \Kuida\KuidaObject|null $result
 * @property-read string|null $error
 */
class EventResult extends \Kuida\KuidaObject
{
    const STATUS_PROCESSED = 'processed';
    const STATUS_DUPLICATE = 'duplicate';
    const STATUS_UNHANDLED = 'unhandled';
    const STATUS_INVALID = 'invalid';
    const STATUS_FAILED = 'failed';
}
