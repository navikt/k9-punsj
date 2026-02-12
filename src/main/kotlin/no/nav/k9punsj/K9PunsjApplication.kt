package no.nav.k9punsj

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.huxhorn.sulky.ulid.ULID
import no.nav.k9punsj.utils.UlidDeserializer
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.reactor.netty.NettyReactiveWebServerFactory
import org.springframework.boot.runApplication
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@EnableScheduling
@EnableTransactionManagement
@SpringBootApplication
class K9PunsjApplication {

    @Bean
    fun objectMapperBuilder(): Jackson2ObjectMapperBuilder {
        return Jackson2ObjectMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .modulesToInstall(JavaTimeModule())
            .featuresToDisable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .deserializerByType(ULID.Value::class.java, UlidDeserializer())
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
