package io.github.mgadek84.bdd.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.mgadek84.bdd.support.ChessGrpcClient;
import io.github.mgadek84.bdd.support.ScenarioContext;
import io.github.mgadek84.chess.v1.Color;
import io.github.mgadek84.chess.v1.GetPlayerProfileRequest;
import io.github.mgadek84.chess.v1.PlayerProfile;
import io.github.mgadek84.chess.v1.Rating;
import io.github.mgadek84.chess.v1.ValidateFenRequest;
import io.github.mgadek84.chess.v1.ValidateFenResponse;
import io.github.mgadek84.lichess.api.model.User;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ChessGrpcSteps {

  private final ScenarioContext context;

  public ChessGrpcSteps(ScenarioContext context) {
    this.context = context;
  }

  @Given("the chess gRPC service is running")
  public void serviceIsRunning() throws Exception {
    ChessGrpcClient.start();
  }

  @When("I validate the FEN {string}")
  public void validateFen(String fen) {
    call(
        () ->
            ChessGrpcClient.stub()
                .validateFen(ValidateFenRequest.newBuilder().setFen(fen).build()));
  }

  @Then("the FEN is valid")
  public void fenIsValid() {
    ValidateFenResponse response = context.lastGrpcResponse(ValidateFenResponse.class);
    assertThat(response.getErrorsList()).isEmpty();
    assertThat(response.getValid()).isTrue();
  }

  @Then("the FEN is invalid")
  public void fenIsInvalid() {
    assertThat(context.lastGrpcResponse(ValidateFenResponse.class).getValid()).isFalse();
  }

  @And("it is {} to move")
  public void sideToMove(Color color) {
    assertThat(context.lastGrpcResponse(ValidateFenResponse.class).getSideToMove())
        .isEqualTo(color);
  }

  @And("each side has {int} pieces")
  public void eachSideHasPieces(int pieces) {
    ValidateFenResponse response = context.lastGrpcResponse(ValidateFenResponse.class);
    assertThat(response.getWhitePieceCount()).isEqualTo(pieces);
    assertThat(response.getBlackPieceCount()).isEqualTo(pieces);
  }

  @And("the en passant square is {string}")
  public void enPassantSquareIs(String square) {
    assertThat(context.lastGrpcResponse(ValidateFenResponse.class).getEnPassantSquare())
        .isEqualTo(square);
  }

  @And("one of the errors mentions {string}")
  public void oneOfTheErrorsMentions(String text) {
    assertThat(context.lastGrpcResponse(ValidateFenResponse.class).getErrorsList())
        .anySatisfy(error -> assertThat(error).contains(text));
  }

  @When("I request the player profile of {string} over gRPC")
  public void requestPlayerProfile(String username) {
    call(
        () ->
            ChessGrpcClient.stub()
                .getPlayerProfile(
                    GetPlayerProfileRequest.newBuilder().setUsername(username).build()));
  }

  @Then("the gRPC call fails with status {}")
  public void callFailsWithStatus(Status.Code code) {
    assertThat(context.lastGrpcError).as("gRPC error").isNotNull();
    assertThat(context.lastGrpcError.getStatus().getCode()).isEqualTo(code);
  }

  /** Contract between our gateway and the upstream it wraps. */
  @Then("the gRPC profile matches the REST profile")
  public void grpcProfileMatchesRestProfile() {
    PlayerProfile grpc = context.lastGrpcResponse(PlayerProfile.class);
    User rest = context.lastRestResponse.as(User.class);

    assertThat(grpc.getId()).isEqualTo(rest.id());
    assertThat(grpc.getUsername()).isEqualTo(rest.username());
    assertThat(grpc.getUrl()).isEqualTo(rest.url());
    assertThat(grpc.getCreatedAtMillis()).isEqualTo(rest.createdAt());

    Map<String, Integer> restRatings =
        rest.perfs().entrySet().stream()
            .filter(e -> e.getValue().isRated())
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().rating()));
    Map<String, Integer> grpcRatings =
        grpc.getRatingsList().stream()
            .collect(Collectors.toMap(Rating::getPerf, Rating::getRating));
    assertThat(grpcRatings).isEqualTo(restRatings);
  }

  private void call(Supplier<Object> rpc) {
    context.lastGrpcResponse = null;
    context.lastGrpcError = null;
    try {
      context.lastGrpcResponse = rpc.get();
    } catch (StatusRuntimeException e) {
      context.lastGrpcError = e;
    }
  }
}
