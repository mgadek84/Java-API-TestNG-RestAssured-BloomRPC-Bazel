package io.github.mgadek84.lichess.api.client;

import static io.restassured.RestAssured.given;

import io.github.mgadek84.lichess.api.http.RateLimitedExecutor;
import io.github.mgadek84.lichess.api.spec.LichessSpecs;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** API object for {@code GET /game/export/{gameId}}. */
public final class GamesApi {

  private final RateLimitedExecutor executor;

  GamesApi(RateLimitedExecutor executor) {
    this.executor = executor;
  }

  /** Game as JSON (content negotiation via {@code Accept: application/json}). */
  public Response exportJson(String gameId) {
    return export(LichessSpecs.base(), gameId);
  }

  /** Same game as PGN text (content negotiation via {@code Accept: application/x-chess-pgn}). */
  public Response exportPgn(String gameId) {
    return export(LichessSpecs.pgn(), gameId);
  }

  private Response export(RequestSpecification spec, String gameId) {
    return executor.execute(
        () ->
            given()
                .spec(spec)
                .pathParam("gameId", gameId)
                .queryParam("moves", true)
                .queryParam("opening", true)
                .queryParam("clocks", false)
                .queryParam("evals", false)
                .when()
                .get("/game/export/{gameId}"));
  }
}
