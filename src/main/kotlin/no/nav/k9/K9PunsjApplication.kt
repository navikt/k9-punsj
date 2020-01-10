package no.nav.k9

import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class K9PunsjApplication {
	@Bean
	fun objectMapperBuilder(): Jackson2ObjectMapperBuilder {
		val builder = Jackson2ObjectMapperBuilder()
		builder.propertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
		return builder
	}
	@Bean
	fun reactiveWebServerFactory(): ReactiveWebServerFactory {
		return NettyReactiveWebServerFactory()
	}
}

fun main(args: Array<String>) {
	runApplication<K9PunsjApplication>(*args) {
		setBannerMode(Banner.Mode.OFF)
	}
}