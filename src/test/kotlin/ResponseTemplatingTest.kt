import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset

class ResponseTemplatingTest {

  private val testClient = HttpClient.newHttpClient()
  private val wireMockServer = WireMockServer(
    WireMockConfiguration.options()
      .port(8080)
      .extensions(
        ResponseTemplateTransformer(false)
      )
  )

  @BeforeEach
  fun setUp() {
    wireMockServer.start()
  }

  @AfterEach
  fun tearDown() {
    wireMockServer.stop()
  }

  @Test
  fun helpers_JSONPath() {
    // given
    val requestBody =
      """
      {
        "test": "Stuff"
      }
    """.trimIndent()

    val responseBody =
      """
        {
          "result": "{{jsonPath request.body '$.test'}}"
        }
      """.trimIndent()

    wireMockServer.stubFor(
      WireMock.post(WireMock.urlEqualTo("/some/thing"))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(
          WireMock.aResponse()
            .withStatus(200)
            .withBody(responseBody)
            .withTransformers("response-template")
        )
    )
    wireMockServer.start()

    // when
    val httpResponse: HttpResponse<String> = testClient.send(
      /* request = */ HttpRequest.newBuilder()
        .uri(URI("http://localhost:8080/some/thing"))
        .POST(
          HttpRequest.BodyPublishers.ofString(requestBody, Charset.defaultCharset())
        )
        .build(),
      /* responseBodyHandler = */ HttpResponse.BodyHandlers.ofString()
    )

    // then
    val expectedResponseBody = """
        {
          "result": "Stuff"
        }
      """.trimIndent()
    assertEquals(expectedResponseBody, httpResponse.body())
  }

  @Test
  fun helpers_data_and_time() {
    // given
    val requestBody =
      """
      {
        "test": "Stuff"
      }
    """.trimIndent()

    val responseBody =
      """
        {
          "result": "{{jsonPath request.body '$.test'}}",
          "date": "{{now format='yyyy-MM-dd\'T\'HH:mm:ss.S'}}"
        }
      """.trimIndent()

    wireMockServer.stubFor(
      WireMock.post(WireMock.urlEqualTo("/some/thing"))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(
          WireMock.aResponse()
            .withStatus(200)
            .withBody(responseBody)
            .withTransformers("response-template")
        )
    )
    wireMockServer.start()

    // when
    val httpResponse: HttpResponse<String> = testClient.send(
      /* request = */ HttpRequest.newBuilder()
        .uri(URI("http://localhost:8080/some/thing"))
        .POST(
          HttpRequest.BodyPublishers.ofString(requestBody, Charset.defaultCharset())
        )
        .build(),
      /* responseBodyHandler = */ HttpResponse.BodyHandlers.ofString()
    )

    // then
    println(httpResponse.body())
  }
}
