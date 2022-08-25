package no.nav.k9punsj.ruting

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.k9punsj.felles.AktørId
import no.nav.k9punsj.felles.CorrelationId
import no.nav.k9punsj.felles.Identitetsnummer
import no.nav.k9punsj.felles.JournalpostId
import no.nav.k9punsj.integrasjoner.infotrygd.InfotrygdClient
import no.nav.k9punsj.integrasjoner.infotrygd.PunsjbolleSøknadstype
import no.nav.k9punsj.integrasjoner.k9sak.K9SakServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.time.LocalDate

@Configuration
internal class RutingService(
    private val k9SakService: K9SakServiceImpl,
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
        søker: Identitetsnummer,
        fraOgMed: LocalDate,
        pleietrengende: Identitetsnummer? = null,
        annenPart: Identitetsnummer? = null,
        punsjbolleSøknadstype: PunsjbolleSøknadstype,
        aktørIder: Set<AktørId>,
        journalpostIds: Set<JournalpostId>,
        correlationId: CorrelationId
    ): Destinasjon {

        val input = DestinasjonInput(
            søker = søker,
            fraOgMed = fraOgMed,
            pleietrengende = pleietrengende,
            annenPart = annenPart,
            punsjbolleSøknadstype = punsjbolleSøknadstype,
            aktørIder = aktørIder,
            journalpostIds = journalpostIds
        )

        return when (val cacheValue = cache.getIfPresent(input)) {
            null -> slåOppDestinasjon(
                input = input,
                correlationId = correlationId
            ).also { cache.put(input, it) }
            else -> cacheValue.also {
                logger.info("Rutes til ${it.name}, oppslaget finnes i cache.")
            }
        }
    }

    private suspend fun slåOppDestinasjon(
        input: DestinasjonInput,
        correlationId: CorrelationId
    ): Destinasjon {

        if (input.punsjbolleSøknadstype == PunsjbolleSøknadstype.PleiepengerLivetsSluttfase) {
            if (k9SakService.inngårIUnntaksliste(
                    aktørIder = input.aktørIder,
                    punsjbolleSøknadstype = input.punsjbolleSøknadstype,
                    correlationId = correlationId
                )
            ) {
                logger.info("Rutes til Infotrygd ettersom minst en part er lagt til i unntakslisten i K9Sak.")
                return Destinasjon.Infotrygd
            }
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

        val k9SakGrunnlag = k9SakService.harLopendeSakSomInvolvererEnAv(
            søker = input.søker,
            fraOgMed = input.fraOgMed,
            pleietrengende = input.pleietrengende,
            annenPart = input.annenPart,
            punsjbolleSøknadstype = input.punsjbolleSøknadstype,
            correlationId = correlationId
        )

        if (k9SakGrunnlag.minstEnPart) {
            logger.info("Rutes til K9Sak ettersom minst en part er involvert i løpende sak. K9Sak=[$k9SakGrunnlag]")
            return Destinasjon.K9Sak
        }

        // Endast PPN kan rutes til Infotrygd, allt annet går direkt till K9Sak
        if (input.punsjbolleSøknadstype != PunsjbolleSøknadstype.PleiepengerLivetsSluttfase) {
            logger.info("Søknadstype ${input.punsjbolleSøknadstype} rutes alltid til K9Sak")
            return Destinasjon.K9Sak
        }

        val infotrygdGrunnlag = infotrygdClient.harLøpendeSakSomInvolvererEnAv(
            søker = input.søker,
            fraOgMed = input.fraOgMed.minusYears(2),
            pleietrengende = input.pleietrengende,
            annenPart = input.annenPart,
            punsjbolleSøknadstype = input.punsjbolleSøknadstype,
            correlationId = correlationId
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

    internal enum class Destinasjon {
        K9Sak,
        Infotrygd
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(RutingService::class.java)

        private data class DestinasjonInput(
            val søker: Identitetsnummer,
            val fraOgMed: LocalDate,
            val pleietrengende: Identitetsnummer?,
            val annenPart: Identitetsnummer?,
            val punsjbolleSøknadstype: PunsjbolleSøknadstype,
            val aktørIder: Set<AktørId>,
            val journalpostIds: Set<JournalpostId>
        )
    }
}

