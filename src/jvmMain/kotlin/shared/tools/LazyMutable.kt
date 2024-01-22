package shared.tools

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> lazyMutable(set: (T) -> LazyMutable.Wrapper<T>? = { LazyMutable.Wrapper(it) }, initializer: () -> T) = LazyMutable(initializer, set)

class LazyMutable<T>(
    private val initializer: () -> T,
    private val set: (T) -> Wrapper<T>?,
) : ReadWriteProperty<Any?, T> {
    companion object {
        private val UNINITIALIZED_VALUE = Any()
    }

    private var prop: Any? = UNINITIALIZED_VALUE

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return if (prop === UNINITIALIZED_VALUE) {
            synchronized(this) {
                return if (prop === UNINITIALIZED_VALUE) initializer().also { prop = it } else prop as T
            }
        } else prop as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        synchronized(this) {
            val wrapper = set.invoke(value)
            if (wrapper != null) prop = wrapper.value
        }
    }

    @JvmInline
    value class Wrapper<T>(val value: T)
}
