package no.nav.k9punsj.kafka

class Topics {

    //TODO (legg alle topics her og beskriv hva som er p: (producer) og c: (consumer)
    internal companion object {

        // topic (p:k9-punsj -> c:k9-los-api) for å lage oppgaver i k9-los
        internal const val SEND_AKSJONSPUNKTHENDELSE_TIL_K9LOS = "privat-k9punsj-aksjonspunkthendelse-v1"

        // topic (p:k9-punsj -> c:k9-fordel) for å sende søknader inn til k9-fordel
        internal const val PLEIEPENGER_SYKT_BARN_TOPIC = "privat-punsjet-pleiepengesoknad"
    }
}
