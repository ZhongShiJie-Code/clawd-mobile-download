package com.clawd.mobile.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawd.mobile.data.*
import com.clawd.mobile.ui.components.formatConnectionTime
import com.clawd.mobile.ui.theme.ClawdAccent
import kotlin.math.sqrt

@Composable
internal fun FootprintsPanel(
    snapshot: FootprintsSnapshot?, period: String, loading: Boolean, error: String?,
    connected: Boolean, usage: UsageSnapshot?, modifier: Modifier = Modifier,
    onPeriod: (String) -> Unit, onRefresh: () -> Unit, onConnection: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("足迹", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                TextButton(onClick = onRefresh, enabled = connected && !loading) { Text(if (loading) "读取中" else "刷新") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("today" to "今天", "week" to "本周", "month" to "本月", "year" to "今年").forEach { (key, label) ->
                    TextButton(onClick = { onPeriod(key) }, colors = ButtonDefaults.textButtonColors(
                        contentColor = if (key == period) ClawdAccent else colors.onSurfaceVariant)) { Text(label) }
                }
            }
            Text(error ?: when {
                !connected -> "离线 · 显示上次读取的足迹"
                loading -> "正在读取电脑足迹…"
                snapshot == null -> "暂无足迹，请连接支持此功能的网关"
                else -> "${snapshot.startDate} — ${snapshot.endDate} · ${snapshot.timeZone}"
            }, fontSize = 12.sp, color = colors.onSurfaceVariant)
            snapshot?.let {
                val old = System.currentTimeMillis() - it.timestamp > 5 * 60 * 1000L
                Text("读取于 ${formatConnectionTime(it.timestamp)}${if (old) " · 缓存较旧，请刷新" else " · 手动刷新获取最新记录"}", fontSize = 10.sp, color = colors.onSurfaceVariant)
            }
        }
        if (snapshot != null && snapshot.status == "ready") {
            val rows = snapshot.days.flatMap { it.rows }
            item {
                FootprintCard {
                    Text("会话活动", fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Spacer(Modifier.height(10.dp))
                    val totals = listOf(
                        "开始的会话" to rows.metricTotal(sessionMetric = true) { it.sessionsStarted },
                        "完成回合" to rows.metricTotal { it.turnsCompleted },
                        "工具调用" to rows.metricTotal { it.toolCalls },
                        "活动信号" to rows.metricTotal { it.activityEvents },
                    )
                    totals.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth()) {
                            pair.forEach { (label, value) ->
                                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                    Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant)
                                    Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                                }
                            }
                        }
                    }
                    Text("— 表示没有可靠统计；≥ 表示仅含可统计部分。活动信号不等于工作时长。",
                        fontSize = 11.sp, color = colors.onSurfaceVariant)
                }
            }
            item {
                FootprintCard {
                    Text("活动时段", fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Spacer(Modifier.height(14.dp))
                    if (period == "today") {
                        val hourly = (0..23).map { hour -> rows.sumOf { it.hours.getOrNull(hour) ?: 0L } }
                        val max = hourly.maxOrNull()?.coerceAtLeast(1L) ?: 1L
                        Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = androidx.compose.ui.Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            hourly.forEachIndexed { hour, value ->
                                Box(Modifier.weight(1f).height((if (value == 0L) 2 else (value * 68.0 / max).toInt().coerceAtLeast(3)).dp)
                                    .background(if (value > 0) ClawdAccent else colors.onSurface.copy(alpha = 0.10f), RoundedCornerShape(2.dp))
                                    .semantics { contentDescription = "$hour 时：$value 个活动信号" })
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("00", "06", "12", "18", "23").forEach { Text(it, fontSize = 10.sp, color = colors.onSurfaceVariant) }
                        }
                    } else {
                        val max = snapshot.days.maxOfOrNull { day -> day.rows.sumOf { it.metrics.activityEvents ?: 0 } }?.coerceAtLeast(1) ?: 1
                        // Seven cells per row, aligned to Monday; no-data cells remain distinct from recorded zero.
                        val offset = runCatching { java.time.LocalDate.parse(snapshot.startDate).dayOfWeek.value - 1 }.getOrDefault(0)
                        val cells = List<FootprintsDay?>(offset) { null } + snapshot.days
                        cells.chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                week.forEach { day ->
                                    val value = day?.rows?.sumOf { it.metrics.activityEvents ?: 0 } ?: 0L
                                    val color = when {
                                        day == null -> colors.surfaceVariant.copy(alpha = 0f)
                                        !day.recorded -> colors.onSurface.copy(alpha = 0.04f)
                                        value == 0L -> colors.onSurface.copy(alpha = 0.13f)
                                        else -> ClawdAccent.copy(alpha = (0.25 + 0.75 * sqrt(value.toDouble() / max)).toFloat())
                                    }
                                    Box(Modifier.weight(1f).height(18.dp).background(color, RoundedCornerShape(3.dp))
                                        .semantics { contentDescription = if (day == null) "" else "${day.localDate}：${if (day.recorded) "$value 个活动信号" else "无记录"}" })
                                }
                                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        Text("每行周一至周日 · 颜色越深活动越多 · 淡格无记录", fontSize = 10.sp, color = colors.onSurfaceVariant)
                    }
                    Text("覆盖 ${snapshot.days.count { it.recorded }} / ${snapshot.days.size} 天；电脑未记录的时段不作零用量。",
                        fontSize = 11.sp, color = colors.onSurfaceVariant)
                    if (snapshot.incomplete) Text("部分统计文件不可读取，当前结果不完整", color = colors.error, fontSize = 11.sp)
                }
            }
            rows.groupBy { it.agentId }.forEach { (agent, agentRows) ->
                item(key = agent) {
                    FootprintCard {
                        Text(agent, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Text("回合 ${agentRows.metricTotal { it.turnsCompleted }} · 工具 ${agentRows.metricTotal { it.toolCalls }} · 活动 ${agentRows.metricTotal { it.activityEvents }}",
                            fontSize = 12.sp, color = colors.onSurfaceVariant)
                    }
                }
            }
        } else if (snapshot != null) {
            item { Text("这个时间范围暂无电脑足迹记录。", color = colors.onSurfaceVariant) }
        }
        usage?.let { item { AccountUsagePanel(it, connected) } }
        item {
            Text("账户额度是当前余量，不是历史 Token 消耗账单。", fontSize = 11.sp, color = colors.onSurfaceVariant)
            TextButton(onClick = onConnection) { Text("连接信息") }
        }
    }
}

@Composable
private fun FootprintCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}
