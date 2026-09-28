<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Una URL de su sistema a la que Kuida le avisa lo que pasa.
 *
 * @property-read string $id Id del endpoint.
 * @property-read string $object
 * @property-read string $url
 * @property-read string|null $description
 * @property-read string[] $enabledEvents Tipos de evento que recibe. `*` = todos.
 * @property-read string $status Valores: `enabled`, `disabled`.
 * @property-read string|null $secret Secreto para verificar la firma (`whsec_…`). Solo viene en la respuesta de creación. Puede no venir.
 * @property-read string|null $lastDeliveredAt Fecha y hora ISO 8601.
 * @property-read \Kuida\KuidaObject|null $lastError
 * @property-read string $createdAt Fecha y hora ISO 8601.
 */
class WebhookEndpoint extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'webhook_endpoint';

    const STATUS_ENABLED = 'enabled';
    const STATUS_DISABLED = 'disabled';
}
