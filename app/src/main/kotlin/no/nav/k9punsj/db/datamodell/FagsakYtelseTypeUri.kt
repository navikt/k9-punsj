package no.nav.k9punsj.db.datamodell

sealed class FagsakYtelseTypeUri {
        companion object {
                internal const val PLEIEPENGER_SYKT_BARN = "pleiepenger-sykt-barn-soknad"
                internal const val PLEIEPENGER_LIVETS_SLUTTFASE = "pleiepenger-livets-sluttfase-soknad"
                internal const val OMSORGSPENGER_DELING = "omsorgspenger-deling-av-omsorgsdager-melding"
                internal const val OMSORGSPENGER_OVERFØRING_DAGER = "omsorgspenger-overfoer-dager-soknad"
                internal const val OMSORGSPENGER_KRONISK_SYKT_BARN = "omsorgspenger-kronisk-sykt-barn-soknad"
                internal const val OMSORGSPENGER_ALENE_OM_OMSORGEN = "omsorgspenger-alene-om-omsorgen-soknad"
                internal const val OMSORGSPENGER_MIDLERTIDIG_ALENE = "omsorgspenger-midlertidig-alene-soknad"
                internal const val OMSORGSPENGER = "omsorgspenger-soknad"
                internal const val UKJENT = "ukjent"
        }
}




