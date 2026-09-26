package com.clawd.mobile.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawd.mobile.data.UsageSnapshot
import com.clawd.mobile.data.isUsageStale
import com.clawd.mobile.data.observedAt
import com.clawd.mobile.R
import com.clawd.mobile.ui.components.formatConnectionTime
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
internal fun AccountUsagePanel(snapshot: UsageSnapshot, isConnected: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = System.currentTimeMillis()
        }
    }
    val codex = snapshot.usage.codex
    val deepseek = snapshot.usage.deepseek
    val deepseekText = deepseek.balance?.let {
        "¥" + String.format(Locale.US, "%.2f", it)
    } ?: "--"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceVariant.copy(alpha = 0.72f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("账户额度", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Text(stringResource(R.string.usage_source), fontSize = 11.sp, color = colors.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Codex", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(5.dp))
                    if (codex.status == "ok" && codex.windows.isNotEmpty()) {
                        codex.windows.take(2).forEach { window ->
                            Text(
                                "${window.label.ifBlank { window.key }} · ${stringResource(R.string.usage_remaining, window.remainingPercent)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface,
                            )
                        }
                    } else {
                        Text("--", fontSize = 15.sp, color = colors.onSurfaceVariant)
                    }
                    UsageFreshness(snapshot, codex.updatedAt, isConnected, now)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("DeepSeek", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        "余额 $deepseekText",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (deepseek.balance != null) colors.onSurface else colors.onSurfaceVariant,
                    )
                    UsageFreshness(snapshot, deepseek.updatedAt, isConnected, now)
                }
            }
            val agy = snapshot.usage.antigravity
            Spacer(modifier = Modifier.height(12.dp))
            Text("AGY / Antigravity", fontSize = 12.sp, color = colors.onSurfaceVariant)
            if (agy.status == "ok" && agy.windows.isNotEmpty()) {
                agy.windows.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { window ->
                            Text(
                                "${window.label} · ${stringResource(R.string.usage_remaining, window.remainingPercent)}",
                                modifier = Modifier.weight(1f), fontSize = 12.sp,
                                fontWeight = FontWeight.Medium, color = colors.onSurface,
                            )
                        }
                    }
                }
            } else {
                Text("暂无桌面额度数据", fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
            UsageFreshness(snapshot, agy.updatedAt, isConnected, now)
        }
    }
}

@Composable
private fun UsageFreshness(snapshot: UsageSnapshot, providerUpdatedAt: Long, isConnected: Boolean, now: Long) {
    val observedAt = snapshot.observedAt(providerUpdatedAt)
    val state = when {
        !isConnected -> stringResource(R.string.usage_offline)
        snapshot.isUsageStale(providerUpdatedAt, now) -> stringResource(R.string.usage_stale)
        else -> null
    }
    val time = if (observedAt > 0L) {
        stringResource(R.string.usage_updated_at, formatConnectionTime(observedAt))
    } else {
        stringResource(R.string.usage_time_unknown)
    }
    Text(
        listOfNotNull(state, time).joinToString(" · "),
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
