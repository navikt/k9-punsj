package no.nav.k9punsj.domenetjenester.mappers

import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9Format.Companion.somK9Perioder
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import no.nav.k9punsj.util.LesFraFilUtil
import no.nav.k9punsj.util.PleiepengerSøknadVisningDtoUtils.mapTilK9Format
import no.nav.k9punsj.util.PleiepengerSøknadVisningDtoUtils.minimalSøknadSomValiderer
import no.nav.k9punsj.util.PleiepengerSøknadVisningDtoUtils.somPleiepengerSøknadVisningDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MapTilK9FormatTest {

    @Test
    fun `Endringer innenfor endringsperioder i K9`() {
        val søknad = endringsperiodeSøknad()
        val (ytelse, feil) = søknad.mapTilK9Format(lengrePeriodeIK9)
        assertThat(feil).isEmpty()
        assertThat(lengrePeriodeIK9.somK9Perioder()).hasSameElementsAs(ytelse.endringsperiode)
        assertThat(lengrePeriodeIK9.somK9Perioder()).hasSameElementsAs(ytelse.endringsperioderFraJson())
        assertThat(ytelse.trekkKravPerioder).isEmpty()
        // TODO: Dette er riktig når vi endrer til k9-format som utleder endringsperioder
        //assertThat(endringsperioder).hasSameElementsAs(ytelse.endringsperiode)
        //assertThat(endringsperioder).hasSameElementsAs(ytelse.endringsperioderFraJson())
    }

    @Test
    fun `Endringer utenfor endringsperioder i K9`() {
        val søknad = endringsperiodeSøknad()
        val (ytelse, feil) = søknad.mapTilK9Format(korterePeriodeIK9)
        assertThat(ytelse.trekkKravPerioder).isEmpty()
        assertThat(feil).isNotEmpty
        assertThat(feil).allMatch { it.feilkode == "ugyldigPeriode" }
    }

    @Test
    fun `Trekk av perioder`() {
        val trekkAvPerioderSøknad = minimalSøknadSomValiderer(manipuler = {
            it["trekkKravPerioder"] = setOf(
                mapOf("fom" to "2021-01-01", "tom" to "2021-04-05"),
                mapOf("fom" to "2024-04-04", "tom" to "2024-12-10")
            )
        })

        val (_, feil) = trekkAvPerioderSøknad.mapTilK9Format(listOf(
            PeriodeDto(fom = LocalDate.parse("2021-01-01"), tom = LocalDate.parse("2024-12-10"))
        ))

        assertThat(feil).isEmpty()
    }

    private fun endringsperiodeSøknad() : PleiepengerSøknadVisningDto {
        return LesFraFilUtil.søknadFraFrontend().somPleiepengerSøknadVisningDto {
            it.replace("soeknadsperiode", mapOf(
                "fom" to "${søknadsperiode.fom}",
                "tom" to "${søknadsperiode.tom}"
            ))
        }
    }

    private companion object {
        private val søknadsperiode = PeriodeDto(fom = LocalDate.parse("2018-12-30"), tom = LocalDate.parse("2019-09-20"))  // Settes tilbake en måned, opprinnelig "2019-10-20"
        private val endringsperiode = PeriodeDto(fom = LocalDate.parse("2019-09-21"), tom = LocalDate.parse("2019-10-20"))
        private val lengrePeriodeIK9 = listOf(endringsperiode.copy(tom = endringsperiode.tom!!.plusMonths(4)))
        private val korterePeriodeIK9 = listOf(endringsperiode.copy(tom = endringsperiode.tom!!.minusDays(5)))
        private val endringsperioder = listOf(endringsperiode).somK9Perioder()
        private fun PleiepengerSyktBarn.endringsperioderFraJson() = (objectMapper().readTree(objectMapper().writeValueAsString(this)).get("endringsperiode") as ArrayNode)
            .map { Periode(it.asText()) }
    }
}