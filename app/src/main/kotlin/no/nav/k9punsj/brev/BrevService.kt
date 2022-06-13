package no.nav.k9punsj.brev

import no.nav.k9punsj.brev.dto.DokumentbestillingDto

interface BrevService {

    suspend fun bestillBrev(
        dokumentbestillingDto: DokumentbestillingDto,
        saksbehandler: String
    ) : Boolean
}
