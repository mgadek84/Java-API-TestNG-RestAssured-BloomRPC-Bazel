package io.github.mgadek84.lichess.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** {@code GET /game/export/{id}} with {@code Accept: application/json}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Game(
    String id,
    boolean rated,
    String variant,
    String speed,
    String status,
    String winner,
    Players players,
    Opening opening,
    String moves,
    Clock clock) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Players(Player white, Player black) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Player(LightUser user, int rating, int ratingDiff) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record LightUser(String id, String name) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Opening(String eco, String name, int ply) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Clock(int initial, int increment, int totalTime) {}
}
