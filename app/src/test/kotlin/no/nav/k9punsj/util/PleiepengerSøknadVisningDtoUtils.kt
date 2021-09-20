package no.nav.k9punsj.util

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9punsj.domenetjenester.mappers.MapFraVisningTilEksternFormat
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9Format
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

internal object PleiepengerSøknadVisningDtoUtils {

    internal fun MutableMap<String, Any?>.somPleiepengerSøknadVisningDto(manipuler: (MutableMap<String, Any?>) -> Unit)
        = manipuler(this).let { objectMapper().convertValue<PleiepengerSøknadVisningDto>(this) }

    internal fun minimalSøknadSomValiderer(
        søker: String = "11111111111",
        barn: String = "22222222222",
        søknadsperiode: Pair<LocalDate, LocalDate>? = null,
        manipuler: (MutableMap<String, Any?>) -> Unit = {}
    ) : PleiepengerSøknadVisningDto {
        @Language("JSON")
        val json = """
            {
              "soeknadId": "${UUID.randomUUID()}",
              "mottattDato": "${LocalDate.now()}",
              "klokkeslett": "${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}",
              "soekerId": "$søker",
              "barn": {
                "norskIdent": "$barn"
              },
              "journalposter": [
                "12345678"
              ]
            }
            """
        val søknad: MutableMap<String, Any?> = objectMapper().readValue(json)
        søknadsperiode?.also {
            søknad["soeknadsperiode"] = mapOf("fom" to "${it.first}", "tom" to "${it.second}")
        }
        manipuler(søknad)
        return objectMapper().convertValue(søknad)
    }

    internal fun PleiepengerSøknadVisningDto.mapTilSendingsFormat() =
        MapFraVisningTilEksternFormat.mapTilSendingsformat(this)

    internal fun PleiepengerSøknadVisningDto.mapTilK9Format(
        perioderSomFinnesIK9: List<PeriodeDto> = emptyList()) =
        MapTilK9Format.mapTilEksternFormat(
            søknad = mapTilSendingsFormat(),
            soeknadId = soeknadId,
            perioderSomFinnesIK9 = perioderSomFinnesIK9,
            journalpostIder = journalposter?.toSet() ?: emptySet()
        ).let { it.first.getYtelse<PleiepengerSyktBarn>() to it.second }

    init {
        val k9Feil = minimalSøknadSomValiderer(
            søknadsperiode = LocalDate.now() to LocalDate.now().plusWeeks(1)
        ).mapTilK9Format(emptyList()).second
        check(k9Feil.isEmpty()) {
            "Minimal søknad mangler felter. Feil=$k9Feil"
        }
    }
}