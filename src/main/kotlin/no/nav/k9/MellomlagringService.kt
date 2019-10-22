package no.nav.k9

import org.springframework.stereotype.Service

@Service
internal class MellomlagringService {
    private val map = mutableMapOf<Pair<JournalpostId, Innholdstype>, Innsending>()

    internal suspend fun lagre(
            journalPostId: JournalpostId,
            innholdstype: Innholdstype,
            innsending: Innsending) {
        map[Pair(journalPostId, innholdstype)] = innsending
    }

    internal suspend fun hent(
            journalPostId: JournalpostId,
            innholdstype: Innholdstype) = map.getOrDefault(Pair(journalPostId, innholdstype), null)

    internal suspend fun slett(
            journalPostId: JournalpostId,
            innholdstype: Innholdstype) {
        map.remove(Pair(journalPostId, innholdstype))
    }
}

interface Innholdstype {
    fun type() : String
}
