package io.github.mgadek84.bdd.support;

import io.github.mgadek84.chess.lichess.HttpLichessUserClient;
import io.github.mgadek84.chess.server.ChessServer;
import io.github.mgadek84.chess.v1.ChessServiceGrpc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import java.util.concurrent.TimeUnit;

/**
 * One gRPC channel for the whole test run.
 *
 * <p>By default it starts ChessService in-process on a free port. Set {@code
 * -Dchess.grpc.target=host:port} to run the same scenarios against an already running server
 * instead (e.g. one started with {@code bazel run //chess-service:server}).
 */
public final class ChessGrpcClient {

  private static ChessServer embeddedServer;
  private static ManagedChannel channel;

  private ChessGrpcClient() {}

  public static synchronized void start() throws Exception {
    if (channel != null) {
      return;
    }
    String target = System.getProperty("chess.grpc.target");
    if (target == null || target.isBlank()) {
      embeddedServer =
          ChessServer.start(
              "localhost", 0, new HttpLichessUserClient(HttpLichessUserClient.DEFAULT_BASE_URL));
      target = "localhost:" + embeddedServer.port();
    }
    channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
  }

  public static synchronized void stop() throws Exception {
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      channel = null;
    }
    if (embeddedServer != null) {
      embeddedServer.stop();
      embeddedServer = null;
    }
  }

  public static ChessServiceGrpc.ChessServiceBlockingStub stub() {
    if (channel == null) {
      throw new IllegalStateException("gRPC client not started");
    }
    return ChessServiceGrpc.newBlockingStub(channel).withDeadlineAfter(15, TimeUnit.SECONDS);
  }
}
