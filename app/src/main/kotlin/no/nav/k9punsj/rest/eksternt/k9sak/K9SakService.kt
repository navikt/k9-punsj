package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.db.datamodell.Periode
import no.nav.k9punsj.rest.web.dto.PleiepengerSøknadDto
import no.nav.k9punsj.rest.web.dto.SaksnummerDto

interface K9SakService {

    suspend fun hentSisteMottattePsbSøknad(norskIdent: NorskIdent, periode: Periode): PleiepengerSøknadDto?

    suspend fun opprettEllerHentFagsakNummer() : SaksnummerDto

}
