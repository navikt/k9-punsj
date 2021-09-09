package no.nav.k9punsj.domenetjenester.mappers

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.ytelse.psb.v1.PleiepengerSyktBarn
import no.nav.k9punsj.domenetjenester.mappers.MapTilK9Format.Companion.somK9Perioder
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadMottakDto
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadVisningDto
import no.nav.k9punsj.util.LesFraFilUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class MapTilK9FormatTest {

    @Test
    fun `Innenfor perioder i K9`() {
        val søknad = pleiepengerSyktBarnSøknad()
        val (ytelse, feil) = søknad.mapTilK9Format(lengrePeriodeIK9)
        assertThat(feil).isEmpty()
        assertThat(endringsperioder).hasSameElementsAs(ytelse.endringsperiode)
        assertThat(endringsperioder).hasSameElementsAs(ytelse.endringsperioderFraJson())
    }

    @Test
    fun `Utenfor perioder i K9`() {
        val søknad = pleiepengerSyktBarnSøknad()
        val (_, feil) = søknad.mapTilK9Format(korterePeriodeIK9)
        assertThat(feil).isNotEmpty
        assertThat(feil).allMatch { it.feilkode == "ugyldigPeriode" }
    }

    private fun pleiepengerSyktBarnSøknad() : PleiepengerSøknadMottakDto {
        val søknad = LesFraFilUtil.søknadFraFrontend()

        søknad.replace("soeknadsperiode", mapOf(
            "fom" to "${søknadsperiode.fom}",
            "tom" to "${søknadsperiode.tom}"
        ))

        val dto = objectMapper().convertValue<PleiepengerSøknadVisningDto>(søknad)
        return MapFraVisningTilEksternFormat.mapTilSendingsformat(dto)
    }

    private fun PleiepengerSøknadMottakDto.mapTilK9Format(perioderSomFinnesIK9: List<PeriodeDto>) = MapTilK9Format.mapTilEksternFormat(
        søknad = this,
        soeknadId = "${UUID.randomUUID()}",
        journalpostIder = setOf("111111111"),
        perioderSomFinnesIK9 = perioderSomFinnesIK9
    ).let { it.first.getYtelse<PleiepengerSyktBarn>() to it.second }

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