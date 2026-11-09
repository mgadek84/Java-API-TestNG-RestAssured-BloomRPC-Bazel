package io.github.mgadek84.chess.fen;

import java.util.List;

/** Outcome of {@link FenValidator#validate(String)}. Position fields are only set when valid. */
public record FenResult(
    boolean valid,
    List<String> errors,
    Side sideToMove,
    String castlingRights,
    String enPassantSquare,
    int halfmoveClock,
    int fullmoveNumber,
    int whitePieceCount,
    int blackPieceCount) {

  public enum Side {
    WHITE,
    BLACK
  }

  public FenResult {
    errors = List.copyOf(errors);
  }

  static FenResult invalid(List<String> errors) {
    return new FenResult(false, errors, null, "", "", 0, 0, 0, 0);
  }
}
