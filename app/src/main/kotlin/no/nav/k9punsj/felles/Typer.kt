package no.nav.k9punsj.felles

import no.nav.k9.kodeverk.dokument.Brevkode

data class JournalpostId private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "$value er en ugyldig journalpostId" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{5,40}".toRegex()
        internal fun String.somJournalpostId() = JournalpostId(this)
    }
}

data class Identitetsnummer private constructor(private val value: String) {
    init { require(value.matches(Regex)) { "Ugyldig identitetsnummer" } }
    override fun toString() = value
    internal companion object {
        private val Regex = "\\d{11,25}".toRegex()
        internal fun String.somIdentitetsnummer() = Identitetsnummer(this)
    }
}

data class IdentOgJournalpost(
    val norskIdent: String,
    val journalpostId: String
)

data class Sak(
    val sakstype: SaksType,
    val fagsakId: String? = null,
) {
    val fagsaksystem = if (sakstype == SaksType.FAGSAK) FagsakSystem.K9 else null
    init {
        when (sakstype) {
            SaksType.FAGSAK -> {
                require(fagsaksystem != null && !fagsakId.isNullOrBlank()) {
                    "Dersom sakstype er ${SaksType.FAGSAK}, så må fagsaksystem og fagsakId være satt. fagsaksystem=[$fagsaksystem], fagsakId=[$fagsakId]"
                }
            }
            SaksType.GENERELL_SAK -> {
                require(fagsaksystem == null && fagsakId.isNullOrBlank()) {
                    "Dersom sakstype er ${SaksType.GENERELL_SAK}, så kan ikke fagsaksystem og fagsakId være satt. fagsaksystem=[$fagsaksystem], fagsakId=[$fagsakId]"
                }
            }
            SaksType.ARKIVSAK -> throw UnsupportedOperationException("ARKIVSAK skal kun brukes etter avtale.")
        }
    }

    enum class SaksType { FAGSAK, GENERELL_SAK, ARKIVSAK }
    enum class FagsakSystem { K9 }

}

enum class Søknadstype(
    internal val k9YtelseType: String,
    internal val brevkode: Brevkode) {
    PleiepengerSyktBarn("PSB", Brevkode.PLEIEPENGER_BARN_SOKNAD),
    PleiepengerLivetsSluttfase("PPN", Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE),
    Omsorgspenger("OMP", Brevkode.SØKNAD_UTBETALING_OMS),
    OmsorgspengerUtbetaling_Arbeidstaker("OMP", Brevkode.SØKNAD_UTBETALING_OMS_AT),
    OmsorgspengerUtbetaling_Papirsøknad_Arbeidstaker("OMP", Brevkode.PAPIRSØKNAD_UTBETALING_OMS_AT),
    OmsorgspengerUtbetaling_Korrigering("OMP", Brevkode.FRAVÆRSKORRIGERING_IM_OMS),
    OmsorgspengerKroniskSyktBarn("OMP_KS", Brevkode.SØKNAD_OMS_UTVIDETRETT_KS),
    OmsorgspengerAleneOmsorg("OMP_AO", Brevkode.SØKNAD_OMS_UTVIDETRETT_AO),
    Opplæringspenger("OLP", Brevkode.OPPLÆRINGSPENGER_SOKNAD),
    OmsorgspengerMidlertidigAlene("OMP_MA", Brevkode.SØKNAD_OMS_UTVIDETRETT_MA);

    internal companion object {
        internal fun fraK9FormatYtelsetype(ytelsetype: String) = when (ytelsetype) {
            "PLEIEPENGER_SYKT_BARN" -> PleiepengerSyktBarn
            "PLEIEPENGER_LIVETS_SLUTTFASE" -> PleiepengerLivetsSluttfase
            "OMP_UT" -> OmsorgspengerUtbetaling_Korrigering
            "OMP_UTV_KS" -> OmsorgspengerKroniskSyktBarn
            "OMP_UTV_MA" -> OmsorgspengerMidlertidigAlene
            "OMP_UTV_AO" -> OmsorgspengerAleneOmsorg
            "OMP" -> Omsorgspenger
            "OPPLÆRINGSPENGER" -> Opplæringspenger
            else -> throw IllegalStateException("Ukjent ytelsestype $ytelsetype")
        }

        fun fraBrevkode(brevkode: Brevkode): Søknadstype = when(brevkode) {
            Brevkode.PLEIEPENGER_BARN_SOKNAD -> PleiepengerSyktBarn
            Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE -> PleiepengerLivetsSluttfase
            Brevkode.SØKNAD_OMS_UTVIDETRETT_KS -> OmsorgspengerKroniskSyktBarn
            Brevkode.SØKNAD_OMS_UTVIDETRETT_MA -> OmsorgspengerMidlertidigAlene
            Brevkode.SØKNAD_OMS_UTVIDETRETT_AO -> OmsorgspengerAleneOmsorg
            Brevkode.SØKNAD_UTBETALING_OMS -> Omsorgspenger
            Brevkode.SØKNAD_UTBETALING_OMS_AT -> OmsorgspengerUtbetaling_Arbeidstaker
            Brevkode.PAPIRSØKNAD_UTBETALING_OMS_AT -> OmsorgspengerUtbetaling_Papirsøknad_Arbeidstaker
            Brevkode.FRAVÆRSKORRIGERING_IM_OMS -> OmsorgspengerUtbetaling_Korrigering
            Brevkode.OPPLÆRINGSPENGER_SOKNAD -> Opplæringspenger
            else -> throw IllegalStateException("Ikke støttet brevkoode $brevkode")
        }
    }
}

