package no.nav.syfo.ws

data class OidcToken(val access_token: String, val token_type: String, val expires_in: Long)
