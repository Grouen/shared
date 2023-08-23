@file:Suppress("NOTHING_TO_INLINE")

package shared.extension

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

fun ZipInputStream.unZipTo(destination: File) = use {
    destination.mkdirs()

    while (true) {
        val entry: ZipEntry = nextEntry ?: break

        val file = destination.resolve(entry.name)
        file.parentFile.mkdirs()
        FileOutputStream(file).use { outputStream ->
            copyTo(outputStream)
        }
    }
}

inline fun File.readLinesSafe(): List<String> = if (exists()) readLines() else emptyList()

inline fun File.takeIfExists() = takeIf { it.exists() }

inline fun ProcessBuilder.environment(key: String, value: String) = apply { environment()[key] = value }
