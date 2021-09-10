package no.nav.k9punsj.gosys

import no.nav.k9punsj.objectMapper

// https://kodeverk-web.nais.preprod.local/kodeverksoversikt/kodeverk/Behandlingstema
enum class Behandlingstema(
    internal val kodeverksverdi: String) {
    Omsorgspenger(kodeverksverdi = "ab0149"),
    PleiepengerSyktBarn(kodeverksverdi = "ab0320"),
    PleiepengerVedInstitusjonsopphold(kodeverksverdi = "ab0153"),
    PleiepengerPårørende(kodeverksverdi = "ab0094"),
    Opplæringspenger(kodeverksverdi = "ab0141")
}

// https://kodeverk-web.nais.preprod.local/kodeverksoversikt/kodeverk/Behandlingstyper
enum class Behandlingstype(
    internal val kodeverksverdi: String) {
    Overføring(kodeverksverdi = "ae0085"),
    Utbetaling(kodeverksverdi = "ae0007"),
    DigitalEttersendelse(kodeverksverdi = "ae0246"),
    Ansatte(kodeverksverdi = "ae0249"),
    SelvstendigNæringsdrivende(kodeverksverdi = "ae0221")
}

// https://github.com/navikt/kodeverksmapper/blob/master/web/src/main/resources/underkategori.csv
// https://github.com/navikt/oppgave/blob/master/src/main/resources/data/gjelder.json
enum class Gjelder(
    internal val tekst: String,
    internal val behandlingstema: Behandlingstema? = null,
    internal val behandlingstype: Behandlingstype? = null) {
    /** Pleiepenger sykt barn **/
    PleiepengerSyktBarn(
        tekst = "Pleiepenger ny ordning",
        behandlingstema = Behandlingstema.PleiepengerSyktBarn
    ),
    PleiepengerVedInstitusjonsopphold(
        tekst = "Pleiepenger institusjon",
        behandlingstema = Behandlingstema.PleiepengerVedInstitusjonsopphold
    ),
    DigitalEttersendelsePleiepengerSyktBarn(
        tekst = "Pleiepenger ny ordning - digital ettersendelse",
        behandlingstema = Behandlingstema.PleiepengerSyktBarn,
        behandlingstype = Behandlingstype.DigitalEttersendelse
    ),
    /** Omsorgspenger **/
    Omsorgspenger(
        tekst = "Omsorgspenger",
        behandlingstema = Behandlingstema.Omsorgspenger
    ),
    DigitalEttersendelseOmsorgspenger(
        tekst = "Omsorgspenger - digital ettersendelse",
        behandlingstema = Behandlingstema.Omsorgspenger,
        behandlingstype = Behandlingstype.DigitalEttersendelse
    ),
    OverføreOmsorgsdager(
        tekst = "Omsorgspenger - overføring",
        behandlingstema = Behandlingstema.Omsorgspenger,
        behandlingstype = Behandlingstype.Overføring
    ),
    UtbetaleOmsorgspenger(
        tekst = "Omsorgspenger - utbetaling",
        behandlingstema = Behandlingstema.Omsorgspenger,
        behandlingstype = Behandlingstype.Utbetaling
    ),
    OmsorgspengerForAnsatte(
        tekst = "Omsorgspenger — Ansatte",
        behandlingstema = Behandlingstema.Omsorgspenger,
        behandlingstype = Behandlingstype.Ansatte
    ),
    OmsorgspengerSelvstendigNæringsdrivende(
        tekst = "Omsorgspenger — Selvst næringsdrivende",
        behandlingstema = Behandlingstema.Omsorgspenger,
        behandlingstype = Behandlingstype.SelvstendigNæringsdrivende
    ),
    /** Opplæringspenger **/
    Opplæringspenger(
        tekst = "Opplæringspenger",
        behandlingstema = Behandlingstema.Opplæringspenger
    ),
    /** Pleiepenger pårørende **/
    PleiepengerPårørende(
        tekst = "Pleiepenger pårørende",
        behandlingstema = Behandlingstema.PleiepengerPårørende
    ),
    /** Annet **/
    Annet(
        tekst = "Gjelder noe annet, må velges i Gosys"
    );

    internal companion object {
        internal val JSON = values()
            .associate { it.name to it.tekst }
            .let { objectMapper().writeValueAsString(it) }
    }
}