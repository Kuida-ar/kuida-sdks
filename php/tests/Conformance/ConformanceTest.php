<?php

namespace Kuida\Tests\Conformance;

use Kuida\Collection;
use Kuida\Exception\ApiConnectionException;
use Kuida\Exception\ApiErrorException;
use Kuida\Exception\AuthenticationException;
use Kuida\Exception\IdempotencyException;
use Kuida\Exception\InvalidRequestException;
use Kuida\Exception\KuidaException;
use Kuida\Exception\PermissionException;
use Kuida\Exception\SignatureVerificationException;
use Kuida\KuidaClient;
use Kuida\KuidaObject;
use Kuida\Model\Account;
use Kuida\Model\Appointment;
use Kuida\Model\DeletedObject;
use Kuida\Model\EventBatchResponse;
use Kuida\Model\EventResult;
use Kuida\Model\IntakeRequest;
use Kuida\Model\Patient;
use Kuida\Model\Treatment;
use Kuida\Model\Visit;
use Kuida\Model\WebhookDelivery;
use Kuida\Model\WebhookEndpoint;
use Kuida\Model\WebhookEvent;
use Kuida\Webhook;

/**
 * Los 17 escenarios de conformidad de SDK_DESIGN.md §9.
 */
class ConformanceTest extends MockTestCase
{
    const PHONE = '+54 9 342 555 0000';

    /** 1. account_retrieve */
    public function testAccountRetrieve(): void
    {
        $account = $this->client()->account->retrieve();

        $this->assertInstanceOf(Account::class, $account);
        $this->assertSame('account', $account->object);
        $this->assertSame('account', $account['object']);
        $this->assertInstanceOf(KuidaObject::class, $account->apiKey);
        $this->assertContains('patients:write', $account->apiKey->scopes);
        $this->assertSame($account->apiKey->scopes, $account['apiKey']['scopes']);
        $this->assertNotNull($account->getLastResponse());
        $this->assertStringStartsWith('req_', (string) $account->getLastResponse()->getRequestId());
        $this->assertStringContainsString('"object":"account"', (string) $account->getRawJson());
    }

    /** 2. headers */
    public function testHeaders(): void
    {
        $kuida = $this->client();
        $kuida->account->retrieve();
        $kuida->patients->create(['phone' => self::PHONE, 'fullName' => 'Paciente Demo']);
        $kuida->patients->list(['limit' => 1]);

        $requests = $this->mockRequests();
        $this->assertCount(3, $requests);
        foreach ($requests as $request) {
            $h = $request['headers'];
            $this->assertSame('Bearer ' . self::KEY_ALL, $h['authorization']);
            $this->assertSame('2026-09-28', $h['kuida-version']);
            $this->assertSame('Kuida/v1 PhpBindings/' . KuidaClient::VERSION, $h['user-agent']);
            $this->assertMatchesRegularExpression('#^Kuida/v1 PhpBindings/\d+\.\d+\.\d+$#', $h['user-agent']);
            $ua = json_decode($h['x-kuida-client-user-agent'], true);
            $this->assertSame('php', $ua['lang']);
            $this->assertSame(KuidaClient::VERSION, $ua['bindings_version']);
            $this->assertSame(PHP_VERSION, $ua['lang_version']);
            $this->assertArrayHasKey('platform', $ua);
            if ($request['method'] === 'POST') {
                $this->assertMatchesRegularExpression(self::UUID_V4, $h['idempotency-key']);
                $this->assertSame('application/json', $h['content-type']);
            } else {
                $this->assertNull($h['idempotency-key']);
            }
        }
        $this->assertSame(['GET', 'POST', 'GET'], array_column($requests, 'method'));
    }

    /** 3. patients_crud */
    public function testPatientsCrud(): void
    {
        $kuida = $this->client();
        $created = $kuida->patients->create([
            'phone' => self::PHONE,
            'fullName' => 'Paciente Demo',
            'dni' => '11111111',
            'dateOfBirth' => new \DateTimeImmutable('1980-05-17T10:00:00-03:00'),
        ]);
        $this->assertInstanceOf(Patient::class, $created);
        $this->assertStringStartsWith('pat_', $created->id);
        $this->assertSame('1980-05-17', $created->dateOfBirth);

        $retrieved = $kuida->patients->retrieve($created->id);
        $this->assertSame($created->id, $retrieved->id);
        $this->assertSame('Paciente Demo', $retrieved->fullName);

        $updated = $kuida->patients->update($created->id, ['email' => 'paciente.demo@example.com']);
        $this->assertSame('paciente.demo@example.com', $updated->email);
        $this->assertSame('11111111', $updated->dni);

        $page = $kuida->patients->list(['phone' => self::PHONE]);
        $this->assertInstanceOf(Collection::class, $page);
        $this->assertCount(1, $page);
        $this->assertSame($created->id, $page->data[0]->id);
        $this->assertInstanceOf(Patient::class, $page->data[0]);
        $last = $this->mockRequests();
        $this->assertSame(['phone' => self::PHONE], end($last)['query']);
    }

    /** 4. idempotency_explicit */
    public function testIdempotencyExplicit(): void
    {
        $kuida = $this->client();
        $key = 'php-test-' . bin2hex(random_bytes(8));
        $params = ['phone' => self::PHONE, 'fullName' => 'Paciente Demo'];

        $first = $kuida->patients->create($params, ['idempotency_key' => $key]);
        $second = $kuida->patients->create($params, ['idempotency_key' => $key]);
        $this->assertSame($first->id, $second->id);
        $this->assertSame('true', $second->getLastResponse()->getHeader('Idempotent-Replayed'));
        foreach ($this->mockRequests() as $request) {
            $this->assertSame($key, $request['headers']['idempotency-key']);
        }

        try {
            $kuida->patients->create(['phone' => '+54 9 342 555 0001'], ['idempotency_key' => $key]);
            $this->fail('Se esperaba IdempotencyException');
        } catch (IdempotencyException $e) {
            $this->assertSame('idempotency_error', $e->getErrorType());
            $this->assertSame('idempotency_key_reused', $e->getErrorCode());
            $this->assertSame(400, $e->getHttpStatus());
        }
    }

    /** 5. pagination_manual */
    public function testPaginationManual(): void
    {
        $kuida = $this->client();
        $ids = $this->createPatients($kuida, 5);

        $page1 = $kuida->patients->list(['limit' => 2]);
        $this->assertCount(2, $page1->data);
        $this->assertTrue($page1->hasMore);

        $page2 = $kuida->patients->list(['limit' => 2, 'startingAfter' => $page1->data[1]->id]);
        $this->assertCount(2, $page2->data);
        $this->assertTrue($page2->hasMore);

        $page3 = $page2->nextPage();
        $this->assertCount(1, $page3->data);
        $this->assertFalse($page3->hasMore);

        $seen = [];
        foreach ([$page1, $page2, $page3] as $page) {
            foreach ($page as $patient) {
                $seen[] = $patient->id;
            }
        }
        $this->assertCount(5, array_unique($seen));
        $this->assertEqualsCanonicalizing($ids, $seen);
    }

    /** 6. pagination_auto */
    public function testPaginationAuto(): void
    {
        $kuida = $this->client();
        $ids = $this->createPatients($kuida, 5);
        $this->resetLogOnly();

        $seen = [];
        foreach ($kuida->patients->list(['limit' => 2])->autoPagingIterator() as $patient) {
            $this->assertInstanceOf(Patient::class, $patient);
            $seen[] = $patient->id;
        }
        $this->assertCount(5, $seen);
        $this->assertCount(5, array_unique($seen));
        $this->assertEqualsCanonicalizing($ids, $seen);

        $lists = array_values(array_filter($this->mockRequests(), function ($r) {
            return $r['method'] === 'GET' && $r['path'] === '/v1/patients';
        }));
        $this->assertCount(3, $lists);
        foreach ($lists as $i => $request) {
            $this->assertSame('2', $request['query']['limit']);
            if ($i > 0) {
                $this->assertSame($seen[$i * 2 - 1], $request['query']['startingAfter']);
            }
        }
    }

    /** 7. appointments_flow */
    public function testAppointmentsFlow(): void
    {
        $kuida = $this->client();
        $appointment = $kuida->appointments->create([
            'patient' => ['phone' => self::PHONE, 'fullName' => 'Paciente Demo', 'dni' => '11111111'],
            'doctor' => ['externalId' => 'prof-001', 'fullName' => 'Dra. Demo', 'specialty' => 'Clínica médica'],
            'startAt' => new \DateTimeImmutable('2026-10-05T09:30:00-03:00'),
            'type' => 'Control',
            'externalId' => 'turno-demo-1',
        ]);
        $this->assertInstanceOf(Appointment::class, $appointment);
        $this->assertStringStartsWith('pat_', $appointment->patient);
        $this->assertStringStartsWith('doc_', (string) $appointment->doctor);
        $this->assertSame('2026-10-05T12:30:00.000Z', $appointment->startAt);
        $this->assertSame(Appointment::STATUS_CONFIRMED, $appointment->status);

        $updated = $kuida->appointments->update($appointment->id, ['startAt' => '2026-10-06T10:00:00-03:00']);
        $this->assertSame('2026-10-06T13:00:00.000Z', $updated->startAt);

        $cancelled = $kuida->appointments->cancel($appointment->id);
        $this->assertSame('cancelled', $cancelled->status);
        $this->assertNotNull($cancelled->cancelledAt);
        $requests = $this->mockRequests();
        $cancelRequest = end($requests);
        $this->assertSame('/v1/appointments/' . $appointment->id . '/cancel', $cancelRequest['path']);
        $this->assertSame([], $cancelRequest['body']);

        try {
            $kuida->appointments->update($appointment->id, ['type' => 'Otro']);
            $this->fail('Se esperaba InvalidRequestException');
        } catch (InvalidRequestException $e) {
            $this->assertSame(400, $e->getHttpStatus());
            $this->assertSame('invalid_request_error', $e->getErrorType());
        }
    }

    /** 8. visit_closes_appointment */
    public function testVisitClosesAppointment(): void
    {
        $kuida = $this->client();
        $appointment = $kuida->appointments->create([
            'patient' => ['phone' => self::PHONE, 'fullName' => 'Paciente Demo'],
            'startAt' => '2026-10-05T09:30:00-03:00',
            'externalId' => 'turno-demo-2',
        ]);

        $visit = $kuida->visits->create([
            'patient' => ['phone' => self::PHONE],
            'visitedAt' => new \DateTime('2026-10-05T09:45:00-03:00'),
            'appointment' => 'turno-demo-2',
            'externalId' => 'consulta-demo-2',
            'type' => 'Control',
        ]);
        $this->assertInstanceOf(Visit::class, $visit);
        $this->assertStringStartsWith('vis_', $visit->id);
        $this->assertSame($appointment->id, $visit->appointment);
        $this->assertSame($appointment->patient, $visit->patient);

        $this->assertSame('completed', $kuida->appointments->retrieve($appointment->id)->status);
        $this->assertSame($visit->id, $kuida->visits->retrieve($visit->id)->id);
        $this->assertCount(1, $kuida->visits->list(['patient' => $visit->patient]));
    }

    /** 9. intake_and_treatment */
    public function testIntakeAndTreatment(): void
    {
        $kuida = $this->client();
        $intake = $kuida->intakeRequests->create([
            'patient' => ['phone' => self::PHONE, 'fullName' => 'Paciente Demo', 'dni' => '11111111'],
            'contacts' => [['name' => 'Referente Demo', 'relationship' => 'hija', 'phone' => '+54 9 342 555 0001']],
            'coverage' => ['insurer' => 'Obra Social Demo', 'memberId' => '0000001'],
            'requestedService' => 'Internación domiciliaria',
            'summary' => 'Pedido de prueba',
            'hospitalized' => false,
        ]);
        $this->assertInstanceOf(IntakeRequest::class, $intake);
        $this->assertSame('intake_request', $intake->object);
        $this->assertStringStartsWith('pat_', (string) $intake->patient);
        $this->assertSame('Obra Social Demo', $intake->data->coverage->insurer);
        $this->assertSame($intake->id, $kuida->intakeRequests->retrieve($intake->id)->id);
        $this->assertCount(1, $kuida->intakeRequests->list(['status' => 'new']));

        $treatment = $kuida->treatments->create([
            'patient' => $intake->patient,
            'name' => 'Ibuprofeno 400 mg',
            'dosage' => '1 comprimido',
            'frequency' => 'cada 8 horas',
            'startDate' => new \DateTimeImmutable('2026-10-05T08:00:00-03:00'),
        ]);
        $this->assertInstanceOf(Treatment::class, $treatment);
        $this->assertSame($intake->patient, $treatment->patient);
        $this->assertSame(Treatment::KIND_MEDICATION, $treatment->kind);
        $this->assertTrue($treatment->active);
        $this->assertCount(1, $kuida->treatments->list(['patient' => $intake->patient, 'active' => true]));
        $requests = $this->mockRequests();
        $this->assertSame('true', end($requests)['query']['active']);
    }

    /** 10. events_batch */
    public function testEventsBatch(): void
    {
        $kuida = $this->client();
        $valid = [
            'id' => 'paciente-demo-1-alta',
            'type' => 'patient.upserted',
            'occurredAt' => new \DateTimeImmutable('2026-09-28T12:00:00Z'),
            'source' => ['system' => 'SistemaDemo', 'version' => '1.0'],
            'data' => ['patient' => ['phone' => self::PHONE, 'fullName' => 'Paciente Demo']],
        ];
        $invalid = ['id' => 'evento-invalido-1', 'type' => 'no.existe', 'data' => ['x' => 1]];

        $batch = $kuida->events->createBatch([$valid, $invalid]);
        $this->assertInstanceOf(EventBatchResponse::class, $batch);
        $this->assertCount(2, $batch->results);
        $this->assertInstanceOf(EventResult::class, $batch->results[0]);
        $this->assertSame('processed', $batch->results[0]->status);
        $this->assertTrue($batch->results[0]->accepted);
        $this->assertStringStartsWith('evt_', (string) $batch->results[0]->event);
        $this->assertSame('invalid', $batch->results[1]->status);
        $this->assertFalse($batch->results[1]->accepted);

        $again = $kuida->events->create($valid);
        $this->assertSame('duplicate', $again->results[0]->status);
        $this->assertSame($batch->results[0]->event, $again->results[0]->event);

        $event = $kuida->events->retrieve((string) $batch->results[0]->event);
        $this->assertSame('patient.upserted', $event->type);
        $this->assertSame('Paciente Demo', $event->data->patient->fullName);
        $this->assertCount(1, $kuida->events->list(['type' => 'patient.upserted']));

        try {
            $kuida->events->createBatch([$invalid]);
            $this->fail('Se esperaba InvalidRequestException');
        } catch (InvalidRequestException $e) {
            $body = $e->getJsonBody();
            $this->assertSame('event_batch', $body['object']);
            $this->assertSame('invalid', $body['results'][0]['status']);
        }
    }

    /** 11. webhook_endpoints_flow */
    public function testWebhookEndpointsFlow(): void
    {
        $kuida = $this->client();
        $endpoint = $kuida->webhookEndpoints->create([
            'url' => 'https://sistema-demo.example/webhooks/kuida',
            'enabledEvents' => ['intake.ready', 'visit.completed'],
            'description' => 'Sistema de gestión demo',
        ]);
        $this->assertInstanceOf(WebhookEndpoint::class, $endpoint);
        $this->assertStringStartsWith('whsec_', (string) $endpoint->secret);

        $retrieved = $kuida->webhookEndpoints->retrieve($endpoint->id);
        $this->assertNull($retrieved->secret);
        $this->assertFalse(isset($retrieved['secret']));

        $updated = $kuida->webhookEndpoints->update($endpoint->id, ['enabledEvents' => ['*'], 'description' => 'Todos']);
        $this->assertSame(['*'], $updated->enabledEvents);
        $this->assertSame('Todos', $updated->description);
        $this->assertCount(1, $kuida->webhookEndpoints->list());

        $delivery = $kuida->webhookEndpoints->ping($endpoint->id);
        $this->assertInstanceOf(WebhookDelivery::class, $delivery);
        $this->assertSame('pending', $delivery->status);
        $this->assertSame('webhook.ping', $delivery->eventType);
        $requests = $this->mockRequests();
        $ping = end($requests);
        $this->assertSame([], $ping['body']);
        $this->assertMatchesRegularExpression(self::UUID_V4, $ping['headers']['idempotency-key']);

        $deliveries = $kuida->webhookDeliveries->list(['webhookEndpoint' => $endpoint->id]);
        $this->assertCount(1, $deliveries);
        $this->assertSame($delivery->id, $deliveries->data[0]->id);
        $this->assertSame($delivery->id, $kuida->webhookDeliveries->retrieve($delivery->id)->id);

        $deleted = $kuida->webhookEndpoints->delete($endpoint->id);
        $this->assertInstanceOf(DeletedObject::class, $deleted);
        $this->assertTrue($deleted->deleted);
        $this->assertSame($endpoint->id, $deleted->id);
    }

    /** 12. errors */
    public function testErrors(): void
    {
        $previous = getenv('KUIDA_API_KEY');
        putenv('KUIDA_API_KEY');
        try {
            new KuidaClient(['base_url' => self::apiBase()]);
            $this->fail('Se esperaba AuthenticationException sin clave');
        } catch (AuthenticationException $e) {
            $this->assertSame('api_key_missing', $e->getErrorCode());
        } finally {
            if (is_string($previous)) {
                putenv('KUIDA_API_KEY=' . $previous);
            }
        }

        $this->assertApiError(AuthenticationException::class, 401, 'api_key_invalid', function () {
            $this->client('kd_test_000000000000_clavequenoexistenoexiste')->account->retrieve();
        });
        $this->assertApiError(AuthenticationException::class, 401, 'api_key_revoked', function () {
            $this->client(self::KEY_REVOKED)->account->retrieve();
        });
        $this->assertApiError(PermissionException::class, 403, 'scope_missing', function () {
            $this->client(self::KEY_EVENTS_ONLY)->patients->list();
        });

        $e = $this->assertApiError(InvalidRequestException::class, 404, 'resource_missing', function () {
            $this->client()->patients->retrieve('pat_noexiste');
        });
        $this->assertStringStartsWith('req_', (string) $e->getRequestId());
        $this->assertSame($e->getRequestId(), $e->getHttpHeaders()['request-id']);
        $this->assertSame('https://www.kuida.ar/desarrolladores/errores#resource_missing', $e->getDocUrl());

        $e = $this->assertApiError(InvalidRequestException::class, 400, 'parameter_missing', function () {
            $this->client()->patients->create(['fullName' => 'Paciente Demo']);
        });
        $this->assertSame('phone', $e->getParam());
    }

    /** 13. retries_5xx */
    public function testRetries5xx(): void
    {
        $patient = $this->client(self::KEY_FLAKY_503)->patients->create(['phone' => self::PHONE, 'fullName' => 'Paciente Demo']);
        $this->assertStringStartsWith('pat_', $patient->id);

        $posts = array_values(array_filter($this->mockRequests(), function ($r) {
            return $r['method'] === 'POST';
        }));
        $this->assertCount(2, $posts);
        $this->assertMatchesRegularExpression(self::UUID_V4, $posts[0]['headers']['idempotency-key']);
        $this->assertSame($posts[0]['headers']['idempotency-key'], $posts[1]['headers']['idempotency-key']);
        $this->assertCount(1, $this->sleeps);
        $this->assertGreaterThanOrEqual(0.375, $this->sleeps[0]);
        $this->assertLessThanOrEqual(0.625, $this->sleeps[0]);
    }

    /** 14. retries_429 */
    public function testRetries429(): void
    {
        $account = $this->client(self::KEY_FLAKY_429)->account->retrieve();
        $this->assertSame('account', $account->object);
        $this->assertCount(2, $this->mockRequests());
        $this->assertSame([1.0], $this->sleeps);
    }

    /** 15. no_retry */
    public function testNoRetry(): void
    {
        $this->assertApiError(ApiErrorException::class, 503, 'internal_error', function () {
            $this->client(self::KEY_FLAKY_503, ['max_retries' => 0])->patients->create(['phone' => self::PHONE]);
        });
        $this->assertCount(1, $this->mockRequests());

        // También como opción por pedido.
        $this->resetMock();
        $this->assertApiError(ApiErrorException::class, 503, 'internal_error', function () {
            $this->client(self::KEY_FLAKY_503)->patients->create(['phone' => self::PHONE], ['max_retries' => 0]);
        });
        $this->assertCount(1, $this->mockRequests());
        $this->assertSame([], $this->sleeps);
    }

    /** 16. connection_error */
    public function testConnectionError(): void
    {
        try {
            $this->client(self::KEY_ALL, ['base_url' => 'http://127.0.0.1:1/api', 'max_retries' => 0])->account->retrieve();
            $this->fail('Se esperaba ApiConnectionException');
        } catch (ApiConnectionException $e) {
            $this->assertNull($e->getHttpStatus());
            $this->assertInstanceOf(KuidaException::class, $e);
        }
        $this->assertSame([], $this->sleeps);

        try {
            $this->client(self::KEY_ALL, ['base_url' => 'http://127.0.0.1:1/api'])->account->retrieve();
            $this->fail('Se esperaba ApiConnectionException');
        } catch (ApiConnectionException $e) {
            $this->assertCount(2, $this->sleeps);
        }
    }

    /** 17. webhook_signature */
    public function testWebhookSignature(): void
    {
        $path = dirname(__DIR__, 3) . '/conformance/webhook-vectors.json';
        $v = json_decode((string) file_get_contents($path), true);
        $this->assertIsArray($v);
        $now = (int) $v['timestamp'];

        $this->assertTrue(Webhook::verifySignature($v['payload'], $v['signatureHeader'], $v['timestamp'], $v['secret'], 300, $now));
        $this->assertTrue(Webhook::verifySignature($v['payload'], $v['signatureHeader'], $v['timestamp'], $v['secret'], 0));
        $this->assertSame($v['signatureHeader'], Webhook::computeSignature($v['timestamp'], $v['payload'], $v['secret']));

        $event = Webhook::constructEvent($v['payload'], $v['signatureHeader'], $v['timestamp'], $v['secret'], 300, $now);
        $this->assertInstanceOf(WebhookEvent::class, $event);
        $this->assertSame('event', $event->object);
        $this->assertSame(WebhookEvent::TYPE_INTAKE_READY, $event->type);
        $this->assertSame('Paciente Demo', $event->data->patient->fullName);
        $this->assertSame('LISTA', $event['data']['intake']['status']);

        $rejects = [
            'secreto equivocado' => [$v['payload'], $v['signatureHeader'], $v['timestamp'], $v['wrongSecret'], 300, $now],
            'cuerpo alterado' => [$v['tamperedPayload'], $v['signatureHeader'], $v['timestamp'], $v['secret'], 300, $now],
            'fuera de tolerancia' => [$v['payload'], $v['signatureHeader'], $v['timestamp'], $v['secret'], 300, null],
            'sin firma' => [$v['payload'], null, $v['timestamp'], $v['secret'], 0, null],
            'sin timestamp' => [$v['payload'], $v['signatureHeader'], null, $v['secret'], 0, null],
        ];
        foreach ($rejects as $case => $args) {
            try {
                Webhook::constructEvent($args[0], $args[1], $args[2], $args[3], $args[4], $args[5]);
                $this->fail('Se esperaba SignatureVerificationException: ' . $case);
            } catch (SignatureVerificationException $e) {
                $this->assertInstanceOf(KuidaException::class, $e);
            }
        }

        // Un cuerpo firmado acá con la hora actual verifica con la tolerancia default.
        $payload = '{"id":"whd_demo","object":"event","type":"webhook.ping","apiVersion":"2026-09-28",'
            . '"livemode":false,"occurredAt":"2026-09-28T12:00:00.000Z","ref":"ping_demo","data":{"message":"hola"}}';
        $timestamp = (string) time();
        $signature = 'sha256=' . hash_hmac('sha256', $timestamp . '.' . $payload, $v['secret']);
        $event = Webhook::constructEvent($payload, $signature, $timestamp, $v['secret']);
        $this->assertSame('webhook.ping', $event->type);
        $this->assertSame('hola', $event->data->message);
    }

    /**
     * @param class-string<KuidaException> $class
     */
    private function assertApiError(string $class, int $status, string $code, callable $fn): KuidaException
    {
        try {
            $fn();
        } catch (KuidaException $e) {
            $this->assertInstanceOf($class, $e, (string) $e);
            $this->assertSame($status, $e->getHttpStatus());
            $this->assertSame($code, $e->getErrorCode());
            $this->assertNotSame('', $e->getMessage());

            return $e;
        }
        $this->fail('Se esperaba ' . $class);
    }

    /**
     * @return string[]
     */
    private function createPatients(KuidaClient $kuida, int $count): array
    {
        $ids = [];
        for ($i = 1; $i <= $count; $i++) {
            $ids[] = $kuida->patients->create([
                'phone' => sprintf('+54 9 342 555 %04d', $i),
                'fullName' => 'Paciente Demo ' . $i,
            ])->id;
        }

        return $ids;
    }

    /**
     * Vacía el registro de pedidos del mock sin perder los datos: no hay
     * endpoint para eso, así que se filtra por posición.
     */
    private function resetLogOnly(): void
    {
        $this->logOffset = count($this->mockRequests());
    }

    /** @var int */
    private $logOffset = 0;

    protected function mockRequests(): array
    {
        return array_slice(parent::mockRequests(), $this->logOffset);
    }
}
