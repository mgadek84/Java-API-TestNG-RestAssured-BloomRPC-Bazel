package io.github.mgadek84.lichess.api.tests;

import static io.github.mgadek84.lichess.api.assertions.LichessAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.mgadek84.lichess.api.TestData;
import io.github.mgadek84.lichess.api.client.Lichess;
import io.github.mgadek84.lichess.api.model.CloudEval;
import io.restassured.response.Response;
import org.testng.annotations.Test;

public class CloudEvalApiTest {

  @Test(groups = {"smoke", "lichess"})
  public void startingPositionHasDeepCloudEvaluation() {
    Response response = Lichess.cloudEval().evaluate(TestData.START_FEN, 1);

    assertThat(response).hasStatus(200).hasJsonBody();
    CloudEval eval = response.as(CloudEval.class);
    assertThat(eval.fen()).isEqualTo(TestData.START_FEN);
    assertThat(eval.depth()).isGreaterThanOrEqualTo(20);
    assertThat(eval.pvs()).isNotEmpty();
    CloudEval.Pv best = eval.pvs().get(0);
    assertThat(best.moves()).matches("([a-h][1-8][a-h][1-8][qrbn]? ?)+");
    // The starting position is roughly equal: no forced mate, small centipawn advantage.
    assertThat(best.mate()).isNull();
    assertThat(best.cp()).isBetween(-100, 100);
  }

  @Test(groups = {"negative", "lichess"})
  public void uncachedPositionReturns404() {
    Response response = Lichess.cloudEval().evaluate(TestData.UNCACHED_FEN, 1);

    assertThat(response)
        .hasStatus(404)
        .hasErrorMessage("No cloud evaluation available for that position");
  }
}
