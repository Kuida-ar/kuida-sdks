<?php

namespace Kuida\Exception;

/**
 * A la clave le falta el permiso (scope) o la organización no tiene contratada la línea de servicio (`permission_error`, HTTP 403).
 */
class PermissionException extends KuidaException
{
    const ERROR_TYPE = 'permission_error';
}
