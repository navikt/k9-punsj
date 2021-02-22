package no.nav.k9punsj.db.datamodell

typealias Organisasjonsnummer = String

data class Arbeid(

        val selvstendigNæringsdrivende: List<SelvstendigNæringsdrivende>,
        val frilanser: Frilanser,
        val arbeidstaker: List<Arbeidstaker>

)

data class SelvstendigNæringsdrivende(
        val perioder: Map<Periode, SelvstendigNæringsdrivendeInfo>,
        val organisasjonsnummer: String

)

data class SelvstendigNæringsdrivendeInfo(
        val info: String
)

data class Frilanser(
        val info: String
)

data class Arbeidstaker(
        val norskIdentitetsnummer: NorskIdent?,
        val organisasjonsnummer: Organisasjonsnummer?,
        val arbeidstidInfo:  ArbeidstidInfo?,
)

data class ArbeidstidInfo(
        val test: String
)


