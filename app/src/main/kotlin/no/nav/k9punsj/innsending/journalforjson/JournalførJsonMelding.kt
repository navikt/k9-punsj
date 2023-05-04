package no.nav.k9punsj.innsending.journalforjson

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.k9.kodeverk.Fagsystem
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.Søknadstype
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal object JournalførJsonMelding {

    private fun JsonNode.farge() = when {
        isMissingOrNull() -> DEFAULT_FARGE
        hentString().matches(FARGE_REGEX) -> hentString()
        else -> DEFAULT_FARGE.also {
            logger.warn("Ugyldig farge=[${hentString()} satt i meldingen, defaulter til farge=[$it]")
        }
    }

    internal data class JournalførJson(
        internal val json: ObjectNode,
        internal val brevkode: String,
        internal val tittel: String,
        internal val mottatt: ZonedDateTime,
        internal val farge: String,
        internal val fagsystem: String,
        internal val identitetsnummer: Identitetsnummer,
        internal val saksnummer: String,
        internal val avsenderNavn: String
    )

    private val logger = LoggerFactory.getLogger(JournalførJsonMelding::class.java)
    private val FARGE_REGEX = "#[a-fA-F0-9]{6}".toRegex()
    private const val DEFAULT_FARGE = "#C1B5D0"
    private fun JsonNode.hentString() = asText().replace("\"","")

    private fun ObjectNode.objectNodeOrNull(key: String) = when (hasNonNull(key) && get(key) is ObjectNode) {
        true -> get(key) as ObjectNode
        false -> null
    }

    private fun String.renameKeys(fra: String, til: String) = replace(
        oldValue = """"$fra":""",
        newValue = """"$til":""",
        ignoreCase = false
    )

    private fun String.renameValues(fraKey: String, fraValue: String, tilValue: String) = replace(
        oldValue = """"$fraKey":"$fraValue"""",
        newValue = """"$fraKey":"$tilValue"""",
        ignoreCase = false
    )

    private fun String.renameLand() : String {
        var current = this
        Land.values().forEach { land -> current = current.renameValues("land", land.name, land.navn) }
        return current
    }

    private fun String.lowercaseFirst() = "${get(0).lowercase()}${substring(1)}"

    internal fun ObjectNode.manipulerSøknadsJson(søknadstype: Søknadstype) : ObjectNode {
        // Fjerner informasjon på toppnivå
        val søknad = deepCopy()
        søknad.remove(setOf("versjon", "språk"))
        // Fjerner informasjon i "ytelse"
        søknad.objectNodeOrNull("ytelse")?.also { ytelse ->
            ytelse.remove(setOf("type", "uttak"))
        }
        return "$søknad"
            .renameKeys("ytelse", søknadstype.name.lowercaseFirst())
            .renameKeys("mottattDato", "mottatt")
            .renameKeys("søknadsperiode", "søknadsperioder")
            .renameKeys("endringsperiode", "endringsperioder")
            .renameKeys("norskIdentitetsnummer", "identitetsnummer")
            .renameKeys("arbeidstakerList", "arbeidstakere")
            .renameKeys("frilanserArbeidstidInfo", "frilanser")
            .renameKeys("jobberFortsattSomFrilans", "jobberFortsattSomFrilanser")
            .renameKeys("selvstendigNæringsdrivendeArbeidstidInfo", "selvstendigNæringsdrivende")
            .renameKeys("arbeidstidInfo", "arbeidstid")
            .renameKeys("arbeidAktivitet", "arbeid")
            .renameKeys("virksomhetNavn", "virksomhetsnavn")
            .renameKeys("dataBruktTilUtledning", "overordnet")
            .renameKeys("etablertTilsynTimerPerDag", "etablertTilsynPerDag")
            .renameKeys("jobberNormaltTimerPerDag", "normalArbeidstidPerDag")
            .renameKeys("faktiskArbeidTimerPerDag", "faktiskArbeidstidPerDag")
            .renameKeys("inneholderInfomasjonSomIkkeKanPunsjes", "inneholderInformasjonSomIkkeKanPunsjes")
            .renameKeys("trekkKravPerioder", "fjernedeSøknadsperioder")
            .renameKeys("begrunnelseForInnsending", "begrunnelseForFjerningAvSøknadsperiode")
            .renameValues("årsak", "barnetInnlagtIHelseinstitusjonForNorskOffentligRegning", "Barnet innlagt på helseinstitusjon for norsk offentelig regning")
            .renameValues("årsak", "barnetInnlagtIHelseinstitusjonDekketEtterAvtaleMedEtAnnetLandOmTrygd", "Barnet innlagt på helseinstitusjon dekket etter avtale med annet land")
            .renameLand()
            .let { jacksonObjectMapper().readTree(it) as ObjectNode }
    }
}