package io.github.mgadek84.chess.server;

import io.github.mgadek84.chess.lichess.LichessException;
import io.github.mgadek84.chess.lichess.LichessUser;
import io.github.mgadek84.chess.lichess.LichessUserClient;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** In-memory Lichess used by gRPC tests, so they never touch the network. */
public final class FakeLichessUserClient implements LichessUserClient {

  private final Map<String, LichessUser> users = new HashMap<>();
  private LichessException nextFailure;

  public FakeLichessUserClient withUser(LichessUser user) {
    users.put(user.id(), user);
    return this;
  }

  public void failNextCallWith(LichessException failure) {
    this.nextFailure = failure;
  }

  @Override
  public Optional<LichessUser> findUser(String username) {
    if (nextFailure != null) {
      LichessException failure = nextFailure;
      nextFailure = null;
      throw failure;
    }
    return Optional.ofNullable(users.get(username.toLowerCase(Locale.ROOT)));
  }
}
