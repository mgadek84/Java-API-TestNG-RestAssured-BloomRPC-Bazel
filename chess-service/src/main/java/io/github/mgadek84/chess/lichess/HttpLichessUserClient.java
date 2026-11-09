package io.github.mgadek84.chess.lichess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/** {@link LichessUserClient} backed by the public Lichess HTTP API (no token needed). */
public final class HttpLichessUserClient implements LichessUserClient {

  public static final String DEFAULT_BASE_URL = "https://lichess.org";
  private static final String USER_AGENT =
      "lichess-qa-chess-service/0.1 (+https://github.com/mgadek84/Java-API-TestNG-RestAssured-BloomRPC-Bazel)";

  private final HttpClient http;
  private final ObjectMapper json = new ObjectMapper();
  private final String baseUrl;

  public HttpLichessUserClient(String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  @Override
  public Optional<LichessUser> findUser(String username) {
    HttpRequest request =
        HttpRequest.newBuilder(
                URI.create(
                    baseUrl + "/api/user/" + URLEncoder.encode(username, StandardCharsets.UTF_8)))
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

    HttpResponse<String> response;
    try {
      response = http.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new LichessException(LichessException.Reason.UNAVAILABLE, "Lichess call failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new LichessException(LichessException.Reason.UNAVAILABLE, "Interrupted", e);
    }

    return switch (response.statusCode()) {
      case 200 -> Optional.of(parse(response.body()));
      case 404 -> Optional.empty();
      case 429 ->
          throw new LichessException(
              LichessException.Reason.RATE_LIMITED, "Lichess rate limit hit (HTTP 429)");
      default ->
          throw new LichessException(
              LichessException.Reason.UNAVAILABLE,
              "Unexpected Lichess status " + response.statusCode());
    };
  }

  private LichessUser parse(String body) {
    try {
      return json.readValue(body, LichessUser.class);
    } catch (IOException e) {
      throw new LichessException(
          LichessException.Reason.UNAVAILABLE, "Could not parse Lichess response", e);
    }
  }
}
