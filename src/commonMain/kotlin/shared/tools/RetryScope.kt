package shared.tools

inline fun <R> withRetry(
    times: Int,
    isValidException: (Throwable) -> Boolean = { true },
    actionBeforeNextTry: ((Int) -> Unit) = {},
    block: RetryScope.() -> R,
): R {
    var index = 1
    var lastException: Throwable? = null

    val retryScope = object : RetryScope {
        override fun resetRetry() {
            index = 1
        }
    }

    while (index++ <= times) {
        try {
            return block.invoke(retryScope)
        } catch (e: Throwable) {
            if (isValidException.invoke(e)) {
                lastException = e
                actionBeforeNextTry(index)
            } else {
                throw e
            }
        }
    }

    throw lastException!!
}

interface RetryScope {
    fun resetRetry()
}
