using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>La organización dueña de la clave de API (<c>kuida.Account</c>).</summary>
    public sealed class AccountService : KuidaService
    {
        internal AccountService(ApiRequestor requestor) : base(requestor) { }

        /// <summary>Devuelve la organización dueña de la clave y los scopes de la clave. Sirve para probar la conexión.</summary>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<Account> RetrieveAsync(RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<Account>("/v1/account", null, options, cancellationToken);
    }
}
