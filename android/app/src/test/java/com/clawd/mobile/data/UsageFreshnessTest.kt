package com.clawd.mobile.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageFreshnessTest {
    private val now = 1_800_000_000_000L

    @Test fun usesProviderTimeBeforeEnvelopeTime() {
        val snapshot = UsageSnapshot(timestamp = now)
        assertTrue(snapshot.isUsageStale(now - 31 * 60 * 1000L, now))
        assertFalse(snapshot.isUsageStale(now - 29 * 60 * 1000L, now))
    }

    @Test fun fallsBackToEnvelopeAndRejectsUnknownTime() {
        assertFalse(UsageSnapshot(timestamp = now).isUsageStale(0L, now))
        assertTrue(UsageSnapshot().isUsageStale(0L, now))
    }
}
