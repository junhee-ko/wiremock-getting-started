import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.github.tomakehurst.wiremock.http.RequestMethod.POST
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.wiremock.webhooks.Webhooks
import org.wiremock.webhooks.Webhooks.webhook
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset

class WebHookTest {

  private val testClient = HttpClient.newHttpClient()
  private val wireMockServer = WireMockServer(
    WireMockConfiguration.options()
      .port(8080)
      .extensions(
        ResponseTemplateTransformer(false),
        Webhooks()
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
  fun single_webhook() {
    // given
    wireMockServer.stubFor(
      post(urlPathEqualTo("/something-async"))
        .willReturn(ok())
        .withPostServeAction(
          "webhook", webhook()
            .withMethod(POST)
            .withUrl("http://localhost:8080/callback")
            .withHeader("Content-Type", "application/json")
            .withBody("{ \"result\": \"SUCCESS\" }")
        )
    )

    // when
    testClient.send(
      /* request = */ HttpRequest.newBuilder()
        .uri(URI("http://localhost:8080/something-async"))
        .POST(HttpRequest.BodyPublishers.noBody())
        .build(),
      /* responseBodyHandler = */ HttpResponse.BodyHandlers.ofString()
    )

    // then
    verify(1, postRequestedFor(urlEqualTo("/something-async")))
    verify(
      1, postRequestedFor(urlEqualTo("/callback"))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson("{ \"result\": \"SUCCESS\" }"))
    )
  }

  @Test
  fun using_data_from_the_original_request() {
    // given
    val requestBody =
      """
      {
        "transactionId": "12345"
      }
      """.trimIndent()

    wireMockServer.stubFor(
      post(urlPathEqualTo("/templating"))
        .willReturn(ok())
        .withPostServeAction(
          "webhook", webhook()
            .withMethod(POST)
            .withUrl("http://localhost:8080/callback")
            .withHeader("Content-Type", "application/json")
            .withBody("{ \"message\": \"success\", \"transactionId\": \"{{jsonPath originalRequest.body '$.transactionId'}}\" }")
        )
    )

    // when
    testClient.send(
      /* request = */ HttpRequest.newBuilder()
        .uri(URI("http://localhost:8080/templating"))
        .POST(
          HttpRequest.BodyPublishers.ofString(requestBody, Charset.defaultCharset())
        )
        .build(),
      /* responseBodyHandler = */ HttpResponse.BodyHandlers.ofString()
    )

    // then
    verify(
      1, postRequestedFor(urlEqualTo("/callback"))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalToJson("{ \"message\": \"success\", \"transactionId\": \"12345\" }"))
    )
  }
}
