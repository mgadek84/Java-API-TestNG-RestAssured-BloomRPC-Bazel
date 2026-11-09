package io.github.mgadek84.virgin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.path.json.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.testng.annotations.Test;

/**
 * The flight search the Virgin Australia booking UI sends as GraphQL.
 *
 * <p>DevTools shows {@code POST /api/graphql}. The body asks for {@code bookingAirSearch}, and the
 * field {@code originalResponse} is the whole Sabre shopping document: fare families, offers,
 * segments, prices. That document is large and changes with every search, so the test walks a few
 * stable paths with RestAssured {@link JsonPath} instead of comparing the payload.
 *
 * <p>The call itself goes through {@link HttpClient}. RestAssured's own client is Apache
 * HttpClient, and Imperva (the booking site's firewall) answers that POST with 403. The same body
 * from the JDK client is accepted. The static {@code passengerConfiguration.json} GET is not
 * blocked, so that test stays on the RestAssured DSL.
 *
 * <p>Brisbane's airport code is {@code BNE}. The storefront header is required; without {@code
 * x-sabre-storefront: VADX} Sabre answers 400. One-way search uses {@code searchType BRANDED} and
 * omits {@code plusMinusDays}.
 */
public class AirSearchTest {

  static final String GRAPHQL_URL = "https://book.virginaustralia.com/api/graphql";

  static final String BOOKING_AIR_SEARCH =
      "query bookingAirSearch($airSearchInput: CustomAirSearchInput) {"
          + " bookingAirSearch(airSearchInput: $airSearchInput) { originalResponse } }";

  /** First segment of the first offer inside the nested shopping document. */
  static final String FIRST_SEGMENT =
      "data.bookingAirSearch.originalResponse"
          + ".unbundledOffers[0][0].itineraryPart[0].segments[0]";

  static final String FIRST_TOTAL =
      "data.bookingAirSearch.originalResponse"
          + ".unbundledOffers[0][0].total.alternatives[0][0]";

  @Test(groups = {"contract", "virgin"})
  public void airSearchReturnsSydneyToBrisbaneOffers() throws Exception {
    String departureDate = LocalDate.now(ZoneOffset.UTC).plusDays(14).toString();
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(GRAPHQL_URL))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "virgin-booking-qa/0.1 (interview practice)")
            .header("Origin", "https://book.virginaustralia.com")
            .header("x-sabre-storefront", "VADX")
            .POST(HttpRequest.BodyPublishers.ofString(searchBody(departureDate)))
            .build();

    HttpResponse<String> response;
    try (HttpClient client = HttpClient.newHttpClient()) {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    assertThat(response.statusCode(), equalTo(200));
    assertThat(response.headers().firstValue("content-type").orElse(""), containsString("json"));

    JsonPath json = JsonPath.from(response.body());
    assertThat(json.get("errors"), nullValue());
    assertThat(json.get("data.bookingAirSearch"), notNullValue());
    assertThat(json.getString("data.bookingAirSearch.originalResponse.currency"), equalTo("AUD"));
    assertThat(
        json.getInt("data.bookingAirSearch.originalResponse.unbundledOffers[0].size()"),
        greaterThan(0));
    assertThat(json.getString(FIRST_SEGMENT + ".origin"), equalTo("SYD"));
    assertThat(json.getString(FIRST_SEGMENT + ".destination"), equalTo("BNE"));
    assertThat(json.getString(FIRST_SEGMENT + ".flight.airlineCode"), equalTo("VA"));
    assertThat(json.getString(FIRST_SEGMENT + ".departure"), startsWith(departureDate));
    assertThat(json.getString(FIRST_TOTAL + ".currency"), equalTo("AUD"));
    assertThat(json.getDouble(FIRST_TOTAL + ".amount"), greaterThan(0d));
  }

  private static String searchBody(String departureDate) {
    return """
        {
          "operationName": "bookingAirSearch",
          "query": "%s",
          "variables": {
            "airSearchInput": {
              "cabinClass": "Economy",
              "awardBooking": false,
              "currency": "AUD",
              "promoCodes": [],
              "searchType": "BRANDED",
              "passengers": { "ADT": 1 },
              "itineraryParts": [
                {
                  "from": { "useNearbyLocations": false, "code": "SYD" },
                  "to": { "useNearbyLocations": false, "code": "BNE" },
                  "when": { "date": "%s" }
                }
              ]
            }
          }
        }
        """
        .formatted(BOOKING_AIR_SEARCH, departureDate);
  }
}
