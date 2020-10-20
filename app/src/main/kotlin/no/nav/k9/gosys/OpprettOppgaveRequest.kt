package no.nav.k9.gosys

import java.time.LocalDate

data class OpprettOppgaveRequest(
        val aktivDato: LocalDate,
        val aktoerId: String,
        val journalpostId: String,
        val oppgavetype: String,
        val prioritet: Prioritet,
        val tema: String
)

enum class Prioritet {
    HOY, NORM, LAV
}