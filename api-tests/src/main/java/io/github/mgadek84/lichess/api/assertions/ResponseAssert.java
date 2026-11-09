package io.github.mgadek84.lichess.api.assertions;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

import io.github.mgadek84.lichess.api.config.LichessConfig;
import io.github.mgadek84.lichess.api.model.ApiError;
import io.restassured.response.Response;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

/** Fluent, HTTP-level assertions on a RestAssured {@link Response}. */
public final class ResponseAssert extends AbstractAssert<ResponseAssert, Response> {

  ResponseAssert(Response actual) {
    super(actual, ResponseAssert.class);
  }

  public ResponseAssert hasStatus(int expected) {
    isNotNull();
    if (actual.statusCode() != expected) {
      failWithMessage(
          "Expected HTTP %d but was %d.%nBody: %s",
          expected, actual.statusCode(), abbreviate(actual.asString()));
    }
    return this;
  }

  public ResponseAssert hasContentType(String expectedPrefix) {
    isNotNull();
    String contentType = actual.contentType();
    if (contentType == null || !contentType.startsWith(expectedPrefix)) {
      failWithMessage("Expected Content-Type %s but was %s", expectedPrefix, contentType);
    }
    return this;
  }

  public ResponseAssert hasJsonBody() {
    return hasContentType("application/json");
  }

  /** Soft SLA from {@code lichess.maxResponseTimeMillis}. */
  public ResponseAssert respondedWithinSla() {
    isNotNull();
    long limit = LichessConfig.get().maxResponseTime().toMillis();
    if (actual.time() > limit) {
      failWithMessage("Expected response within %d ms but took %d ms", limit, actual.time());
    }
    return this;
  }

  /** Validates the body against a JSON Schema file on the classpath, e.g. "schemas/user.json". */
  public ResponseAssert matchesJsonSchema(String classpathSchema) {
    isNotNull();
    Matcher<?> matcher = matchesJsonSchemaInClasspath(classpathSchema);
    String body = actual.asString();
    if (!matcher.matches(body)) {
      StringDescription report = new StringDescription();
      matcher.describeMismatch(body, report);
      failWithMessage(
          "Body does not match JSON schema %s.%n%s", classpathSchema, abbreviate(report.toString()));
    }
    return this;
  }

  /** Lichess errors look like {@code {"error": "Not found"}}. */
  public ResponseAssert hasErrorMessage(String expected) {
    hasJsonBody();
    Assertions.assertThat(actual.as(ApiError.class).error())
        .as("Lichess error message")
        .isEqualTo(expected);
    return this;
  }

  private static String abbreviate(String s) {
    return s.length() <= 2000 ? s : s.substring(0, 2000) + "...";
  }
}
