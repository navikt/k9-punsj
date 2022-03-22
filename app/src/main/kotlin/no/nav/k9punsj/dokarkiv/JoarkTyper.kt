package no.nav.k9punsj.dokarkiv

internal object JoarkTyper {
    internal data class JournalpostStatus private constructor(private val value: String) {
        override fun toString() = value
        internal val erJournalført = value == "JOURNALFOERT"
        internal val erFerdigstilt = value == "FERDIGSTILT"
        internal companion object {
            internal fun String.somJournalpostStatus() = JournalpostStatus(this)
        }
    }

    internal data class JournalpostType private constructor(private val value: String) {
        override fun toString() = value
        internal val erInngående = value == "I"
        internal val erNotat = value == "N"
        internal companion object {
            internal fun String.somJournalpostType() = JournalpostType(this)
        }
    }
}
