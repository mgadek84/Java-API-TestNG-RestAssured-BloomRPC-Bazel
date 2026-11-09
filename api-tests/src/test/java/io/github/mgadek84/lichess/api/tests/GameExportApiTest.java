package io.github.mgadek84.lichess.api.tests;

import static io.github.mgadek84.lichess.api.assertions.LichessAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.mgadek84.lichess.api.TestData;
import io.github.mgadek84.lichess.api.client.Lichess;
import io.github.mgadek84.lichess.api.model.Game;
import io.restassured.response.Response;
import org.testng.annotations.Test;

public class GameExportApiTest {

  @Test(groups = {"smoke", "lichess"})
  public void exportsFinishedGameAsJson() {
    Response response = Lichess.games().exportJson(TestData.KNOWN_GAME_ID);

    assertThat(response).hasStatus(200).hasJsonBody();
    Game game = response.as(Game.class);
    assertThat(game.id()).isEqualTo(TestData.KNOWN_GAME_ID);
    assertThat(game.rated()).isTrue();
    assertThat(game.speed()).isEqualTo("blitz");
    assertThat(game.status()).isEqualTo("mate");
    assertThat(game.winner()).isEqualTo("white");
    assertThat(game.players().white().user().id()).isEqualTo("mando_emiliano");
    assertThat(game.players().black().user().id()).isEqualTo("taufikbra");
    assertThat(game.opening().eco()).isEqualTo("C05");
    assertThat(game.opening().name()).startsWith("French Defense");
    assertThat(game.clock().initial()).isEqualTo(180);
    assertThat(game.clock().increment()).isEqualTo(2);
    assertThat(game.moves()).startsWith("e4 e6 d4 d5").endsWith("Qxb7#");
  }

  /** Same resource, different representation: content negotiation via the Accept header. */
  @Test(groups = {"regression", "lichess"})
  public void exportsSameGameAsPgnWhenAskedForPgn() {
    Response response = Lichess.games().exportPgn(TestData.KNOWN_GAME_ID);

    assertThat(response).hasStatus(200).hasContentType("application/x-chess-pgn");
    String pgn = response.asString();
    assertThat(pgn)
        .contains("[Site \"https://lichess.org/game/" + TestData.KNOWN_GAME_ID + "\"]")
        .contains("[Result \"1-0\"]")
        .contains("[ECO \"C05\"]")
        .contains("[TimeControl \"180+2\"]")
        .contains("1. e4 e6 2. d4 d5");
  }

  @Test(groups = {"negative", "lichess"})
  public void unknownGameReturns404() {
    Response response = Lichess.games().exportJson(TestData.UNKNOWN_GAME_ID);

    assertThat(response).hasStatus(404).hasErrorMessage("Not found");
  }
}
