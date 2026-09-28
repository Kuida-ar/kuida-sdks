<?php

namespace Kuida;

use Kuida\Exception\ApiConnectionException;
use Kuida\Exception\InvalidArgumentException;
use Kuida\Exception\KuidaException;
use Kuida\HttpClient\ClientInterface;

/**
 * Arma los pedidos HTTP, aplica idempotencia y reintentos, y convierte los
 * errores en excepciones.
 *
 * @internal Usa los servicios del cliente ({@see KuidaClient}).
 */
class ApiRequestor
{
    const REQUEST_OPTIONS = ['idempotency_key', 'timeout', 'max_retries'];

    const RETRYABLE_STATUSES = [429, 500, 502, 503, 504];

    /** @var KuidaClient */
    private $client;

    /** @var ClientInterface */
    private $http;

    /** @var callable(float): void */
    private $sleeper;

    /**
     * @param callable(float): void $sleeper
     */
    public function __construct(KuidaClient $client, ClientInterface $http, callable $sleeper)
    {
        $this->client = $client;
        $this->http = $http;
        $this->sleeper = $sleeper;
    }

    /**
     * Manda un pedido a la API y devuelve la respuesta 2xx.
     *
     * @param string $method GET, POST, PATCH o DELETE.
     * @param string $path Ruta desde la base (`/v1/patients`).
     * @param array<mixed>|null $params Query (GET/DELETE) o cuerpo JSON (POST/PATCH).
     * @param array<string, mixed> $opts Opciones por pedido.
     * @throws KuidaException
     */
    public function request(string $method, string $path, ?array $params = null, array $opts = []): ApiResponse
    {
        foreach (array_keys($opts) as $name) {
            if (!in_array($name, self::REQUEST_OPTIONS, true)) {
                throw new InvalidArgumentException(
                    'Opción de pedido desconocida: "' . $name . '". Las válidas son: ' . implode(', ', self::REQUEST_OPTIONS) . '.'
                );
            }
        }
        $timeout = isset($opts['timeout']) ? (float) $opts['timeout'] : $this->client->getTimeout();
        $maxRetries = isset($opts['max_retries']) ? (int) $opts['max_retries'] : $this->client->getMaxRetries();

        $url = $this->client->getBaseUrl() . $path;
        $body = null;
        $hasBody = $method === 'POST' || $method === 'PATCH';
        if ($hasBody) {
            $body = Util::encodeBody($params);
        } elseif ($params !== null && $params !== []) {
            $query = Util::encodeQuery($params);
            if ($query !== '') {
                $url .= '?' . $query;
            }
        }

        $headers = $this->defaultHeaders();
        if ($hasBody) {
            $headers['Content-Type'] = 'application/json';
        }
        if ($method === 'POST') {
            $key = isset($opts['idempotency_key']) ? (string) $opts['idempotency_key'] : '';
            // La misma clave viaja en todos los reintentos de este pedido.
            $headers['Idempotency-Key'] = $key !== '' ? $key : Util::uuidV4();
        }

        $attempt = 0;
        while (true) {
            try {
                list($status, $responseHeaders, $responseBody) = $this->http->request($method, $url, $headers, $body, $timeout);
            } catch (ApiConnectionException $e) {
                if ($attempt < $maxRetries) {
                    $this->sleep($this->backoff($attempt, null));
                    $attempt++;
                    continue;
                }
                throw $e;
            }

            if ($status >= 200 && $status < 300) {
                return new ApiResponse($status, $responseHeaders, $responseBody);
            }

            $error = KuidaException::fromResponse($status, $responseBody, $responseHeaders);
            if ($attempt < $maxRetries && self::shouldRetry($status, $error)) {
                $retryAfter = isset($responseHeaders['retry-after']) ? $responseHeaders['retry-after'] : null;
                $this->sleep($this->backoff($attempt, $retryAfter));
                $attempt++;
                continue;
            }
            throw $error;
        }
    }

    /**
     * @return array<string, string>
     */
    private function defaultHeaders(): array
    {
        return [
            'Authorization' => 'Bearer ' . $this->client->getApiKey(),
            'Accept' => 'application/json',
            'Kuida-Version' => $this->client->getApiVersion(),
            'User-Agent' => 'Kuida/v1 PhpBindings/' . KuidaClient::VERSION,
            'X-Kuida-Client-User-Agent' => self::clientUserAgent(),
        ];
    }

    private static function clientUserAgent(): string
    {
        $platform = PHP_OS;
        if (function_exists('php_uname')) {
            $platform = trim(php_uname('s') . ' ' . php_uname('r') . ' ' . php_uname('m'));
        }
        $json = json_encode([
            'bindings_version' => KuidaClient::VERSION,
            'lang' => 'php',
            'lang_version' => PHP_VERSION,
            'platform' => $platform,
        ], JSON_UNESCAPED_SLASHES);

        return $json === false ? '{}' : $json;
    }

    private static function shouldRetry(int $status, KuidaException $error): bool
    {
        if ($status === 409) {
            return $error->getErrorCode() === 'idempotency_key_in_use';
        }

        return in_array($status, self::RETRYABLE_STATUSES, true);
    }

    /**
     * Espera antes del reintento número `$attempt + 1`, en segundos.
     */
    private function backoff(int $attempt, ?string $retryAfter): float
    {
        if ($retryAfter !== null && is_numeric(trim($retryAfter))) {
            return min(max((float) trim($retryAfter), 0.0), 60.0);
        }
        $base = min(0.5 * pow(2, $attempt), 8.0);
        $jitter = 1 + (mt_rand(-250, 250) / 1000);

        return $base * $jitter;
    }

    private function sleep(float $seconds): void
    {
        call_user_func($this->sleeper, $seconds);
    }
}
