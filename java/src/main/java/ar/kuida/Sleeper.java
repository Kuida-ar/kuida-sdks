package ar.kuida;

/**
 * Espera entre reintentos. El default duerme el hilo; los tests inyectan uno que no espera.
 */
public interface Sleeper {
  /** Espera {@code millis} milisegundos. */
  void sleep(long millis) throws InterruptedException;

  /** Espera real con {@link Thread#sleep(long)}. */
  Sleeper THREAD = new Sleeper() {
    @Override
    public void sleep(long millis) throws InterruptedException {
      Thread.sleep(millis);
    }
  };

  /** No espera: útil en tests. */
  Sleeper NONE = new Sleeper() {
    @Override
    public void sleep(long millis) {}
  };
}
