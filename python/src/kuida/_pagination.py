"""Listas paginadas por cursor y el iterador automático."""

import inspect
from typing import Any, Callable, Dict, Generic, Iterator, List, Optional, TypeVar, Union, overload

from ._object import KuidaObject

T = TypeVar("T")


class ListObject(KuidaObject, Generic[T]):
    """Una página de resultados (``object: "list"``).

    Es iterable sobre los elementos de la página::

        page = client.patients.list(limit=20)
        for patient in page:
            print(patient.id)

    ``page.has_more`` indica si hay más resultados. Para recorrer todas las páginas usá
    :meth:`auto_paging_iter` (``for`` en el cliente sincrónico, ``async for`` en
    :class:`~kuida.AsyncKuida`).
    """

    object: str
    data: List[T]
    has_more: bool
    url: str

    def __init__(self, data: Optional[Dict[str, Any]] = None, **kwargs: Any) -> None:
        super().__init__(data, **kwargs)
        object.__setattr__(self, "_fetch", None)
        object.__setattr__(self, "_query", {})

    def _bind(self, fetch: Callable[[Dict[str, Any]], Any], query: Dict[str, Any]) -> "ListObject[T]":
        object.__setattr__(self, "_fetch", fetch)
        object.__setattr__(self, "_query", dict(query))
        return self

    # ─── página actual ───────────────────────────────────────────────────────

    def _items(self) -> List[T]:
        return list(self._values.get("data") or [])

    def __iter__(self) -> Iterator[T]:  # type: ignore[override]
        return iter(self._items())

    def __len__(self) -> int:
        return len(self._items())

    @overload
    def __getitem__(self, key: int) -> T: ...

    @overload
    def __getitem__(self, key: str) -> Any: ...

    def __getitem__(self, key: Union[int, str]) -> Any:
        if isinstance(key, (int, slice)):
            return self._items()[key]
        return super().__getitem__(key)

    @property
    def is_empty(self) -> bool:
        """``True`` si la página no trae elementos."""
        return not self._items()

    # ─── paginación ──────────────────────────────────────────────────────────

    def _next_query(self) -> Optional[Dict[str, Any]]:
        items = self._items()
        if not self._values.get("hasMore") or not items:
            return None
        query = dict(self._query)
        if query.get("endingBefore") and not query.get("startingAfter"):
            query["endingBefore"] = getattr(items[0], "id", None)
        else:
            query.pop("endingBefore", None)
            query["startingAfter"] = getattr(items[-1], "id", None)
        return query

    def auto_paging_iter(self) -> "AutoPagingIterator[T]":
        """Itera todos los elementos, pidiendo las páginas siguientes a medida que hace falta.

        Respeta el ``limit`` y los filtros del pedido original. Con el cliente sincrónico se
        usa con ``for``; con :class:`~kuida.AsyncKuida`, con ``async for``.
        Si el pedido original usó ``ending_before``, recorre hacia atrás.
        """
        return AutoPagingIterator(self)


class AutoPagingIterator(Generic[T]):
    """Iterador sobre todas las páginas de una lista. Soporta ``for`` y ``async for``."""

    def __init__(self, page: ListObject[T]) -> None:
        self._page = page
        self._index = 0

    def _take(self) -> Optional[T]:
        items = self._page._items()
        if self._index < len(items):
            item = items[self._index]
            self._index += 1
            return item
        return None

    def __iter__(self) -> "AutoPagingIterator[T]":
        return self

    def __next__(self) -> T:
        while True:
            item = self._take()
            if item is not None:
                return item
            query = self._page._next_query()
            if query is None or self._page._fetch is None:
                raise StopIteration
            page = self._page._fetch(query)
            if inspect.isawaitable(page):
                close = getattr(page, "close", None)
                if close:
                    close()
                raise TypeError("Esta lista viene de AsyncKuida: recorrela con `async for`.")
            self._page, self._index = page, 0

    def __aiter__(self) -> "AutoPagingIterator[T]":
        return self

    async def __anext__(self) -> T:
        while True:
            item = self._take()
            if item is not None:
                return item
            query = self._page._next_query()
            if query is None or self._page._fetch is None:
                raise StopAsyncIteration
            page = self._page._fetch(query)
            if inspect.isawaitable(page):
                page = await page
            self._page, self._index = page, 0
