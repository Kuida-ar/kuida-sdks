<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Un resultado por evento, en el mismo orden.
 *
 * @property-read string $object
 * @property-read EventResult[] $results
 */
class EventBatchResponse extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'event_batch';

    const NESTED_TYPES = [
        'results' => EventResult::class,
    ];
}
