package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.db.datamodell.Periode
import no.nav.k9punsj.rest.web.SøknadJson
import no.nav.k9punsj.rest.web.dto.SaksnummerDto

interface K9SakService {

    suspend fun hentSisteMottattePsbSøknad(norskIdent: NorskIdent, periode: Periode): SøknadJson?

    suspend fun opprettEllerHentFagsakNummer() : SaksnummerDto

}
