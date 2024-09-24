package no.nav.k9punsj.journalpost

import no.nav.k9.kodeverk.behandling.FagsakYtelseType
import no.nav.k9punsj.domenetjenester.PersonService
import no.nav.k9punsj.felles.Identitetsnummer.Companion.somIdentitetsnummer
import no.nav.k9punsj.felles.JournalpostId
import no.nav.k9punsj.felles.PunsjFagsakYtelseType
import no.nav.k9punsj.felles.PunsjFagsakYtelseType.Companion.somPunsjFagsakYtelseType
import no.nav.k9punsj.fordel.FordelPunsjEventDto
import no.nav.k9punsj.fordel.HendelseMottaker
import no.nav.k9punsj.fordel.K9FordelType
import no.nav.k9punsj.integrasjoner.dokarkiv.DokarkivGateway
import no.nav.k9punsj.integrasjoner.dokarkiv.SafDtos
import no.nav.k9punsj.integrasjoner.dokarkiv.SafGateway
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.dto.HentK9SaksnummerGrunnlag
import no.nav.k9punsj.journalpost.dto.KopierJournalpostDto
import no.nav.k9punsj.journalpost.dto.KopierJournalpostInfo
import no.nav.k9punsj.journalpost.dto.utledK9sakFagsakYtelseType
import no.nav.k9punsj.utils.PeriodeUtils.somPeriodeDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException

@Service
class JournalpostkopieringService(
    private val journalpostRepository: JournalpostRepository,
    private val k9SakService: K9SakService,
    private val hendeMottaker: HendelseMottaker,
    private val safGateway: SafGateway,
    private val dokarkivGateway: DokarkivGateway,
    private val personService: PersonService
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(JournalpostkopieringService::class.java)
    }

    internal suspend fun kopierJournalpost(
        journalpostId: JournalpostId,
        kopierJournalpostDto: KopierJournalpostDto,
    ): KopierJournalpostInfo {
        val (safJournalpost, k9FagsakYtelseType: FagsakYtelseType) = validerJournalpostKopiering(
            journalpostId = journalpostId,
            kopierJournalpostDto = kopierJournalpostDto
        )

        val k9SakGrunnlag = kopierJournalpostDto.somK9SakGrunnlag(
            k9FagsakYtelseType = k9FagsakYtelseType,
            periodeDto = safJournalpost.datoOpprettet.toLocalDate().somPeriodeDto()
        )

        val saksnummer = hentEllerOpprettSaksnummer(
            journalpostId = journalpostId.toString(),
            kopierJournalpostDto = kopierJournalpostDto,
            k9SakGrunnlag = k9SakGrunnlag
        )

        val tilPersonFnr = kopierJournalpostDto.til
        val nyJournalpostId = dokarkivGateway.knyttTilAnnenSak(
            journalpostId = journalpostId,
            identitetsnummer = tilPersonFnr.somIdentitetsnummer(),
            saksnummer = saksnummer
        )
        logger.info("Kopiert journalpost: $journalpostId til ny journalpost: $nyJournalpostId med saksnummer: $saksnummer")

        val tilPersonAktørId = personService.finnAktørId(tilPersonFnr)
        hendeMottaker.prosesser(
            FordelPunsjEventDto(
                aktørId = tilPersonAktørId,
                journalpostId = nyJournalpostId.toString(),
                type = K9FordelType.KOPI.kode,
                ytelse = k9FagsakYtelseType.kode,
                gosysoppgaveId = null
            )
        )

        return KopierJournalpostInfo(
            nyJournalpostId = nyJournalpostId.toString(),
            saksnummer = saksnummer,
            fra = kopierJournalpostDto.fra,
            til = tilPersonFnr,
            pleietrengende = kopierJournalpostDto.barn,
            annenPart = kopierJournalpostDto.annenPart,
            ytelse = k9FagsakYtelseType.somPunsjFagsakYtelseType()
        )
    }

    private suspend fun validerJournalpostKopiering(
        journalpostId: JournalpostId,
        kopierJournalpostDto: KopierJournalpostDto,
    ): Pair<SafDtos.Journalpost, FagsakYtelseType> {

        val journalpost = journalpostRepository.hentHvis(journalpostId.toString())
            ?: throw KanIkkeKopieresErrorResponse("Finner ikke journalpost i DB.")

        val safJournalpost = safGateway.hentJournalpostInfo(journalpostId.toString())
            ?: throw KanIkkeKopieresErrorResponse("Finner ikke SAF journalpost.")

        if (safJournalpost.erUtgående) {
            throw KanIkkeKopieresErrorResponse("Ikke støttet journalposttype: ${safJournalpost.journalposttype}")
        }

        if (!safJournalpost.kanKopieres) {
            throw KanIkkeKopieresErrorResponse("Kan ikke kopieres. $journalpost.")
        }

        if (journalpost.type != null && journalpost.type == K9FordelType.INNTEKTSMELDING_UTGÅTT.kode) {
            throw KanIkkeKopieresErrorResponse("Kan ikke kopier journalpost med type inntektsmelding utgått.")
        }

        val k9FagsakYtelseType: FagsakYtelseType =
            kopierJournalpostDto.ytelse?.somK9FagsakYtelseType() ?: journalpost.ytelse?.let {
                val punsjFagsakYtelseType = PunsjFagsakYtelseType.fromKode(it)
                journalpost.utledK9sakFagsakYtelseType(punsjFagsakYtelseType.somK9FagsakYtelseType())
            } ?: throw KanIkkeKopieresErrorResponse("Mangler ytelse for journalpost.")

        val støttedeYtelseTyperForKopiering = listOf(
            FagsakYtelseType.OMSORGSPENGER_KS,
            FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
            FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE
        )

        if (!støttedeYtelseTyperForKopiering.contains(k9FagsakYtelseType)) {
            throw KanIkkeKopieresErrorResponse("Støtter ikke kopiering av ${k9FagsakYtelseType.navn} for relaterte journalposter")
        }

        return Pair(safJournalpost, k9FagsakYtelseType)
    }

    private suspend fun hentEllerOpprettSaksnummer(
        journalpostId: String,
        kopierJournalpostDto: KopierJournalpostDto,
        k9SakGrunnlag: HentK9SaksnummerGrunnlag,
    ): String {

        // Sjekker om journalposten kopieres til samme person og logger hvis så er tilfelle
        if (kopierJournalpostDto.fra == kopierJournalpostDto.til) {
            val saksnummer = k9SakService.hentEllerOpprettSaksnummer(k9SakGrunnlag)
            logger.info("Kopierer journalpost: $journalpostId til samme person med saksnummer: $saksnummer")
            return saksnummer // Bruker eksisterende saksnummer for samme person
        }

        // Hvis journalposten kopieres til en annen person, Hent eller opprett nytt saksnummer
        val saksnummer = k9SakService.hentEllerOpprettSaksnummer(
            k9SakGrunnlag.copy(søker = kopierJournalpostDto.til)
        )
        logger.info("Kopierer journalpost: $journalpostId til ny person med saksnummer: $saksnummer")
        return saksnummer
    }

    class KanIkkeKopieresErrorResponse(feil: String, status: HttpStatus = HttpStatus.CONFLICT) :
        ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, feil), null)

}
