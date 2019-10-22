package no.nav.k9

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
class K9PunsjApplication

fun main(args: Array<String>) {
	runApplication<K9PunsjApplication>(*args)
}


