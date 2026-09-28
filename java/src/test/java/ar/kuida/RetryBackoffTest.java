package ar.kuida;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Espera entre reintentos (SDK_DESIGN.md §6). */
class RetryBackoffTest {
  private static KuidaResponse withRetryAfter(String value) {
    Map<String, List<String>> headers = Collections.singletonMap("Retry-After", Arrays.asList(value));
    return new KuidaResponse(429, headers, "{}");
  }

  @Test
  void exponentialWithJitter() {
    double[] bases = {500, 1000, 2000, 4000, 8000, 8000};
    for (int attempt = 0; attempt < bases.length; attempt++) {
      for (int i = 0; i < 50; i++) {
        long d = ApiRequestor.delayMillis(attempt, null);
        assertTrue(d >= bases[attempt] * 0.75 - 1 && d <= bases[attempt] * 1.25 + 1, "intento " + attempt + ": " + d);
      }
    }
  }

  @Test
  void retryAfterIsRespectedUpTo60Seconds() {
    assertEquals(1000, ApiRequestor.delayMillis(0, withRetryAfter("1")));
    assertEquals(60000, ApiRequestor.delayMillis(0, withRetryAfter("600")));
  }
}
