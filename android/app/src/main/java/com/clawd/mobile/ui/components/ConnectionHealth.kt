package com.clawd.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.clawd.mobile.R
import com.clawd.mobile.ws.ConnectionIssue
import java.text.DateFormat
import java.util.Date

@Composable
fun connectionIssueText(issue: ConnectionIssue): String? = when (issue) {
    ConnectionIssue.NONE -> null
    ConnectionIssue.AUTH -> stringResource(R.string.connection_issue_auth)
    ConnectionIssue.DNS -> stringResource(R.string.connection_issue_dns)
    ConnectionIssue.TLS -> stringResource(R.string.connection_issue_tls)
    ConnectionIssue.TIMEOUT -> stringResource(R.string.connection_issue_timeout)
    ConnectionIssue.SERVER -> stringResource(R.string.connection_issue_server)
    ConnectionIssue.NETWORK -> stringResource(R.string.connection_issue_network)
    ConnectionIssue.CLOSED -> stringResource(R.string.connection_issue_closed)
    ConnectionIssue.PEER_OFFLINE -> stringResource(R.string.connection_issue_peer_offline)
    ConnectionIssue.UNKNOWN -> stringResource(R.string.connection_issue_unknown)
}

fun formatConnectionTime(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
