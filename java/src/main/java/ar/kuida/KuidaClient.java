package ar.kuida;

import ar.kuida.exception.AuthenticationException;
import ar.kuida.net.HttpTransport;
import ar.kuida.net.UrlConnectionTransport;
import ar.kuida.service.AccountService;
import ar.kuida.service.AppointmentService;
import ar.kuida.service.DoctorService;
import ar.kuida.service.EventService;
import ar.kuida.service.IntakeRequestService;
import ar.kuida.service.PatientService;
import ar.kuida.service.TreatmentService;
import ar.kuida.service.VisitService;
import ar.kuida.service.WebhookDeliveryService;
import ar.kuida.service.WebhookEndpointService;
import com.google.gson.JsonObject;
import java.time.Duration;

/**
 * Cliente de la API de Kuida.
 *
 * <pre>{@code
 * KuidaClient kuida = new KuidaClient("kd_live_...");
 * Patient patient = kuida.patients().create(PatientCreateParams.builder()
 *     .phone("+54 9 342 555 0000")
 *     .fullName("Paciente Demo")
 *     .build());
 * }</pre>
 *
 * <p>Es thread-safe y reutilizable: conviene crear uno por proceso y compartirlo.
 */
public final class KuidaClient {
  /** Versión de este SDK. */
  public static final String VERSION = "0.1.0";
  /** Versión de la API que usa este SDK por defecto (header {@code Kuida-Version}). */
  public static final String API_VERSION = "2026-09-28";
  /** URL base por defecto. */
  public static final String DEFAULT_BASE_URL = "https://www.kuida.ar/api";
  /** Timeout por defecto de cada intento. */
  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
  /** Reintentos por defecto (3 intentos en total). */
  public static final int DEFAULT_MAX_RETRIES = 2;

  static final String USER_AGENT = "Kuida/v1 JavaBindings/" + VERSION;

  private final String baseUrl;
  private final String apiVersion;
  private final AccountService account;
  private final PatientService patients;
  private final DoctorService doctors;
  private final AppointmentService appointments;
  private final VisitService visits;
  private final IntakeRequestService intakeRequests;
  private final TreatmentService treatments;
  private final EventService events;
  private final WebhookEndpointService webhookEndpoints;
  private final WebhookDeliveryService webhookDeliveries;

  /**
   * Crea un cliente con la clave dada y las opciones por defecto.
   *
   * @param apiKey clave de API ({@code kd_live_…} o {@code kd_test_…}); si es {@code null} se lee
   *     {@code KUIDA_API_KEY} del entorno
   * @throws AuthenticationException si no hay clave
   */
  public KuidaClient(String apiKey) {
    this(builder().apiKey(apiKey));
  }

  /**
   * Crea un cliente con la clave de {@code KUIDA_API_KEY}.
   *
   * @throws AuthenticationException si la variable no está definida
   */
  public KuidaClient() {
    this(builder());
  }

  private KuidaClient(Builder b) {
    String key = b.apiKey != null ? b.apiKey : System.getenv("KUIDA_API_KEY");
    if (key == null || key.trim().isEmpty()) {
      throw new AuthenticationException(
          "Falta la clave de API: se pasa al construir el cliente o en la variable KUIDA_API_KEY.",
          0, "authentication_error", "api_key_missing", null, null, null, null, null, null);
    }
    String base = b.baseUrl != null ? b.baseUrl : System.getenv("KUIDA_API_BASE");
    if (base == null || base.trim().isEmpty()) base = DEFAULT_BASE_URL;
    base = base.trim();
    while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
    this.baseUrl = base;
    this.apiVersion = b.apiVersion != null ? b.apiVersion : API_VERSION;
    ApiRequestor requestor = new ApiRequestor(key.trim(), base, apiVersion,
        b.timeout != null ? b.timeout : DEFAULT_TIMEOUT,
        b.maxRetries != null ? b.maxRetries : DEFAULT_MAX_RETRIES,
        b.httpTransport != null ? b.httpTransport : new UrlConnectionTransport(),
        b.sleeper != null ? b.sleeper : Sleeper.THREAD);
    this.account = new AccountService(requestor);
    this.patients = new PatientService(requestor);
    this.doctors = new DoctorService(requestor);
    this.appointments = new AppointmentService(requestor);
    this.visits = new VisitService(requestor);
    this.intakeRequests = new IntakeRequestService(requestor);
    this.treatments = new TreatmentService(requestor);
    this.events = new EventService(requestor);
    this.webhookEndpoints = new WebhookEndpointService(requestor);
    this.webhookDeliveries = new WebhookDeliveryService(requestor);
  }

  /** Builder con todas las opciones del cliente. */
  public static Builder builder() {
    return new Builder();
  }

  /** La cuenta dueña de la clave. */
  public AccountService account() {
    return account;
  }

  /** Pacientes. */
  public PatientService patients() {
    return patients;
  }

  /** Profesionales (solo lectura). */
  public DoctorService doctors() {
    return doctors;
  }

  /** Turnos. */
  public AppointmentService appointments() {
    return appointments;
  }

  /** Consultas atendidas. */
  public VisitService visits() {
    return visits;
  }

  /** Solicitudes de ingreso. */
  public IntakeRequestService intakeRequests() {
    return intakeRequests;
  }

  /** Tratamientos. */
  public TreatmentService treatments() {
    return treatments;
  }

  /** Eventos con el sobre común. */
  public EventService events() {
    return events;
  }

  /** Endpoints de webhooks. */
  public WebhookEndpointService webhookEndpoints() {
    return webhookEndpoints;
  }

  /** Entregas de webhooks. */
  public WebhookDeliveryService webhookDeliveries() {
    return webhookDeliveries;
  }

  /** URL base efectiva, sin barra final. */
  public String getBaseUrl() {
    return baseUrl;
  }

  /** Versión de la API que manda este cliente en {@code Kuida-Version}. */
  public String getApiVersion() {
    return apiVersion;
  }

  static String clientUserAgent() {
    JsonObject ua = new JsonObject();
    ua.addProperty("bindings_version", VERSION);
    ua.addProperty("lang", "java");
    ua.addProperty("lang_version", System.getProperty("java.version", "unknown"));
    ua.addProperty("platform", System.getProperty("os.name", "") + " " + System.getProperty("os.version", "") + " "
        + System.getProperty("os.arch", ""));
    return ua.toString();
  }

  /** Builder de {@link KuidaClient}. */
  public static final class Builder {
    private String apiKey;
    private String baseUrl;
    private String apiVersion;
    private Duration timeout;
    private Integer maxRetries;
    private HttpTransport httpTransport;
    private Sleeper sleeper;

    private Builder() {}

    /** Clave de API. Si no se pasa, se lee {@code KUIDA_API_KEY}. */
    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * URL base (default {@code https://www.kuida.ar/api}). Si no se pasa, se lee {@code KUIDA_API_BASE}.
     */
    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    /** Versión de la API (header {@code Kuida-Version}); default {@value KuidaClient#API_VERSION}. */
    public Builder apiVersion(String apiVersion) {
      this.apiVersion = apiVersion;
      return this;
    }

    /** Timeout de conexión y de lectura de cada intento (default 30 s). */
    public Builder timeout(Duration timeout) {
      if (timeout == null || timeout.isNegative() || timeout.isZero()) {
        throw new IllegalArgumentException("timeout tiene que ser positivo");
      }
      this.timeout = timeout;
      return this;
    }

    /** Reintentos ante errores de red, 409 {@code idempotency_key_in_use}, 429 y 5xx (default 2). */
    public Builder maxRetries(int maxRetries) {
      if (maxRetries < 0) throw new IllegalArgumentException("maxRetries no puede ser negativo");
      this.maxRetries = maxRetries;
      return this;
    }

    /** Transporte HTTP propio (default {@link UrlConnectionTransport}). */
    public Builder httpTransport(HttpTransport httpTransport) {
      this.httpTransport = httpTransport;
      return this;
    }

    /** Espera entre reintentos (default {@link Sleeper#THREAD}); en tests, {@link Sleeper#NONE}. */
    public Builder sleeper(Sleeper sleeper) {
      this.sleeper = sleeper;
      return this;
    }

    /**
     * Crea el cliente.
     *
     * @throws AuthenticationException si no hay clave
     */
    public KuidaClient build() {
      return new KuidaClient(this);
    }
  }
}
