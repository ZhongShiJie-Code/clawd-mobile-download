package com.clawd.mobile.data

import com.clawd.mobile.ws.MessageParser
import com.clawd.mobile.ws.ParsedMessage
import org.junit.Assert.*
import org.junit.Test

class FootprintsTest {
    @Test fun `AGY parses without overwriting other account quotas`() {
        val result = MessageParser().parse("""{"type":"usage_snapshot","usage":{"antigravity":{"status":"ok","updatedAt":123,"windows":[{"key":"thirdPartyWeekly","remainingPercent":100}]}}}""") as ParsedMessage.Usage
        assertEquals("ok", result.snapshot.usage.antigravity.status)
        assertEquals(123L, result.snapshot.usage.antigravity.updatedAt)
        assertEquals(100, result.snapshot.usage.antigravity.windows.single().remainingPercent)
        assertEquals("unavailable", result.snapshot.usage.codex.status)
        assertTrue(result.snapshot.hasDisplayableUsage())
    }

    @Test fun `legacy quota snapshots remain valid`() {
        val result = MessageParser().parse("""{"type":"usage_snapshot","usage":{"codex":{"status":"ok"}}}""") as ParsedMessage.Usage
        assertEquals("unavailable", result.snapshot.usage.antigravity.status)
    }

    @Test fun `footprints preserve unsupported counts and coverage gaps`() {
        val result = MessageParser().parse("""{"type":"footprints_snapshot","status":"ready","period":"week","days":[{"localDate":"2026-09-26","recorded":true,"rows":[{"agentId":"codex","metrics":{"sessionsStarted":null,"turnsCompleted":4,"toolCalls":9},"hours":[1,2]}]}]}""") as ParsedMessage.Footprints
        assertNull(result.snapshot.days.single().rows.single().metrics.sessionsStarted)
        assertNull(result.snapshot.days.single().coverageMinutes)
        assertEquals(4L, result.snapshot.days.single().rows.single().metrics.turnsCompleted)
    }

    @Test fun `invalid footprints periods cannot populate cache`() {
        assertTrue(MessageParser().parse("""{"type":"footprints_snapshot","period":"evil"}""") is ParsedMessage.Unknown)
    }

    @Test fun `unknown totals and partial totals stay distinct from zero`() {
        val unknown = FootprintsRow(metrics = FootprintsMetrics(toolCalls = null))
        val known = FootprintsRow(metrics = FootprintsMetrics(toolCalls = 0, turnsCompleted = 2))
        assertEquals("—", listOf(unknown).metricTotal { it.toolCalls })
        assertEquals("0", listOf(known).metricTotal { it.toolCalls })
        assertEquals("≥ 0", listOf(unknown, known).metricTotal { it.toolCalls })
    }
}
