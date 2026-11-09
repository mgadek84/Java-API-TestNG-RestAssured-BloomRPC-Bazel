package io.github.mgadek84.chess.lichess;

import java.util.Optional;

/**
 * The service's view of the Lichess API. Keeping it behind an interface lets tests swap in a fake
 * and keeps gRPC tests hermetic.
 */
public interface LichessUserClient {

  /**
   * Looks up a user by name.
   *
   * @return the user, or empty if Lichess answers 404
   * @throws LichessException for rate limiting, timeouts and other upstream failures
   */
  Optional<LichessUser> findUser(String username);
}
