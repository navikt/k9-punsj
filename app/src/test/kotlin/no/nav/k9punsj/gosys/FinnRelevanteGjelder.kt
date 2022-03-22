package no.nav.k9punsj.gosys

import no.nav.k9punsj.integrasjoner.gosys.Behandlingstema
import no.nav.k9punsj.integrasjoner.gosys.Behandlingstype
import no.nav.k9punsj.integrasjoner.gosys.Gjelder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class FinnRelevanteGjelder {

    @Test
    fun `Finne gyldige verdier for gjelder`() {
        val alle = KodeverkCsv
            .lines()
            .filterNot { it.startsWith("Underkategori") }
            .map {
                val split = it.replace(" ", "").split(";")
                KodeverkEntry(
                    underkategori = split[0],
                    behandlingstema = split[1].blankAsNull(),
                    behandlingstype = split[2].blankAsNull(),
                    tema = split[3]
                )
            }.filter { it.tema == "OMS" }
            .filterNot { it in kodeverkEntriesSomIkkeSkalBrukes }

        println("Antall: ${alle.size}")
        val finnesIPunsj = alle.filter { it.somAktivGjelderOrNull() != null }
        println("Finnes i Punsj: ${finnesIPunsj.size}")
        finnesIPunsj.forEach { println(" - $it (${it.somAktivGjelderOrNull()})") }
        val ugyldigeGjelderIPunsj = Gjelder.aktive().minus(Gjelder.Annet).minus(alle.mapNotNull { it.somAktivGjelderOrNull() })
        println("Ugyldige verdier i Punsj: ${ugyldigeGjelderIPunsj.size}")
        ugyldigeGjelderIPunsj.forEach { println(" - $it") }
        val manglerIPunsj = alle.minus(finnesIPunsj)
        println("Mangler i Punsj: ${manglerIPunsj.size}")
        manglerIPunsj.forEach { println(" - $it (${it.behandlingstema.behandlingstemaOrNull()}/${it.behandlingstype.behandlingstypeOrNull()})") }
        assertThat(manglerIPunsj.map { it.underkategori }).hasSameElementsAs(underkategorierSomIkkeTilbysIPunsj)
    }

    private companion object {

        private fun String.blankAsNull() = when (isBlank()) {
            true -> null
            false -> this
        }
        private fun String?.behandlingstypeOrNull() = Behandlingstype.values().firstOrNull { it.kodeverksverdi == this }
        private fun String?.behandlingstemaOrNull() = Behandlingstema.values().firstOrNull { it.kodeverksverdi == this }
        private val kodeverkEntriesSomIkkeSkalBrukes = listOf(
            KodeverkEntry(underkategori = "PLPNG_NY_DIG_SOK_OMS", behandlingstema = "ab0320", "ae0227", tema = "OMS"), // Digital søknad, skal ikke inn i Punsj
            KodeverkEntry(underkategori = "OMSPNG_DIG_SOK_OMS", behandlingstema = "ab0149", "ae0227", tema = "OMS"), // Digital søknad, skal ikke inn i Punsj
            KodeverkEntry(underkategori = "PLPNG_OMS", "ab0435", behandlingstype = null, tema = "OMS"), // Generell kategori "Pleiepenger" som ikke skal brukes
            KodeverkEntry(underkategori = "PLEIEPENGERSY_OMS", behandlingstema = "ab0069", behandlingstype = null, tema = "OMS") // Pleiepenger sykt barn gammel ordning
        )
        private val underkategorierSomIkkeTilbysIPunsj = listOf(
            "VEDTAK_OMS", "UTBETALING_OMS", "UTLAND_OMS", "TIDLIG_HJEMSENDT_OMS",
            "HJEMSENDT_NY_OMS", "SAM_BS_OMS", "FEILUTB_UTL_OMS", "FEILUTB_OMS",
            "EU_EOS_NY_BEH_OMS", "MEDLEM_OMS", "KLAGE_ANKE_OMS", "PART_OMS",
            "PLEIEPENGERIN_OMS", "OMSPNG_UTBET_OMS", "OMSPNG_OVERFOR_OMS",
            "PLPNG_NY_DIG_ETT_OMS", "OMSPNG_SELVS_OMS", "OMSPNG_ANSATTE_OMS",
            "OMSPNG_DIG_ETT_OMS"
        )

        data class KodeverkEntry(
            val underkategori: String,
            val behandlingstema: String?,
            val behandlingstype: String?,
            val tema: String) {
            fun somAktivGjelderOrNull() = Gjelder.aktive()
                .firstOrNull { behandlingstema == it.behandlingstema?.kodeverksverdi && behandlingstype == it.behandlingstype?.kodeverksverdi}
        }

        /**
         * Gydlige verdier ligger på privat GitHub-repo;
         * https://raw.githubusercontent.com/navikt/kodeverksmapper/master/web/src/main/resources/underkategori.csv
         * Ved å legge til hele filens innhold her og kjøre testen vil den printe ut mangler om det er tilkommet noe nytt
         * på tema OMS. Disse veridene er de som gjelder tema OMS per 10.09.2021
         */
        private val KodeverkCsv = """
        Underkategori;Behandlingstema;Behandlingstype;Tema
        VEDTAK_OMS;;ae0004;OMS
        UTBETALING_OMS;;ae0007;OMS
        ANKE_OMS;;ae0046;OMS
        KLAGE_OMS;;ae0058;OMS       
        UTLAND_OMS;;ae0106;OMS
        TIDLIG_HJEMSENDT_OMS;;ae0114;OMS
        HJEMSENDT_NY_OMS;;ae0115;OMS
        SAM_BS_OMS;;ae0119;OMS
        FEILUTB_UTL_OMS;;ae0160;OMS
        FEILUTB_OMS;;ae0161;OMS
        KLAGE_ANKE_OMS;;ae0173;OMS
        PLEIEPENGERSY_OMS;ab0069;;OMS
        PLEIEPENGERPA_OMS;ab0094;;OMS
        PLEIEPENGERIN_OMS;ab0137;;OMS
        OPPLARINGSPE_OMS;ab0141;;OMS
        OMSORGSPE_OMS;ab0149;;OMS
        PLEIEPENGERNY_OMS;ab0320;;OMS
        PART_OMS;;ae0224;OMS
        PLPNG_NY_DIG_SOK_OMS;ab0320;ae0227;OMS
        OMSPNG_DIG_SOK_OMS;ab0149;ae0227;OMS
        EU_EOS_NY_BEH_OMS;;ae0237;OMS
        OMSPNG_UTBET_OMS;ab0149;ae0007;OMS
        OMSPNG_OVERFOR_OMS;ab0149;ae0085;OMS
        PLPNG_NY_DIG_ETT_OMS;ab0320;ae0246;OMS
        OMSPNG_DIG_ETT_OMS;ab0149;ae0246;OMS
        OMSPNG_SELVS_OMS;ab0149;ae0221;OMS
        OMSPNG_ANSATTE_OMS;ab0149;ae0249;OMS
        PLPNG_OMS;ab0435;;OMS
        MEDLEM_OMS;ab0269;;OMS
        """.trimIndent()
    }
}