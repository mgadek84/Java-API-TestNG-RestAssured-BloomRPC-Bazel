package io.github.mgadek84.chess.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.github.mgadek84.chess.fen.FenValidator;
import io.github.mgadek84.chess.lichess.LichessException;
import io.github.mgadek84.chess.lichess.LichessUser;
import io.github.mgadek84.chess.v1.ChessServiceGrpc;
import io.github.mgadek84.chess.v1.Color;
import io.github.mgadek84.chess.v1.GetPlayerProfileRequest;
import io.github.mgadek84.chess.v1.PlayerProfile;
import io.github.mgadek84.chess.v1.Rating;
import io.github.mgadek84.chess.v1.ValidateFenRequest;
import io.github.mgadek84.chess.v1.ValidateFenResponse;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Exercises the service through a real gRPC stub over the in-process transport: same
 * serialization, status codes and stubs as over the network, but no sockets and no Lichess.
 */
@Test(groups = "grpc")
public class ChessServiceGrpcTest {

  private static final LichessUser THIBAULT =
      new LichessUser(
          "thibault",
          "thibault",
          "https://lichess.org/@/thibault",
          1271349040000L,
          false,
          Map.of(
              "rapid", new LichessUser.Perf(900, 1836, false),
              "blitz", new LichessUser.Perf(11000, 1745, false),
              "storm", new LichessUser.Perf(null, null, false)));

  private final FakeLichessUserClient lichess = new FakeLichessUserClient().withUser(THIBAULT);
  private Server server;
  private ManagedChannel channel;
  private ChessServiceGrpc.ChessServiceBlockingStub stub;

  @BeforeClass
  public void startServer() throws Exception {
    String name = InProcessServerBuilder.generateName();
    server =
        InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new ChessServiceImpl(new FenValidator(), lichess))
            .build()
            .start();
    channel = InProcessChannelBuilder.forName(name).directExecutor().build();
    stub = ChessServiceGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS);
  }

  @AfterClass(alwaysRun = true)
  public void stopServer() throws Exception {
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null) {
      server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  public void validateFenDescribesStartingPosition() {
    ValidateFenResponse response =
        stub.validateFen(
            ValidateFenRequest.newBuilder()
                .setFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
                .build());

    assertThat(response.getValid()).isTrue();
    assertThat(response.getErrorsList()).isEmpty();
    assertThat(response.getSideToMove()).isEqualTo(Color.WHITE);
    assertThat(response.getCastlingRights()).isEqualTo("KQkq");
    assertThat(response.getWhitePieceCount()).isEqualTo(16);
    assertThat(response.getBlackPieceCount()).isEqualTo(16);
  }

  public void validateFenReturnsErrorsForBrokenFenButStatusOk() {
    ValidateFenResponse response =
        stub.validateFen(ValidateFenRequest.newBuilder().setFen("8/8/8 w - - 0 1").build());

    assertThat(response.getValid()).isFalse();
    assertThat(response.getErrorsList()).isNotEmpty();
    assertThat(response.getSideToMove()).isEqualTo(Color.COLOR_UNSPECIFIED);
  }

  public void validateFenRejectsEmptyFenWithInvalidArgument() {
    StatusRuntimeException error =
        catchThrowableOfType(
            StatusRuntimeException.class,
            () -> stub.validateFen(ValidateFenRequest.getDefaultInstance()));

    assertThat(error.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(error.getStatus().getDescription()).contains("fen");
  }

  public void getPlayerProfileMapsLichessUserSortsRatingsAndDropsUnratedModes() {
    PlayerProfile profile =
        stub.getPlayerProfile(GetPlayerProfileRequest.newBuilder().setUsername("Thibault").build());

    assertThat(profile.getId()).isEqualTo("thibault");
    assertThat(profile.getUrl()).isEqualTo("https://lichess.org/@/thibault");
    assertThat(profile.getRatingsList())
        .extracting(Rating::getPerf)
        .containsExactly("blitz", "rapid");
    assertThat(profile.getRatings(0).getRating()).isEqualTo(1745);
  }

  public void getPlayerProfileReturnsNotFoundForUnknownUser() {
    assertStatus(
        () -> stub.getPlayerProfile(username("no_such_player_42")), Status.Code.NOT_FOUND);
  }

  public void getPlayerProfileRejectsInvalidUsername() {
    assertStatus(() -> stub.getPlayerProfile(username("a")), Status.Code.INVALID_ARGUMENT);
    assertStatus(() -> stub.getPlayerProfile(username("bad name!")), Status.Code.INVALID_ARGUMENT);
  }

  public void getPlayerProfileMapsRateLimitToResourceExhausted() {
    lichess.failNextCallWith(
        new LichessException(LichessException.Reason.RATE_LIMITED, "HTTP 429"));

    assertStatus(
        () -> stub.getPlayerProfile(username("thibault")), Status.Code.RESOURCE_EXHAUSTED);
  }

  public void getPlayerProfileMapsUpstreamOutageToUnavailable() {
    lichess.failNextCallWith(new LichessException(LichessException.Reason.UNAVAILABLE, "down"));

    assertStatus(() -> stub.getPlayerProfile(username("thibault")), Status.Code.UNAVAILABLE);
  }

  private static GetPlayerProfileRequest username(String name) {
    return GetPlayerProfileRequest.newBuilder().setUsername(name).build();
  }

  private static void assertStatus(Runnable call, Status.Code expected) {
    StatusRuntimeException error = catchThrowableOfType(StatusRuntimeException.class, call::run);
    assertThat(error).as("expected gRPC error %s", expected).isNotNull();
    assertThat(error.getStatus().getCode()).isEqualTo(expected);
  }
}
