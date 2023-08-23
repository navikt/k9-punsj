package no.nav.k9punsj

import no.nav.k9punsj.kafka.HendelseProducer
import no.nav.k9punsj.util.DatabaseUtil
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

@Profile("test")
annotation class TestProfil

@TestConfiguration
@TestProfil
class TestBeans {
    @Bean
    fun testDataSource(): DataSource = DatabaseUtil.dataSource

    @Bean
    fun testHendelseProducer() = object : HendelseProducer {
        override fun send(topicName: String, data: String, key: String) {
        }
        override fun sendMedOnSuccess(topicName: String, data: String, key: String, onSuccess: () -> Unit) {
            onSuccess.invoke()
        }
    }
}
