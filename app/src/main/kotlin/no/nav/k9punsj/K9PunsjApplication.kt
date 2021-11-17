package no.nav.k9punsj

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.huxhorn.sulky.ulid.ULID
import no.nav.k9punsj.db.config.DbConfiguration
import no.nav.k9punsj.db.config.hikariConfig
import no.nav.k9punsj.jackson.UlidDeserializer
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource

@EnableScheduling
@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class, FlywayAutoConfiguration::class])
class K9PunsjApplication {

	@Bean
	fun objectMapperBuilder(): Jackson2ObjectMapperBuilder {
		return Jackson2ObjectMapperBuilder()
				.propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
				.modulesToInstall(JavaTimeModule())
				.featuresToDisable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
				.deserializerByType(ULID.Value::class.java, UlidDeserializer())
	}

	@Bean
	fun reactiveWebServerFactory(): ReactiveWebServerFactory {
		return NettyReactiveWebServerFactory()
	}

	@Bean
	@StandardProfil
	fun databaseInitializer(dbConfiguration: DbConfiguration): DataSource {
		return hikariConfig(dbConfiguration)
	}
}

fun main(args: Array<String>) {
	runApplication<K9PunsjApplication>(*args) {
		setBannerMode(Banner.Mode.OFF)
	}
}
