package io.github.mgadek84.virgin;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import org.testng.annotations.Test;

/**
 * The passenger form config the Virgin Australia booking UI loads while you search a fare.
 *
 * <p>The path contains the booking-app build id ({@code 7.0.57-1557...}). When Virgin ships a new
 * build this URL starts returning 404 and the constant below has to move with it. Found from the
 * browser while searching SYD to Brisbane: DevTools, the {@code passengerConfiguration.json}
 * request on {@code book.virginaustralia.com}.
 */
public class PassengerConfigurationTest {

  static final String PASSENGER_CONFIGURATION_URL =
      "https://book.virginaustralia.com/dx/VADX/7.0.57-1557.R17.4.0-1-PROD"
          + "/data/features/passengerConfiguration.json";

  @Test(groups = {"contract", "virgin"})
  public void passengerConfigurationIsJsonWithAdultFormRules() {
    given()
        .header("Accept", "application/json")
        .header("User-Agent", "virgin-booking-qa/0.1 (interview practice)")
        .when()
        .get(PASSENGER_CONFIGURATION_URL)
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("adultPassengerType", equalTo("ADT"))
        .body("nextPage", equalTo("/ancillaries"))
        .body("personNameSortOrder", contains("prefix", "firstName", "middleName", "lastName"))
        .body("passengerForm.ADT.dateOfBirth.ageRangeMin", equalTo(12))
        .body("passengerForm.ADT.dateOfBirth.ageRangeMax", equalTo(106))
        .body("vaPassengersValidation.unaccompaniedMinor.age", equalTo(15))
        .body("vaPassengersValidation.unaccompaniedMinor.rule", equalTo("OPERATING_CARRIER_15_UMNR"));
  }
}
