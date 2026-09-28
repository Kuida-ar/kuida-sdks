<?php

// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.

namespace Kuida\Model;

/**
 * La organización dueña de la clave de API.
 *
 * @property-read string $id Id de la organización.
 * @property-read string $object
 * @property-read string $name
 * @property-read string $type Tipo de institución (clinic, pharmacy, lab, …).
 * @property-read string $timeZone
 * @property-read string[] $serviceLines Líneas de servicio contratadas: seguimiento de pacientes y red de derivaciones.
 * @property-read bool $livemode `true` en producción, `false` en el entorno de pruebas.
 * @property-read \Kuida\KuidaObject $apiKey
 */
class Account extends \Kuida\KuidaObject
{
    /** Valor del campo `object`. */
    const OBJECT_NAME = 'account';
}
