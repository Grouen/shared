package shared.tools

import shared.interfaces.Storage
import shared.extension.takeIfExists
import java.io.File
import java.io.InputStream

abstract class BaseFileStorage(
    name: String
) : Storage<File> {
    protected open val dir = File("storage", name).also { it.mkdirs() }

    fun File(name: String) = File(dir, name)
    fun File.copy(from: InputStream) {
        from.use { input ->
            outputStream().use {
                input.copyTo(it)
            }
        }
    }

    override fun File.read(): InputStream = takeIfExists()?.inputStream() ?: InputStream.nullInputStream()
    override fun File.write(inputStream: InputStream) = copy(inputStream)
    override fun File.delete() = delete()
    override fun File.exists() = exists()
}
