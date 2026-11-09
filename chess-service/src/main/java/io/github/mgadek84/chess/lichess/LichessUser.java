package io.github.mgadek84.chess.lichess;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/** Subset of the JSON returned by {@code GET https://lichess.org/api/user/{username}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LichessUser(
    String id,
    String username,
    String url,
    long createdAt,
    boolean disabled,
    Map<String, Perf> perfs) {

  public LichessUser {
    perfs = perfs == null ? Map.of() : Map.copyOf(perfs);
  }

  /**
   * One entry of {@code perfs}. Rated perfs (blitz, rapid, puzzle...) have games and rating;
   * score-based modes (storm, racer, streak) have runs and score instead, so rating is null.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Perf(Integer games, Integer rating, boolean prov) {

    public boolean isRated() {
      return rating != null;
    }
  }
}
