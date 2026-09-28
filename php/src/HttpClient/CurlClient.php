<?php

namespace Kuida\HttpClient;

use Kuida\Exception\ApiConnectionException;

/**
 * Transporte HTTP con `ext-curl`. Reusa la conexión entre pedidos.
 */
class CurlClient implements ClientInterface
{
    /** @var resource|null Handle de cURL (en PHP 8 es un objeto \CurlHandle). */
    private $handle;

    public function request(string $method, string $url, array $headers, ?string $body, float $timeout): array
    {
        if ($method === '' || $url === '') {
            throw new ApiConnectionException('Pedido HTTP sin método o sin URL.');
        }
        if ($this->handle === null) {
            $handle = curl_init();
            if ($handle === false) {
                throw new ApiConnectionException('No se pudo inicializar cURL.');
            }
            $this->handle = $handle;
        } else {
            curl_reset($this->handle);
        }
        $ch = $this->handle;

        $headerLines = ['Expect:'];
        foreach ($headers as $name => $value) {
            $headerLines[] = $name . ': ' . $value;
        }

        $responseHeaders = [];
        $timeoutMs = max(1, (int) round($timeout * 1000));
        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_CUSTOMREQUEST => $method,
            CURLOPT_HTTPHEADER => $headerLines,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_NOSIGNAL => true,
            CURLOPT_CONNECTTIMEOUT_MS => $timeoutMs,
            CURLOPT_TIMEOUT_MS => $timeoutMs,
            CURLOPT_HEADERFUNCTION => static function ($curl, string $line) use (&$responseHeaders): int {
                $parts = explode(':', $line, 2);
                if (count($parts) === 2) {
                    $responseHeaders[strtolower(trim($parts[0]))] = trim($parts[1]);
                }

                return strlen($line);
            },
        ]);
        if ($body !== null) {
            curl_setopt($ch, CURLOPT_POSTFIELDS, $body);
        }

        $result = curl_exec($ch);
        if ($result === false || !is_string($result)) {
            $errno = curl_errno($ch);
            $message = curl_error($ch);
            $this->handle = null;
            throw new ApiConnectionException(self::connectionMessage($errno, $message, $url));
        }
        $status = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);

        return [$status, $responseHeaders, $result];
    }

    private static function connectionMessage(int $errno, string $message, string $url): string
    {
        $host = parse_url($url, PHP_URL_HOST);
        $where = is_string($host) ? $host : $url;
        // Códigos de libcurl: 6 DNS, 7 conexión, 28 timeout, 35/58/60/77 TLS.
        switch ($errno) {
            case 6:
                $reason = 'no se pudo resolver el nombre de ' . $where;
                break;
            case 7:
                $reason = 'no se pudo conectar con ' . $where;
                break;
            case 28:
                $reason = 'se agotó el tiempo de espera con ' . $where;
                break;
            case 35:
            case 58:
            case 60:
            case 77:
                $reason = 'falló la conexión segura (TLS) con ' . $where;
                break;
            default:
                $reason = 'error de red con ' . $where;
        }

        return 'No se pudo comunicar con Kuida: ' . $reason . ' (cURL ' . $errno . ': ' . $message . ').';
    }
}
