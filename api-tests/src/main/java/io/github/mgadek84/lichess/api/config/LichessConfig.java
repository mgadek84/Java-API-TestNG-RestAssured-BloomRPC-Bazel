package io.github.mgadek84.lichess.api.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Properties;

/**
 * Test configuration: {@code lichess.properties} on the classpath, overridden by JVM system
 * properties with the same key. One place to point the suite at another environment.
 */
public record LichessConfig(
    String baseUrl,
    String userAgent,
    Duration minInterval,
    Duration rateLimitBackoff,
    int rateLimitMaxRetries,
    Duration timeout,
    Duration maxResponseTime) {

  private static final LichessConfig INSTANCE = load();

  public static LichessConfig get() {
    return INSTANCE;
  }

  private static LichessConfig load() {
    Properties defaults = new Properties();
    try (InputStream in = LichessConfig.class.getResourceAsStream("/lichess.properties")) {
      if (in == null) {
        throw new IllegalStateException("lichess.properties not found on classpath");
      }
      defaults.load(in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return new LichessConfig(
        value(defaults, "lichess.baseUrl"),
        value(defaults, "lichess.userAgent"),
        Duration.ofMillis(Long.parseLong(value(defaults, "lichess.minIntervalMillis"))),
        Duration.ofSeconds(Long.parseLong(value(defaults, "lichess.rateLimit.backoffSeconds"))),
        Integer.parseInt(value(defaults, "lichess.rateLimit.maxRetries")),
        Duration.ofMillis(Long.parseLong(value(defaults, "lichess.timeoutMillis"))),
        Duration.ofMillis(Long.parseLong(value(defaults, "lichess.maxResponseTimeMillis"))));
  }

  private static String value(Properties defaults, String key) {
    String value = System.getProperty(key, defaults.getProperty(key));
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Missing configuration value: " + key);
    }
    return value.strip();
  }
}
