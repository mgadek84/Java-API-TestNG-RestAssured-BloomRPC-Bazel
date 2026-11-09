package io.github.mgadek84.chess.server;

import io.github.mgadek84.chess.fen.FenResult;
import io.github.mgadek84.chess.fen.FenValidator;
import io.github.mgadek84.chess.lichess.LichessException;
import io.github.mgadek84.chess.lichess.LichessUser;
import io.github.mgadek84.chess.lichess.LichessUserClient;
import io.github.mgadek84.chess.v1.ChessServiceGrpc;
import io.github.mgadek84.chess.v1.Color;
import io.github.mgadek84.chess.v1.GetPlayerProfileRequest;
import io.github.mgadek84.chess.v1.PlayerProfile;
import io.github.mgadek84.chess.v1.Rating;
import io.github.mgadek84.chess.v1.ValidateFenRequest;
import io.github.mgadek84.chess.v1.ValidateFenResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.regex.Pattern;

/** gRPC implementation of {@code chess.v1.ChessService}. */
public final class ChessServiceImpl extends ChessServiceGrpc.ChessServiceImplBase {

  // Lichess usernames: 2-30 chars, letters, digits, '_' and '-'.
  private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{1,29}");

  private final FenValidator fenValidator;
  private final LichessUserClient lichess;

  public ChessServiceImpl(FenValidator fenValidator, LichessUserClient lichess) {
    this.fenValidator = fenValidator;
    this.lichess = lichess;
  }

  @Override
  public void validateFen(
      ValidateFenRequest request, StreamObserver<ValidateFenResponse> responseObserver) {
    if (request.getFen().isBlank()) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription("fen must not be empty").asRuntimeException());
      return;
    }

    FenResult result = fenValidator.validate(request.getFen());
    ValidateFenResponse.Builder response =
        ValidateFenResponse.newBuilder().setValid(result.valid()).addAllErrors(result.errors());
    if (result.valid()) {
      response
          .setSideToMove(result.sideToMove() == FenResult.Side.WHITE ? Color.WHITE : Color.BLACK)
          .setCastlingRights(result.castlingRights())
          .setEnPassantSquare(result.enPassantSquare())
          .setHalfmoveClock(result.halfmoveClock())
          .setFullmoveNumber(result.fullmoveNumber())
          .setWhitePieceCount(result.whitePieceCount())
          .setBlackPieceCount(result.blackPieceCount());
    }
    responseObserver.onNext(response.build());
    responseObserver.onCompleted();
  }

  @Override
  public void getPlayerProfile(
      GetPlayerProfileRequest request, StreamObserver<PlayerProfile> responseObserver) {
    String username = request.getUsername().strip();
    if (!USERNAME.matcher(username).matches()) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT
              .withDescription("username must be 2-30 characters: letters, digits, '_' or '-'")
              .asRuntimeException());
      return;
    }

    try {
      lichess
          .findUser(username)
          .ifPresentOrElse(
              user -> {
                responseObserver.onNext(toProto(user));
                responseObserver.onCompleted();
              },
              () ->
                  responseObserver.onError(
                      Status.NOT_FOUND
                          .withDescription("Lichess user '" + username + "' not found")
                          .asRuntimeException()));
    } catch (LichessException e) {
      Status status =
          e.reason() == LichessException.Reason.RATE_LIMITED
              ? Status.RESOURCE_EXHAUSTED
              : Status.UNAVAILABLE;
      responseObserver.onError(
          status.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  private static PlayerProfile toProto(LichessUser user) {
    PlayerProfile.Builder profile =
        PlayerProfile.newBuilder()
            .setId(nullToEmpty(user.id()))
            .setUsername(nullToEmpty(user.username()))
            .setUrl(nullToEmpty(user.url()))
            .setCreatedAtMillis(user.createdAt())
            .setDisabled(user.disabled());
    user.perfs().entrySet().stream()
        .filter(e -> e.getValue().isRated())
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            e ->
                profile.addRatings(
                    Rating.newBuilder()
                        .setPerf(e.getKey())
                        .setRating(e.getValue().rating())
                        .setGames(e.getValue().games() == null ? 0 : e.getValue().games())
                        .setProvisional(e.getValue().prov())));
    return profile.build();
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
