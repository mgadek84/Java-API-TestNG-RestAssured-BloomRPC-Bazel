package io.github.mgadek84.lichess.api.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.mgadek84.lichess.api.config.LichessConfig;
import io.restassured.builder.ResponseBuilder;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;
import org.testng.SkipException;
import org.testng.annotations.Test;

/** Hermetic: proves the 429 handling without ever being rate limited by the real Lichess. */
@Test(groups = "unit")
public class RateLimitedExecutorTest {

  private static LichessConfig config(int maxRetries) {
    return new LichessConfig(
        "http://unused",
        "test",
        Duration.ZERO,
        Duration.ofMillis(10),
        maxRetries,
        Duration.ofSeconds(1),
        Duration.ofSeconds(1));
  }

  private static Response status(int code) {
    return new ResponseBuilder().setStatusCode(code).setBody("").build();
  }

  private static Supplier<Response> sequence(Response... responses) {
    Deque<Response> queue = new ArrayDeque<>(List.of(responses));
    return queue::removeFirst;
  }

  public void returnsFirstResponseWhenNotRateLimited() {
    RateLimitedExecutor executor = new RateLimitedExecutor(config(1));

    Response response = executor.execute(sequence(status(200)));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(executor.requestCount()).isEqualTo(1);
  }

  public void retriesAfter429AndReturnsTheRetriedResponse() {
    RateLimitedExecutor executor = new RateLimitedExecutor(config(1));

    Response response = executor.execute(sequence(status(429), status(200)));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(executor.requestCount()).isEqualTo(2);
  }

  public void honoursRetryAfterHeader() {
    RateLimitedExecutor executor = new RateLimitedExecutor(config(1));
    Response limited =
        new ResponseBuilder().setStatusCode(429).setHeader("Retry-After", "0").setBody("").build();

    long start = System.nanoTime();
    executor.execute(sequence(limited, status(200)));

    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
  }

  public void skipsTestWhenStillRateLimitedAfterRetries() {
    RateLimitedExecutor executor = new RateLimitedExecutor(config(1));

    assertThatThrownBy(() -> executor.execute(sequence(status(429), status(429))))
        .isInstanceOf(SkipException.class)
        .hasMessageContaining("HTTP 429");
    assertThat(executor.requestCount()).isEqualTo(2);
  }

  public void doesNotRetryOtherErrors() {
    RateLimitedExecutor executor = new RateLimitedExecutor(config(3));

    Response response = executor.execute(sequence(status(500)));

    assertThat(response.statusCode()).isEqualTo(500);
    assertThat(executor.requestCount()).isEqualTo(1);
  }
}
