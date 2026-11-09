package io.github.mgadek84.lichess.api;

/**
 * Stable, public test data on lichess.org. Prefer old, immutable resources (finished games,
 * published puzzles) over "today" data so assertions can be exact.
 */
public final class TestData {

  private TestData() {}

  /** Lichess founder; account exists since 2010 and is active. */
  public static final String KNOWN_USER = "thibault";

  public static final String UNKNOWN_USER = "no-such-user-lichess-qa-zz9";

  /** A finished rated blitz game (2024-03-10), French Defense, white wins by mate. */
  public static final String KNOWN_GAME_ID = "F3H4Ogo2";

  public static final String UNKNOWN_GAME_ID = "zzzzzzzz";

  /** Mate-in-2 puzzle taken from {@link #KNOWN_GAME_ID}. */
  public static final String KNOWN_PUZZLE_ID = "pnNfB";

  public static final String START_FEN =
      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  /** Bare kings: a legal position that is never stored in the cloud evaluation cache. */
  public static final String UNCACHED_FEN = "8/8/8/8/8/8/8/K6k w - - 0 1";
}
