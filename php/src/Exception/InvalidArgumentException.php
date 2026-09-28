<?php

namespace Kuida\Exception;

/**
 * Uso incorrecto del SDK detectado antes de mandar el pedido
 * (por ejemplo, un id vacío o una opción desconocida).
 */
class InvalidArgumentException extends \InvalidArgumentException implements ExceptionInterface
{
}
