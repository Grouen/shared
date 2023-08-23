package shared.extension

fun <T, R> MutableList<T>.addOrReplaceBy(element: T, toTheEnd: Boolean = true, by: (T) -> R): MutableList<T> = apply {
    val elementR = by.invoke(element)
    val indexOfFirst = indexOfFirst { elementR == by.invoke(it) }
    if (indexOfFirst == -1) {
        if (toTheEnd) {
            add(element)
        } else {
            add(0, element)
        }
    } else {
        set(indexOfFirst, element)
    }
}

fun <T> MutableCollection<T>.removeFirstBy(predicate: (T) -> Boolean): T? {
    val iterator = iterator()
    while (iterator.hasNext()) {
        val item = iterator.next()
        if (predicate(item)) {
            iterator.remove()
            return item
        }
    }
    return null
}
