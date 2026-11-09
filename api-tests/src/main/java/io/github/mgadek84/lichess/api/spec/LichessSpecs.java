package io.github.mgadek84.lichess.api.spec;

import static org.hamcrest.Matchers.lessThan;

import io.github.mgadek84.lichess.api.config.LichessConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.util.concurrent.TimeUnit;

/** Shared RestAssured request/response specifications for Lichess. */
public final class LichessSpecs {

  private LichessSpecs() {}

  /** Base request: base URI, identifying User-Agent, JSON, timeouts, one-line request logs. */
  public static RequestSpecification base() {
    LichessConfig config = LichessConfig.get();
    int timeoutMillis = (int) config.timeout().toMillis();
    return new RequestSpecBuilder()
        .setBaseUri(config.baseUrl())
        .addHeader("User-Agent", config.userAgent())
        // Exactly "application/json": RestAssured's ContentType.JSON sends several media types
        // and Lichess then falls back to PGN (game export) or HTML (404 pages).
        .setAccept("application/json")
        .setConfig(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", timeoutMillis)
                        .setParam("http.socket.timeout", timeoutMillis)))
        .addFilter(new RequestLoggingFilter(LogDetail.URI))
        .addFilter(new ResponseLoggingFilter(LogDetail.STATUS))
        .build();
  }

  /** Same as {@link #base()} but asks for PGN, Lichess' plain-text chess game format. */
  public static RequestSpecification pgn() {
    return new RequestSpecBuilder()
        .addRequestSpecification(base())
        .setAccept("application/x-chess-pgn")
        .build();
  }

  /** 200 + JSON + within the soft response-time SLA. */
  public static ResponseSpecification okJson() {
    return new ResponseSpecBuilder()
        .expectStatusCode(200)
        .expectContentType(ContentType.JSON)
        .expectResponseTime(
            lessThan(LichessConfig.get().maxResponseTime().toMillis()), TimeUnit.MILLISECONDS)
        .build();
  }

  /** Lichess' standard 404 shape: {@code {"error": "..."}}. */
  public static ResponseSpecification notFoundJson() {
    return new ResponseSpecBuilder()
        .expectStatusCode(404)
        .expectContentType(ContentType.JSON)
        .build();
  }
}
