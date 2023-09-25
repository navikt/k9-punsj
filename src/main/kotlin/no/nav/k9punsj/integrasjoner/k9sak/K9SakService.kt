package no.nav.k9punsj.integrasjoner.k9sak

import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.felles.dto.PeriodeDto
import no.nav.k9punsj.felles.dto.SaksnummerDto
import no.nav.k9punsj.integrasjoner.k9sak.dto.PunsjetSøknad
import no.nav.k9punsj.integrasjoner.k9sak.dto.SendPunsjetSoeknadTilK9SakGrunnlag

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
     * Bruker behandlingsAar fra PunsjJournalpost for å sikkre riktig periode for saksnummeret.
     */
    suspend fun hentEllerOpprettSaksnummer(k9SaksnummerGrunnlag: HentK9SaksnummerGrunnlag): Pair<String?, String?>

    suspend fun hentSisteSaksnummerForPeriode(
        fagsakYtelseType: FagsakYtelseType,
        periode: PeriodeDto?,
        søker: String,
        pleietrengende: String?
    ): Pair<SaksnummerDto?, String?>

    suspend fun sendInnSoeknad(
        soeknad: PunsjetSøknad,
        grunnlag: SendPunsjetSoeknadTilK9SakGrunnlag
    )
}
