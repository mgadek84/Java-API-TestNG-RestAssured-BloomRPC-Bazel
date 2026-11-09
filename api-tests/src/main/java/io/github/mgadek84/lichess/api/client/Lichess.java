package io.github.mgadek84.lichess.api.client;

import io.github.mgadek84.lichess.api.http.RateLimitedExecutor;

/**
 * Entry point to the Lichess API objects. All of them share one {@link RateLimitedExecutor}, so
 * rate limiting is global no matter which test (TestNG or Cucumber) sends the request.
 */
public final class Lichess {

  private static final RateLimitedExecutor EXECUTOR = RateLimitedExecutor.get();

  private Lichess() {}

  public static UsersApi users() {
    return new UsersApi(EXECUTOR);
  }

  public static GamesApi games() {
    return new GamesApi(EXECUTOR);
  }

  public static PuzzlesApi puzzles() {
    return new PuzzlesApi(EXECUTOR);
  }

  public static CloudEvalApi cloudEval() {
    return new CloudEvalApi(EXECUTOR);
  }

  public static int requestCount() {
    return EXECUTOR.requestCount();
  }
}
