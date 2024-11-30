package shared.tools

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.withLock
import org.slf4j.LoggerFactory
import shared.model.ProxyInfo
import java.io.IOException
import java.net.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class ProxyPool(
    proxyList: List<ProxyInfo>,
    private var mode: Mode = Mode.AUTO,
) : ProxySelector(), okhttp3.Authenticator {
    companion object {
        private val PING_REQUEST = Request.Builder().url("https://api.ipify.org".toHttpUrl()).build()
        private val NO_PROXY = listOf(Proxy.NO_PROXY)
        private val logger = LoggerFactory.getLogger("ProxyPool")
    }

    var proxyList = proxyList
        set(value) {
            lock.withLock {
                field = value
                update()
            }
        }

    private val bannedProxyPerKey = mutableMapOf<String, MutableSet<Proxy>>()
    private val lock = ReentrantLock()

    init {
        update()
    }

    private lateinit var proxies: List<Proxy>

    private lateinit var proxiesMapForAuth: MutableMap<String, ProxyInfo>

    private var index = 0
        set(value) {
            field = if (value > proxies.lastIndex) {
                0
            } else {
                value
            }
        }

    fun nextProxy(key: String, previousProxy: Proxy? = null): Proxy? = lock.withLock {
        if (proxyList.isEmpty()) return null

        val bannedProxyForKey = bannedProxyPerKey.getOrDefault(key, mutableSetOf())

        if (bannedProxyForKey.size >= proxyList.size) {
            return null
        }

        if (previousProxy != null) {
            bannedProxyForKey.add(previousProxy)
        }

        for (proxy in proxies) {
            if (bannedProxyForKey.contains(proxy).not()) {
                return proxy
            }
        }

        return null
    }

    fun getProxy(): Proxy? = lock.withLock {
        return proxies.getOrNull(when (mode) {
            Mode.AUTO -> index++
            Mode.MANUAL -> index
        })
    }

    fun getProxyInfo(): ProxyInfo? = lock.withLock {
        return proxyList.getOrNull(when (mode) {
            Mode.AUTO -> index++
            Mode.MANUAL -> index
        })
    }

    fun next(): Proxy? = lock.withLock {
        return proxies.getOrNull(++index)
    }

    fun validate(httpClient: OkHttpClient): List<ProxyInfo> {
        val invalidProxies: MutableList<ProxyInfo> = mutableListOf()
        val executors = Executors.newVirtualThreadPerTaskExecutor()
        val lock = ReentrantLock()

        for (proxy in proxyList) {
            executors.execute {
                val isValid = validate(httpClient, proxy.toProxy())

                if (isValid.not()) {
                    lock.withLock {
                        invalidProxies.add(proxy)
                        logger.info("Invalid proxy: {}", proxy)
                    }
                }
            }
        }

        executors.shutdown()
        executors.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

        proxyList = proxyList.toMutableList().apply {
            removeAll(invalidProxies)
        }

        return invalidProxies
    }

    fun validate(httpClient: OkHttpClient, proxy: Proxy): Boolean {
        return runCatching {
            httpClient
                .newBuilder()
                .proxyAuthenticator(this)
                .proxy(proxy)
                .build()
                .newCall(PING_REQUEST)
                .execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    override fun select(uri: URI?): List<Proxy> {
        val nextProxy = getProxy() ?: return NO_PROXY

        return proxies.toMutableList().apply {
            remove(nextProxy)
            add(0, nextProxy)
        }
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {

    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val challenges = response.challenges()
        val request = response.request
        val proxy = route?.proxy ?: return null

        for (challenge in challenges) {
            if (challenge.scheme != "Basic") {
                continue
            }

            val proxyAddress = proxy.address() as InetSocketAddress

            val auth = proxiesMapForAuth[proxyAddress.hostName] ?: return null

            val credential = runCatching { Credentials.basic(auth.userName!!, auth.password!!, challenge.charset) }.getOrNull() ?: return null
            return request.newBuilder()
                .header("Proxy-Authorization", credential)
                .build()
        }

        return null // No challenges were satisfied!
    }

    private fun update() {
        bannedProxyPerKey.clear()
        proxies = proxyList.map { it.toProxy() }
        proxiesMapForAuth = proxyList.associateBy { it.address }.toMutableMap()
        index = 0
    }

    enum class Mode {
        AUTO, MANUAL
    }
}
