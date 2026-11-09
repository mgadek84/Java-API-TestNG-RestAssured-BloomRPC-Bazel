@grpc @lichess
Feature: Player profile gateway
  ChessService.GetPlayerProfile wraps the Lichess REST API.
  The gateway must return the same data as the upstream API it wraps.

  Background:
    Given the chess gRPC service is running

  Scenario: gRPC profile matches the Lichess REST profile
    When I request the Lichess profile of "thibault"
    And I request the player profile of "thibault" over gRPC
    Then the gRPC profile matches the REST profile

  Scenario: An unknown player maps to NOT_FOUND
    When I request the player profile of "no-such-user-lichess-qa-zz9" over gRPC
    Then the gRPC call fails with status NOT_FOUND
