package io.github.mgadek84.lichess.api.client;

import static io.restassured.RestAssured.given;

import io.github.mgadek84.lichess.api.http.RateLimitedExecutor;
import io.github.mgadek84.lichess.api.spec.LichessSpecs;
import io.restassured.response.Response;

/** API object for Lichess user endpoints. Returns raw responses; tests decide what to assert. */
public final class UsersApi {

  private final RateLimitedExecutor executor;

  UsersApi(RateLimitedExecutor executor) {
    this.executor = executor;
  }

  /** {@code GET /api/user/{username}} - public profile, ratings and game counts. */
  public Response getProfile(String username) {
    return executor.execute(
        () ->
            given()
                .spec(LichessSpecs.base())
                .pathParam("username", username)
                .when()
                .get("/api/user/{username}"));
  }
}
