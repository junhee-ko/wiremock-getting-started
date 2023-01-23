import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@WireMockTest(httpPort = 8080)
class StubbingTest {

  private val testClient = HttpClient.newHttpClient()

  @Test
  fun exactUrlOnly() {
    // given
    stubFor(
      get(urlEqualTo("/some/thing"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "text/plain")
            .withBody("Hello world!")
        )
    )

    // when
    val httpResponse1: HttpResponse<String> = testClient.send(
      /* request = */ HttpRequest.newBuilder()
        .uri(URI("http://localhost:8080/some/thing"))
        .GET()
        .build(),
      /* responseBodyHandler = */ HttpResponse.BodyHandlers.ofString(
      )
    )
    val httpResponse2: HttpResponse<String> = testClient.send(
      /* request = */ HttpRequest.newBuilder()
        .uri(URI("http://localhost:8080/some/thing/else"))
        .GET()
        .build()
      ,
      /* responseBodyHandler = */ HttpResponse.BodyHandlers.ofString()
    )

    // then
    assertEquals(httpResponse1.statusCode(), 200)
    assertEquals(httpResponse2.statusCode(), 404)
  }

  @Test
  fun statusMessage() {
    stubFor(
      get(urlEqualTo("/some/thing"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withStatusMessage("Everything was just fine!")
            .withHeader("Content-Type", "text/plain")
        )
    )
    // when
    val httpResponse: HttpResponse<String> = testClient.send(
      /* request = */ HttpRequest.newBuilder()
        .uri(URI("http://localhost:8080/some/thing"))
        .GET()
        .build(),
      /* responseBodyHandler = */ HttpResponse.BodyHandlers.ofString(
      )
    )

    // then
    assertEquals(httpResponse.statusCode(), 200)
  }
}
