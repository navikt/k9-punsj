package no.nav.k9punsj.journalpost

import no.nav.k9punsj.journalpost.dto.SøkUferdigJournalposter
import org.springframework.web.bind.annotation.*

@RestController
internal class JournalpostInfoContoller {
    @PostMapping(JournalpostInfoRoutes.Urls.HentÅpneJournalposterPost, produces = ["application/json"], consumes = ["application/json"])
    fun HentÅpneJournalposterPost(@RequestBody dto: SøkUferdigJournalposter) {
    }
}
