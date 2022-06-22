package no.nav.k9punsj.util

import java.time.Month

internal object MockUtil {
    internal fun String.erFødtI(month: Month) = kotlin.runCatching {
        substring(2, 4) == "${month.value}".padStart(2, '0')
    }.fold(onSuccess = { it }, onFailure = { false })
}
