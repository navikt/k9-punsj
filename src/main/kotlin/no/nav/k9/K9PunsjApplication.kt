package no.nav.k9

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.zaxxer.hikari.HikariDataSource
import no.nav.k9.db.DbConfiguration
import no.nav.k9.db.hikariConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class, FlywayAutoConfiguration::class])
class K9PunsjApplication @Autowired constructor(var dbConfiguration: DbConfiguration) {
	@Bean
	fun objectMapperBuilder(): Jackson2ObjectMapperBuilder {
		return Jackson2ObjectMapperBuilder()
		.propertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
		.modulesToInstall(JavaTimeModule())
		.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.featuresToDisable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
	}

	@Bean
	fun reactiveWebServerFactory(): ReactiveWebServerFactory {
		return NettyReactiveWebServerFactory()
	}

	@Bean
	fun databaseInitializer(): HikariDataSource {
		return hikariConfig(dbConfiguration)
	}
}

fun main(args: Array<String>) {
	runApplication<K9PunsjApplication>(*args) {
		setBannerMode(Banner.Mode.OFF)
	}
}