package no.nav.k9.pleiepengersyktbarn.soknad

import no.nav.k9.*
import no.nav.k9.MellomlagringService
import org.springframework.stereotype.Service

@Service
internal class PleiepengerSyktBarnSoknadService(
        private val mellomlagringService: MellomlagringService
) {

    private companion object {
        private val innholdstype = PleiepengerSyktBarnSoknadInnholdstype()
    }

    internal suspend fun oppdater(journalpostId: JournalpostId,
                          nyInnsending: Innsending) : Innsending {
        val lagretInnsending = mellomlagringService.hent(journalpostId, innholdstype)
        val oppdatertInnsending = lagretInnsending?.oppdater(nyInnsending) ?: nyInnsending
        mellomlagringService.lagre(journalpostId, innholdstype, oppdatertInnsending)
        return oppdatertInnsending
    }

    internal suspend fun hent(journalpostId: JournalpostId) = mellomlagringService.hent(journalpostId, innholdstype)

    internal suspend fun sendKompletteSøknader(journalpostId: JournalpostId, innsending: Innsending) {
        // 1. Oppdater JournalPost med innsendingen
        mellomlagringService.slett(journalpostId, innholdstype)
    }

    internal suspend fun sendUkompletteSøknader(journalpostId: JournalpostId, innsending: Innsending) {
        // 1. Oppdater JournalPost med innsendingen
        mellomlagringService.slett(journalpostId, innholdstype)
    }
}

typealias PleiepengerSyktBarnSøknad = Innhold

private class PleiepengerSyktBarnSoknadInnholdstype : Innholdstype {
    override fun type() = "PleiepengerSyktBarn"
}