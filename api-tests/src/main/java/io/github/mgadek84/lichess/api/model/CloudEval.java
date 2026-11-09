package io.github.mgadek84.lichess.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** {@code GET /api/cloud-eval?fen=...}: a cached Stockfish evaluation of a position. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CloudEval(String fen, long knodes, int depth, List<Pv> pvs) {

  public CloudEval {
    pvs = pvs == null ? List.of() : List.copyOf(pvs);
  }

  /**
   * One principal variation. Exactly one of {@code cp} (centipawns, white's point of view) and
   * {@code mate} (moves to mate) is set.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Pv(String moves, Integer cp, Integer mate) {}
}
