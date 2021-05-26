package no.nav.k9punsj.db.datamodell.pleiepengersyktbarn

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.k9punsj.db.datamodell.*
import java.math.BigDecimal
import java.time.LocalDate


data class PleiepengersyktbarnEntitet(

    val søker: Søker,
    val ytelse: PleiepengerYtelse,
) {
    data class Søker(
        val personId: PersonId,
    )

    data class PleiepengerYtelse(
        val barn: Barn,
        val søknadsperiode: String?,
        val arbeidAktivitet: ArbeidAktivitet?,
        val dataBruktTilUtledning: DataBruktTilUtledning?,
        val bosteder: Bosteder?,
        val utenlandsopphold: Utenlandsopphold?,
        val beredskap: Beredskap?,
        val nattevåk: Nattevåk?,
        val tilsynsordning: Tilsynsordning?,
        val lovbestemtFerie: LovbestemtFerie?,
        val arbeidstid: Arbeidstid?,
        val uttak: Uttak?,
        val omsorg: Omsorg?,
    ) {
        data class Barn(
            val personId: PersonId,
            val fødselsdato: LocalDate?,
        )

        data class ArbeidAktivitet(
            val selvstendigNæringsdrivende: List<SelvstendigNæringsdrivende>?,
            val frilanser: FrilanserDto?,
            val arbeidstaker: List<Arbeidstaker>?,
        ) {
            data class SelvstendigNæringsdrivende(
                val perioder: Map<String, SelvstendigNæringsdrivendePeriodeInfo>?,
                val organisasjonsnummer: String?,
                val virksomhetNavn: String?,
            ) {
                data class SelvstendigNæringsdrivendePeriodeInfo(
                    val virksomhetstyper: List<String>?,
                    val regnskapsførerNavn: String?,
                    val regnskapsførerTlf: String?,
                    val erVarigEndring: Boolean?,
                    val endringDato: LocalDate?,
                    val endringBegrunnelse: String?,
                    val bruttoInntekt: BigDecimal?,
                    val erNyoppstartet: Boolean?,
                    val registrertIUtlandet: Boolean?,
                    val landkode: String?,
                )
            }

            data class FrilanserDto(
                @JsonFormat(pattern = "yyyy-MM-dd")
                val startdato: LocalDate?,
                val jobberFortsattSomFrilans: Boolean?,

                )

            data class Arbeidstaker(
                val personId: NorskIdent?,
                val organisasjonsnummer: String?,
                val arbeidstidInfo: ArbeidstidInfo,
            ) {
                data class ArbeidstidInfo(
                    val jobberNormaltTimerPerDag: String?,
                    val perioder: Map<String, ArbeidstidPeriodeInfo>?,
                ) {
                    data class ArbeidstidPeriodeInfo(
                        val faktiskArbeidTimerPerDag: String?,
                    )
                }
            }
        }

        data class DataBruktTilUtledning(
            val harForståttRettigheterOgPlikter: Boolean?,
            val harBekreftetOpplysninger: Boolean?,
            val samtidigHjemme: Boolean?,
            val harMedsøker: Boolean?,
            val bekrefterPeriodeOver8Uker: Boolean?,
        )

        data class Bosteder(
            val perioder: Map<String, BostedPeriodeInfo>?,
        ) {
            data class BostedPeriodeInfo(
                val land: String?,
            )
        }

        data class Utenlandsopphold(
            val perioder: Map<String, UtenlandsoppholdPeriodeInfo>?,

            ) {
            data class UtenlandsoppholdPeriodeInfo(
                val land: String?,
                val årsak: String?,
            )
        }

        data class Beredskap(
            val perioder: Map<String, BeredskapPeriodeInfo>?,

            ) {
            data class BeredskapPeriodeInfo(
                val tilleggsinformasjon: String?,
            )
        }

        data class Nattevåk(
            val perioder: Map<String, NattevåkPeriodeInfo>?,
        ) {
            data class NattevåkPeriodeInfo(
                val tilleggsinformasjon: String?,
            )
        }

        data class Tilsynsordning(
            val perioder: Map<String, TilsynPeriodeInfoDto>?,
        ) {
            data class TilsynPeriodeInfoDto(
                val etablertTilsynTimerPerDag: String?,
            )
        }

        data class LovbestemtFerie(
            val perioder: List<String>?,
        )

        data class Arbeidstid(
            val arbeidstakerList: List<ArbeidAktivitet.Arbeidstaker>?,
            val frilanserArbeidstidInfo: ArbeidAktivitet.Arbeidstaker.ArbeidstidInfo?,
            val selvstendigNæringsdrivendeArbeidstidInfo: ArbeidAktivitet.Arbeidstaker.ArbeidstidInfo?,
        )

        data class Uttak(
            val perioder: Map<String, UttakPeriodeInfo>,
        ) {
            data class UttakPeriodeInfo(
                val timerPleieAvBarnetPerDag: String?,
            )
        }
        data class Omsorg(
            val relasjonTilBarnet: String?,
            val samtykketOmsorgForBarnet: Boolean?,
            val beskrivelseAvOmsorgsrollen: String?,
        )
    }
}









