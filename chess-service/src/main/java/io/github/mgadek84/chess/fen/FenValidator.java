package io.github.mgadek84.chess.fen;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Structural validation of FEN (Forsyth-Edwards Notation) strings.
 *
 * <p>It checks syntax and basic sanity (one king per side, no pawns on the back ranks, consistent
 * en passant square), not full legality such as "the side not to move is in check".
 */
public final class FenValidator {

  private static final Pattern CASTLING = Pattern.compile("-|K?Q?k?q?");
  private static final Pattern EN_PASSANT = Pattern.compile("-|[a-h][36]");
  private static final Pattern NON_NEGATIVE_INT = Pattern.compile("\\d{1,4}");

  public FenResult validate(String fen) {
    List<String> errors = new ArrayList<>();
    if (fen == null || fen.isBlank()) {
      errors.add("FEN is empty");
      return FenResult.invalid(errors);
    }

    String[] fields = fen.strip().split(" ", -1);
    if (fields.length != 6) {
      errors.add("FEN must have 6 space-separated fields, got " + fields.length);
      return FenResult.invalid(errors);
    }

    int[] pieceCounts = validatePlacement(fields[0], errors);

    String side = fields[1];
    if (!side.equals("w") && !side.equals("b")) {
      errors.add("Side to move must be 'w' or 'b', got '" + side + "'");
    }

    String castling = fields[2];
    if (castling.isEmpty() || !CASTLING.matcher(castling).matches()) {
      errors.add(
          "Castling rights must be '-' or a subset of 'KQkq' in that order, got '"
              + castling
              + "'");
    }

    String enPassant = fields[3];
    if (!EN_PASSANT.matcher(enPassant).matches()) {
      errors.add(
          "En passant square must be '-' or a square on rank 3 or 6, got '" + enPassant + "'");
    } else if (!enPassant.equals("-")) {
      char rank = enPassant.charAt(1);
      if ((side.equals("w") && rank != '6') || (side.equals("b") && rank != '3')) {
        errors.add(
            "En passant square " + enPassant + " is impossible when '" + side + "' is to move");
      }
    }

    int halfmove = parseCounter(fields[4], "Halfmove clock", 0, errors);
    int fullmove = parseCounter(fields[5], "Fullmove number", 1, errors);

    if (!errors.isEmpty()) {
      return FenResult.invalid(errors);
    }
    return new FenResult(
        true,
        List.of(),
        side.equals("w") ? FenResult.Side.WHITE : FenResult.Side.BLACK,
        castling,
        enPassant,
        halfmove,
        fullmove,
        pieceCounts[0],
        pieceCounts[1]);
  }

  /** Returns {white pieces, black pieces}. */
  private static int[] validatePlacement(String placement, List<String> errors) {
    String[] ranks = placement.split("/", -1);
    if (ranks.length != 8) {
      errors.add("Piece placement must have 8 ranks separated by '/', got " + ranks.length);
      return new int[] {0, 0};
    }

    int white = 0;
    int black = 0;
    int whiteKings = 0;
    int blackKings = 0;
    int whitePawns = 0;
    int blackPawns = 0;

    for (int i = 0; i < ranks.length; i++) {
      int rankNumber = 8 - i;
      String rank = ranks[i];
      int squares = 0;
      boolean previousWasDigit = false;
      for (char c : rank.toCharArray()) {
        if (c >= '1' && c <= '8') {
          if (previousWasDigit) {
            errors.add("Rank " + rankNumber + " has two consecutive digits");
          }
          squares += c - '0';
          previousWasDigit = true;
          continue;
        }
        previousWasDigit = false;
        if ("pnbrqkPNBRQK".indexOf(c) < 0) {
          errors.add("Rank " + rankNumber + " contains invalid character '" + c + "'");
          continue;
        }
        squares++;
        if (Character.isUpperCase(c)) {
          white++;
        } else {
          black++;
        }
        switch (c) {
          case 'K' -> whiteKings++;
          case 'k' -> blackKings++;
          case 'P' -> whitePawns++;
          case 'p' -> blackPawns++;
          default -> {}
        }
        if ((c == 'P' || c == 'p') && (rankNumber == 1 || rankNumber == 8)) {
          errors.add("Pawn on rank " + rankNumber + " is not possible");
        }
      }
      if (squares != 8) {
        errors.add("Rank " + rankNumber + " describes " + squares + " squares instead of 8");
      }
    }

    if (whiteKings != 1) {
      errors.add("White must have exactly one king, found " + whiteKings);
    }
    if (blackKings != 1) {
      errors.add("Black must have exactly one king, found " + blackKings);
    }
    if (whitePawns > 8) {
      errors.add("White has more than 8 pawns");
    }
    if (blackPawns > 8) {
      errors.add("Black has more than 8 pawns");
    }
    return new int[] {white, black};
  }

  private static int parseCounter(String value, String name, int min, List<String> errors) {
    if (!NON_NEGATIVE_INT.matcher(value).matches() || Integer.parseInt(value) < min) {
      errors.add(name + " must be an integer >= " + min + ", got '" + value + "'");
      return 0;
    }
    return Integer.parseInt(value);
  }
}
