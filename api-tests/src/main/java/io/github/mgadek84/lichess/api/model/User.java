package io.github.mgadek84.lichess.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * {@code GET /api/user/{username}}. Only the fields the tests care about are mapped; unknown
 * fields are ignored so new Lichess fields don't break deserialization (the JSON schema test is
 * where the contract is checked).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record User(
    String id,
    String username,
    String url,
    long createdAt,
    long seenAt,
    boolean disabled,
    Map<String, Perf> perfs,
    Count count) {

  public User {
    perfs = perfs == null ? Map.of() : Map.copyOf(perfs);
  }

  /**
   * One entry of {@code perfs}. Rated perfs (blitz, rapid, puzzle...) carry a Glicko-2 rating;
   * score-based modes (storm, racer, streak) send {@code runs}/{@code score} instead, so their
   * rating is null.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Perf(int games, Integer rating, int rd, int prog, boolean prov) {

    public boolean isRated() {
      return rating != null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Count(int all, int rated, int win, int loss, int draw) {}
}
