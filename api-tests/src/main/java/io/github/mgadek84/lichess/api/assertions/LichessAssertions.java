package io.github.mgadek84.lichess.api.assertions;

import io.github.mgadek84.lichess.api.model.User;
import io.restassured.response.Response;

/**
 * Single static-import entry point for custom assertions:
 *
 * <pre>
 *   import static io.github.mgadek84.lichess.api.assertions.LichessAssertions.assertThat;
 *   assertThat(response).hasStatus(200).hasJsonBody();
 *   assertThat(user).hasEstablishedRating("blitz");
 * </pre>
 */
public final class LichessAssertions {

  private LichessAssertions() {}

  public static ResponseAssert assertThat(Response actual) {
    return new ResponseAssert(actual);
  }

  public static UserAssert assertThat(User actual) {
    return new UserAssert(actual);
  }
}
