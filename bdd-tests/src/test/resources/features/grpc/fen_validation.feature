@grpc
Feature: FEN validation over gRPC
  ChessService.ValidateFen checks a position written in Forsyth-Edwards Notation.
  A malformed FEN is a normal answer (valid = false with reasons),
  only an empty request is a gRPC error.

  Background:
    Given the chess gRPC service is running

  Scenario: The starting position is valid
    When I validate the FEN "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    Then the FEN is valid
    And it is WHITE to move
    And each side has 16 pieces

  Scenario: Black to move after 1. e4 with an en passant square
    When I validate the FEN "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
    Then the FEN is valid
    And it is BLACK to move
    And the en passant square is "e3"

  Scenario Outline: Broken FENs are rejected with a reason
    When I validate the FEN "<fen>"
    Then the FEN is invalid
    And one of the errors mentions "<reason>"

    Examples:
      | fen                                                         | reason                           |
      | 8/8/8/8/8/8/8/8 w - - 0 1                                   | White must have exactly one king |
      | rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1    | Side to move                     |
      | rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 x  | 6 space-separated fields         |
      | Pnbqkbnr/pppppppp/8/8/8/8/1PPPPPPP/RNBQKBNR w KQkq - 0 1    | Pawn on rank 8                   |

  Scenario: An empty FEN is a bad request
    When I validate the FEN ""
    Then the gRPC call fails with status INVALID_ARGUMENT
