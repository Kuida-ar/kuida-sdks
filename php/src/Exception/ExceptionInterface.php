<?php

namespace Kuida\Exception;

/**
 * Interfaz común a todas las excepciones del SDK de Kuida.
 *
 * Sirve para atrapar cualquier error del SDK, incluidos los errores de uso
 * ({@see InvalidArgumentException}) que no vienen de la API.
 */
interface ExceptionInterface extends \Throwable
{
}
