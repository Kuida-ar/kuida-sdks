using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Kuida
{
    /// <summary>Turnos (<c>kuida.Appointments</c>).</summary>
    public sealed class AppointmentService : KuidaService
    {
        internal AppointmentService(ApiRequestor requestor) : base(requestor) { }

        /// <summary>Crea un turno. El paciente y el profesional pueden ir por id o por sus datos. Si ya existe un turno con el mismo <c>ExternalId</c>, devuelve ese.</summary>
        /// <param name="parameters">Datos del objeto.</param>
        /// <param name="options">Opciones del pedido (por ejemplo <c>IdempotencyKey</c>).</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<Appointment> CreateAsync(AppointmentCreateParams parameters, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PostAsync<Appointment>("/v1/appointments", Required(parameters, nameof(parameters)), options, cancellationToken);

        /// <summary>Trae un turno por id.</summary>
        /// <param name="id">Id de Kuida (<c>apt_…</c>).</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        /// <exception cref="InvalidRequestException">Si no existe (<c>HttpStatus</c> 404, <c>Code</c> <c>resource_missing</c>).</exception>
        public Task<Appointment> RetrieveAsync(string id, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<Appointment>("/v1/appointments/" + Id(id), null, options, cancellationToken);

        /// <summary>Reprograma o cambia un turno. Un turno cancelado o atendido no se puede cambiar.</summary>
        /// <param name="id">Id de Kuida.</param>
        /// <param name="parameters">Campos a cambiar; los que no se pasan quedan como están.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<Appointment> UpdateAsync(string id, AppointmentUpdateParams parameters, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PatchAsync<Appointment>("/v1/appointments/" + Id(id), Required(parameters, nameof(parameters)), options, cancellationToken);

        /// <summary>Cancela un turno. Cancelar uno ya cancelado no hace nada; uno atendido no se puede cancelar.</summary>
        /// <param name="id">Id del turno (<c>apt_…</c>).</param>
        /// <param name="parameters">Motivo, opcional. Sin parámetros se manda <c>{}</c>.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<Appointment> CancelAsync(string id, AppointmentCancelParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            PostAsync<Appointment>("/v1/appointments/" + Id(id) + "/cancel", parameters, options, cancellationToken);

        /// <summary>Lista turnos, del más nuevo al más viejo, de a una página.</summary>
        /// <param name="parameters">Filtros y cursores. Opcional.</param>
        /// <param name="options">Opciones del pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<KuidaList<Appointment>> ListAsync(AppointmentListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            GetAsync<KuidaList<Appointment>>("/v1/appointments", parameters, options, cancellationToken);

        /// <summary>
        /// Recorre todas las páginas de turnos pidiendo de a <c>Limit</c>. Úselo con <c>await foreach</c>.
        /// </summary>
        /// <param name="parameters">Filtros, tamaño de página y cursor inicial. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public IAsyncEnumerable<Appointment> ListAutoPagingAsync(AppointmentListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            AutoPage<Appointment, AppointmentListParams>(parameters, (p, ct) => ListAsync(p, options, ct), cancellationToken);

        /// <summary>
        /// Trae todas las páginas de turnos en una sola lista. Alternativa a <see cref="ListAutoPagingAsync"/>
        /// para código que no puede usar <c>await foreach</c> (C# 7.3 en .NET Framework).
        /// </summary>
        /// <param name="parameters">Filtros y tamaño de página. Opcional.</param>
        /// <param name="options">Opciones de cada pedido.</param>
        /// <param name="cancellationToken">Token de cancelación.</param>
        public Task<List<Appointment>> ListAllAsync(AppointmentListParams? parameters = null, RequestOptions? options = null, CancellationToken cancellationToken = default) =>
            CollectAsync(ListAutoPagingAsync(parameters, options, cancellationToken), cancellationToken);
    }
}
