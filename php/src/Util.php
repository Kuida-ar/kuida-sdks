<?php

namespace Kuida;

use Kuida\Exception\InvalidArgumentException;
use Kuida\Model\ObjectTypes;

/**
 * Utilidades internas del SDK.
 *
 * @internal
 */
final class Util
{
    /**
     * Genera un UUID v4 aleatorio (para `Idempotency-Key`).
     */
    public static function uuidV4(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);
        $hex = bin2hex($bytes);

        return substr($hex, 0, 8) . '-' . substr($hex, 8, 4) . '-' . substr($hex, 12, 4) . '-'
            . substr($hex, 16, 4) . '-' . substr($hex, 20, 12);
    }

    /**
     * Serializa el cuerpo de un pedido como JSON. Las fechas (`DateTimeInterface`)
     * se mandan en ISO 8601 con zona; los campos de solo fecha, como `AAAA-MM-DD`.
     *
     * @param array<mixed>|null $params
     */
    public static function encodeBody(?array $params): string
    {
        if ($params === null || $params === []) {
            return '{}';
        }
        $json = json_encode(
            self::prepare($params, null),
            JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_PRESERVE_ZERO_FRACTION
        );
        if ($json === false) {
            throw new InvalidArgumentException('No se pudieron serializar los parámetros como JSON: ' . json_last_error_msg());
        }

        return $json;
    }

    /**
     * Arma el query string de un listado. Los booleanos van como `true`/`false`
     * y las fechas en ISO 8601.
     *
     * @param array<string, mixed> $params
     */
    public static function encodeQuery(array $params): string
    {
        $pairs = [];
        foreach ($params as $key => $value) {
            if ($value === null) {
                continue;
            }
            $values = is_array($value) ? $value : [$value];
            foreach ($values as $item) {
                if (is_array($item) || (is_object($item) && !$item instanceof \DateTimeInterface)) {
                    throw new InvalidArgumentException('El parámetro "' . $key . '" de un listado tiene que ser un valor simple.');
                }
                $pairs[] = rawurlencode((string) $key) . '=' . rawurlencode(self::scalarToString($item, (string) $key));
            }
        }

        return implode('&', $pairs);
    }

    /**
     * @param mixed $value
     */
    private static function scalarToString($value, string $key): string
    {
        if ($value instanceof \DateTimeInterface) {
            return self::formatDate($value, $key);
        }
        if (is_bool($value)) {
            return $value ? 'true' : 'false';
        }

        return (string) $value;
    }

    /**
     * @param mixed $value
     * @return mixed
     */
    private static function prepare($value, ?string $key)
    {
        if ($value instanceof \DateTimeInterface) {
            return self::formatDate($value, $key);
        }
        if ($value instanceof KuidaObject) {
            return $value->toArray();
        }
        if (is_array($value)) {
            $out = [];
            foreach ($value as $k => $v) {
                $out[$k] = self::prepare($v, is_string($k) ? $k : $key);
            }

            return $out;
        }

        return $value;
    }

    private static function formatDate(\DateTimeInterface $date, ?string $key): string
    {
        if ($key !== null && in_array($key, ObjectTypes::DATE_ONLY_FIELDS, true)) {
            return $date->format('Y-m-d');
        }

        return $date->format('Y-m-d\TH:i:sP');
    }
}
