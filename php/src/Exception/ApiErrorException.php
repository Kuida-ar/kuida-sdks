<?php

namespace Kuida\Exception;

/**
 * Error del lado de Kuida (`api_error`) o respuesta que no se pudo interpretar.
 */
class ApiErrorException extends KuidaException
{
    const ERROR_TYPE = 'api_error';
}
