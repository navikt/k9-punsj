package no.nav.k9punsj.metrikker

enum class Metrikk(val navn: String) {
    ANTALL_INNSENDINGER("antall_innsendinger_counter"),
    ANTALL_UKER_SØKNADER_GJELDER_BUCKET("antall_uker_soknaden_gjelder_histogram"),
    JOURNALPOST_COUNTER("journalpost_counter"),
    ANTALL_ARBEIDSGIVERE_BUCKET("antall_arbeidsgivere_histogram"),
    ARBEIDSTID_FRILANSER_COUNTER("arbeidstid_frilanser_counter"),
    ARBEIDSTID_SELVSTENDING_COUNTER("arbeidstid_selvstendig_counter"),
    BEREDSKAP_COUNTER("beredskap_counter"),
    NATTEVAAK_COUNTER("nattevaak_counter"),
    ANTALL_FERDIG_BEHANDLEDE_JOURNALPOSTER("antall_ferdig_behandlede_journalposter"),
    ANTALL_UFERDIGE_BEHANDLEDE_JOURNALPOSTER("antall_uferdige_behandlede_journalposter"),
    ANTALL_OPPRETTET_JOURNALPOST_COUNTER("antall_opprettet_journalpost_counter"),
    TILSYNSORDNING_COUNTER("tilsynsordning_counter");
}
