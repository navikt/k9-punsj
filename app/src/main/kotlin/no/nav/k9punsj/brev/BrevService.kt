package no.nav.k9punsj.brev

import no.nav.k9punsj.brev.dto.BrevType
import no.nav.k9punsj.brev.dto.DokumentbestillingDto

interface BrevService {

    suspend fun bestillBrev(
        brevData: DokumentbestillingDto,
        brevType: BrevType,
        saksbehandler: String
    ) : Boolean
}
