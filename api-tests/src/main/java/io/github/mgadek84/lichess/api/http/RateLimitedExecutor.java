package io.github.mgadek84.lichess.api.http;

import io.github.mgadek84.lichess.api.config.LichessConfig;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.testng.SkipException;

/**
 * Sends every Lichess request through one gate so the whole JVM respects the Lichess API rules:
 *
 * <ul>
 *   <li>one request at a time, with a minimum gap between requests;
 *   <li>on HTTP 429, wait (Retry-After if present, otherwise the configured back-off, 60 s by
 *       default) and retry a limited number of times;
 *   <li>if Lichess still says 429, skip the test instead of failing it: being rate limited says
 *       nothing about the quality of the API under test.
 * </ul>
 *
 * <p>This is not a RestAssured {@code Filter} on purpose: a filter cannot re-send a request
 * ({@code FilterContext.next} only works once), so retries live here.
 */
public final class RateLimitedExecutor {

  private static final RateLimitedExecutor INSTANCE = new RateLimitedExecutor(LichessConfig.get());

  private final LichessConfig config;
  private final AtomicInteger requestCount = new AtomicInteger();
  private long lastRequestNanos;

  RateLimitedExecutor(LichessConfig config) {
    this.config = config;
  }

  public static RateLimitedExecutor get() {
    return INSTANCE;
  }

  /** Number of HTTP requests sent so far, retries included. */
  public int requestCount() {
    return requestCount.get();
  }

  public synchronized Response execute(Supplier<Response> request) {
    for (int attempt = 0; ; attempt++) {
      waitForSlot();
      Response response = request.get();
      requestCount.incrementAndGet();
      lastRequestNanos = System.nanoTime();

      if (response.statusCode() != 429) {
        return response;
      }
      if (attempt >= config.rateLimitMaxRetries()) {
        throw new SkipException(
            "Lichess rate limit (HTTP 429) still active after "
                + attempt
                + " retries; skipping rather than failing. Wait a minute and re-run.");
      }
      Duration wait = retryAfter(response);
      System.err.printf(
          "Lichess returned HTTP 429, backing off for %d s (retry %d/%d)%n",
          wait.toSeconds(), attempt + 1, config.rateLimitMaxRetries());
      sleep(wait);
    }
  }

  private void waitForSlot() {
    if (lastRequestNanos == 0) {
      return;
    }
    long elapsed = System.nanoTime() - lastRequestNanos;
    long remaining = config.minInterval().toNanos() - elapsed;
    if (remaining > 0) {
      sleep(Duration.ofNanos(remaining));
    }
  }

  private Duration retryAfter(Response response) {
    String header = response.getHeader("Retry-After");
    if (header != null && header.strip().matches("\\d+")) {
      return Duration.ofSeconds(Long.parseLong(header.strip()));
    }
    return config.rateLimitBackoff();
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for Lichess rate limit", e);
    }
  }
}
