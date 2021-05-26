package no.nav.k9punsj.fordel

import no.nav.k9punsj.rest.web.dto.AktørIdDto
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto

data class FordelPunsjEventDto(
        val aktørId: AktørIdDto? = null,
        val journalpostId: JournalpostIdDto,
        val type : String? = null,
        val ytelse : String? = null
)




