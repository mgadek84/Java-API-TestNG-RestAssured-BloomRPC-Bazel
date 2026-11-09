@lichess
Feature: Cloud engine evaluation
  Lichess caches deep Stockfish evaluations of popular positions.

  Scenario: The starting position is evaluated as roughly equal
    Given the starting position
    When I ask Lichess for a cloud evaluation
    Then the response status is 200
    And the evaluation depth is at least 20
    And the best line is not a forced mate
    And the evaluation is between -100 and 100 centipawns

  Scenario: A position that is not in the cache
    Given the position "8/8/8/8/8/8/8/K6k w - - 0 1"
    When I ask Lichess for a cloud evaluation
    Then the response status is 404
