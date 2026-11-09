package io.github.mgadek84.lichess.api.assertions;

import io.github.mgadek84.lichess.api.model.User;
import java.util.Locale;
import java.util.Map;
import org.assertj.core.api.AbstractAssert;

/** Domain assertions for a Lichess {@link User}: tests read like the business rule. */
public final class UserAssert extends AbstractAssert<UserAssert, User> {

  UserAssert(User actual) {
    super(actual, UserAssert.class);
  }

  public UserAssert hasUsernameIgnoringCase(String expected) {
    isNotNull();
    if (!expected.equalsIgnoreCase(actual.username())) {
      failWithMessage("Expected username <%s> but was <%s>", expected, actual.username());
    }
    String expectedId = expected.toLowerCase(Locale.ROOT);
    if (!expectedId.equals(actual.id())) {
      failWithMessage(
          "Expected id to be lower-case username <%s> but was <%s>", expectedId, actual.id());
    }
    return this;
  }

  public UserAssert isActive() {
    isNotNull();
    if (actual.disabled()) {
      failWithMessage(
          "Expected user <%s> to be active but the account is disabled", actual.username());
    }
    return this;
  }

  public UserAssert hasProfileUrl() {
    isNotNull();
    String expected = "https://lichess.org/@/" + actual.username();
    if (!expected.equals(actual.url())) {
      failWithMessage("Expected profile URL <%s> but was <%s>", expected, actual.url());
    }
    return this;
  }

  /** The player has played rated games in {@code perf} and has a plausible Glicko-2 rating. */
  public UserAssert hasEstablishedRating(String perf) {
    isNotNull();
    User.Perf p = actual.perfs().get(perf);
    if (p == null || !p.isRated()) {
      failWithMessage(
          "Expected a <%s> rating but rated perfs were %s",
          perf,
          actual.perfs().entrySet().stream()
              .filter(e -> e.getValue().isRated())
              .map(Map.Entry::getKey)
              .sorted()
              .toList());
      return this;
    }
    if (p.games() <= 0) {
      failWithMessage("Expected <%s> games > 0 but was %d", perf, p.games());
    }
    if (p.rating() < 400 || p.rating() > 3500) {
      failWithMessage("Expected <%s> rating within 400..3500 but was %d", perf, p.rating());
    }
    return this;
  }

  public UserAssert hasConsistentGameCounts() {
    isNotNull();
    User.Count c = actual.count();
    if (c == null) {
      failWithMessage("Expected game counts to be present");
      return this;
    }
    if (c.win() + c.loss() + c.draw() > c.all()) {
      failWithMessage(
          "win + loss + draw (%d) must not exceed all games (%d)",
          c.win() + c.loss() + c.draw(),
          c.all());
    }
    if (c.rated() > c.all()) {
      failWithMessage("rated games (%d) must not exceed all games (%d)", c.rated(), c.all());
    }
    return this;
  }
}
