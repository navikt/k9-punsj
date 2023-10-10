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

    val tema = "OMS"

    init {
        check(gjelder.aktiv) {
            "Gjelderkategorien $gjelder er ikke aktiv."
        }
    }
}

internal data class PatchOppgaveRequest(
    val id: Int,
    val versjon: Int,
    val status: OppgaveStatus
)

internal data class GetOppgaveResponse(
    val id: Int,
    val versjon: Int,
    val status: OppgaveStatus
)

internal enum class OppgaveStatus {
    OPPRETTET, AAPNET, UNDER_BEHANDLING, FERDIGSTILT, FEILREGISTRERT
}
