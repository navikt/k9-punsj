package no.nav.k9

import no.nav.k9.wiremock.initWireMock
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
class K9PunsjApplicationTests {

	private val wireMockServer = initWireMock(
			port = 8081
	)

	init {
	    MockConfiguration.config(
				wireMockServer = wireMockServer
		).setAsProperties()
	}

	@Test
	fun contextLoads() {
	}

}
