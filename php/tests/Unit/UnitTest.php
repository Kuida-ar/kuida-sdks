<?php

namespace Kuida\Tests\Unit;

use Kuida\Collection;
use Kuida\Exception\ExceptionInterface;
use Kuida\Exception\InvalidArgumentException;
use Kuida\Exception\KuidaException;
use Kuida\Exception\RateLimitException;
use Kuida\KuidaClient;
use Kuida\KuidaObject;
use Kuida\Model\EventBatchResponse;
use Kuida\Model\EventResult;
use Kuida\Model\Patient;
use Kuida\Util;
use PHPUnit\Framework\TestCase;

/**
 * Pruebas sin red: objetos, serialización, errores y opciones.
 */
class UnitTest extends TestCase
{
    public function testObjectAccessAndConversion(): void
    {
        $json = '{"id":"pat_1","object":"patient","fullName":"Paciente Demo","campoNuevo":{"a":1},"tags":[]}';
        $patient = Patient::constructFrom(json_decode($json));

        $this->assertSame('Paciente Demo', $patient->fullName);
        $this->assertSame('Paciente Demo', $patient['fullName']);
        $this->assertNull($patient->email);
        $this->assertFalse(isset($patient->email));
        $this->assertInstanceOf(KuidaObject::class, $patient->campoNuevo);
        $this->assertSame(1, $patient->campoNuevo->a);
        $this->assertSame(
            ['id' => 'pat_1', 'object' => 'patient', 'fullName' => 'Paciente Demo', 'campoNuevo' => ['a' => 1], 'tags' => []],
            $patient->toArray()
        );
        $this->assertSame($patient->toArray(), json_decode($patient->toJson(), true));

        $this->expectException(InvalidArgumentException::class);
        $patient['fullName'] = 'Otro';
    }

    public function testNestedTypesAndObjectMapping(): void
    {
        $batch = EventBatchResponse::constructFrom(json_decode(
            '{"object":"event_batch","results":[{"id":"a","accepted":true,"status":"processed","event":"evt_1","result":{"object":"visit","id":"vis_1"},"error":null}]}'
        ));
        $this->assertInstanceOf(EventResult::class, $batch->results[0]);

        $value = KuidaObject::convertValue(json_decode('{"object":"list","data":[{"id":"pat_1","object":"patient"}],"hasMore":false,"url":"/v1/patients"}'));
        $this->assertInstanceOf(Collection::class, $value);
        $this->assertInstanceOf(Patient::class, $value->data[0]);
    }

    public function testEncoding(): void
    {
        $date = new \DateTimeImmutable('2026-09-28T09:30:00-03:00');
        $this->assertSame(
            '{"startAt":"2026-09-28T09:30:00-03:00","patient":{"phone":"+54 9 342 555 0000","dateOfBirth":"2026-09-28"},"hospitalized":false}',
            Util::encodeBody([
                'startAt' => $date,
                'patient' => ['phone' => '+54 9 342 555 0000', 'dateOfBirth' => $date],
                'hospitalized' => false,
            ])
        );
        $this->assertSame('{}', Util::encodeBody([]));
        $this->assertSame('{}', Util::encodeBody(null));
        $this->assertSame(
            'limit=2&active=true&phone=%2B54%209%20342%20555%200000&startAtGte=2026-09-28T09%3A30%3A00-03%3A00',
            Util::encodeQuery(['limit' => 2, 'active' => true, 'phone' => '+54 9 342 555 0000', 'startAtGte' => $date, 'dni' => null])
        );
        $this->assertMatchesRegularExpression('/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/', Util::uuidV4());
    }

    public function testErrorMapping(): void
    {
        $body = '{"error":{"type":"rate_limit_error","code":"rate_limited","message":"Despacio","param":null,"requestId":"req_1","docUrl":"https://x"}}';
        $e = KuidaException::fromResponse(429, $body, ['request-id' => 'req_1']);
        $this->assertInstanceOf(RateLimitException::class, $e);
        $this->assertSame('rate_limited', $e->getErrorCode());
        $this->assertSame('req_1', $e->getRequestId());

        $e = KuidaException::fromResponse(502, '<html>Bad gateway</html>', ['request-id' => 'req_2']);
        $this->assertInstanceOf(\Kuida\Exception\ApiErrorException::class, $e);
        $this->assertSame('<html>Bad gateway</html>', $e->getMessage());
        $this->assertSame('api_error', $e->getErrorType());
        $this->assertSame('req_2', $e->getRequestId());
    }

    public function testClientOptions(): void
    {
        $client = new KuidaClient('kd_test_000000000000_mocksecretmocksecret00');
        $this->assertSame(2, $client->getMaxRetries());
        $this->assertSame(30.0, $client->getTimeout());
        $this->assertSame('2026-09-28', $client->getApiVersion());
        $this->assertInstanceOf(\Kuida\Service\IntakeRequestService::class, $client->intakeRequests);
        $this->assertSame($client->patients, $client->patients);

        try {
            new KuidaClient(['api_key' => 'kd_test_x', 'maxRetries' => 1]);
            $this->fail('Se esperaba InvalidArgumentException');
        } catch (InvalidArgumentException $e) {
            $this->assertInstanceOf(ExceptionInterface::class, $e);
        }

        $this->expectException(InvalidArgumentException::class);
        $client->patients->retrieve('');
    }
}
