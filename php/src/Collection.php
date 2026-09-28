<?php

namespace Kuida;

/**
 * Una página de una lista de la API (`object: "list"`).
 *
 * Se recorre con `foreach` (solo los elementos de esta página). Para recorrer
 * todas las páginas, pidiendo las siguientes a medida que hacen falta, usá
 * `autoPagingIterator()`.
 *
 * ```php
 * $page = $kuida->patients->list(['limit' => 20]);
 * foreach ($page->autoPagingIterator() as $patient) {
 *     echo $patient->fullName, "\n";
 * }
 * ```
 *
 * @template TObject of KuidaObject
 * @property-read string $object Siempre `list`.
 * @property-read TObject[] $data Los objetos de esta página.
 * @property-read bool $hasMore Si hay más objetos después de esta página.
 * @property-read string $url Ruta del listado.
 */
class Collection extends KuidaObject
{
    /** @var (callable(array<string, mixed>): Collection<TObject>)|null */
    private $fetcher;

    /** @var array<string, mixed> */
    private $filters = [];

    /**
     * Construye una página a partir de la respuesta de la API, convirtiendo
     * cada elemento en la clase indicada.
     *
     * @internal
     * @template T of KuidaObject
     * @param \stdClass $decoded Cuerpo decodificado.
     * @param class-string<T> $itemClass Clase de los elementos.
     * @return Collection<T>
     */
    public static function fromApi(\stdClass $decoded, string $itemClass, ?ApiResponse $response = null): Collection
    {
        $items = isset($decoded->data) && is_array($decoded->data) ? $decoded->data : [];
        $copy = clone $decoded;
        $copy->data = [];
        $page = self::constructFrom($copy, $response);
        $data = [];
        foreach ($items as $item) {
            $data[] = KuidaObject::convertValue($item, $itemClass);
        }
        $page->values['data'] = $data;

        /** @var Collection<T> $page */
        return $page;
    }

    /**
     * @internal Lo usa el SDK para poder pedir las páginas siguientes.
     *
     * @param callable(array<string, mixed>): Collection<TObject> $fetcher
     * @param array<string, mixed> $filters Parámetros con que se pidió esta página.
     */
    public function setPaging(callable $fetcher, array $filters): void
    {
        $this->fetcher = $fetcher;
        $this->filters = $filters;
    }

    /**
     * Parámetros con que se pidió esta página.
     *
     * @return array<string, mixed>
     */
    public function getFilters(): array
    {
        return $this->filters;
    }

    /**
     * Los objetos de esta página.
     *
     * @return TObject[]
     */
    public function getData(): array
    {
        $data = $this->__get('data');

        return is_array($data) ? $data : [];
    }

    /**
     * Si hay más objetos después de esta página.
     */
    public function hasMore(): bool
    {
        return $this->__get('hasMore') === true;
    }

    /**
     * Si la página no trae objetos.
     */
    public function isEmpty(): bool
    {
        return $this->getData() === [];
    }

    /**
     * Cantidad de objetos en esta página.
     */
    public function count(): int
    {
        return count($this->getData());
    }

    /**
     * Recorre los objetos de esta página.
     *
     * @return \Traversable<int, TObject>
     */
    public function getIterator(): \Traversable
    {
        return new \ArrayIterator($this->getData());
    }

    /**
     * Pide la página siguiente (o la anterior, si esta se pidió con
     * `endingBefore`). Devuelve una página vacía si no hay más.
     *
     * @return Collection<TObject>
     */
    public function nextPage(): Collection
    {
        $data = $this->getData();
        if (!$this->hasMore() || $data === [] || $this->fetcher === null) {
            $empty = self::constructFrom(['object' => 'list', 'data' => [], 'hasMore' => false, 'url' => $this->__get('url')]);
            if ($this->fetcher !== null) {
                $empty->setPaging($this->fetcher, $this->filters);
            }

            return $empty;
        }

        $params = $this->filters;
        if (isset($params['endingBefore'])) {
            $first = $data[0];
            $params['endingBefore'] = $first['id'];
        } else {
            $last = $data[count($data) - 1];
            $params['startingAfter'] = $last['id'];
        }

        return call_user_func($this->fetcher, $params);
    }

    /**
     * Recorre todos los objetos de la lista, página por página, pidiendo cada
     * página nueva a la API recién cuando hace falta (con el mismo `limit`).
     *
     * Si la primera página se pidió con `endingBefore`, recorre hacia atrás.
     *
     * @return \Generator<int, TObject>
     */
    public function autoPagingIterator(): \Generator
    {
        $page = $this;
        $backwards = isset($this->filters['endingBefore']);
        while (true) {
            $items = $page->getData();
            if ($backwards) {
                $items = array_reverse($items);
            }
            foreach ($items as $item) {
                yield $item;
            }
            if (!$page->hasMore() || $items === []) {
                return;
            }
            $page = $page->nextPage();
        }
    }
}
