<?php

namespace Kuida;

use Kuida\Exception\SignatureVerificationException;
use Kuida\Exception\UnexpectedValueException;
use Kuida\Model\WebhookEvent;

/**
 * Verificación de los webhooks que manda Kuida. No necesita cliente ni clave de API.
 *
 * Kuida firma cada envío con HMAC-SHA256 sobre `"{timestamp}.{cuerpo}"` usando el
 * secreto del endpoint (`whsec_…`) y manda los headers `x-kuida-signature`
 * (`sha256=<hex>`), `x-kuida-timestamp` (epoch en segundos), `x-kuida-event-type`
 * y `x-kuida-delivery-attempt`.
 *
 * ```php
 * $event = \Kuida\Webhook::constructEvent(
 *     file_get_contents('php://input'),
 *     $_SERVER['HTTP_X_KUIDA_SIGNATURE'] ?? null,
 *     $_SERVER['HTTP_X_KUIDA_TIMESTAMP'] ?? null,
 *     getenv('KUIDA_WEBHOOK_SECRET')
 * );
 * ```
 */
final class Webhook
{
    /** Tolerancia por defecto entre el timestamp del envío y la hora actual, en segundos. */
    const DEFAULT_TOLERANCE = 300;

    const SIGNATURE_HEADER = 'x-kuida-signature';

    const TIMESTAMP_HEADER = 'x-kuida-timestamp';

    const EVENT_TYPE_HEADER = 'x-kuida-event-type';

    const DELIVERY_ATTEMPT_HEADER = 'x-kuida-delivery-attempt';

    /**
     * Verifica la firma y devuelve el evento.
     *
     * @param string $payload Cuerpo crudo del pedido, tal como llegó (nunca un JSON re-serializado).
     * @param string|null $signatureHeader Valor del header `x-kuida-signature`.
     * @param string|null $timestampHeader Valor del header `x-kuida-timestamp`.
     * @param string $secret Secreto del endpoint (`whsec_…`).
     * @param int $tolerance Diferencia máxima en segundos con la hora actual; `0` desactiva el chequeo.
     * @param int|null $now Hora actual en epoch (para tests). Default `time()`.
     * @throws SignatureVerificationException Si falta un header, la firma no coincide o el timestamp está fuera de tolerancia.
     * @throws UnexpectedValueException Si el cuerpo firmado no es un objeto JSON.
     */
    public static function constructEvent(
        string $payload,
        ?string $signatureHeader,
        ?string $timestampHeader,
        string $secret,
        int $tolerance = self::DEFAULT_TOLERANCE,
        ?int $now = null
    ): WebhookEvent {
        self::verifySignature($payload, $signatureHeader, $timestampHeader, $secret, $tolerance, $now);

        $decoded = json_decode($payload);
        if (!$decoded instanceof \stdClass) {
            throw new UnexpectedValueException('El cuerpo del webhook no es un objeto JSON válido.');
        }

        return WebhookEvent::constructFrom($decoded);
    }

    /**
     * Verifica la firma de un webhook. Devuelve `true` o lanza una excepción.
     *
     * @param string $payload Cuerpo crudo del pedido.
     * @param string|null $signatureHeader Valor del header `x-kuida-signature`.
     * @param string|null $timestampHeader Valor del header `x-kuida-timestamp`.
     * @param string $secret Secreto del endpoint (`whsec_…`).
     * @param int $tolerance Diferencia máxima en segundos con la hora actual; `0` desactiva el chequeo.
     * @param int|null $now Hora actual en epoch (para tests). Default `time()`.
     * @throws SignatureVerificationException
     */
    public static function verifySignature(
        string $payload,
        ?string $signatureHeader,
        ?string $timestampHeader,
        string $secret,
        int $tolerance = self::DEFAULT_TOLERANCE,
        ?int $now = null
    ): bool {
        if ($signatureHeader === null || trim($signatureHeader) === '') {
            throw SignatureVerificationException::factory('Falta el header ' . self::SIGNATURE_HEADER . '.', $payload, $signatureHeader);
        }
        if ($timestampHeader === null || trim($timestampHeader) === '') {
            throw SignatureVerificationException::factory('Falta el header ' . self::TIMESTAMP_HEADER . '.', $payload, $signatureHeader);
        }
        if ($secret === '') {
            throw SignatureVerificationException::factory('Falta el secreto del endpoint para verificar la firma.', $payload, $signatureHeader);
        }
        $timestamp = trim($timestampHeader);
        if (preg_match('/^\d+$/', $timestamp) !== 1) {
            throw SignatureVerificationException::factory('El header ' . self::TIMESTAMP_HEADER . ' no es un epoch en segundos.', $payload, $signatureHeader);
        }

        $expected = self::computeSignature($timestamp, $payload, $secret);
        $matched = false;
        foreach (explode(',', $signatureHeader) as $candidate) {
            if (hash_equals($expected, trim($candidate))) {
                $matched = true;
            }
        }
        if (!$matched) {
            throw SignatureVerificationException::factory(
                'La firma no coincide con el cuerpo recibido. ¿Usaste el cuerpo crudo y el secreto de este endpoint?',
                $payload,
                $signatureHeader
            );
        }

        if ($tolerance > 0) {
            $current = $now === null ? time() : $now;
            if (abs($current - (int) $timestamp) > $tolerance) {
                throw SignatureVerificationException::factory(
                    'El timestamp del webhook está fuera de la tolerancia de ' . $tolerance . ' segundos.',
                    $payload,
                    $signatureHeader
                );
            }
        }

        return true;
    }

    /**
     * Calcula la firma que Kuida manda en `x-kuida-signature` (`sha256=<hex>`).
     * Útil para probar tu endpoint con envíos firmados por vos.
     *
     * @param string $timestamp Epoch en segundos, como texto.
     * @param string $payload Cuerpo crudo.
     * @param string $secret Secreto del endpoint.
     */
    public static function computeSignature(string $timestamp, string $payload, string $secret): string
    {
        return 'sha256=' . hash_hmac('sha256', $timestamp . '.' . $payload, $secret);
    }
}
