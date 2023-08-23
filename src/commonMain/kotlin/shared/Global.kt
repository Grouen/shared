package shared

import kotlinx.serialization.json.Json

typealias Mutator<T> = (T) -> T
typealias Validator<T> = (T) -> Boolean
typealias Callback<T> = (T) -> Unit

inline fun <T, R> T.then(block: (T) -> R) = let(block)

val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}
