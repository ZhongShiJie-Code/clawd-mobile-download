package com.clawd.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawd.mobile.R
import com.clawd.mobile.ui.components.ClawdIcons
import com.clawd.mobile.ui.components.connectionIssueText
import com.clawd.mobile.ui.components.formatConnectionTime
import com.clawd.mobile.ui.theme.*
import com.clawd.mobile.service.WsConnectionService
import com.clawd.mobile.ws.ConnectionState
import com.clawd.mobile.ws.ConnectionTag
import com.clawd.mobile.ws.ConnectionDiagnostic
import com.clawd.mobile.ws.StreamingClient

@Composable
internal fun ConnectionStatusCard(
    isConnected: Boolean,
    streamingClient: StreamingClient,
    onScan: () -> Unit,
    onManual: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val colors = MaterialTheme.colorScheme
    val lanState by streamingClient.connectionState.collectAsState()
    val lanDiagnostic by streamingClient.connectionDiagnostic.collectAsState()
    val relayClient = WsConnectionService.getClientByTag(ConnectionTag.RELAY)
    val relayState = relayClient?.connectionState?.collectAsState()?.value
    val relayDiagnostic = relayClient?.connectionDiagnostic?.collectAsState()?.value
    val dotColor = if (isConnected) ClawdGreenBright else colors.onSurfaceVariant
    val statusText = if (isConnected) stringResource(R.string.status_connected) else stringResource(R.string.status_not_connected)
    val statusColor = if (isConnected) ClawdGreenBright else colors.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant.copy(alpha = 0.72f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status dot + text
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(dotColor)
                )
                Text(
                    statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            if (lanState == ConnectionState.CONNECTED) {
                // Connected: show IP + port
                val host = streamingClient.currentHost ?: ""
                val port = streamingClient.currentPort?.toString() ?: ""
                Spacer(modifier = Modifier.height(10.dp))
                CopyableRow(stringResource(R.string.settings_ip_address), host) { clipboard.setText(AnnotatedString(host)) }
                CopyableRow(stringResource(R.string.settings_port), port) { clipboard.setText(AnnotatedString(port)) }
            } else if (!isConnected) {
                // Disconnected: show scan + manual buttons
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onScan,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(ClawdIcons.QrCode, null, modifier = Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_scan_open), color = colors.onSurfaceVariant, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onManual,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(ClawdIcons.DeviceDesktop, null, modifier = Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_manual_open), color = colors.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            ConnectionDetail(stringResource(R.string.connection_lan), lanState, lanDiagnostic)
            if (relayState != null && relayDiagnostic != null) {
                ConnectionDetail(stringResource(R.string.connection_relay), relayState, relayDiagnostic)
            }
            if (!isConnected && relayDiagnostic?.peerConnected != false) {
                TextButton(onClick = { streamingClient.reconnect(); relayClient?.reconnect() }) {
                    Text(stringResource(R.string.connection_retry))
                }
            }
        }
    }
}

@Composable
private fun ConnectionDetail(label: String, state: ConnectionState, diagnostic: ConnectionDiagnostic) {
    val colors = MaterialTheme.colorScheme
    Text(
        "$label · ${if (state == ConnectionState.CONNECTED && diagnostic.peerConnected != false) stringResource(R.string.status_connected) else stringResource(R.string.status_not_connected)}",
        fontSize = 12.sp,
        color = colors.onSurface,
    )
    if (state != ConnectionState.CONNECTED || diagnostic.peerConnected == false) {
        connectionIssueText(diagnostic.issue)?.let {
            Text(it, fontSize = 11.sp, color = colors.onSurfaceVariant)
        }
    }
    Text(
        "${stringResource(R.string.connection_last_success)} · " +
            if (diagnostic.lastConnectedAt > 0L) formatConnectionTime(diagnostic.lastConnectedAt)
            else stringResource(R.string.connection_never_connected),
        fontSize = 11.sp,
        color = colors.onSurfaceVariant,
    )
}

@Composable
private fun CopyableRow(label: String, value: String, onCopy: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.width(60.dp))
        Text(
            value,
            fontSize = 13.sp,
            color = colors.onSurface,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
            Icon(
                ClawdIcons.Checks,
                stringResource(R.string.settings_copy),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
