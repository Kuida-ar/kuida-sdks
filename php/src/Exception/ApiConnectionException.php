<?php

namespace Kuida\Exception;

/**
 * No hubo respuesta de la API: error de red, DNS, TLS o tiempo de espera agotado.
 *
 * El SDK ya reintentó según `max_retries` antes de lanzarla.
 */
class ApiConnectionException extends KuidaException
{
}
