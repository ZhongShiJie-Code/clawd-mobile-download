package com.clawd.mobile.ws

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionDiagnosticTest {
    @Test fun classifiesAuthenticationAndServerResponses() {
        assertEquals(ConnectionIssue.AUTH, classifyConnectionIssue(null, 401))
        assertEquals(ConnectionIssue.AUTH, classifyConnectionIssue(null, 403))
        assertEquals(ConnectionIssue.SERVER, classifyConnectionIssue(null, 503))
    }

    @Test fun classifiesNetworkFailuresWithoutExposingMessages() {
        assertEquals(ConnectionIssue.DNS, classifyConnectionIssue(UnknownHostException("secret host"), null))
        assertEquals(ConnectionIssue.TLS, classifyConnectionIssue(SSLHandshakeException("certificate"), null))
        assertEquals(ConnectionIssue.TIMEOUT, classifyConnectionIssue(SocketTimeoutException(), null))
        assertEquals(ConnectionIssue.NETWORK, classifyConnectionIssue(ConnectException(), null))
        assertEquals(ConnectionIssue.DNS, classifyConnectionIssue(RuntimeException(UnknownHostException()), null))
    }
}
