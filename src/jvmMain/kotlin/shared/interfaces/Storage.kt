package shared.interfaces

import java.io.InputStream

interface Storage<T> {
    fun T.read(): InputStream
    fun T.write(inputStream: InputStream)
    fun T.delete(): Boolean
    fun T.exists(): Boolean

    fun T.readIfExists(): InputStream? = takeIf { it.exists() }?.read()
}
