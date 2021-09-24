package no.nav.k9punsj.arbeidsgivere

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate

@Service
internal class ArbeidsgiverService(
    private val aaregClient: AaregClient) {

    private val cache: Cache<Triple<String, LocalDate, LocalDate>, Arbeidsgivere> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .maximumSize(100)
        .build()

    internal suspend fun hentArbeidsgivere(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate) : Arbeidsgivere {
        val cacheKey = Triple(identitetsnummer,fom,tom)

        return when (val cacheValue = cache.getIfPresent(cacheKey)) {
            null -> slåOppArbeidsgivere(
                identitetsnummer = identitetsnummer,
                fom = fom,
                tom = tom
            ).also { cache.put(cacheKey, it) }
            else -> cacheValue
        }
    }

    private suspend fun slåOppArbeidsgivere(
        identitetsnummer: String,
        fom: LocalDate,
        tom: LocalDate) : Arbeidsgivere {
        val arbeidsforhold = aaregClient.hentArbeidsforhold(
            identitetsnummer = identitetsnummer,
            fom = fom,
            tom = tom
        )

        return Arbeidsgivere(
            organisasjoner = arbeidsforhold.organisasjoner.distinctBy { it.organisasjonsnummer }.map { OrganisasjonArbeidsgiver(
                organisasjonsnummer = it.organisasjonsnummer,
                navn = "NAV AS" // TODO
            )}.toSet()
        )
    }
}