package io.github.mgadek84.bdd.steps;

import static io.github.mgadek84.lichess.api.assertions.LichessAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.mgadek84.bdd.support.ScenarioContext;
import io.github.mgadek84.lichess.api.TestData;
import io.github.mgadek84.lichess.api.client.Lichess;
import io.github.mgadek84.lichess.api.model.CloudEval;
import io.github.mgadek84.lichess.api.model.User;

/** REST steps: thin glue over the same API objects and assertions the TestNG tests use. */
public class LichessRestSteps {

  private final ScenarioContext context;

  public LichessRestSteps(ScenarioContext context) {
    this.context = context;
  }

  @When("I request the Lichess profile of {string}")
  public void requestProfile(String username) {
    context.lastRestResponse = Lichess.users().getProfile(username);
  }

  @Then("the response status is {int}")
  public void responseStatusIs(int status) {
    assertThat(context.lastRestResponse).hasStatus(status);
  }

  @And("the profile username is {string}")
  public void profileUsernameIs(String username) {
    assertThat(context.lastRestResponse.as(User.class)).hasUsernameIgnoringCase(username);
  }

  @And("the player has an established {string} rating")
  public void playerHasEstablishedRating(String perf) {
    assertThat(context.lastRestResponse.as(User.class)).hasEstablishedRating(perf);
  }

  @And("the Lichess error message is {string}")
  public void errorMessageIs(String message) {
    assertThat(context.lastRestResponse).hasErrorMessage(message);
  }

  @Given("the starting position")
  public void theStartingPosition() {
    context.fen = TestData.START_FEN;
  }

  @Given("the position {string}")
  public void thePosition(String fen) {
    context.fen = fen;
  }

  @When("I ask Lichess for a cloud evaluation")
  public void askForCloudEvaluation() {
    context.lastRestResponse = Lichess.cloudEval().evaluate(context.fen, 1);
  }

  @And("the evaluation depth is at least {int}")
  public void evaluationDepthIsAtLeast(int depth) {
    assertThat(cloudEval().depth()).isGreaterThanOrEqualTo(depth);
  }

  @And("the best line is not a forced mate")
  public void bestLineIsNotForcedMate() {
    assertThat(cloudEval().pvs().get(0).mate()).isNull();
  }

  @And("the evaluation is between {int} and {int} centipawns")
  public void evaluationIsBetween(int min, int max) {
    assertThat(cloudEval().pvs().get(0).cp()).isBetween(min, max);
  }

  private CloudEval cloudEval() {
    CloudEval eval = context.lastRestResponse.as(CloudEval.class);
    assertThat(eval.pvs()).as("principal variations").isNotEmpty();
    return eval;
  }
}
