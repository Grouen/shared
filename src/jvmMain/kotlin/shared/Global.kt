@file:JvmName("GlobalJvm")
@file:Suppress("NOTHING_TO_INLINE")

package shared

import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import net.openhft.hashing.LongHashFunction
import okhttp3.*
import shared.extension.timeout
import shared.tools.ProxyPool
import shared.tools.SameThreadExecutorService
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration.Companion.minutes

typealias Hash = LongHashFunction

val TERMINAL by lazy {
    Terminal().also { terminal ->
        terminal.cursor.hide(showOnExit = false).also {
            Runtime.getRuntime().addShutdownHook(Thread {
                terminal.cursor.show()
            })
        }
    }
}

fun createZip(folder: File): File? {
    if (folder.exists().not() || folder.isDirectory.not()) return null

    val zipFile = Files.createTempFile(null, ".zip").toFile().apply {
        deleteOnExit()
    }

    val folderPath = folder.toPath()
    ZipOutputStream(FileOutputStream(zipFile)).use { stream ->
        for (file in folder.walkTopDown().filter { it.isDirectory.not() }) {
            val zipEntry = ZipEntry(folderPath.relativize(file.toPath()).toString())
            stream.putNextEntry(zipEntry)
            stream.write(file.readBytes())
            stream.closeEntry()
        }
    }

    return zipFile
}

fun createHttpClient(proxyPool: ProxyPool? = null, isUnsafe: Boolean = false, builder: (OkHttpClient.Builder.() -> Unit)? = null) = OkHttpClient()
    .newBuilder()
    .apply {
        dispatcher(Dispatcher(SameThreadExecutorService()).apply {
            maxRequests = Int.MAX_VALUE
            maxRequestsPerHost = Int.MAX_VALUE
        })
        timeout(1.minutes)

        builder?.invoke(this)

        if (proxyPool != null) {
            proxySelector(proxyPool)
            proxyAuthenticator(proxyPool)
        }

        if (isUnsafe) {
            val trustAllCerts: Array<X509TrustManager> = arrayOf(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                        //nothing to do
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                        //nothing to do
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory

            sslSocketFactory(sslSocketFactory, trustAllCerts[0])
            hostnameVerifier { _, _ -> true }
            connectionSpecs(listOf(ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
        }
    }.build()


inline fun <reified T> combineFlows(vararg flows: Flow<T>): Flow<Array<T>> = channelFlow {
    val array = Array<T?>(flows.size) {
        null
    }
    var allSet = false

    flows.forEachIndexed { index, flow ->
        launch {
            flow.collect { emittedElement ->
                array[index] = emittedElement

                if (allSet.not() && array.any { it !is T })
                    return@collect

                allSet = true
                @Suppress("UNCHECKED_CAST")
                send(array as Array<T>)
            }
        }
    }
}

inline fun println(
    message: Any?,
    whitespace: Whitespace = Whitespace.PRE,
    align: TextAlign = TextAlign.NONE,
    overflowWrap: OverflowWrap = OverflowWrap.NORMAL,
    width: Int? = null,
    stderr: Boolean = false,
) = TERMINAL.println(message, whitespace, align, overflowWrap, width, stderr)
