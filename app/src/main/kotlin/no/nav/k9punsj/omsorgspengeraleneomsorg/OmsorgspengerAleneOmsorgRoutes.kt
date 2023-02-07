package no.nav.k9punsj.omsorgspengeraleneomsorg

import io.swagger.v3.oas.annotations.Operation
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.felles.dto.OpprettNySøknad
import no.nav.k9punsj.felles.dto.SendK9SoknadDto
import no.nav.k9punsj.felles.dto.SendSøknad
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@ProtectedWithClaims(issuer = "azuread")
@RequestMapping("omsorgspenger-alene-om-omsorgen-soknad")
internal class OmsorgspengerAleneOmsorgRoutes(
    private val omsorgspengerAleneOmsorgService: OmsorgspengerAleneOmsorgService
) {

    @GetMapping(
        name = "/mappe",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Hent søknader i mappe")
    fun henteMappe(
        @RequestHeader(value = "X-Nav-NorskIdent", required = true) ident: String
    ): ResponseEntity<SvarOmsAODto> {
        return runBlocking { omsorgspengerAleneOmsorgService.henteMappe(ident) }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping(
        name = "/mappe",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        params = ["soeknad_id"]
    )
    @Operation(summary = "Hent søknad")
    fun henteSoknad(
        @RequestParam("soeknad_id") soknadId: String
    ): ResponseEntity<OmsorgspengerAleneOmsorgSøknadDto> {
        return runBlocking { omsorgspengerAleneOmsorgService.henteSøknad(soknadId) }
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping("/", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Opprett ny søknad")
    fun nySøknad(
        @RequestHeader("X-Nav-NorskIdent") ident: String,
        @RequestBody opprettNySoeknad: OpprettNySøknad
    ): ResponseEntity<OmsorgspengerAleneOmsorgSøknadDto> {
        val (søknadId, søknadDto) = runBlocking { omsorgspengerAleneOmsorgService.nySøknad(opprettNySoeknad) }
        return ResponseEntity
            .created(URI.create("mappe/$søknadId"))
            .body(søknadDto)
    }

    @PutMapping(
        "/oppdater",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Oppdater eksisterende søknad")
    fun oppdaterSøknad(
        @RequestBody soeknadDto: OmsorgspengerAleneOmsorgSøknadDto
    ): ResponseEntity<OmsorgspengerAleneOmsorgSøknadDto> {
        return runBlocking { omsorgspengerAleneOmsorgService.oppdaterEksisterendeSøknad(soeknadDto) }
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.badRequest().build()
    }

    @PostMapping("/send", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendSøknad(
        @RequestBody soeknadDto: SendSøknad
    ): ResponseEntity<SendK9SoknadDto> {
        return runBlocking {
            try {
                omsorgspengerAleneOmsorgService.sendEksisterendeSøknad(soeknadDto)?.let { (feil, soeknad) ->
                    if (feil != null) {
                        ResponseEntity.badRequest().body(feil)
                    } else {
                        ResponseEntity.ok(soeknad)
                    }
                }
            } catch (e: Exception) {
                ResponseEntity.internalServerError()
            }
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping(
        "/valider",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun validerSøknad(
        @RequestBody soeknadDto: OmsorgspengerAleneOmsorgSøknadDto
    ): ResponseEntity<SendK9SoknadDto> {
        return runBlocking {
            try {
                omsorgspengerAleneOmsorgService.validerSøknad(soeknadDto)?.let { (feil, soeknad) ->
                    if (feil != null) {
                        ResponseEntity.badRequest().body(feil)
                    } else {
                        ResponseEntity.ok(soeknad)
                    }
                }
            } catch (e: Exception) {
                ResponseEntity.internalServerError()
            }
            ResponseEntity.notFound().build()
        }
    }
}
