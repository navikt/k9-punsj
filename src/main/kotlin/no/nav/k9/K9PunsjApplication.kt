package no.nav.k9

import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder



@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class K9PunsjApplication {
	@Bean
	fun objectMapperBuilder(): Jackson2ObjectMapperBuilder {
		val builder = Jackson2ObjectMapperBuilder()
		builder.propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
		return builder
	}

}

fun main(args: Array<String>) {
	runApplication<K9PunsjApplication>(*args) {
		setBannerMode(Banner.Mode.OFF)
	}
}

