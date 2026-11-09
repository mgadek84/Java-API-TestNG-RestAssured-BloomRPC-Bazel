package io.github.mgadek84.chess.server;

import io.github.mgadek84.chess.fen.FenValidator;
import io.github.mgadek84.chess.lichess.HttpLichessUserClient;
import io.github.mgadek84.chess.lichess.LichessUserClient;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Runs ChessService on a plain-text (no TLS) port, for local use with BloomRPC, Postman or
 * grpcurl.
 *
 * <pre>
 *   bazel run //chess-service:server                 # listens on localhost:50051
 *   bazel run //chess-service:server -- --port=6000
 * </pre>
 */
public final class ChessServer {

  public static final int DEFAULT_PORT = 50051;

  private final Server server;

  private ChessServer(Server server) {
    this.server = server;
  }

  /** Starts a server on {@code host:port}; pass port 0 to pick a free port (handy in tests). */
  public static ChessServer start(String host, int port, LichessUserClient lichess)
      throws IOException {
    Server server =
        NettyServerBuilder.forAddress(new InetSocketAddress(host, port))
            .addService(new ChessServiceImpl(new FenValidator(), lichess))
            .build()
            .start();
    return new ChessServer(server);
  }

  public int port() {
    return server.getPort();
  }

  public void stop() throws InterruptedException {
    server.shutdown();
    if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
      server.shutdownNow();
    }
  }

  public static void main(String[] args) throws Exception {
    int port = DEFAULT_PORT;
    String host = "localhost";
    String lichessUrl = HttpLichessUserClient.DEFAULT_BASE_URL;
    for (String arg : args) {
      if (arg.startsWith("--port=")) {
        port = Integer.parseInt(arg.substring("--port=".length()));
      } else if (arg.startsWith("--host=")) {
        host = arg.substring("--host=".length());
      } else if (arg.startsWith("--lichess-url=")) {
        lichessUrl = arg.substring("--lichess-url=".length());
      } else {
        System.err.println("Unknown argument: " + arg);
        System.err.println("Usage: server [--port=50051] [--host=localhost] [--lichess-url=URL]");
        System.exit(2);
      }
    }

    ChessServer chess = start(host, port, new HttpLichessUserClient(lichessUrl));
    System.out.printf(
        "ChessService listening on %s:%d (plaintext). Ctrl+C to stop.%n", host, chess.port());
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    chess.stop();
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }));
    chess.server.awaitTermination();
  }
}
