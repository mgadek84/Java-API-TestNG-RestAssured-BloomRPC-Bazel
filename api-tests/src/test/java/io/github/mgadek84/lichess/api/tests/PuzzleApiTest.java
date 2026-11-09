package io.github.mgadek84.lichess.api.tests;

import static io.github.mgadek84.lichess.api.assertions.LichessAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.mgadek84.lichess.api.TestData;
import io.github.mgadek84.lichess.api.client.Lichess;
import io.github.mgadek84.lichess.api.model.PuzzleResponse;
import io.restassured.response.Response;
import org.testng.annotations.Test;

public class PuzzleApiTest {

  private static final String UCI_MOVE = "[a-h][1-8][a-h][1-8][qrbn]?";

  @Test(groups = {"smoke", "lichess"})
  public void fixedPuzzleHasExpectedSolutionAndThemes() {
    Response response = Lichess.puzzles().getById(TestData.KNOWN_PUZZLE_ID);

    assertThat(response).hasStatus(200).hasJsonBody();
    PuzzleResponse body = response.as(PuzzleResponse.class);
    assertThat(body.puzzle().id()).isEqualTo(TestData.KNOWN_PUZZLE_ID);
    assertThat(body.puzzle().solution()).containsExactly("g4e6", "f7e6", "d3g6");
    assertThat(body.puzzle().themes()).contains("mateIn2", "sacrifice");
    assertThat(body.puzzle().fen()).isNotBlank();
    assertThat(body.game().id()).isEqualTo(TestData.KNOWN_GAME_ID);
    // Rating and play count change over time, so only sanity-check them.
    assertThat(body.puzzle().rating()).isBetween(400, 3500);
    assertThat(body.puzzle().plays()).isPositive();
  }

  /** The daily puzzle changes every day: assert on shape and invariants, never on values. */
  @Test(groups = {"regression", "lichess"})
  public void dailyPuzzleHasValidShape() {
    Response response = Lichess.puzzles().getDaily();

    assertThat(response).hasStatus(200).hasJsonBody();
    PuzzleResponse body = response.as(PuzzleResponse.class);
    assertThat(body.puzzle().id()).matches("[A-Za-z0-9]{5}");
    assertThat(body.puzzle().solution()).isNotEmpty().allMatch(move -> move.matches(UCI_MOVE));
    assertThat(body.puzzle().themes()).isNotEmpty();
    assertThat(body.game().id()).matches("[A-Za-z0-9]{8}");
  }
}
