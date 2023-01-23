import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@WireMockTest(httpPort = 8081)
class DeclarativeWireMockTest {

  @Test
  fun test_something_with_wiremock(wmRuntimeInfo: WireMockRuntimeInfo) {
    // The static DSL will be automatically configured for you
    stubFor(get("/static-dsl").willReturn(ok()))

    // Instance DSL can be obtained from the runtime info parameter
    val wireMock: WireMock = wmRuntimeInfo.wireMock
    wireMock.register(get("/instance-dsl").willReturn(ok()))

    // Info such as port numbers is also available
    val port: Int = wmRuntimeInfo.httpPort

    // Do some testing...
    assertEquals(8081, port)
  }
}
