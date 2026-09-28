import type { KuidaResponse } from "./types.js";

/** Una página de resultados de un `list`. */
export interface ApiList<T> {
  object: "list";
  /** Los objetos de esta página, del más nuevo al más viejo. */
  data: T[];
  /** `true` si hay más objetos después de esta página. */
  hasMore: boolean;
  /** Ruta del recurso listado. */
  url: string;
}

/** Parámetros comunes de paginación por cursor. */
export interface PaginationParams {
  limit?: number;
  startingAfter?: string;
  endingBefore?: string;
}

/** Opciones de {@link ListPromise.autoPagingToArray}. */
export interface AutoPagingToArrayOptions {
  /** Máximo de objetos a juntar. Obligatorio para no traer por accidente toda la base. */
  limit: number;
}

type PageFetcher<T, P> = (params: P) => Promise<KuidaResponse<ApiList<T>>>;

/**
 * Resultado de un `list()`: se puede esperar como una promesa de la primera
 * página, o recorrer con `for await` para paginar automáticamente.
 *
 * ```ts
 * const pagina = await kuida.patients.list({ limit: 10 });
 * for await (const paciente of kuida.patients.list({ limit: 100 })) { … }
 * ```
 *
 * El pedido de la primera página sale recién cuando se espera la promesa o se
 * empieza a iterar, y se reusa entre las dos formas.
 */
export class ListPromise<T extends { id: string }, P extends PaginationParams = PaginationParams>
  implements PromiseLike<KuidaResponse<ApiList<T>>>, AsyncIterable<T>
{
  readonly #fetchPage: PageFetcher<T, P>;
  readonly #params: P;
  #first: Promise<KuidaResponse<ApiList<T>>> | null = null;

  /** @internal */
  constructor(fetchPage: PageFetcher<T, P>, params: P) {
    this.#fetchPage = fetchPage;
    this.#params = params;
  }

  get [Symbol.toStringTag](): string {
    return "ListPromise";
  }

  #firstPage(): Promise<KuidaResponse<ApiList<T>>> {
    if (!this.#first) this.#first = this.#fetchPage(this.#params);
    return this.#first;
  }

  then<R1 = KuidaResponse<ApiList<T>>, R2 = never>(
    onfulfilled?: ((value: KuidaResponse<ApiList<T>>) => R1 | PromiseLike<R1>) | null,
    onrejected?: ((reason: unknown) => R2 | PromiseLike<R2>) | null,
  ): Promise<R1 | R2> {
    return this.#firstPage().then(onfulfilled, onrejected);
  }

  catch<R = never>(onrejected?: ((reason: unknown) => R | PromiseLike<R>) | null): Promise<KuidaResponse<ApiList<T>> | R> {
    return this.#firstPage().catch(onrejected);
  }

  finally(onfinally?: (() => void) | null): Promise<KuidaResponse<ApiList<T>>> {
    return this.#firstPage().finally(onfinally);
  }

  /**
   * Recorre todos los objetos, pidiendo páginas de a `limit` con
   * `startingAfter` (o `endingBefore` si se empezó con ese cursor).
   */
  async *[Symbol.asyncIterator](): AsyncIterator<T> {
    const backwards = this.#params.endingBefore !== undefined && this.#params.startingAfter === undefined;
    let page = await this.#firstPage();
    for (;;) {
      for (const item of page.data) yield item;
      if (!page.hasMore || page.data.length === 0) return;
      const cursor = backwards ? page.data[0]!.id : page.data[page.data.length - 1]!.id;
      const next = { ...this.#params } as P;
      if (backwards) {
        next.endingBefore = cursor;
        delete next.startingAfter;
      } else {
        next.startingAfter = cursor;
        delete next.endingBefore;
      }
      page = await this.#fetchPage(next);
    }
  }

  /**
   * Llama a `fn` con cada objeto, paginando solo. Si `fn` devuelve `false`
   * (o una promesa de `false`), se detiene.
   */
  async autoPagingEach(fn: (item: T) => void | boolean | Promise<void | boolean>): Promise<void> {
    for await (const item of this) {
      if ((await fn(item)) === false) return;
    }
  }

  /** Junta hasta `limit` objetos de todas las páginas en un array. */
  async autoPagingToArray(options: AutoPagingToArrayOptions): Promise<T[]> {
    const max = options?.limit;
    if (typeof max !== "number" || !Number.isInteger(max) || max < 1) {
      throw new TypeError("autoPagingToArray necesita { limit } con un entero positivo");
    }
    const out: T[] = [];
    for await (const item of this) {
      out.push(item);
      if (out.length >= max) break;
    }
    return out;
  }
}
