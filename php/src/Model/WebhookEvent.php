<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Lo que Kuida le manda a su endpoint.
 *
 * @property-read string $id Id de la entrega (`whd_…`). Estable entre reintentos: deduplique por este campo.
 * @property-read string $object
 * @property-read string $type Valores: `intake.requested`, `patient.upserted`, `appointment.created`, `appointment.rescheduled`, `appointment.cancelled`, `visit.completed`, `treatment.prescribed`, `order.issued`, `intake.created`, `intake.ready`, `conversation.handoff`, `patient.silent`, `webhook.ping`.
 * @property-read string $apiVersion
 * @property-read bool $livemode
 * @property-read string $occurredAt Cuándo pasó, ISO 8601.
 * @property-read string $ref Id interno de lo que disparó el evento.
 * @property-read \Kuida\KuidaObject $data Los datos del hecho.
 */
class WebhookEvent extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'event';

    const TYPE_INTAKE_REQUESTED = 'intake.requested';
    const TYPE_PATIENT_UPSERTED = 'patient.upserted';
    const TYPE_APPOINTMENT_CREATED = 'appointment.created';
    const TYPE_APPOINTMENT_RESCHEDULED = 'appointment.rescheduled';
    const TYPE_APPOINTMENT_CANCELLED = 'appointment.cancelled';
    const TYPE_VISIT_COMPLETED = 'visit.completed';
    const TYPE_TREATMENT_PRESCRIBED = 'treatment.prescribed';
    const TYPE_ORDER_ISSUED = 'order.issued';
    const TYPE_INTAKE_CREATED = 'intake.created';
    const TYPE_INTAKE_READY = 'intake.ready';
    const TYPE_CONVERSATION_HANDOFF = 'conversation.handoff';
    const TYPE_PATIENT_SILENT = 'patient.silent';
    const TYPE_WEBHOOK_PING = 'webhook.ping';
}
