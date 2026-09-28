<?php

namespace Kuida\Exception;

/**
 * Un dato recibido no tiene la forma esperada (por ejemplo, el cuerpo de un
 * webhook con firma válida que no es JSON).
 */
class UnexpectedValueException extends \UnexpectedValueException implements ExceptionInterface
{
}
