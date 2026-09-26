package com.clawd.mobile.ws

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRefreshGateTest {
    @Test fun `burst publishes latest data once within 100 ms`() = runTest {
        var value = 0
        val published = mutableListOf<Int>()
        val gate = SessionRefreshGate(this) { published.add(value) }
        repeat(100) { value = it; gate.deferred() }
        runCurrent()
        advanceTimeBy(99)
        assertEquals(emptyList<Int>(), published)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(99), published)
    }

    @Test fun `critical state flushes immediately and cancels delayed output`() = runTest {
        var value = "output"
        val published = mutableListOf<String>()
        val gate = SessionRefreshGate(this) { published.add(value) }
        gate.deferred()
        runCurrent()
        value = "finished"
        gate.immediate()
        assertEquals(listOf("finished"), published)
        advanceTimeBy(200)
        runCurrent()
        assertEquals(listOf("finished"), published)
        value = "next output"
        gate.deferred()
        runCurrent()
        advanceTimeBy(100)
        runCurrent()
        assertEquals(listOf("finished", "next output"), published)
    }
}
