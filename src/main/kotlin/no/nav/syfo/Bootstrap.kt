package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import io.prometheus.client.hotspot.DefaultExports
import java.net.URL
import java.util.concurrent.TimeUnit
import no.nav.emottak.subscription.SubscriptionPort
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.emottak.EmottakClient
import no.nav.syfo.ws.createPort
import org.apache.cxf.ws.addressing.WSAddressingFeature
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.smgcp-proxy")

fun main() {
    val env = Environment()
    val serviceUser = ServiceUser()
    DefaultExports.initialize()

    val jwkProvider =
        JwkProviderBuilder(URL(env.jwkKeysUrl))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

    val subscriptionEmottak =
        createPort<SubscriptionPort>(env.emottakEndpointURL) {
            proxy { features.add(WSAddressingFeature()) }
            port { withBasicAuth(serviceUser.username, serviceUser.password) }
        }
    val emottakClient = EmottakClient(subscriptionEmottak)

    val applicationState = ApplicationState()
    val applicationEngine =
        createApplicationEngine(env, applicationState, jwkProvider, emottakClient)
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
}
