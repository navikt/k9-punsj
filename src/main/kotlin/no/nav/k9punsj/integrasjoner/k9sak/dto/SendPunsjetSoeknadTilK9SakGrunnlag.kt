package no.nav.k9punsj.integrasjoner.k9sak.dto

import no.nav.k9punsj.felles.JournalpostId

data class SendPunsjetSoeknadTilK9SakGrunnlag(
    internal val saksnummer: String,
    internal val journalpostId: JournalpostId,
    internal val referanse: String
)