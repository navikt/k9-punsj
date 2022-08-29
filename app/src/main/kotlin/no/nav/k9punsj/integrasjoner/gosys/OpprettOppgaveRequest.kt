package no.nav.k9punsj.integrasjoner.gosys

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

internal data class OpprettOppgaveRequest(
    val aktoerId: String,
    val journalpostId: String,
    @JsonIgnore
    private val gjelder: Gjelder
) {

    val prioritet = "NORM"
    val aktivDato: LocalDate = LocalDate.now(ZoneId.of("Europe/Oslo"))
    val fristFerdigstillelse: LocalDate = aktivDato.treVirkerdagerFrem()
    val tema = "OMS"
    val oppgavetype = "JFR"
    val behandlingstema = gjelder.behandlingstema?.kodeverksverdi
    val behandlingstype = gjelder.behandlingstype?.kodeverksverdi

    init {
        check(gjelder.aktiv) {
            "Gjelderkategorien $gjelder er ikke aktiv."
        }
    }

    private companion object {
        private fun LocalDate.treVirkerdagerFrem() = when (dayOfWeek) {
            DayOfWeek.FRIDAY -> plusDays(5)
            DayOfWeek.SATURDAY -> plusDays(4)
            else -> plusDays(3)
        }
    }
}

internal data class PatchOppgaveRequest(
    val id: Int,
    val status: OppgaveStatus
)

internal enum class OppgaveStatus {
    OPPRETTET, AAPNET, UNDER_BEHANDLING, FERDIGSTILT, FEILREGISTRERT
}
