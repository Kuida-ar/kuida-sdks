<?php

namespace Kuida\Exception;

/**
 * La `Idempotency-Key` ya se usó con otro pedido (`idempotency_error`).
 */
class IdempotencyException extends KuidaException
{
    const ERROR_TYPE = 'idempotency_error';
}
