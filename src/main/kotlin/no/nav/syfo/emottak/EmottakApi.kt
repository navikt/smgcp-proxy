package no.nav.syfo.emottak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveOrNull
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.syfo.log

fun Route.registerEmottakApi(emottakClient: EmottakClient) {
    post("/emottak/startsubscription") {
        log.info("Mottatt proxy-request for emottak")
        val startSubscriptionRequest = call.receiveOrNull<StartSubscriptionRequest>()

        if (startSubscriptionRequest == null) {
            log.warn("Mottatt request uten body")
            call.respond(HttpStatusCode.BadRequest, "Body mangler")
            return@post
        }

        try {
            emottakClient.startSubscription(
                tssIdent = startSubscriptionRequest.tssIdent,
                sender = startSubscriptionRequest.sender,
                partnerreferanse = startSubscriptionRequest.partnerreferanse
            )
            call.respond(HttpStatusCode.OK)
        } catch (e: Exception) {
            log.error("Noe gikk galt ved kall til emottak: ${e.message}")
            call.respond(HttpStatusCode.InternalServerError, e.message ?: "Noe gikk galt ved proxykall til emottak")
        }
    }
}
