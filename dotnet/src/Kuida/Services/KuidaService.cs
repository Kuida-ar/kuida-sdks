using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Base de los recursos del cliente (<c>kuida.Patients</c>, <c>kuida.Appointments</c>, …).</summary>
    public abstract class KuidaService
    {
        internal KuidaService(ApiRequestor requestor)
        {
            Requestor = requestor;
        }

        internal ApiRequestor Requestor { get; }

        internal Task<T> GetAsync<T>(string path, ListParams? query, RequestOptions? options, CancellationToken cancellationToken) where T : class =>
            Requestor.RequestAsync<T>(HttpMethod.Get, path, null, query, options, cancellationToken);

        internal Task<T> PostAsync<T>(string path, object? body, RequestOptions? options, CancellationToken cancellationToken) where T : class =>
            Requestor.RequestAsync<T>(HttpMethod.Post, path, body, null, options, cancellationToken);

        internal Task<T> PatchAsync<T>(string path, object body, RequestOptions? options, CancellationToken cancellationToken) where T : class =>
            Requestor.RequestAsync<T>(ApiRequestor.Patch, path, body, null, options, cancellationToken);

        internal Task<T> DeleteAsync<T>(string path, RequestOptions? options, CancellationToken cancellationToken) where T : class =>
            Requestor.RequestAsync<T>(HttpMethod.Delete, path, null, null, options, cancellationToken);

        internal static string Id(string id) => ApiRequestor.EscapePath(id);

        internal static T Required<T>(T? value, string name) where T : class =>
            value ?? throw new ArgumentNullException(name);

        /// <summary>
        /// Recorre todas las páginas de una lista. Avanza con <c>startingAfter</c> (o retrocede con
        /// <c>endingBefore</c> si se lo pasó) y pide de a <c>Limit</c> objetos.
        /// </summary>
        internal static async IAsyncEnumerable<T> AutoPage<T, TParams>(
            TParams? parameters,
            Func<TParams, CancellationToken, Task<KuidaList<T>>> fetchPage,
            [EnumeratorCancellation] CancellationToken cancellationToken)
            where T : IHasId
            where TParams : ListParams, new()
        {
            var current = parameters == null ? new TParams() : (TParams)parameters.Copy();
            var backward = current.EndingBefore != null && current.StartingAfter == null;
            while (true)
            {
                var page = await fetchPage(current, cancellationToken).ConfigureAwait(false);
                foreach (var item in page.Data)
                {
                    cancellationToken.ThrowIfCancellationRequested();
                    yield return item;
                }
                if (!page.HasMore || page.Data.Count == 0) yield break;
                current = (TParams)current.Copy();
                if (backward) current.EndingBefore = page.Data[0].Id;
                else current.StartingAfter = page.Data[page.Data.Count - 1].Id;
            }
        }

        internal static async Task<List<T>> CollectAsync<T>(IAsyncEnumerable<T> items, CancellationToken cancellationToken)
        {
            var all = new List<T>();
            await foreach (var item in items.WithCancellation(cancellationToken).ConfigureAwait(false)) all.Add(item);
            return all;
        }
    }
}
