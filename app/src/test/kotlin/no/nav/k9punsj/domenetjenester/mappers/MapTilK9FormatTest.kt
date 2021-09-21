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
        assertThat(ytelse.trekkKravPerioder).isEmpty()
        assertThat(lengrePeriodeIK9.somK9Perioder()).hasSameElementsAs(ytelse.endringsperiode)
        assertThat(lengrePeriodeIK9.somK9Perioder()).hasSameElementsAs(ytelse.endringsperioderFraJson())
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
    fun `Trekk av perioder innenfor endringsperioder i K9`() {
        val trekkperiode1 = PeriodeDto(fom = "2021-01-01".dato(), tom = "2021-01-01".dato())
        val trekkperiode2 = PeriodeDto(fom = "2024-04-04".dato(), tom = "2024-12-10".dato())
        val trekkperioder = listOf(trekkperiode1, trekkperiode2)
        val endringsperioder = listOf(
            PeriodeDto(fom = trekkperiode1.fom!!.minusWeeks(1), trekkperiode1.tom!!.plusWeeks(1)),
            PeriodeDto(fom = trekkperiode2.fom!!.minusWeeks(1), trekkperiode2.tom!!.plusWeeks(1))
        )

        val trekkAvPerioderSøknad = minimalSøknadSomValiderer(manipuler = {
            it["trekkKravPerioder"] = setOf(
                mapOf("fom" to "${trekkperiode1.fom}", "tom" to "${trekkperiode1.tom}"),
                mapOf("fom" to "${trekkperiode2.fom}", "tom" to "${trekkperiode2.tom}")
            )
        })

        val (ytelse, feil) = trekkAvPerioderSøknad.mapTilK9Format(endringsperioder)

        assertThat(feil).isEmpty()
        assertThat(trekkperioder.somK9Perioder()).hasSameElementsAs(ytelse.trekkKravPerioder)
        assertThat(endringsperioder.somK9Perioder()).hasSameElementsAs(ytelse.endringsperiode)
        assertThat(endringsperioder.somK9Perioder()).hasSameElementsAs(ytelse.endringsperioderFraJson())
        // TODO: Dette er riktig når vi endrer til k9-format som utleder endringsperioder
        //assertThat(trekkperioder.somK9Perioder()).hasSameElementsAs(ytelse.endringsperiode)
        //assertThat(trekkperioder.somK9Perioder()).hasSameElementsAs(ytelse.endringsperioderFraJson())
        //assertThat(ytelse.endringsperiode).hasSameElementsAs(ytelse.trekkKravPerioder)
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
        private fun String.dato() = LocalDate.parse(this)
        private val søknadsperiode = PeriodeDto(fom = "2018-12-30".dato(), tom = "2019-09-20".dato())  // Settes tilbake en måned, opprinnelig "2019-10-20"
        private val endringsperiode = PeriodeDto(fom = "2019-09-21".dato(), tom = "2019-10-20".dato())
        private val lengrePeriodeIK9 = listOf(endringsperiode.copy(tom = endringsperiode.tom!!.plusMonths(4)))
        private val korterePeriodeIK9 = listOf(endringsperiode.copy(tom = endringsperiode.tom!!.minusDays(5)))
        private val endringsperioder = listOf(endringsperiode).somK9Perioder()
        private fun PleiepengerSyktBarn.endringsperioderFraJson() = (objectMapper().readTree(objectMapper().writeValueAsString(this)).get("endringsperiode") as ArrayNode)
            .map { Periode(it.asText()) }
    }
}