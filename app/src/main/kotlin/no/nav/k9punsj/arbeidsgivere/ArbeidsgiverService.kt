package no.nav.k9punsj.arbeidsgivere

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate

@Service
internal class ArbeidsgiverService(
    private val aaregClient: AaregClient,
    private val eregClient: EregClient,
) {

    private val arbeidsgivereCache: Cache<Triple<String, LocalDate, LocalDate>, Arbeidsgivere> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .maximumSize(100)
        .build()

    private val arbeidsgivereMedIdCache: Cache<Triple<String, LocalDate, LocalDate>, ArbeidsgivereMedArbeidsforholdId> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .maximumSize(100)
        .build()

    private val organisasjonsnavnCache: Cache<String, String> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .maximumSize(100)
        .build()

    internal suspend fun hentArbeidsgivere(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
    ): Arbeidsgivere {
        val cacheKey = Triple(identitetsnummer, fom, tom)

        return when (val cacheValue = arbeidsgivereCache.getIfPresent(cacheKey)) {
            null -> slåOppArbeidsgivere(
                identitetsnummer = identitetsnummer,
                fom = fom,
                tom = tom
            ).also { arbeidsgivereCache.put(cacheKey, it) }
            else -> cacheValue
        }
    }

    internal suspend fun hentArbeidsgivereMedId(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
    ): ArbeidsgivereMedArbeidsforholdId {
        val cacheKey = Triple(identitetsnummer, fom, tom)

        return when (val cacheValue = arbeidsgivereMedIdCache.getIfPresent(cacheKey)) {
            null -> slåOppArbeidsgivereMedId(
                identitetsnummer = identitetsnummer,
                fom = fom,
                tom = tom
            ).also { arbeidsgivereMedIdCache.put(cacheKey, it) }
            else -> cacheValue
        }
    }

    internal suspend fun hentOrganisasjonsnavn(organisasjonsnummer: String) =
        when (val cacheValue = organisasjonsnavnCache.getIfPresent(organisasjonsnummer)) {
            null -> slåOppOrganisasjonsnavn(organisasjonsnummer)?.also {
                organisasjonsnavnCache.put(organisasjonsnummer,
                    it)
            }
            else -> cacheValue
        }

    private suspend fun slåOppArbeidsgivere(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
    ): Arbeidsgivere {
        val arbeidsforhold = aaregClient.hentArbeidsforhold(
            identitetsnummer = identitetsnummer,
            fom = fom,
            tom = tom
        )

        return Arbeidsgivere(
            organisasjoner = arbeidsforhold.organisasjoner.distinctBy { it.organisasjonsnummer }.map {
                OrganisasjonArbeidsgiver(
                    organisasjonsnummer = it.organisasjonsnummer,
                    navn = hentOrganisasjonsnavn(it.organisasjonsnummer) ?: "Ikke tilgjengelig"
                )
            }.toSet()
        )
    }

    private suspend fun slåOppArbeidsgivereMedId(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
    ): ArbeidsgivereMedArbeidsforholdId {
        val arbeidsforhold = aaregClient.hentArbeidsforhold(
            identitetsnummer = identitetsnummer,
            fom = fom,
            tom = tom
        )

        return ArbeidsgivereMedArbeidsforholdId(arbeidsforhold.organisasjoner.groupBy { it.organisasjonsnummer }
            .map { entry ->
                OrganisasjonArbeidsgiverMedId(entry.key,
                    hentOrganisasjonsnavn(entry.key) ?: "Ikke tilgjengelig",
                    entry.value.filter { it.arbeidsforholdId != null }
                        .map { it.arbeidsforholdId!! })
            }.toSet())
    }

    private suspend fun slåOppOrganisasjonsnavn(
        organisasjonsnummer: String,
    ) = eregClient.hentOrganisasjonsnavn(organisasjonsnummer)
}
