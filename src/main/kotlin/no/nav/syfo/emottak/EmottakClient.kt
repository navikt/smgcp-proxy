package no.nav.syfo.emottak

import no.nav.emottak.subscription.StartSubscriptionRequest
import no.nav.emottak.subscription.SubscriptionPort

class EmottakClient(private val subscriptionEmottak: SubscriptionPort) {
    fun startSubscription(tssIdent: String, sender: ByteArray, partnerreferanse: Int) {
        subscriptionEmottak.startSubscription(
            StartSubscriptionRequest().apply {
                key = tssIdent
                data = sender
                partnerid = partnerreferanse
            }
        )
    }
}
