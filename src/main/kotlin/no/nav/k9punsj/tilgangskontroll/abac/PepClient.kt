package no.nav.k9punsj.tilgangskontroll.abac

import no.nav.k9punsj.StandardProfil
import no.nav.k9punsj.idToken
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import no.nav.k9punsj.tilgangskontroll.audit.*
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt
import no.nav.sif.abac.kontrakt.person.PersonIdent
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.coroutines.coroutineContext

@Configuration
@StandardProfil
class PepClient(
    private val auditlogger: Auditlogger,
    private val pdlService: PdlService,
    private val sifAbacPdpKlient: SifAbacPdpKlient,
) : IPepClient {

    companion object {
        const val TILGANG_SAK = "no.nav.abac.attributter.k9.fagsak"
    }

    override suspend fun harLesetilgang(fnr: String, urlKallet: String): Boolean {
        val harTilgang =
            sifAbacPdpKlient.harTilgangTilPersoner(BeskyttetRessursActionAttributt.READ, listOf(PersonIdent(fnr)))
        if (harTilgang) {
            val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
            loggTilAudit(identTilInnloggetBruker, fnr, EventClassId.AUDIT_ACCESS, TILGANG_SAK, "read", urlKallet)
        }
        return harTilgang
    }

    override suspend fun harLesetilgang(fnr: List<String>, fnrForSporingslogg: List<String>, urlKallet: String): Boolean {
        if (!fnr.containsAll(fnrForSporingslogg)){
            throw IllegalArgumentException("fnrForSporingslogg skal være sub-sett av fnr")
        }
        val harTilgang =
            sifAbacPdpKlient.harTilgangTilPersoner(BeskyttetRessursActionAttributt.READ, fnr.map { PersonIdent(it) })
        if (harTilgang) {
            val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
            fnr.forEach {
                loggTilAudit(identTilInnloggetBruker, it, EventClassId.AUDIT_ACCESS, TILGANG_SAK, "read", urlKallet)
            }
        }
        return harTilgang
    }

    override suspend fun harSendeInnTilgang(fnr: String, urlKallet: String): Boolean {
        val harTilgang =
            sifAbacPdpKlient.harTilgangTilPersoner(BeskyttetRessursActionAttributt.CREATE, listOf(PersonIdent(fnr)))
        if (harTilgang) {
            val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
            loggTilAudit(identTilInnloggetBruker, fnr, EventClassId.AUDIT_CREATE, TILGANG_SAK, "create", urlKallet)
        }
        return harTilgang
    }

    override suspend fun harSendeInnTilgang(fnr: List<String>, fnrForSporingslogg: List<String>, urlKallet: String): Boolean {
        if (!fnr.containsAll(fnrForSporingslogg)){
            throw IllegalArgumentException("fnrForSporingslogg skal være sub-sett av fnr")
        }
        val harTilgang =
            sifAbacPdpKlient.harTilgangTilPersoner(BeskyttetRessursActionAttributt.CREATE, fnr.map { PersonIdent(it) })
        if (harTilgang) {
            val identTilInnloggetBruker = coroutineContext.idToken().getNavIdent()
            fnr.forEach {
                loggTilAudit(identTilInnloggetBruker, it, EventClassId.AUDIT_CREATE, TILGANG_SAK, "create", urlKallet)
            }
        }
        return harTilgang
    }

    override suspend fun erSaksbehandler(): Boolean {
        return coroutineContext.idToken().erSaksbehandler()
    }

    private suspend fun loggTilAudit(
        identTilInnloggetBruker: String,
        fnr: String,
        eventClassId: EventClassId,
        type: String,
        action: String,
        url: String
    ) {
        auditlogger.logg(
            Auditdata(
                header = AuditdataHeader(
                    vendor = auditlogger.defaultVendor,
                    product = auditlogger.defaultProduct,
                    eventClassId = eventClassId,
                    name = "ABAC Sporingslogg",
                    severity = "INFO"
                ),
                fields = setOf(
                    CefField(CefFieldName.EVENT_TIME, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) * 1000L),
                    CefField(CefFieldName.REQUEST, url),
                    CefField(CefFieldName.ABAC_RESOURCE_TYPE, type),
                    CefField(CefFieldName.ABAC_ACTION, action),
                    CefField(CefFieldName.USER_ID, identTilInnloggetBruker),
                    CefField(CefFieldName.BERORT_BRUKER_ID, pdlService.aktørIdFor(fnr))
                )
            )
        )
    }
}
