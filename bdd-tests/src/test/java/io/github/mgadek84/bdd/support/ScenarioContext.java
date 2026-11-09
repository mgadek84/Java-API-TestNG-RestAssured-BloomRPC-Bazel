package io.github.mgadek84.bdd.support;

import io.grpc.StatusRuntimeException;
import io.restassured.response.Response;

/**
 * State shared between step classes within one scenario. PicoContainer creates a fresh instance
 * per scenario and injects it into every step class that asks for it in its constructor.
 */
public class ScenarioContext {

  public Response lastRestResponse;
  public Object lastGrpcResponse;
  public StatusRuntimeException lastGrpcError;
  public String fen;

  public <T> T lastGrpcResponse(Class<T> type) {
    if (lastGrpcError != null) {
      throw new AssertionError("gRPC call failed: " + lastGrpcError.getStatus(), lastGrpcError);
    }
    return type.cast(lastGrpcResponse);
  }
}
