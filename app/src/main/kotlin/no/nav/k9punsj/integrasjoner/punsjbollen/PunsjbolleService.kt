package no.nav.k9punsj.integrasjoner.punsjbollen

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.domenetjenester.dto.PeriodeDto
import no.nav.k9punsj.domenetjenester.dto.SaksnummerDto
import no.nav.k9punsj.journalpost.JournalpostId

interface PunsjbolleService {
    suspend fun opprettEllerHentFagsaksnummer(
        søker: String,
        pleietrengende: String? = null,
        annenPart: String? = null,
        journalpostId: String?,
        periode: PeriodeDto?,
        correlationId: CorrelationId,
        fagsakYtelseType: FagsakYtelseType,
    ): SaksnummerDto

    suspend fun opprettEllerHentFagsaksnummer(
        søker: String,
        pleietrengende: String? = null,
        annenPart: String? = null,
        søknad: Søknad,
        correlationId: CorrelationId,
    ): SaksnummerDto

    suspend fun ruting(
        søker: String,
        pleietrengende: String? = null,
        annenPart: String? = null,
        journalpostId: String?,
        periode: PeriodeDto?,
        correlationId: CorrelationId,
        fagsakYtelseType: FagsakYtelseType,
    ): PunsjbolleRuting
}
