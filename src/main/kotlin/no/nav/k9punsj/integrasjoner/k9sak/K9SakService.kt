package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9.kodeverk.dokument.Brevkode
import no.nav.k9.søknad.Søknad
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SaksnummerDto

interface K9SakService {

    suspend fun hentPerioderSomFinnesIK9(
        søker: String,
        barn: String? = null,
        fagsakYtelseType: FagsakYtelseType
    ): Pair<List<PeriodeDto>?, String?>

    suspend fun hentPerioderSomFinnesIK9ForPeriode(
        søker: String,
        barn: String? = null,
        fagsakYtelseType: FagsakYtelseType,
        periode: PeriodeDto
    ): Pair<List<PeriodeDto>?, String?>

    suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: String,
        fagsakYtelseType: FagsakYtelseType,
        periodeDto: PeriodeDto
    ): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?>

    suspend fun hentFagsaker(søker: String): Pair<Set<Fagsak>?, String?>

    /**
     * Henter saksnummer fra K9Sak, hvis det ikke finnes oppretter vi en ny fagsak.
     * Periode hentes fra soknaden å defaulter til 1/1 og 31/12 hvis fom eller tom ikke er satt.
     */
    suspend fun hentEllerOpprettSaksnummer(søknadId: String): Pair<String?, String?>

    suspend fun hentSisteSaksnummerForPeriode(
        fagsakYtelseType: FagsakYtelseType,
        periode: PeriodeDto?,
        søker: String,
        pleietrengende: String?
    ): Pair<SaksnummerDto?, String?>

    suspend fun sendInnSoeknad(
        soknad: Søknad,
        journalpostId: String,
        fagsakYtelseType: FagsakYtelseType,
        saksnummer: String,
        brevkode: Brevkode
    )
}
