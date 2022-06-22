package no.nav.k9punsj.person

import no.nav.k9punsj.IkkeLokalProfil
import no.nav.k9punsj.integrasjoner.pdl.PdlService
import org.springframework.stereotype.Component

@Component
@IkkeLokalProfil
internal class PdlBarnService(
    private val pdlService: PdlService
) : BarnService {

    override suspend fun hentBarn(identitetsnummer: String): Set<Barn> {
        return pdlService.hentPersonopplysninger(
            identitetsnummer = pdlService.hentBarn(identitetsnummer)
        ).filter { it.erUgradert }.map {
            Barn(
                identitetsnummer = it.identitetsnummer,
                fødselsdato = it.fødselsdato,
                fornavn = it.fornavn,
                mellomnavn = it.mellomnavn,
                etternavn = it.etternavn
            )
        }.toSet()
    }
}
