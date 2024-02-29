package shared.tools

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val mapStringLock: MutableMap<Any, LockAndCounter> = ConcurrentHashMap()

@OptIn(ExperimentalContracts::class)
inline fun <R> withLock(key: Any, block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    val lockAndCounter = mapStringLock.compute(key) { _, mapValue ->
        return@compute (mapValue ?: LockAndCounter()).apply {
            counter.incrementAndGet()
        }
    }!!

    try {
        return lockAndCounter.lock.withLock(block)
    } finally {
        if (lockAndCounter.counter.decrementAndGet() == 0) {
            mapStringLock.compute(key) { _, lockAndCounterInner ->
                if (lockAndCounterInner == null || lockAndCounterInner.counter.get() == 0) {
                    return@compute null
                }

                lockAndCounterInner
            }
        }
    }
}

class LockAndCounter {
    val lock = ReentrantLock()
    val counter = AtomicInteger()
}
