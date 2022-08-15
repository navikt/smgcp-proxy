package no.nav.syfo.emottak

data class StartSubscriptionRequest(
    val tssIdent: String,
    val sender: ByteArray,
    val partnerreferanse: Int
)
