<?php

namespace Kuida\Exception;

/**
 * Parámetros inválidos, recurso inexistente o conflicto (`invalid_request_error`, HTTP 400, 404 o 409).
 */
class InvalidRequestException extends KuidaException
{
    const ERROR_TYPE = 'invalid_request_error';
}
