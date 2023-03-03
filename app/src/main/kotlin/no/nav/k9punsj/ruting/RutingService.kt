package no.nav.k9punsj.ruting

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.k9punsj.felles.FagsakYtelseType
import no.nav.k9punsj.felles.JournalpostId
import no.nav.k9punsj.integrasjoner.infotrygd.InfotrygdClient
import no.nav.k9punsj.integrasjoner.k9sak.K9SakService
import no.nav.k9punsj.integrasjoner.k9sak.LopendeSakDto
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.time.LocalDate

@Configuration
internal class RutingService(
    private val k9SakService: K9SakService,
    private val infotrygdClient: InfotrygdClient,
    private val overstyrTilK9SakJournalpostIds: Set<JournalpostId>
) {

    init {
        logger.info("JournalpostIder som overstyres til K9Sak=$overstyrTilK9SakJournalpostIds")
    }

    private val cache: Cache<DestinasjonInput, Destinasjon> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(2))
        .maximumSize(100)
        .build()

    internal suspend fun destinasjon(
        søker: String,
        fraOgMed: LocalDate,
        pleietrengende: String? = null,
        annenPart: String? = null,
        fagsakYtelseType: FagsakYtelseType,
        aktørIder: Set<String>,
        journalpostIds: Set<String>,
    ): Destinasjon {

        val input = DestinasjonInput(
            søker = søker,
            fraOgMed = fraOgMed,
            pleietrengende = pleietrengende,
            annenPart = annenPart,
            fagsakYtelseType = fagsakYtelseType,
            aktørIder = aktørIder,
            journalpostIds = journalpostIds
        )

        return when (val cacheValue = cache.getIfPresent(input)) {
            null -> slåOppDestinasjon(input = input).also { cache.put(input, it) }
            else -> cacheValue.also {
                logger.info("Rutes til ${it.name}, oppslaget finnes i cache.")
            }
        }
    }

    private suspend fun slåOppDestinasjon(
        input: DestinasjonInput
    ): Destinasjon {
        logger.info("DEBUG Ruting.slåOppDestinasjon " +
            "ytelse:[${input.fagsakYtelseType}] fraOgMed:[${input.fraOgMed}] journalpostId:[${input.journalpostIds}]") // TODO: Fjernes
        /* PILS sjekkes mot unntaksliste i K9Sak, alla andre ytelser går til K9sak */
        if (input.fagsakYtelseType == FagsakYtelseType.PLEIEPENGER_LIVETS_SLUTTFASE) {
            if (k9SakService.inngårIUnntaksliste(aktørIder = input.aktørIder)) {
                logger.info("Rutes til Infotrygd ettersom minst en part er lagt til i unntakslisten i K9Sak.")
                return Destinasjon.Infotrygd
            }
        } else {
            return Destinasjon.K9Sak
        }

        val overstyresTilK9Sak = input.journalpostIds.intersect(overstyrTilK9SakJournalpostIds)
        if (overstyresTilK9Sak.isNotEmpty()) {
            if (overstyresTilK9Sak == input.journalpostIds) {
                logger.info("Rutes til K9Sak da alle journalpostIdene er overstyrt til K9Sak.")
                return Destinasjon.K9Sak
            } else {
                throw IllegalStateException("Et subset av journalpostIdene er overstyrt til K9Sak. Må enten være ingen, eller alle. JournalpostIds=${input.journalpostIds}, OverstyresTilK9Sak=$overstyresTilK9Sak")
            }
        }

        val lopendeSakDto = LopendeSakDto(
            søker = input.søker,
            fraOgMed = input.fraOgMed,
            pleietrengende = input.pleietrengende,
            annenPart = input.annenPart,
            fagsakYtelseType = input.fagsakYtelseType
        )

        val k9SakGrunnlag = k9SakService.harLopendeSakSomInvolvererEnAv(lopendeSakDto)

        if (k9SakGrunnlag.minstEnPart) {
            logger.info("Rutes til K9Sak ettersom minst en part er involvert i løpende sak. K9Sak=[$k9SakGrunnlag]")
            return Destinasjon.K9Sak
        }

        val infotrygdGrunnlag = infotrygdClient.harLøpendeSakSomInvolvererEnAv(
            søker = input.søker,
            fraOgMed = input.fraOgMed.minusYears(2),
            pleietrengende = input.pleietrengende,
            annenPart = input.annenPart,
            fagsakYtelseType = input.fagsakYtelseType
        )

        return when {
            infotrygdGrunnlag.minstEnPart -> Destinasjon.Infotrygd.also {
                logger.info("Rutes til Infotrygd ettersom minst en part er involvert i en løpende sak. Infotrygd=[$infotrygdGrunnlag]")
            }
            else -> Destinasjon.K9Sak.also {
                logger.info("Rutes til K9Sak ettersom ingen parter er involvert hverken i Infotrygd eller K9Sak fra før")
            }
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(RutingService::class.java)

        private data class DestinasjonInput(
            val søker: String,
            val fraOgMed: LocalDate,
            val pleietrengende: String?,
            val annenPart: String?,
            val fagsakYtelseType: FagsakYtelseType,
            val aktørIder: Set<String>,
            val journalpostIds: Set<String>
        )
    }
}

