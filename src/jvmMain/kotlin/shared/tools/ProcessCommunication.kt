package shared.tools

import okhttp3.internal.EMPTY_BYTE_ARRAY
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.nio.charset.Charset
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("NOTHING_TO_INLINE")
class ProcessCommunication(processBuilder: ProcessBuilder) : AutoCloseable {
    companion object {
        private const val READ_ARRAY_SIZE = 1024
        private val charset = Charset.forName(System.getProperty("native.encoding"))
    }

    val record: String
        get() = recordBuilder.toString()

    private val recordBuilder = StringBuilder()

    private val process = processBuilder.redirectErrorStream(true).start()
    private val inputStream = process.inputStream
    private val writer = process.outputWriter()

    private var array = ByteArray(READ_ARRAY_SIZE)

    inline fun readUntilEOF(timeout: Duration = 30.seconds): String {
        return buildString {
            readUntil(timeout) {
                append(it)
                true
            }
        }
    }

    inline fun readUntilContains(text: String): String? {
        var contains = false
        val string = readUntil { string ->
            string.contains(text).also {
                contains = it
            }
        }

        return if (contains) string else null
    }

    fun readUntil(timeout: Duration = 30.seconds, validator: (CharSequence) -> Boolean): String? {
        var isValid = false
        val timeoutInMillis = timeout.inWholeMilliseconds

        return buildString {
            var readCount: Int
            var startTime: Long = System.currentTimeMillis()

            while (true) {
                val available = process.inputStream.available().takeIf { it != 0 || process.isAlive } ?: break

                if (available == 0 && (System.currentTimeMillis() - startTime) >= timeoutInMillis) break

                readCount = if (available == 0) 0 else inputStream.read(array, 0, min(available, READ_ARRAY_SIZE))

                if (readCount == -1) {
                    break
                } else if (readCount == 0) {
                    Thread.sleep(100)
                    continue
                }

                append(String(array, 0, readCount, charset))

                if (validator.invoke(this)) {
                    isValid = true
                    break
                }

                startTime = System.currentTimeMillis()
            }
        }.also { recordBuilder.append(it) }.takeIf { isValid }
    }

    fun writeln(string: String) {
        write("$string${System.lineSeparator()}")
    }

    fun write(string: String) {
        try {
            if (process.isAlive.not()) return

            writer.write(string)
            writer.flush()
        } catch (e: IOException) {
            if (e.message != "Broken pipe") {
                throw e
            }
        }
    }

    override fun close() {
        process.toHandle()?.destroyForcibly()
        inputStream.closeQuietly()
        writer.closeQuietly()
        recordBuilder.clear()
        array = EMPTY_BYTE_ARRAY
    }
}
