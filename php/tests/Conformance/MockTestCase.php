<?php

namespace Kuida\Tests\Conformance;

use Kuida\KuidaClient;
use PHPUnit\Framework\TestCase;

/**
 * Base de la suite de conformidad (SDK_DESIGN.md §9): habla con el mock de
 * conformance/mock-server.mjs en KUIDA_API_BASE.
 */
abstract class MockTestCase extends TestCase
{
    const KEY_ALL = 'kd_test_000000000000_mocksecretmocksecret00';
    const KEY_EVENTS_ONLY = 'kd_test_1e9ac7000000_mocksecretmocksecret00';
    const KEY_REVOKED = 'kd_test_dead00000000_mocksecretmocksecret00';
    const KEY_FLAKY_503 = 'kd_test_fa11ed000000_mocksecretmocksecret00';
    const KEY_FLAKY_429 = 'kd_test_42900000000a_mocksecretmocksecret00';

    const UUID_V4 = '/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/';

    /** @var float[] Esperas pedidas por el SDK entre reintentos. */
    protected $sleeps = [];

    protected static function apiBase(): string
    {
        $base = getenv('KUIDA_API_BASE');

        return is_string($base) && $base !== '' ? rtrim($base, '/') : 'http://localhost:12111/api';
    }

    protected static function mockRoot(): string
    {
        return (string) preg_replace('#/api$#', '', self::apiBase());
    }

    protected function setUp(): void
    {
        $this->sleeps = [];
        $this->resetMock();
    }

    /**
     * Cliente contra el mock, con espera nula que registra cada espera pedida.
     *
     * @param array<string, mixed> $options
     */
    protected function client(string $apiKey = self::KEY_ALL, array $options = []): KuidaClient
    {
        $sleeps = &$this->sleeps;

        return new KuidaClient($options + [
            'api_key' => $apiKey,
            'base_url' => self::apiBase(),
            'sleeper' => static function (float $seconds) use (&$sleeps): void {
                $sleeps[] = $seconds;
            },
        ]);
    }

    protected function resetMock(): void
    {
        $this->mockCall('POST', '/__mock/reset');
    }

    /**
     * Pedidos que recibió el mock desde el último reset.
     *
     * @return array<int, array<string, mixed>>
     */
    protected function mockRequests(): array
    {
        $out = $this->mockCall('GET', '/__mock/requests');

        return $out['requests'];
    }

    /**
     * @return array<string, mixed>
     */
    private function mockCall(string $method, string $path): array
    {
        $ch = curl_init(self::mockRoot() . $path);
        curl_setopt_array($ch, [
            CURLOPT_CUSTOMREQUEST => $method,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 5,
        ]);
        $body = curl_exec($ch);
        if (!is_string($body)) {
            $this->fail(
                'No responde el mock en ' . self::mockRoot() . '. Levántalo con '
                . '`PORT=12125 node ../conformance/mock-server.mjs` y corre con KUIDA_API_BASE=http://localhost:12125/api.'
            );
        }
        $json = json_decode($body, true);
        $this->assertIsArray($json);

        return $json;
    }
}
