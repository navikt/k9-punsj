package no.nav.k9punsj.db.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.k9punsj.db.datamodell.SøknadEntitet
import no.nav.k9punsj.objectMapper
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.sql.DataSource

@Repository
class SøknadRepository(private val dataSource: DataSource) {

    suspend fun opprettSøknad(søknad: SøknadEntitet): SøknadEntitet {
        return using(sessionOf(dataSource)) {
            return@using it.transaction { tx ->
                //language=PostgreSQL
                tx.run(
                    queryOf(
                        """
                    insert into soknad as k (soknad_id, id_bunke, id_person, id_person_barn, barn_fodselsdato, soknad, journalposter)
                    values (:soknad_id, :id_bunke, :id_person, :id_person_barn, :barn_fodselsdato, :soknad :: jsonb, :journalposter :: jsonb)
                    """, mapOf(
                            "soknad_id" to UUID.fromString(søknad.søknadId),
                            "id_bunke" to UUID.fromString(søknad.bunkeId),
                            "id_person" to UUID.fromString(søknad.person),
                            "id_person_barn" to if (søknad.barn != null) UUID.fromString(søknad.barn) else null,
                            "barn_fodselsdato" to søknad.barnFødselsdato,
                            "soknad" to objectMapper().writeValueAsString(søknad.søknad),
                            "journalposter" to objectMapper().writeValueAsString(søknad.journalposter))
                    ).asUpdate
                )
                return@transaction søknad
            }
        }
    }
}
