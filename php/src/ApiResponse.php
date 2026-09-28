<?php

namespace Kuida;

/**
 * Respuesta HTTP cruda de la API: estado, headers y cuerpo tal como llegaron.
 *
 * Todo objeto devuelto por un método del SDK la expone con `getLastResponse()`.
 */
class ApiResponse
{
    /** @var int Estado HTTP. */
    public $code;

    /** @var array<string, string> Headers de la respuesta, con nombres en minúsculas. */
    public $headers;

    /** @var string Cuerpo crudo (JSON). */
    public $body;

    /** @var mixed Cuerpo decodificado como arrays asociativos, o null si no era JSON. */
    public $json;

    /**
     * @param int $code
     * @param array<string, string> $headers
     * @param string $body
     */
    public function __construct(int $code, array $headers, string $body)
    {
        $this->code = $code;
        $this->headers = $headers;
        $this->body = $body;
        $this->json = json_decode($body, true);
    }

    /**
     * Devuelve un header por nombre (sin distinguir mayúsculas), o null.
     */
    public function getHeader(string $name): ?string
    {
        $key = strtolower($name);

        return isset($this->headers[$key]) ? $this->headers[$key] : null;
    }

    /**
     * Id del pedido (`Request-Id`), útil para soporte.
     */
    public function getRequestId(): ?string
    {
        return $this->getHeader('request-id');
    }
}
