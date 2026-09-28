<?php

namespace Kuida\Exception;

/**
 * Se superó el límite de pedidos por minuto (`rate_limit_error`, HTTP 429). El SDK reintenta solo respetando `Retry-After`.
 */
class RateLimitException extends KuidaException
{
    const ERROR_TYPE = 'rate_limit_error';
}
