package io.github.mgadek84.bdd.steps;

import io.cucumber.java.AfterAll;
import io.github.mgadek84.bdd.support.ChessGrpcClient;

public class Hooks {

  @AfterAll
  public static void stopGrpc() throws Exception {
    ChessGrpcClient.stop();
  }
}
