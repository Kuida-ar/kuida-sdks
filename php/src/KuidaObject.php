<?php

namespace Kuida;

use Kuida\Model\ObjectTypes;

/**
 * Objeto devuelto por la API de Kuida.
 *
 * Los campos se leen como propiedades o como claves de array, con los nombres
 * del JSON (camelCase): `$patient->fullName` o `$patient['fullName']`.
 * Los campos que el SDK todavía no conoce también se pueden leer: la API agrega
 * campos sin cambiar de versión.
 *
 * Los objetos son de solo lectura; para cambiar algo usa el método `update`
 * del recurso.
 *
 * @implements \ArrayAccess<string, mixed>
 * @implements \IteratorAggregate<array-key, mixed>
 */
class KuidaObject implements \ArrayAccess, \Countable, \IteratorAggregate, \JsonSerializable
{
    /**
     * Propiedades cuyo valor es de un tipo conocido que no trae campo `object`.
     * Lo completa el generador en cada modelo.
     *
     * @var array<string, class-string<KuidaObject>>
     */
    const NESTED_TYPES = [];

    /** @var array<string, mixed> */
    protected $values = [];

    /** @var ApiResponse|null */
    protected $lastResponse;

    /**
     * @param array<string, mixed> $values Valores ya convertidos.
     */
    final public function __construct(array $values = [])
    {
        $this->values = $values;
    }

    /**
     * Construye el objeto a partir del JSON decodificado (como `\stdClass` o
     * array asociativo), convirtiendo los objetos anidados.
     *
     * @param array<string, mixed>|\stdClass $values
     * @param ApiResponse|null $response Respuesta HTTP de la que salió el objeto.
     * @return static
     */
    public static function constructFrom($values, ?ApiResponse $response = null)
    {
        if ($values instanceof \stdClass) {
            $values = get_object_vars($values);
        }
        $nested = static::NESTED_TYPES;
        $converted = [];
        foreach ($values as $key => $value) {
            $key = (string) $key;
            $class = isset($nested[$key]) ? $nested[$key] : null;
            $converted[$key] = self::convertValue($value, $class);
        }
        $obj = new static($converted);
        $obj->lastResponse = $response;

        return $obj;
    }

    /**
     * Convierte un valor JSON decodificado en objetos del SDK.
     *
     * @param mixed $value
     * @param class-string<KuidaObject>|null $class Clase forzada para los objetos.
     * @return mixed
     */
    public static function convertValue($value, ?string $class = null)
    {
        if ($value instanceof \stdClass) {
            return self::objectFor($value, $class);
        }
        if (is_array($value)) {
            $isList = $value === [] || array_keys($value) === range(0, count($value) - 1);
            if (!$isList) {
                return self::objectFor($value, $class);
            }
            $out = [];
            foreach ($value as $item) {
                $out[] = self::convertValue($item, $class);
            }

            return $out;
        }

        return $value;
    }

    /**
     * @param array<string, mixed>|\stdClass $value
     * @param class-string<KuidaObject>|null $class
     */
    private static function objectFor($value, ?string $class): KuidaObject
    {
        if ($class === null) {
            $object = is_array($value)
                ? (isset($value['object']) ? $value['object'] : null)
                : (isset($value->object) ? $value->object : null);
            $deleted = is_array($value)
                ? (isset($value['deleted']) ? $value['deleted'] : null)
                : (isset($value->deleted) ? $value->deleted : null);
            if ($deleted === true) {
                $class = Model\DeletedObject::class;
            } elseif ($object === 'list') {
                $class = Collection::class;
            } elseif (is_string($object) && array_key_exists($object, ObjectTypes::MAPPING)) {
                $class = ObjectTypes::MAPPING[$object];
            } else {
                $class = self::class;
            }
        }

        return $class::constructFrom($value);
    }

    /**
     * Respuesta HTTP de la que salió este objeto (null en los objetos anidados).
     */
    public function getLastResponse(): ?ApiResponse
    {
        return $this->lastResponse;
    }

    /**
     * @internal
     */
    public function setLastResponse(?ApiResponse $response): void
    {
        $this->lastResponse = $response;
    }

    /**
     * JSON crudo de la respuesta HTTP, tal como lo mandó la API
     * (null en los objetos anidados; usa `toJson()`).
     */
    public function getRawJson(): ?string
    {
        return $this->lastResponse !== null ? $this->lastResponse->body : null;
    }

    /**
     * Los valores como arrays asociativos de PHP, con las claves del JSON.
     *
     * @return array<string, mixed>
     */
    public function toArray(): array
    {
        $out = [];
        foreach ($this->values as $key => $value) {
            $out[$key] = self::unwrap($value);
        }

        return $out;
    }

    /**
     * El objeto serializado como JSON.
     */
    public function toJson(int $flags = 0): string
    {
        $json = json_encode($this->jsonSerialize(), $flags | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);

        return $json === false ? '{}' : $json;
    }

    /**
     * Nombres de los campos presentes.
     *
     * @return string[]
     */
    public function keys(): array
    {
        return array_keys($this->values);
    }

    /**
     * @param mixed $value
     * @return mixed
     */
    private static function unwrap($value)
    {
        if ($value instanceof KuidaObject) {
            return $value->toArray();
        }
        if (is_array($value)) {
            $out = [];
            foreach ($value as $k => $v) {
                $out[$k] = self::unwrap($v);
            }

            return $out;
        }

        return $value;
    }

    /**
     * @return mixed
     */
    public function __get(string $name)
    {
        return array_key_exists($name, $this->values) ? $this->values[$name] : null;
    }

    public function __isset(string $name): bool
    {
        return isset($this->values[$name]);
    }

    /**
     * @param mixed $value
     */
    public function __set(string $name, $value): void
    {
        throw new Exception\InvalidArgumentException(
            'Los objetos de Kuida son de solo lectura; usa el método update del recurso para cambiar "' . $name . '".'
        );
    }

    public function __unset(string $name): void
    {
        throw new Exception\InvalidArgumentException('Los objetos de Kuida son de solo lectura.');
    }

    /**
     * @param mixed $offset
     */
    public function offsetExists($offset): bool
    {
        return isset($this->values[$offset]);
    }

    /**
     * @param mixed $offset
     * @return mixed
     */
    #[\ReturnTypeWillChange]
    public function offsetGet($offset)
    {
        return array_key_exists($offset, $this->values) ? $this->values[$offset] : null;
    }

    /**
     * @param mixed $offset
     * @param mixed $value
     */
    public function offsetSet($offset, $value): void
    {
        $this->__set((string) $offset, $value);
    }

    /**
     * @param mixed $offset
     */
    public function offsetUnset($offset): void
    {
        $this->__unset((string) $offset);
    }

    public function count(): int
    {
        return count($this->values);
    }

    /**
     * @return \Traversable<array-key, mixed>
     */
    public function getIterator(): \Traversable
    {
        return new \ArrayIterator($this->values);
    }

    /**
     * @return array<string, mixed>
     */
    public function jsonSerialize(): array
    {
        return $this->toArray();
    }

    /**
     * @return array<string, mixed>
     */
    public function __debugInfo()
    {
        return $this->values;
    }

    public function __toString(): string
    {
        return static::class . ' JSON: ' . $this->toJson(JSON_PRETTY_PRINT);
    }
}
