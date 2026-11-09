package io.github.mgadek84.lichess.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** {@code GET /api/puzzle/{id}} and {@code GET /api/puzzle/daily}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PuzzleResponse(PuzzleGame game, Puzzle puzzle) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PuzzleGame(String id, boolean rated, String pgn, String clock) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Puzzle(
      String id,
      int rating,
      int plays,
      int initialPly,
      String fen,
      List<String> solution,
      List<String> themes) {

    public Puzzle {
      solution = solution == null ? List.of() : List.copyOf(solution);
      themes = themes == null ? List.of() : List.copyOf(themes);
    }
  }
}
