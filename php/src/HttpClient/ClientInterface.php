<?php

namespace Kuida\HttpClient;

use Kuida\Exception\ApiConnectionException;

/**
 * Transporte HTTP del SDK. La implementación por defecto usa cURL
 * ({@see CurlClient}); se puede inyectar otra con la opción `http_client`.
 */
interface ClientInterface
{
    /**
     * Manda un pedido HTTP y devuelve la respuesta sin interpretarla.
     *
     * @param string $method Método HTTP en mayúsculas.
     * @param string $url URL absoluta, con query string.
     * @param array<string, string> $headers Headers a mandar.
     * @param string|null $body Cuerpo del pedido, o null.
     * @param float $timeout Tiempo máximo en segundos.
     * @return array{0: int, 1: array<string, string>, 2: string} Estado, headers (nombres en minúsculas) y cuerpo.
     * @throws ApiConnectionException Si no hubo respuesta (red, DNS, TLS, timeout).
     */
    public function request(string $method, string $url, array $headers, ?string $body, float $timeout): array;
}
