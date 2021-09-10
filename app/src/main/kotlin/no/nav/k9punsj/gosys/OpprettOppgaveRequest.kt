package no.nav.k9punsj.gosys

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

internal data class OpprettOppgaveRequest(
    val aktoerId: String,
    val journalpostId: String,
    @JsonIgnore
    private val gjelder: Gjelder) {
    val aktivDato = LocalDate.now(ZoneId.of("Europe/Oslo"))
    val fristFerdigstillelse = aktivDato.treVirkerdagerFrem()
    val prioritet = "NORM"
    val tema = "OMS"
    val oppgavetype = "JFR"
    val behandlingstema = gjelder.behandlingstema?.kodeverksverdi
    val behandlingstype = gjelder.behandlingstype?.kodeverksverdi

    private companion object {
        private fun LocalDate.treVirkerdagerFrem() = when (dayOfWeek) {
            DayOfWeek.FRIDAY -> plusDays(5)
            DayOfWeek.SATURDAY -> plusDays(4)
            else -> plusDays(3)
        }
    }
}