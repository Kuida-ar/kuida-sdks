<?php

namespace Kuida;

use Kuida\Exception\AuthenticationException;
use Kuida\Exception\InvalidArgumentException;
use Kuida\HttpClient\ClientInterface;
use Kuida\HttpClient\CurlClient;
use Kuida\Model\ObjectTypes;

/**
 * Cliente de la API de Kuida.
 *
 * ```php
 * $kuida = new \Kuida\KuidaClient('kd_live_…');
 * $patient = $kuida->patients->create(['phone' => '+54 9 342 555 0000', 'fullName' => 'Paciente Demo']);
 * ```
 *
 * Un cliente por proceso alcanza: es reutilizable entre pedidos.
 *
 * @property-read Service\AccountService $account Tu organización y la clave en uso.
 * @property-read Service\PatientService $patients Pacientes.
 * @property-read Service\DoctorService $doctors Profesionales.
 * @property-read Service\AppointmentService $appointments Turnos.
 * @property-read Service\VisitService $visits Consultas atendidas.
 * @property-read Service\IntakeRequestService $intakeRequests Solicitudes de ingreso.
 * @property-read Service\TreatmentService $treatments Tratamientos e indicaciones.
 * @property-read Service\EventService $events Eventos genéricos de tu sistema.
 * @property-read Service\WebhookEndpointService $webhookEndpoints Endpoints de webhooks.
 * @property-read Service\WebhookDeliveryService $webhookDeliveries Entregas de webhooks.
 */
class KuidaClient
{
    /** Versión de este SDK. */
    const VERSION = '0.1.0';

    /** Versión de la API que usa este SDK por defecto (header `Kuida-Version`). */
    const API_VERSION = ObjectTypes::API_VERSION;

    const DEFAULT_BASE_URL = 'https://www.kuida.ar/api';

    const DEFAULT_TIMEOUT = 30;

    const DEFAULT_MAX_RETRIES = 2;

    const OPTIONS = ['api_key', 'base_url', 'timeout', 'max_retries', 'api_version', 'http_client', 'sleeper'];

    const SERVICES = [
        'account' => Service\AccountService::class,
        'patients' => Service\PatientService::class,
        'doctors' => Service\DoctorService::class,
        'appointments' => Service\AppointmentService::class,
        'visits' => Service\VisitService::class,
        'intakeRequests' => Service\IntakeRequestService::class,
        'treatments' => Service\TreatmentService::class,
        'events' => Service\EventService::class,
        'webhookEndpoints' => Service\WebhookEndpointService::class,
        'webhookDeliveries' => Service\WebhookDeliveryService::class,
    ];

    /** @var string */
    private $apiKey;

    /** @var string */
    private $baseUrl;

    /** @var float */
    private $timeout;

    /** @var int */
    private $maxRetries;

    /** @var string */
    private $apiVersion;

    /** @var ApiRequestor */
    private $requestor;

    /** @var array<string, Service\AbstractService> */
    private $services = [];

    /**
     * Crea el cliente.
     *
     * Se puede pasar solo la clave (`new KuidaClient('kd_live_…')`) o un array de opciones:
     *
     * - `api_key` (string): clave de API. Si falta, se lee `KUIDA_API_KEY` del entorno.
     * - `base_url` (string): default `https://www.kuida.ar/api`; también se lee `KUIDA_API_BASE`.
     * - `timeout` (int|float): segundos por intento. Default 30.
     * - `max_retries` (int): reintentos ante errores transitorios. Default 2 (3 intentos).
     * - `api_version` (string): versión fechada de la API. Default la del SDK.
     * - `http_client` ({@see ClientInterface}): transporte HTTP propio. Default cURL.
     * - `sleeper` (callable(float): void): cómo esperar entre reintentos. Default `usleep`.
     *
     * @param string|array<string, mixed>|null $config Clave de API o array de opciones.
     * @throws AuthenticationException Si no hay clave de API.
     * @throws InvalidArgumentException Si una opción es desconocida o inválida.
     */
    public function __construct($config = null)
    {
        if ($config === null) {
            $config = [];
        } elseif (is_string($config)) {
            $config = ['api_key' => $config];
        } elseif (!is_array($config)) {
            throw new InvalidArgumentException('El cliente se construye con la clave de API (string) o con un array de opciones.');
        }
        foreach (array_keys($config) as $name) {
            if (!in_array($name, self::OPTIONS, true)) {
                throw new InvalidArgumentException(
                    'Opción desconocida: "' . $name . '". Las válidas son: ' . implode(', ', self::OPTIONS) . '.'
                );
            }
        }

        $apiKey = isset($config['api_key']) ? $config['api_key'] : self::env('KUIDA_API_KEY');
        if (!is_string($apiKey) || trim($apiKey) === '') {
            throw new AuthenticationException(
                'Falta la clave de API de Kuida. Pásala al construir el cliente (new KuidaClient(\'kd_live_…\')) '
                . 'o define la variable de entorno KUIDA_API_KEY.',
                null,
                null,
                null,
                [],
                'api_key_missing'
            );
        }
        if (preg_match('/\s/', $apiKey) === 1) {
            throw new AuthenticationException('La clave de API no puede tener espacios.', null, null, null, [], 'api_key_invalid');
        }
        $this->apiKey = $apiKey;

        $baseUrl = isset($config['base_url']) ? $config['base_url'] : self::env('KUIDA_API_BASE');
        if ($baseUrl === null || $baseUrl === '') {
            $baseUrl = self::DEFAULT_BASE_URL;
        }
        if (!is_string($baseUrl) || !preg_match('#^https?://#i', $baseUrl)) {
            throw new InvalidArgumentException('base_url tiene que ser una URL http(s).');
        }
        $this->baseUrl = rtrim($baseUrl, '/');

        $timeout = isset($config['timeout']) ? $config['timeout'] : self::DEFAULT_TIMEOUT;
        if (!is_numeric($timeout) || (float) $timeout <= 0) {
            throw new InvalidArgumentException('timeout tiene que ser un número de segundos mayor que 0.');
        }
        $this->timeout = (float) $timeout;

        $maxRetries = isset($config['max_retries']) ? $config['max_retries'] : self::DEFAULT_MAX_RETRIES;
        if (!is_int($maxRetries) || $maxRetries < 0) {
            throw new InvalidArgumentException('max_retries tiene que ser un entero mayor o igual que 0.');
        }
        $this->maxRetries = $maxRetries;

        $apiVersion = isset($config['api_version']) ? $config['api_version'] : self::API_VERSION;
        if (!is_string($apiVersion) || $apiVersion === '') {
            throw new InvalidArgumentException('api_version tiene que ser una fecha como "' . self::API_VERSION . '".');
        }
        $this->apiVersion = $apiVersion;

        $http = isset($config['http_client']) ? $config['http_client'] : new CurlClient();
        if (!$http instanceof ClientInterface) {
            throw new InvalidArgumentException('http_client tiene que implementar Kuida\HttpClient\ClientInterface.');
        }
        $sleeper = isset($config['sleeper']) ? $config['sleeper'] : static function (float $seconds): void {
            if ($seconds > 0) {
                usleep((int) round($seconds * 1000000));
            }
        };
        if (!is_callable($sleeper)) {
            throw new InvalidArgumentException('sleeper tiene que ser invocable: function (float $segundos): void.');
        }
        $this->requestor = new ApiRequestor($this, $http, $sleeper);
    }

    /**
     * Devuelve el servicio de un recurso (`$kuida->patients`, `$kuida->intakeRequests`, …).
     *
     * @return Service\AbstractService
     */
    public function __get(string $name)
    {
        if (!isset($this->services[$name])) {
            if (!array_key_exists($name, self::SERVICES)) {
                throw new InvalidArgumentException(
                    'El cliente no tiene el recurso "' . $name . '". Los recursos son: ' . implode(', ', array_keys(self::SERVICES)) . '.'
                );
            }
            $class = self::SERVICES[$name];
            $this->services[$name] = new $class($this);
        }

        return $this->services[$name];
    }

    public function __isset(string $name): bool
    {
        return array_key_exists($name, self::SERVICES);
    }

    /**
     * Manda un pedido a la API. Lo usan los servicios; sirve también para
     * endpoints que este SDK todavía no cubre.
     *
     * @param string $method GET, POST, PATCH o DELETE.
     * @param string $path Ruta desde la base, por ejemplo `/v1/patients`.
     * @param array<mixed>|null $params Query (GET/DELETE) o cuerpo JSON (POST/PATCH).
     * @param array<string, mixed> $opts `idempotency_key`, `timeout`, `max_retries`.
     * @throws Exception\KuidaException
     */
    public function request(string $method, string $path, ?array $params = null, array $opts = []): ApiResponse
    {
        return $this->requestor->request(strtoupper($method), $path, $params, $opts);
    }

    /** La clave de API en uso. */
    public function getApiKey(): string
    {
        return $this->apiKey;
    }

    /** URL base de la API, sin barra final. */
    public function getBaseUrl(): string
    {
        return $this->baseUrl;
    }

    /** Tiempo máximo por intento, en segundos. */
    public function getTimeout(): float
    {
        return $this->timeout;
    }

    /** Reintentos ante errores transitorios. */
    public function getMaxRetries(): int
    {
        return $this->maxRetries;
    }

    /** Versión de la API que se manda en `Kuida-Version`. */
    public function getApiVersion(): string
    {
        return $this->apiVersion;
    }

    private static function env(string $name): ?string
    {
        $value = getenv($name);
        if (is_string($value) && $value !== '') {
            return $value;
        }
        foreach ([$_ENV, $_SERVER] as $source) {
            if (isset($source[$name]) && is_string($source[$name]) && $source[$name] !== '') {
                return $source[$name];
            }
        }

        return null;
    }
}
