package no.nav.k9punsj.rest.server

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController


@RestController
internal class JournalpostInfoContoller {
    @GetMapping(JournalpostInfoRoutes.Urls.HentÅpneJournalposter, produces = ["application/json"])
    fun HentÅpneJournalposter(@PathVariable("aktor_id") aktørId: String,
                              @RequestHeader("X-Nav-AktorId-barn") aktørIdBarn: String?) {
    }
}
