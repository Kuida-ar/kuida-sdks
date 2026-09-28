package kuida

import (
	"context"
	"net/url"
)

// ListPage es una página de un listado con cursor.
type ListPage[T any] struct {
	// Object vale "list".
	Object string `json:"object"`
	// Data son los objetos de la página, del más nuevo al más viejo.
	Data []T `json:"data"`
	// HasMore indica si hay más objetos después de esta página.
	HasMore bool `json:"hasMore"`
	// URL es la ruta del listado.
	URL string `json:"url"`

	// LastResponse tiene los datos HTTP del pedido de esta página.
	LastResponse *APIResponse `json:"-"`
}

func (l *ListPage[T]) setLastResponse(r *APIResponse) { l.LastResponse = r }

// Iter recorre todos los objetos de un listado y pide las páginas a medida
// que hacen falta, de a Limit objetos. Se usa así:
//
//	it := client.Patients.List(ctx, &kuida.PatientListParams{})
//	for it.Next() {
//		p := it.Current()
//		…
//	}
//	if err := it.Err(); err != nil { … }
//
// Si los parámetros traen EndingBefore, el recorrido va hacia atrás
// (páginas cada vez más nuevas). Un Iter no es seguro entre goroutines.
type Iter[T any] struct {
	ctx      context.Context
	fetch    func(ctx context.Context, q url.Values) (*ListPage[T], error)
	idOf     func(T) string
	query    url.Values
	backward bool

	page *ListPage[T]
	idx  int
	cur  T
	err  error
}

func newIter[T any](ctx context.Context, b *backend, path string, q url.Values, opts []RequestOption, idOf func(T) string) *Iter[T] {
	if ctx == nil {
		ctx = context.Background()
	}
	return &Iter[T]{
		ctx: ctx,
		fetch: func(ctx context.Context, q url.Values) (*ListPage[T], error) {
			return listPage[T](ctx, b, path, q, opts)
		},
		idOf:     idOf,
		query:    q,
		backward: q.Get("endingBefore") != "",
	}
}

// Next avanza al siguiente objeto. Devuelve false al terminar o ante un
// error; en ese caso Err dice cuál.
func (it *Iter[T]) Next() bool {
	if it.err != nil {
		return false
	}
	for {
		if it.page != nil && it.idx < len(it.page.Data) {
			it.cur = it.page.Data[it.idx]
			it.idx++
			return true
		}
		if it.page != nil && (!it.page.HasMore || len(it.page.Data) == 0) {
			return false
		}
		q := url.Values{}
		for k, v := range it.query {
			q[k] = append([]string(nil), v...)
		}
		if it.page != nil {
			if it.backward {
				q.Del("startingAfter")
				q.Set("endingBefore", it.idOf(it.page.Data[0]))
			} else {
				q.Del("endingBefore")
				q.Set("startingAfter", it.idOf(it.page.Data[len(it.page.Data)-1]))
			}
		}
		page, err := it.fetch(it.ctx, q)
		if err != nil {
			it.err = err
			return false
		}
		it.page, it.idx = page, 0
	}
}

// Current devuelve el objeto actual. Solo es válido después de un Next que
// devolvió true.
func (it *Iter[T]) Current() T { return it.cur }

// Err devuelve el error que cortó el recorrido, o nil.
func (it *Iter[T]) Err() error { return it.err }

// Page devuelve la página que se está recorriendo (nil antes del primer Next).
func (it *Iter[T]) Page() *ListPage[T] { return it.page }
