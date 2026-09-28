<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * Un evento que entró a Kuida, con su resultado.
 *
 * @property-read string $id Id del evento en Kuida.
 * @property-read string $object
 * @property-read string $type Valores: `intake.requested`, `patient.upserted`, `appointment.created`, `appointment.rescheduled`, `appointment.cancelled`, `visit.completed`, `treatment.prescribed`, `order.issued`.
 * @property-read string $source Por dónde entró. Valores: `api`, `pms`, `email`, `tool`, `manual`, `import`.
 * @property-read string $sourceRef Identidad del evento en su fuente. Para la API, el `id` que envió.
 * @property-read string $status `processed` Kuida hizo lo suyo · `unhandled` guardado, sin efecto todavía · `failed` falló, reintente con el mismo id · `duplicate` · `received` en curso. Valores: `received`, `processed`, `failed`, `duplicate`, `unhandled`.
 * @property-read string|null $error
 * @property-read \Kuida\KuidaObject|null $result Lo que produjo el evento (`{ object: "visit", id: "vis_…" }`).
 * @property-read \Kuida\KuidaObject $data
 * @property-read string $occurredAt Fecha y hora ISO 8601.
 * @property-read string $receivedAt Fecha y hora ISO 8601.
 * @property-read string|null $processedAt Fecha y hora ISO 8601.
 */
class Event extends \Kuida\KuidaObject
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
    const SOURCE_API = 'api';
    const SOURCE_PMS = 'pms';
    const SOURCE_EMAIL = 'email';
    const SOURCE_TOOL = 'tool';
    const SOURCE_MANUAL = 'manual';
    const SOURCE_IMPORT = 'import';
    const STATUS_RECEIVED = 'received';
    const STATUS_PROCESSED = 'processed';
    const STATUS_FAILED = 'failed';
    const STATUS_DUPLICATE = 'duplicate';
    const STATUS_UNHANDLED = 'unhandled';
}
