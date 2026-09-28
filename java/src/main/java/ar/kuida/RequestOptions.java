package ar.kuida;

import java.time.Duration;

/**
 * Opciones de un pedido puntual; pisan las del cliente.
 *
 * <pre>{@code
 * RequestOptions opts = RequestOptions.builder()
 *     .idempotencyKey("alta-HC-1234")
 *     .timeout(Duration.ofSeconds(10))
 *     .maxRetries(0)
 *     .build();
 * }</pre>
 */
public final class RequestOptions {
  /** Sin opciones: se usan las del cliente. */
  public static final RequestOptions NONE = builder().build();

  private final String idempotencyKey;
  private final Duration timeout;
  private final Integer maxRetries;

  private RequestOptions(Builder b) {
    this.idempotencyKey = b.idempotencyKey;
    this.timeout = b.timeout;
    this.maxRetries = b.maxRetries;
  }

  /** Builder vacío. */
  public static Builder builder() {
    return new Builder();
  }

  /** Clave de idempotencia; si es {@code null}, el SDK genera un UUID v4 para cada POST. */
  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  /** Timeout del pedido, o {@code null} para usar el del cliente. */
  public Duration getTimeout() {
    return timeout;
  }

  /** Reintentos del pedido, o {@code null} para usar los del cliente. */
  public Integer getMaxRetries() {
    return maxRetries;
  }

  /** Builder de {@link RequestOptions}. */
  public static final class Builder {
    private String idempotencyKey;
    private Duration timeout;
    private Integer maxRetries;

    private Builder() {}

    /**
     * Clave de idempotencia para un POST (hasta 255 caracteres). Reusarla con el mismo cuerpo
     * devuelve la respuesta original; con otro cuerpo, {@code IdempotencyException}.
     */
    public Builder idempotencyKey(String idempotencyKey) {
      this.idempotencyKey = idempotencyKey;
      return this;
    }

    /** Timeout de conexión y de lectura de cada intento. */
    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    /** Cantidad máxima de reintentos (0 = un solo intento). */
    public Builder maxRetries(int maxRetries) {
      if (maxRetries < 0) throw new IllegalArgumentException("maxRetries no puede ser negativo");
      this.maxRetries = maxRetries;
      return this;
    }

    /** Arma las opciones. */
    public RequestOptions build() {
      return new RequestOptions(this);
    }
  }
}
