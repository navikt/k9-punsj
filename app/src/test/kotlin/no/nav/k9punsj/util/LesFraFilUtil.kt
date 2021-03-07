package no.nav.k9punsj.util

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9punsj.objectMapper
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class LesFraFilUtil {
    companion object{
        private fun lesFraFil(filnavn: String): String{
            try {
                return Files.readString(Path.of("src/test/resources/psb/$filnavn"))
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        fun genererSøknadMedFeil() : MutableMap<String, Any?> {
            return objectMapper().readValue(lesFraFil("søknad-med-feil.json"))
        }

        fun søknadFraFrontend() : MutableMap<String, Any?> {
            return objectMapper().readValue(lesFraFil("søknad-fra-frontend.json"))
        }
    }
}
