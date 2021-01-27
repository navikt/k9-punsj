package no.nav.k9punsj.gosys

data class OpprettOppgaveRequest(
        val aktivDato: String,
        val aktoerId: String,
        val journalpostId: String,
        val oppgavetype: String,
        val prioritet: Prioritet,
        val tema: String
)

enum class Prioritet {
    HOY, NORM, LAV
}
