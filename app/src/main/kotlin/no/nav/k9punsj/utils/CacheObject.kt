package no.nav.k9punsj.utils

import java.time.LocalDateTime

data class CacheObject<T>( val value:T,  val expire : LocalDateTime = LocalDateTime.now().plusMinutes(5)) {
}
