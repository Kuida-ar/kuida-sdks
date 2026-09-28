<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Datos del OpenAPI que usa el SDK en tiempo de ejecución.
 */
final class ObjectTypes
{
    /** Versión de la API (`info.version` del OpenAPI). */
    const API_VERSION = '2026-09-28';

    /**
     * Clase de cada valor de `object`.
     *
     * @var array<string, class-string<\Kuida\KuidaObject>>
     */
    const MAPPING = [
        'account' => Account::class,
        'appointment' => Appointment::class,
        'doctor' => Doctor::class,
        'event' => Event::class,
        'event_batch' => EventBatchResponse::class,
        'intake_request' => IntakeRequest::class,
        'patient' => Patient::class,
        'treatment' => Treatment::class,
        'visit' => Visit::class,
        'webhook_delivery' => WebhookDelivery::class,
        'webhook_endpoint' => WebhookEndpoint::class,
    ];

    /**
     * Campos `format: date` (AAAA-MM-DD): un `DateTimeInterface` se manda sin hora.
     *
     * @var string[]
     */
    const DATE_ONLY_FIELDS = [
        'dateOfBirth',
    ];
}
