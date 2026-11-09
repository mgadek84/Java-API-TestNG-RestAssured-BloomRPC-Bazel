package io.github.mgadek84.lichess.api.client;

import static io.restassured.RestAssured.given;

import io.github.mgadek84.lichess.api.http.RateLimitedExecutor;
import io.github.mgadek84.lichess.api.spec.LichessSpecs;
import io.restassured.response.Response;

/** API object for Lichess puzzle endpoints. */
public final class PuzzlesApi {

  private final RateLimitedExecutor executor;

  PuzzlesApi(RateLimitedExecutor executor) {
    this.executor = executor;
  }

  /** {@code GET /api/puzzle/{id}} - a fixed puzzle, good for exact assertions. */
  public Response getById(String puzzleId) {
    return executor.execute(
        () ->
            given()
                .spec(LichessSpecs.base())
                .pathParam("id", puzzleId)
                .when()
                .get("/api/puzzle/{id}"));
  }

  /** {@code GET /api/puzzle/daily} - changes every day, so only assert on shape. */
  public Response getDaily() {
    return executor.execute(
        () -> given().spec(LichessSpecs.base()).when().get("/api/puzzle/daily"));
  }
}
