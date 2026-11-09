package io.github.mgadek84.chess.lichess;

/** An upstream (Lichess) failure that is not "user not found". */
public class LichessException extends RuntimeException {

  public enum Reason {
    RATE_LIMITED,
    UNAVAILABLE
  }

  private final Reason reason;

  public LichessException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public LichessException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
