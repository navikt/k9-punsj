package no.nav.k9punsj.util

import java.util.concurrent.atomic.AtomicLong

internal object IdGenerator {
    private val Id = AtomicLong(10000000001)
    internal fun nesteId() = "${Id.getAndIncrement()}"
}