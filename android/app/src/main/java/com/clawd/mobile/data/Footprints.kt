package com.clawd.mobile.data

import kotlinx.serialization.Serializable

@Serializable
data class FootprintsSnapshot(
    val status: String = "unavailable",
    val timestamp: Long = 0L,
    val period: String = "today",
    val startDate: String = "",
    val endDate: String = "",
    val timeZone: String = "",
    val incomplete: Boolean = false,
    val days: List<FootprintsDay> = emptyList(),
)

@Serializable
data class FootprintsDay(
    val localDate: String = "",
    val recorded: Boolean = false,
    val rows: List<FootprintsRow> = emptyList(),
    val coverageMinutes: List<Int>? = null,
    val hourCapacities: List<Int>? = null,
)

@Serializable
data class FootprintsRow(
    val agentId: String = "",
    val scope: String = "local",
    val metrics: FootprintsMetrics = FootprintsMetrics(),
    val hours: List<Long> = emptyList(),
    val sessionsStartedPartial: Boolean = false,
)

@Serializable
data class FootprintsMetrics(
    val sessionsStarted: Long? = null,
    val turnsCompleted: Long? = null,
    val toolCalls: Long? = null,
    val activityEvents: Long? = null,
)

fun List<FootprintsRow>.metricTotal(sessionMetric: Boolean = false, select: (FootprintsMetrics) -> Long?): String {
    val values = map { select(it.metrics) }
    val known = values.filterNotNull()
    if (known.isEmpty()) return "—"
    val partial = values.any { it == null } || (sessionMetric && any { it.sessionsStartedPartial })
    return (if (partial) "≥ " else "") + known.sum()
}
