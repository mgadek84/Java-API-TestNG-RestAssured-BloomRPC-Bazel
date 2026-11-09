package io.github.mgadek84.lichess.api.client;

import static io.restassured.RestAssured.given;

import io.github.mgadek84.lichess.api.http.RateLimitedExecutor;
import io.github.mgadek84.lichess.api.spec.LichessSpecs;
import io.restassured.response.Response;

/** API object for {@code GET /api/cloud-eval}. */
public final class CloudEvalApi {

  private final RateLimitedExecutor executor;

  CloudEvalApi(RateLimitedExecutor executor) {
    this.executor = executor;
  }

  /**
   * Cached engine evaluation for a FEN. Returns 404 when the position is not in the cloud cache,
   * which is common for random positions.
   */
  public Response evaluate(String fen, int multiPv) {
    return executor.execute(
        () ->
            given()
                .spec(LichessSpecs.base())
                .queryParam("fen", fen)
                .queryParam("multiPv", multiPv)
                .when()
                .get("/api/cloud-eval"));
  }
}
