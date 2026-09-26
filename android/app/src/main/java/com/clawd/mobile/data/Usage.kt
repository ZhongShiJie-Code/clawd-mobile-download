package com.clawd.mobile.data

import kotlinx.serialization.Serializable

/** Usage data forwarded by the desktop gateway. Android does not recalculate billing. */
@Serializable
data class UsageSnapshot(
    val version: String = "",
    val type: String = "",
    val timestamp: Long = 0L,
    val usage: UsageSnapshotData = UsageSnapshotData(),
)

@Serializable
data class UsageSnapshotData(
    val codex: CodexUsage = CodexUsage(),
    val deepseek: DeepSeekUsage = DeepSeekUsage(),
)

@Serializable
data class CodexUsage(
    val status: String = "unavailable",
    val error: String? = null,
    val plan: String? = null,
    val windows: List<QuotaWindow> = emptyList(),
    val updatedAt: Long = 0L,
)

@Serializable
data class QuotaWindow(
    val key: String = "",
    val label: String = "",
    val usedPercent: Int = 0,
    val remainingPercent: Int = 0,
    val resetAt: Long? = null,
)

@Serializable
data class DeepSeekUsage(
    val status: String = "unavailable",
    val error: String? = null,
    val currency: String = "CNY",
    val balance: Double? = null,
    val entries: List<DeepSeekBalanceEntry> = emptyList(),
    val updatedAt: Long = 0L,
)

@Serializable
data class DeepSeekBalanceEntry(
    val currency: String = "CNY",
    val total: Double = 0.0,
)

fun UsageSnapshot.hasDisplayableUsage(): Boolean =
    usage.codex.status == "ok" || usage.deepseek.status == "ok"

private const val USAGE_STALE_AFTER_MS = 30 * 60 * 1000L

fun UsageSnapshot.observedAt(providerUpdatedAt: Long): Long =
    if (providerUpdatedAt > 0L) providerUpdatedAt else timestamp

fun UsageSnapshot.isUsageStale(providerUpdatedAt: Long, now: Long): Boolean {
    val observedAt = observedAt(providerUpdatedAt)
    return observedAt <= 0L || observedAt > now + 5 * 60 * 1000L || now - observedAt > USAGE_STALE_AFTER_MS
}
