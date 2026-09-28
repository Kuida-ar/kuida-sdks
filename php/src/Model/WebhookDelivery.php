<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Un envío de webhook con sus reintentos.
 *
 * @property-read string $id Id de la entrega.
 * @property-read string $object
 * @property-read string $webhookEndpoint Endpoint destino.
 * @property-read string $eventType
 * @property-read string $status `pending` en curso o esperando reintento · `succeeded` su endpoint respondió 2xx · `failed` se agotaron los reintentos. Valores: `pending`, `succeeded`, `failed`.
 * @property-read int $attempts
 * @property-read int|null $lastStatusCode
 * @property-read string|null $lastError
 * @property-read string|null $nextAttemptAt Fecha y hora ISO 8601.
 * @property-read string|null $deliveredAt Fecha y hora ISO 8601.
 * @property-read \Kuida\KuidaObject $payload El cuerpo exacto que se envió.
 * @property-read string $createdAt Fecha y hora ISO 8601.
 */
class WebhookDelivery extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'webhook_delivery';

    const STATUS_PENDING = 'pending';
    const STATUS_SUCCEEDED = 'succeeded';
    const STATUS_FAILED = 'failed';
}
