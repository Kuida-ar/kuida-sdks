<?php

namespace Kuida\Exception;

/**
 * Clave de API faltante, inválida, revocada o vencida (`authentication_error`, HTTP 401).
 */
class AuthenticationException extends KuidaException
{
    const ERROR_TYPE = 'authentication_error';
}
