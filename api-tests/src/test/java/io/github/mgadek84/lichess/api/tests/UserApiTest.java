package io.github.mgadek84.lichess.api.tests;

import static io.github.mgadek84.lichess.api.assertions.LichessAssertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import io.github.mgadek84.lichess.api.TestData;
import io.github.mgadek84.lichess.api.client.Lichess;
import io.github.mgadek84.lichess.api.model.User;
import io.github.mgadek84.lichess.api.spec.LichessSpecs;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UserApiTest {

  private Response knownUser;

  /** One request shared by several focused assertions keeps the Lichess request count low. */
  @BeforeClass
  public void fetchKnownUser() {
    knownUser = Lichess.users().getProfile(TestData.KNOWN_USER);
  }

  @Test(groups = {"smoke", "lichess"})
  public void knownUserReturns200WithJson() {
    assertThat(knownUser).hasStatus(200).hasJsonBody().respondedWithinSla();
  }

  @Test(groups = {"contract", "lichess"}, dependsOnMethods = "knownUserReturns200WithJson")
  public void knownUserMatchesJsonSchema() {
    assertThat(knownUser).matchesJsonSchema("schemas/user-profile.schema.json");
  }

  /** RestAssured's own fluent validation style (Hamcrest matchers on JSON paths). */
  @Test(groups = {"regression", "lichess"}, dependsOnMethods = "knownUserReturns200WithJson")
  public void knownUserFieldsWithRestAssuredDsl() {
    knownUser
        .then()
        .spec(LichessSpecs.okJson())
        .body("id", equalTo(TestData.KNOWN_USER))
        .body("url", equalTo("https://lichess.org/@/" + TestData.KNOWN_USER))
        .body("perfs.blitz.games", greaterThan(0));
  }

  /** POJO style: deserialize once, then use domain assertions. */
  @Test(groups = {"regression", "lichess"}, dependsOnMethods = "knownUserReturns200WithJson")
  public void knownUserAsPojoWithDomainAssertions() {
    User user = knownUser.as(User.class);

    assertThat(user)
        .hasUsernameIgnoringCase(TestData.KNOWN_USER)
        .isActive()
        .hasProfileUrl()
        .hasEstablishedRating("blitz")
        .hasConsistentGameCounts();
  }

  @Test(groups = {"negative", "lichess"})
  public void unknownUserReturns404WithErrorBody() {
    Response response = Lichess.users().getProfile(TestData.UNKNOWN_USER);

    assertThat(response).hasStatus(404).hasErrorMessage("Not found");
  }
}
