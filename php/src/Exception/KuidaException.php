<?php

namespace Kuida\Exception;

/**
 * Base de todos los errores de la API de Kuida.
 *
 * Expone los datos del cuerpo `{ "error": { … } }` de la respuesta:
 * estado HTTP, tipo, código, parámetro, id del pedido y link a la documentación.
 */
class KuidaException extends \Exception implements ExceptionInterface
{
    /**
     * Valor de `error.type` que representa cada subclase.
     *
     * @var string|null
     */
    const ERROR_TYPE = null;

    /** @var int|null */
    protected $httpStatus;

    /** @var string|null */
    protected $errorType;

    /** @var string|null */
    protected $errorCode;

    /** @var string|null */
    protected $param;

    /** @var string|null */
    protected $requestId;

    /** @var string|null */
    protected $docUrl;

    /** @var array<string, string> */
    protected $httpHeaders = [];

    /** @var string|null */
    protected $httpBody;

    /** @var array<string, mixed>|null */
    protected $jsonBody;

    /**
     * @param string $message Explicación legible del error.
     * @param int|null $httpStatus Estado HTTP de la respuesta, o null si no hubo respuesta.
     * @param string|null $httpBody Cuerpo crudo de la respuesta.
     * @param array<string, mixed>|null $jsonBody Cuerpo decodificado, si era JSON.
     * @param array<string, string> $httpHeaders Headers de la respuesta (en minúsculas).
     * @param string|null $errorCode Código estable del error (`resource_missing`, …).
     * @param \Throwable|null $previous Excepción original, si la hay.
     */
    public function __construct(
        string $message = '',
        ?int $httpStatus = null,
        ?string $httpBody = null,
        ?array $jsonBody = null,
        array $httpHeaders = [],
        ?string $errorCode = null,
        ?\Throwable $previous = null
    ) {
        parent::__construct($message, 0, $previous);
        $this->httpStatus = $httpStatus;
        $this->httpBody = $httpBody;
        $this->jsonBody = $jsonBody;
        $this->httpHeaders = $httpHeaders;
        $this->errorCode = $errorCode;

        $error = isset($jsonBody['error']) && is_array($jsonBody['error']) ? $jsonBody['error'] : [];
        $this->errorType = isset($error['type']) && is_string($error['type']) ? $error['type'] : static::ERROR_TYPE;
        if ($this->errorCode === null && isset($error['code']) && is_string($error['code'])) {
            $this->errorCode = $error['code'];
        }
        $this->param = isset($error['param']) && is_string($error['param']) ? $error['param'] : null;
        $this->docUrl = isset($error['docUrl']) && is_string($error['docUrl']) ? $error['docUrl'] : null;
        if (isset($error['requestId']) && is_string($error['requestId'])) {
            $this->requestId = $error['requestId'];
        } elseif (isset($httpHeaders['request-id'])) {
            $this->requestId = $httpHeaders['request-id'];
        }
    }

    /**
     * Construye la excepción que corresponde a una respuesta de error de la API,
     * según `error.type` (§7 del diseño de los SDKs).
     *
     * @param int $httpStatus
     * @param string $httpBody
     * @param array<string, string> $httpHeaders
     * @return KuidaException
     */
    public static function fromResponse(int $httpStatus, string $httpBody, array $httpHeaders): KuidaException
    {
        $json = json_decode($httpBody, true);
        if (!is_array($json) || !isset($json['error']) || !is_array($json['error'])) {
            $text = trim($httpBody);
            $message = $text !== '' && !is_array($json)
                ? $text
                : 'Respuesta inesperada de la API de Kuida (HTTP ' . $httpStatus . ').';

            return new ApiErrorException($message, $httpStatus, $httpBody, is_array($json) ? $json : null, $httpHeaders);
        }

        $error = $json['error'];
        $message = isset($error['message']) && is_string($error['message'])
            ? $error['message']
            : 'Error de la API de Kuida (HTTP ' . $httpStatus . ').';
        $type = isset($error['type']) && is_string($error['type']) ? $error['type'] : null;

        switch ($type) {
            case 'invalid_request_error':
                return new InvalidRequestException($message, $httpStatus, $httpBody, $json, $httpHeaders);
            case 'authentication_error':
                return new AuthenticationException($message, $httpStatus, $httpBody, $json, $httpHeaders);
            case 'permission_error':
                return new PermissionException($message, $httpStatus, $httpBody, $json, $httpHeaders);
            case 'idempotency_error':
                return new IdempotencyException($message, $httpStatus, $httpBody, $json, $httpHeaders);
            case 'rate_limit_error':
                return new RateLimitException($message, $httpStatus, $httpBody, $json, $httpHeaders);
            default:
                return new ApiErrorException($message, $httpStatus, $httpBody, $json, $httpHeaders);
        }
    }

    /**
     * Estado HTTP de la respuesta (null si no hubo respuesta).
     */
    public function getHttpStatus(): ?int
    {
        return $this->httpStatus;
    }

    /**
     * Categoría del error (`invalid_request_error`, `authentication_error`, …).
     */
    public function getErrorType(): ?string
    {
        return $this->errorType;
    }

    /**
     * Código estable del error, para programar contra él (`resource_missing`, `scope_missing`, …).
     */
    public function getErrorCode(): ?string
    {
        return $this->errorCode;
    }

    /**
     * Parámetro que causó el error, si aplica (`phone`, `patient.phone`, …).
     */
    public function getParam(): ?string
    {
        return $this->param;
    }

    /**
     * Id del pedido (`req_…`). Inclúyelo si escribes a soporte por este error.
     */
    public function getRequestId(): ?string
    {
        return $this->requestId;
    }

    /**
     * Link a la explicación del código de error.
     */
    public function getDocUrl(): ?string
    {
        return $this->docUrl;
    }

    /**
     * Headers de la respuesta, con nombres en minúsculas.
     *
     * @return array<string, string>
     */
    public function getHttpHeaders(): array
    {
        return $this->httpHeaders;
    }

    /**
     * Cuerpo crudo de la respuesta, tal como llegó.
     */
    public function getHttpBody(): ?string
    {
        return $this->httpBody;
    }

    /**
     * Cuerpo de la respuesta decodificado, si era JSON.
     *
     * @return array<string, mixed>|null
     */
    public function getJsonBody(): ?array
    {
        return $this->jsonBody;
    }

    public function __toString(): string
    {
        $parts = [static::class];
        if ($this->httpStatus !== null) {
            $parts[] = '(HTTP ' . $this->httpStatus . ')';
        }
        if ($this->errorCode !== null) {
            $parts[] = '[' . $this->errorCode . ']';
        }
        $out = implode(' ', $parts) . ': ' . $this->getMessage();
        if ($this->requestId !== null) {
            $out .= ' (Request-Id: ' . $this->requestId . ')';
        }

        return $out;
    }
}
