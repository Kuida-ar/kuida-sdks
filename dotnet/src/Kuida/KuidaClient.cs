using System;
using System.Net.Http;

namespace Kuida
{
    /// <summary>
    /// Cliente de la API de Kuida. Es thread-safe: cree uno por proceso y reúselo.
    /// <code>
    /// var kuida = new KuidaClient("kd_live_…");
    /// var paciente = await kuida.Patients.CreateAsync(new PatientCreateParams { Phone = "+54 9 342 555 0000" });
    /// </code>
    /// </summary>
    public sealed class KuidaClient
    {
        /// <summary>Versión de la API que usa este SDK (header <c>Kuida-Version</c>).</summary>
        public const string ApiVersion = GeneratedApiVersion.Value;

        /// <summary>Versión de este SDK.</summary>
        public const string SdkVersion = ApiRequestor.SdkVersion;

        /// <summary>URL base por defecto.</summary>
        public const string DefaultBaseUrl = "https://www.kuida.ar/api";

        /// <summary>Reintentos por defecto.</summary>
        public const int DefaultMaxRetries = 2;

        /// <summary>Tiempo máximo por defecto de cada intento.</summary>
        public static readonly TimeSpan DefaultTimeout = TimeSpan.FromSeconds(30);

        private readonly ApiRequestor _requestor;

        /// <summary>Crea el cliente con la clave de la variable de entorno <c>KUIDA_API_KEY</c>.</summary>
        /// <exception cref="AuthenticationException">Si no hay clave.</exception>
        public KuidaClient() : this(null, null, null) { }

        /// <summary>Crea el cliente.</summary>
        /// <param name="apiKey">Clave de API (<c>kd_live_…</c> o <c>kd_test_…</c>). Si es <c>null</c>, se lee <c>KUIDA_API_KEY</c>.</param>
        /// <param name="options">Opciones (URL base, timeout, reintentos, versión).</param>
        /// <exception cref="AuthenticationException">Si no hay clave.</exception>
        public KuidaClient(string? apiKey, KuidaClientOptions? options = null) : this(apiKey, options, null) { }

        /// <summary>Crea el cliente con un <see cref="HttpClient"/> propio (proxy, handler de pruebas, <c>IHttpClientFactory</c>).</summary>
        /// <param name="apiKey">Clave de API. Si es <c>null</c>, se lee <c>KUIDA_API_KEY</c>.</param>
        /// <param name="options">Opciones (URL base, timeout, reintentos, versión).</param>
        /// <param name="httpClient">Cliente HTTP a usar. El SDK no lo libera. Si es <c>null</c>, usa uno compartido.</param>
        /// <exception cref="AuthenticationException">Si no hay clave.</exception>
        public KuidaClient(string? apiKey, KuidaClientOptions? options, HttpClient? httpClient)
        {
            var key = string.IsNullOrWhiteSpace(apiKey) ? Environment.GetEnvironmentVariable("KUIDA_API_KEY") : apiKey;
            if (string.IsNullOrWhiteSpace(key))
            {
                throw new AuthenticationException(
                    "Falta la clave de API. Pásela al construir el cliente (new KuidaClient(\"kd_live_…\")) o defina la variable de entorno KUIDA_API_KEY.")
                {
                    Type = "authentication_error",
                    Code = "api_key_missing",
                };
            }

            _requestor = new ApiRequestor(key!.Trim(), options ?? new KuidaClientOptions(), httpClient);
            Account = new AccountService(_requestor);
            Patients = new PatientService(_requestor);
            Doctors = new DoctorService(_requestor);
            Appointments = new AppointmentService(_requestor);
            Visits = new VisitService(_requestor);
            IntakeRequests = new IntakeRequestService(_requestor);
            Treatments = new TreatmentService(_requestor);
            Events = new EventService(_requestor);
            WebhookEndpoints = new WebhookEndpointService(_requestor);
            WebhookDeliveries = new WebhookDeliveryService(_requestor);
        }

        /// <summary>URL base efectiva.</summary>
        public string BaseUrl => _requestor.BaseUrl;

        /// <summary>La organización dueña de la clave.</summary>
        public AccountService Account { get; }

        /// <summary>Pacientes.</summary>
        public PatientService Patients { get; }

        /// <summary>Profesionales del padrón.</summary>
        public DoctorService Doctors { get; }

        /// <summary>Turnos.</summary>
        public AppointmentService Appointments { get; }

        /// <summary>Consultas atendidas.</summary>
        public VisitService Visits { get; }

        /// <summary>Solicitudes de ingreso.</summary>
        public IntakeRequestService IntakeRequests { get; }

        /// <summary>Tratamientos indicados.</summary>
        public TreatmentService Treatments { get; }

        /// <summary>Eventos del sistema de gestión (la puerta genérica).</summary>
        public EventService Events { get; }

        /// <summary>Endpoints de webhook.</summary>
        public WebhookEndpointService WebhookEndpoints { get; }

        /// <summary>Entregas de webhook.</summary>
        public WebhookDeliveryService WebhookDeliveries { get; }
    }
}
