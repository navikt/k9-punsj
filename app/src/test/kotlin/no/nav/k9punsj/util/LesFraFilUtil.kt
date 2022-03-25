package no.nav.k9punsj.util

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9punsj.objectMapper
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

typealias SøknadJson = MutableMap<String, Any?>

class LesFraFilUtil {
    companion object{
        private fun lesFraFil(filnavn: String): String{
            try {
                return Files.readString(Path.of("src/test/resources/psb/$filnavn"))
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        private fun lesFraFilPls(filnavn: String): String{
            try {
                return Files.readString(Path.of("src/test/resources/pls/$filnavn"))
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        private fun lesFraFilOmsMa(filnavn: String): String{
            try {
                return Files.readString(Path.of("src/test/resources/omp-ma/$filnavn"))
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        private fun lesFraFilOmsAo(filnavn: String): String{
            try {
                return Files.readString(Path.of("src/test/resources/omp-ao/$filnavn"))
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        private fun lesFraOmsMappeFil(filnavn: String): String{
            try {
                return Files.readString(Path.of("src/test/resources/oms/$filnavn"))
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        private fun lesFraOmsKSBFil(filnavn: String): String{
            try {
                return Files.readString(Path.of("src/test/resources/omp-ksb/$filnavn"))
            } catch (e: IOException) {
                throw IllegalStateException(e)
            }
        }

        /**
         *  Pleiepenger
         */

        fun søknadFraFrontend() : SøknadJson {
            return objectMapper().readValue(lesFraFil("søknad-fra-frontend.json"))
        }

        fun søknadFraFrontendUtenlandsoppholdV2() : SøknadJson {
            return objectMapper().readValue(lesFraFil("søknad-fra-frontend-utlandsoppholdv2.json"))
        }

        fun søknadFraFrontendMed2() : SøknadJson {
            return objectMapper().readValue(lesFraFil("søknad-fra-frontend_med_2.json"))
        }

        fun minimalSøknad() : SøknadJson {
            return objectMapper().readValue(lesFraFil("tom-søknad.json"))
        }

        fun ferieSøknad() : SøknadJson {
            return objectMapper().readValue(lesFraFil("ferie-søknad.json"))
        }

        fun tidSøknad() : SøknadJson {
            return objectMapper().readValue(lesFraFil("med-tid-søknad.json"))
        }

        fun utenUttak() : SøknadJson {
            return objectMapper().readValue(lesFraFil("uten-uttak-søknad.json"))
        }

        fun ferieNull() : SøknadJson {
            return objectMapper().readValue(lesFraFil("ferie-null-søknad.json"))
        }

        fun sn() : SøknadJson {
            return objectMapper().readValue(lesFraFil("sn-søknad.json"))
        }

        fun tomtLand() : SøknadJson {
            return objectMapper().readValue(lesFraFil("land_tom_string.json"))
        }

        /**
         *  Pleiepenger livets sluttfase
         */
        fun søknadFraFrontendPls() : SøknadJson {
            return objectMapper().readValue(lesFraFilPls("søknad-fra-frontend.json"))
        }

        /**
         *  Omsorgspenger
         */

        fun søknadFraFrontendOms() : SøknadJson {
            return objectMapper().readValue(lesFraOmsMappeFil("søknad-fra-frontend.json"))
        }

        fun søknadFraFrontendOmsFeil() : SøknadJson {
            return objectMapper().readValue(lesFraOmsMappeFil("søknad-fra-frontend-feil.json"))
        }

        fun søknadFraFrontendOmsTrekk() : SøknadJson {
            return objectMapper().readValue(lesFraOmsMappeFil("søknad-fra-frontend-trekk.json"))
        }

        fun søknadFraFrontendOmsTrekkKompleks() : SøknadJson {
            return objectMapper().readValue(lesFraOmsMappeFil("søknad-fra-frontend-trekk-kompleks.json"))
        }

        /**
         *  Omsorgspenger kronisk sykt barn
         */

        fun søknadFraFrontendOmsKSB() : SøknadJson {
            return objectMapper().readValue(lesFraOmsKSBFil("søknad-fra-frontend.json"))
        }
        fun søknadUtenBarnFraFrontendOmsKSB() : SøknadJson {
            return objectMapper().readValue(lesFraOmsKSBFil("søknad-uten-barn-fra-frontend.json"))
        }

        /**
         *  Omsorgspenger midlertidlig alene
         */
        fun søknadFraFrontendOmsMA() : SøknadJson {
            return objectMapper().readValue(lesFraFilOmsMa("søknad-fra-frontend.json"))
        }

        /**
         *  Omsorgspenger alene omsorg
         */
        fun søknadFraFrontendOmsAO() : SøknadJson {
            return objectMapper().readValue(lesFraFilOmsAo("søknad-fra-frontend.json"))
        }
    }
}
