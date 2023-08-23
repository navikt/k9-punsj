package no.nav.k9punsj.innsending.dto

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.JournalpostId.Companion.somJournalpostId
import no.nav.k9punsj.felles.Periode
import no.nav.k9punsj.felles.Periode.Companion.somPeriode
import no.nav.k9punsj.felles.Periode.Companion.ÅpenPeriode
import no.nav.k9punsj.felles.Søknadstype
import no.nav.k9punsj.integrasjoner.k9sak.dto.PunsjetSøknad
import java.time.ZonedDateTime

private fun ObjectNode.map(
    versjon: String,
    saksbehandler: String,
    saksnummer: String?,
    periode: Periode,
    søknadstype: Søknadstype,
    pleietrengende: Identitetsnummer? = null,
    annenPart: Identitetsnummer? = null) : PunsjetSøknad {
    return PunsjetSøknad(
        versjon = versjon,
        saksnummer = saksnummer,
        søknadId = søknadsId(),
        journalpostIder = journalpostIder(),
        søknadstype = søknadstype,
        søker = søker(),
        pleietrengende = pleietrengende,
        annenPart = annenPart,
        søknadJson = this,
        periode = periode,
        mottatt = mottatt(),
        saksbehandler = saksbehandler
    )
}

internal fun ObjectNode.søknadstype(brevkode: Brevkode? = null) =
    if (brevkode != null) Søknadstype.fraBrevkode(brevkode)
    else Søknadstype.fraK9FormatYtelsetype((get("ytelse") as ObjectNode).get("type").asText())

internal fun ObjectNode.periode(søknadstype: Søknadstype) = when (søknadstype) {
    Søknadstype.PleiepengerSyktBarn -> pleiepengerSyktBarnPeriode(mottatt = mottatt())
    Søknadstype.PleiepengerLivetsSluttfase -> omsorgspengerKroniskSyktBarnPeriode(mottatt = mottatt())
    Søknadstype.OmsorgspengerKroniskSyktBarn -> omsorgspengerKroniskSyktBarnPeriode(mottatt = mottatt())
    Søknadstype.OmsorgspengerMidlertidigAlene -> omsorgspengerMidlertidigAlenePeriode()
    Søknadstype.OmsorgspengerAleneOmsorg -> omsorgspengerAleneOmsorgPeriode()
    Søknadstype.OmsorgspengerUtbetaling_Korrigering -> omsorgspengerUtbetalingKorrigeringIMPeriode()
    Søknadstype.OmsorgspengerUtbetaling_Arbeidstaker, Søknadstype.OmsorgspengerUtbetaling_Papirsøknad_Arbeidstaker -> omsorgspengerUtbetalingHeleAretIPeriode()
    Søknadstype.Omsorgspenger -> ÅpenPeriode
    Søknadstype.Opplæringspenger -> opplaeringspengerPeriode()
}

internal fun ObjectNode.somPunsjetSøknad(
    versjon: String,
    saksbehandler: String,
    saksnummer: String?,
    brevkode: Brevkode
) : PunsjetSøknad {

    val søknadstype = søknadstype(brevkode)
    val periode = periode(søknadstype = søknadstype)

    return when (søknadstype) {
        Søknadstype.PleiepengerSyktBarn -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            pleietrengende = barn(),
            periode = periode,
            saksbehandler = saksbehandler
        )
        Søknadstype.PleiepengerLivetsSluttfase -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            pleietrengende = pleietrengende(),
            periode = periode,
            saksbehandler = saksbehandler
        )
        Søknadstype.OmsorgspengerUtbetaling_Korrigering -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            periode = periode,
            saksbehandler = saksbehandler
        )
        Søknadstype.OmsorgspengerKroniskSyktBarn -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            pleietrengende = barn(),
            periode = periode,
            saksbehandler = saksbehandler
        )
        Søknadstype.OmsorgspengerMidlertidigAlene -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            annenPart = annenForelder(),
            periode = periode,
            saksbehandler = saksbehandler
        )
        Søknadstype.OmsorgspengerAleneOmsorg -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            pleietrengende = barn(),
            periode = periode,
            saksbehandler = saksbehandler
        )
        Søknadstype.Omsorgspenger, Søknadstype.OmsorgspengerUtbetaling_Arbeidstaker, Søknadstype.OmsorgspengerUtbetaling_Papirsøknad_Arbeidstaker -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            periode = periode,
            saksbehandler = saksbehandler
        )
        Søknadstype.Opplæringspenger -> map(
            søknadstype = søknadstype,
            versjon = versjon,
            saksnummer = saksnummer,
            periode = periode,
            pleietrengende = barn(),
            saksbehandler = saksbehandler
        )
    }
}

private fun ObjectNode.søknadsId() = get("søknadId").asText()
private fun ObjectNode.mottatt() = ZonedDateTime.parse(get("mottattDato").asText())
private fun ObjectNode.journalpostIder() = get("journalposter")?.let { (it as ArrayNode).map { it as ObjectNode }.map { it.get("journalpostId").asText().somJournalpostId() }.toSet() } ?: emptySet()
private fun ObjectNode.søker() = get("søker").get("norskIdentitetsnummer").asText().somIdentitetsnummer()
private fun ObjectNode.barn() = get("ytelse").get("barn")?.get("norskIdentitetsnummer")?.asText()?.somIdentitetsnummer()
private fun ObjectNode.pleietrengende() = get("ytelse").get("pleietrengende")?.get("norskIdentitetsnummer")?.asText()?.somIdentitetsnummer()
private fun ObjectNode.annenForelder() = get("ytelse").get("annenForelder").get("norskIdentitetsnummer").asText().somIdentitetsnummer()

private fun ObjectNode.arrayPerioder(navn: String) =
    (get("ytelse").get(navn)?.let { (it as ArrayNode).map { iso8601 -> iso8601.asText().somPeriode() } }) ?: emptyList()
private fun ObjectNode.objectPerioder(navn: String) =
    (get("ytelse").get(navn)?.let { array -> (array as ArrayNode).map { it as ObjectNode }.map { obj -> obj.get("periode").asText().somPeriode() } }) ?: emptyList()

private fun ObjectNode.pleiepengerSyktBarnPeriode(mottatt: ZonedDateTime) =
    arrayPerioder("søknadsperiode")
    .plus(arrayPerioder("endringsperiode"))
    .plus(arrayPerioder("trekkKravPerioder"))
    .somEnPeriode()
    .let { periode -> when (periode.erÅpenPeriode()) {
        true -> Periode(fom = mottatt.toLocalDate(), tom = null)
        false -> periode
    }}

private fun ObjectNode.omsorgspengerKroniskSyktBarnPeriode(mottatt: ZonedDateTime) =
    arrayPerioder("søknadsperiode")
    .somEnPeriode()
    .let { periode -> when (periode.erÅpenPeriode()) {
        true -> Periode(fom = mottatt.toLocalDate(), tom = null)
        false -> periode
    }}

private fun ObjectNode.omsorgspengerUtbetalingKorrigeringIMPeriode() =
    objectPerioder("fraværsperioderKorrigeringIm").somEnPeriode()

private fun ObjectNode.omsorgspengerUtbetalingHeleAretIPeriode() =
    objectPerioder("fraværsperioder").somEnPeriode()

private fun ObjectNode.omsorgspengerMidlertidigAlenePeriode() =
    get("ytelse").get("annenForelder").get("periode")?.asText()?.somPeriode() ?: ÅpenPeriode

private fun ObjectNode.omsorgspengerAleneOmsorgPeriode() =
    get("ytelse").get("periode")?.asText()?.somPeriode() ?: ÅpenPeriode

private fun ObjectNode.opplaeringspengerPeriode() =
    arrayPerioder("søknadsperiode").somEnPeriode()

private fun List<Periode>.somEnPeriode() : Periode {
    val fraOgMedDatoer = map { it.fom }
    val tilOgMedDatoer = map { it.tom }

    return Periode(
        fom = when (null in fraOgMedDatoer) {
            true -> null
            else -> fraOgMedDatoer.filterNotNull().minByOrNull { it }
        },
        tom = when (null in tilOgMedDatoer) {
            true -> null
            else -> tilOgMedDatoer.filterNotNull().maxByOrNull { it }
        }
    )
}
