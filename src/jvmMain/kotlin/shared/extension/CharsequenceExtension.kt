package shared.extension

fun CharSequence.contains(array: Array<CharSequence>): Boolean {
    for (charSequence in array) {
        if (contains(charSequence)) {
            return true
        }
    }

    return false
}
