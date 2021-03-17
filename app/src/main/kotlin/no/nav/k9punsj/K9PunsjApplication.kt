package no.nav.k9punsj

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.huxhorn.sulky.ulid.ULID
import no.nav.k9punsj.db.config.DbConfiguration
import no.nav.k9punsj.db.config.hikariConfig
import no.nav.k9punsj.db.config.hikariConfigLocal
import no.nav.k9punsj.jackson.UlidDeserializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import javax.sql.DataSource

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class, FlywayAutoConfiguration::class])
class K9PunsjApplication @Autowired constructor(var dbConfiguration: DbConfiguration, val environment: Environment) {

	@Bean
	fun objectMapperBuilder(): Jackson2ObjectMapperBuilder {
		return Jackson2ObjectMapperBuilder()
				.propertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
				.modulesToInstall(JavaTimeModule())
				.featuresToDisable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
				.deserializerByType(ULID.Value::class.java, UlidDeserializer())
	}

	@Bean
	fun reactiveWebServerFactory(): ReactiveWebServerFactory {
		return NettyReactiveWebServerFactory()
	}

	@Bean
	@Profile("!test & !local")
	fun databaseInitializer(): DataSource {
		return hikariConfig(dbConfiguration)
	}

	@Bean
	@Profile("local")
	fun databaseInitializerLocal(): DataSource {
		return hikariConfigLocal(dbConfiguration, environment)
	}
}

fun main(args: Array<String>) {
	runApplication<K9PunsjApplication>(*args) {
		setBannerMode(Banner.Mode.OFF)
	}
}
