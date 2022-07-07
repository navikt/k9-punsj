package no.nav.k9punsj.felles.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.felles.DurationMapper.korrigereArbeidstidRettOver80Prosent
import no.nav.k9punsj.felles.DurationMapper.somDuration
import no.nav.k9punsj.felles.DurationMapper.somTimerOgMinutter
import no.nav.k9punsj.felles.dto.TimerOgMinutter.Companion.somTimerOgMinutterDto
import java.math.BigDecimal
import java.time.LocalDate

data class ArbeidAktivitetDto(
    val selvstendigNaeringsdrivende: SelvstendigNæringsdrivendeDto?,
    val frilanser: FrilanserDto?,
    val arbeidstaker: List<ArbeidstakerDto>?
) {
    data class SelvstendigNæringsdrivendeDto(
        val organisasjonsnummer: String?,
        val virksomhetNavn: String?,
        val info: SelvstendigNæringsdrivendePeriodeInfoDto?
    ) {
        data class SelvstendigNæringsdrivendePeriodeInfoDto(
            val periode: PeriodeDto?,
            val virksomhetstyper: List<String>?,
            val registrertIUtlandet: Boolean?,
            val landkode: String?,
            val regnskapsførerNavn: String?,
            val regnskapsførerTlf: String?,
            val bruttoInntekt: BigDecimal?,
            val erNyoppstartet: Boolean?,
            val erVarigEndring: Boolean?,
            val endringInntekt: BigDecimal?,
            @JsonFormat(pattern = "yyyy-MM-dd")
            val endringDato: LocalDate?,
            val endringBegrunnelse: String?
        )
    }

    data class FrilanserDto(
        @JsonFormat(pattern = "yyyy-MM-dd")
        val startdato: String?,
        @JsonFormat(pattern = "yyyy-MM-dd")
        val sluttdato: String?,
        val jobberFortsattSomFrilans: Boolean?
    )

    data class ArbeidstakerDto(
        val norskIdent: String?,
        val organisasjonsnummer: String?,
        val arbeidstidInfo: ArbeidstidInfoDto?
    ) {
        data class ArbeidstidInfoDto(
            val perioder: List<ArbeidstidPeriodeInfoDto>?
        ) {
            data class ArbeidstidPeriodeInfoDto(
                val periode: PeriodeDto?,
                val faktiskArbeidTimerPerDag: String?,
                val jobberNormaltTimerPerDag: String?,
                //val faktiskArbeidPerDag: TimerOgMinutter? = faktiskArbeidTimerPerDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto(),
                val faktiskArbeidPerDag: TimerOgMinutter? = korrigereArbeidstidRettOver80Prosent(
                    faktiskArbeidTimerPerDag, jobberNormaltTimerPerDag
                ),
                val jobberNormaltPerDag: TimerOgMinutter? = jobberNormaltTimerPerDag?.somDuration()?.somTimerOgMinutter()?.somTimerOgMinutterDto()
            )
        }
    }
}
