package no.nav.syfo.emottak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.syfo.log

fun Route.registerEmottakApi(emottakClient: EmottakClient) {
    post("/emottak/startsubscription") {

        val startSubscriptionRequest = call.receiveNullable<StartSubscriptionRequest>()
        val callId = call.request.headers["Nav-Call-Id"]

        if (startSubscriptionRequest == null) {
            log.warn("Mottatt request uten body")
            call.respond(HttpStatusCode.BadRequest, "Body mangler")
            return@post
        }

        if (callId == null) {
            log.warn("Mangler Nav-Call-Id header")
            call.respond(HttpStatusCode.BadRequest, "Mangler Nav-Call-Id")
            return@post
        }
        log.info("Mottatt proxy-request for emottak start subscription for callId $callId")

        try {
            emottakClient.startSubscription(
                tssIdent = startSubscriptionRequest.tssIdent,
                sender = startSubscriptionRequest.sender,
                partnerreferanse = startSubscriptionRequest.partnerreferanse
            )
            call.respond(HttpStatusCode.OK).also {
                log.info("Sender http OK status for callId $callId")
            }
        } catch (e: Exception) {
            log.error("Noe gikk galt ved kall til emottak: ${e.message} for callId $callId")
            call.respond(
                HttpStatusCode.InternalServerError,
                e.message ?: "Noe gikk galt ved proxykall til emottak"
            )
        }
    }
}
