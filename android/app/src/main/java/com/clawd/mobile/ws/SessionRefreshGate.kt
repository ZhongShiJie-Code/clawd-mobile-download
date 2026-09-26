package com.clawd.mobile.ws

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Only tool-output painting is batched; state/snapshot/delete bypass the timer. */
internal class SessionRefreshGate(private val scope: CoroutineScope, private val publish: () -> Unit) {
    private val lock = Any()
    private var pending: Job? = null
    private var generation = 0L

    fun immediate() = synchronized(lock) {
        generation++
        pending?.cancel()
        pending = null
        publish()
    }

    fun deferred() = synchronized(lock) {
        if (pending?.isActive == true) return@synchronized
        val expected = ++generation
        pending = scope.launch {
            delay(100)
            synchronized(lock) {
                if (generation == expected) {
                    publish()
                    pending = null
                }
            }
        }
    }
}
