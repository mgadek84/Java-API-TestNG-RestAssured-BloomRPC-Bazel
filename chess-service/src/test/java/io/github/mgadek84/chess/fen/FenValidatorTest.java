package io.github.mgadek84.chess.fen;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class FenValidatorTest {

  private static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

  private final FenValidator validator = new FenValidator();

  public void startingPositionIsValid() {
    FenResult result = validator.validate(START);

    assertThat(result.valid()).isTrue();
    assertThat(result.errors()).isEmpty();
    assertThat(result.sideToMove()).isEqualTo(FenResult.Side.WHITE);
    assertThat(result.castlingRights()).isEqualTo("KQkq");
    assertThat(result.enPassantSquare()).isEqualTo("-");
    assertThat(result.fullmoveNumber()).isEqualTo(1);
    assertThat(result.whitePieceCount()).isEqualTo(16);
    assertThat(result.blackPieceCount()).isEqualTo(16);
  }

  public void positionAfterOneEFourIsValidWithEnPassantSquare() {
    FenResult result =
        validator.validate("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");

    assertThat(result.valid()).isTrue();
    assertThat(result.sideToMove()).isEqualTo(FenResult.Side.BLACK);
    assertThat(result.enPassantSquare()).isEqualTo("e3");
  }

  @DataProvider
  public Object[][] invalidFens() {
    return new Object[][] {
      {"", "FEN is empty"},
      {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -", "6 space-separated fields"},
      {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1", "8 ranks"},
      {"rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "invalid character '9'"},
      {"rnbqkbnr/ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "7 squares instead of 8"},
      {"rnbqkbnr/pppppppp/44/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "consecutive digits"},
      {"rnbq1bnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "Black must have exactly one king"},
      {"Pnbqkbnr/pppppppp/8/8/8/8/1PPPPPPP/RNBQKBNR w KQkq - 0 1", "Pawn on rank 8"},
      {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1", "Side to move"},
      {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w QK - 0 1", "Castling rights"},
      {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e3 0 1", "impossible when 'w'"},
      {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - -1 1", "Halfmove clock"},
      {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 0", "Fullmove number"},
    };
  }

  @Test(dataProvider = "invalidFens")
  public void invalidFenIsRejectedWithReason(String fen, String expectedError) {
    FenResult result = validator.validate(fen);

    assertThat(result.valid()).as("valid flag for %s", fen).isFalse();
    assertThat(result.errors()).anySatisfy(error -> assertThat(error).contains(expectedError));
  }
}
