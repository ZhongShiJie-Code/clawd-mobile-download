package com.clawd.mobile.ws

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

enum class ConnectionIssue {
    NONE, AUTH, DNS, TLS, TIMEOUT, SERVER, NETWORK, CLOSED, PEER_OFFLINE, UNKNOWN
}

data class ConnectionDiagnostic(
    val issue: ConnectionIssue = ConnectionIssue.NONE,
    val lastConnectedAt: Long = 0L,
    val lastMessageAt: Long = 0L,
    val peerConnected: Boolean? = null,
)

fun classifyConnectionIssue(error: Throwable?, httpCode: Int?): ConnectionIssue {
    if (httpCode == 401 || httpCode == 403) return ConnectionIssue.AUTH
    if (httpCode != null && httpCode >= 500) return ConnectionIssue.SERVER
    var cause = error
    while (cause != null) {
        when (cause) {
            is UnknownHostException -> return ConnectionIssue.DNS
            is SSLException -> return ConnectionIssue.TLS
            is SocketTimeoutException -> return ConnectionIssue.TIMEOUT
            is ConnectException -> return ConnectionIssue.NETWORK
        }
        cause = cause.cause
    }
    return ConnectionIssue.UNKNOWN
}
