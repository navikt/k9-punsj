package no.nav.k9punsj.integrasjoner.punsjbollen

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.CorrelationId
import no.nav.k9punsj.domenetjenester.dto.JournalpostIdDto
import no.nav.k9punsj.domenetjenester.dto.NorskIdentDto
import no.nav.k9punsj.domenetjenester.dto.PeriodeDto
import no.nav.k9punsj.domenetjenester.dto.SaksnummerDto

interface PunsjbolleService {
    suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto? = null,
        annenPart: NorskIdentDto? = null,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId,
        fagsakYtelseType: FagsakYtelseType,
    ): SaksnummerDto

    suspend fun opprettEllerHentFagsaksnummer(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto? = null,
        annenPart: NorskIdentDto? = null,
        søknad: Søknad,
        correlationId: CorrelationId,
    ): SaksnummerDto

    suspend fun ruting(
        søker: NorskIdentDto,
        pleietrengende: NorskIdentDto? = null,
        annenPart: NorskIdentDto? = null,
        journalpostId: JournalpostIdDto?,
        periode: PeriodeDto?,
        correlationId: CorrelationId,
        fagsakYtelseType: FagsakYtelseType,
    ): PunsjbolleRuting
}
