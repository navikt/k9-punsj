package no.nav.k9punsj.barn

import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import org.springframework.stereotype.Component

@Component
internal class BarnService(
    private val pdlService: PdlService) {

    internal suspend fun hentBarn(idenitetsnummer: String) : Set<Barn> {
        return pdlService.hentPersonopplysninger(
            identitetsnummer = pdlService.hentBarn(idenitetsnummer)
        ).filter { it.erUgradert }.map { Barn(
            identitetsnummer = it.identitetsnummer,
            fødselsdato = it.fødselsdato,
            fornavn = it.fornavn,
            mellomnavn = it.mellomnavn,
            etternavn = it.etternavn
        )}.toSet()
    }
}