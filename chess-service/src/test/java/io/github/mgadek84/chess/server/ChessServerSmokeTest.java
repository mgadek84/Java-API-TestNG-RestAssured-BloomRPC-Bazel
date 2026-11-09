package io.github.mgadek84.chess.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.mgadek84.chess.v1.ChessServiceGrpc;
import io.github.mgadek84.chess.v1.ValidateFenRequest;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Starts the real Netty server on a free port and calls it over TCP, like BloomRPC does. */
@Test(groups = "grpc")
public class ChessServerSmokeTest {

  private ChessServer server;
  private ManagedChannel channel;

  @BeforeClass
  public void start() throws Exception {
    server = ChessServer.start("localhost", 0, new FakeLichessUserClient());
    channel =
        Grpc.newChannelBuilderForAddress(
                "localhost", server.port(), InsecureChannelCredentials.create())
            .build();
  }

  @AfterClass(alwaysRun = true)
  public void stop() throws Exception {
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (server != null) {
      server.stop();
    }
  }

  public void serverAnswersOverTcp() {
    var response =
        ChessServiceGrpc.newBlockingStub(channel)
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .validateFen(
                ValidateFenRequest.newBuilder().setFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1").build());

    assertThat(response.getValid()).isTrue();
    assertThat(response.getWhitePieceCount()).isEqualTo(1);
  }
}
