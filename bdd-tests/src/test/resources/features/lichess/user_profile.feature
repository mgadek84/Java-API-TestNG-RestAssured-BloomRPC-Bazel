@lichess
Feature: Lichess player profile
  As a chess player
  I want to look up anyone's public Lichess profile
  So that I can see how strong they are before a game

  Scenario: Look up an existing player
    When I request the Lichess profile of "thibault"
    Then the response status is 200
    And the profile username is "thibault"
    And the player has an established "blitz" rating

  Scenario: Look up a player that does not exist
    When I request the Lichess profile of "no-such-user-lichess-qa-zz9"
    Then the response status is 404
    And the Lichess error message is "Not found"
