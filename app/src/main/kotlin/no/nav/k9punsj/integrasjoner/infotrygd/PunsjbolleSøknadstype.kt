package no.nav.k9punsj.integrasjoner.infotrygd

import no.nav.k9.kodeverk.dokument.Brevkode

enum class PunsjbolleSøknadstype(
    internal val k9YtelseType: String,
    internal val brevkode: Brevkode,
    internal val journalpostType: String) {
    PleiepengerSyktBarn("PSB", Brevkode.PLEIEPENGER_BARN_SOKNAD, "PleiepengerSyktBarn"),
    PleiepengerLivetsSluttfase("PPN", Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE, "PleiepengerLivetsSluttfase"),
    Omsorgspenger("OMP", Brevkode.SØKNAD_UTBETALING_OMS, "UtbetaleOmsorgspenger"),
    OmsorgspengerUtbetaling_Arbeidstaker("OMP", Brevkode.SØKNAD_UTBETALING_OMS_AT, "UtbetaleOmsorgspenger"),
    OmsorgspengerUtbetaling_Papirsøknad_Arbeidstaker("OMP", Brevkode.PAPIRSØKNAD_UTBETALING_OMS_AT, "UtbetaleOmsorgspenger"),
    OmsorgspengerUtbetaling_Korrigering("OMP", Brevkode.FRAVÆRSKORRIGERING_IM_OMS, "UtbetaleOmsorgspenger"),
    OmsorgspengerKroniskSyktBarn("OMP_KS", Brevkode.SØKNAD_OMS_UTVIDETRETT_KS, "KroniskSyktBarn"),
    OmsorgspengerAleneOmsorg("OMP_AO", Brevkode.SØKNAD_OMS_UTVIDETRETT_AO, "AleneOmsorg"),
    OmsorgspengerMidlertidigAlene("OMP_MA", Brevkode.SØKNAD_OMS_UTVIDETRETT_MA, "MidlertidigAlene");

    internal companion object {
        internal fun fraK9FormatYtelsetype(ytelsetype: String) = when (ytelsetype) {
            "PLEIEPENGER_SYKT_BARN" -> PleiepengerSyktBarn
            "PLEIEPENGER_LIVETS_SLUTTFASE" -> PleiepengerLivetsSluttfase
            "OMP_UT" -> OmsorgspengerUtbetaling_Korrigering
            "OMP_UTV_KS" -> OmsorgspengerKroniskSyktBarn
            "OMP_UTV_MA" -> OmsorgspengerMidlertidigAlene
            "OMP_UTV_AO" -> OmsorgspengerAleneOmsorg
            "OMP" -> Omsorgspenger
            else -> throw IllegalStateException("Ukjent ytelsestype $ytelsetype")
        }

        fun fraBrevkode(brevkode: Brevkode): PunsjbolleSøknadstype = when(brevkode) {
            Brevkode.PLEIEPENGER_BARN_SOKNAD -> PleiepengerSyktBarn
            Brevkode.SØKNAD_PLEIEPENGER_LIVETS_SLUTTFASE -> PleiepengerLivetsSluttfase
            Brevkode.SØKNAD_OMS_UTVIDETRETT_KS -> OmsorgspengerKroniskSyktBarn
            Brevkode.SØKNAD_OMS_UTVIDETRETT_MA -> OmsorgspengerMidlertidigAlene
            Brevkode.SØKNAD_OMS_UTVIDETRETT_AO -> OmsorgspengerAleneOmsorg
            Brevkode.SØKNAD_UTBETALING_OMS -> Omsorgspenger
            Brevkode.SØKNAD_UTBETALING_OMS_AT -> OmsorgspengerUtbetaling_Arbeidstaker
            Brevkode.PAPIRSØKNAD_UTBETALING_OMS_AT -> OmsorgspengerUtbetaling_Papirsøknad_Arbeidstaker
            Brevkode.FRAVÆRSKORRIGERING_IM_OMS -> OmsorgspengerUtbetaling_Korrigering
            else -> throw IllegalStateException("Ikke støttet brevkoode $brevkode")
        }
    }
}
