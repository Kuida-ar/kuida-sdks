<?php

namespace Kuida\Service;

use Kuida\ApiResponse;
use Kuida\Collection;
use Kuida\Exception\ApiErrorException;
use Kuida\Exception\InvalidArgumentException;
use Kuida\KuidaClient;
use Kuida\KuidaObject;

/**
 * Base de los servicios de recursos del cliente.
 *
 * Todos los métodos aceptan como último argumento opciones por pedido:
 * `idempotency_key` (solo POST), `timeout` (segundos) y `max_retries`.
 */
abstract class AbstractService
{
    /** @var KuidaClient */
    protected $client;

    public function __construct(KuidaClient $client)
    {
        $this->client = $client;
    }

    /**
     * @template T of KuidaObject
     * @param string $method
     * @param string $path
     * @param array<mixed>|null $params
     * @param array<string, mixed> $opts
     * @param class-string<T> $class
     * @return T
     */
    protected function requestObject(string $method, string $path, ?array $params, array $opts, string $class)
    {
        $response = $this->client->request($method, $path, $params, $opts);

        return $class::constructFrom(self::decode($response), $response);
    }

    /**
     * @template T of KuidaObject
     * @param string $path
     * @param array<string, mixed> $params
     * @param array<string, mixed> $opts
     * @param class-string<T> $itemClass
     * @return Collection<T>
     */
    protected function requestCollection(string $path, array $params, array $opts, string $itemClass): Collection
    {
        $response = $this->client->request('GET', $path, $params, $opts);
        $page = Collection::fromApi(self::decode($response), $itemClass, $response);
        $page->setPaging(function (array $next) use ($path, $opts, $itemClass): Collection {
            return $this->requestCollection($path, $next, $opts, $itemClass);
        }, $params);

        return $page;
    }

    /**
     * Arma una ruta con un id, validado y codificado.
     */
    protected static function path(string $format, string $id): string
    {
        if (trim($id) === '') {
            throw new InvalidArgumentException('El id no puede estar vacío.');
        }

        return sprintf($format, rawurlencode($id));
    }

    private static function decode(ApiResponse $response): \stdClass
    {
        $decoded = json_decode($response->body);
        if (!$decoded instanceof \stdClass) {
            throw new ApiErrorException(
                'La API de Kuida devolvió una respuesta que no es un objeto JSON.',
                $response->code,
                $response->body,
                null,
                $response->headers
            );
        }

        return $decoded;
    }
}
