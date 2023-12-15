package shared.tools

import okio.withLock
import shared.model.SOCKSProxyInfo
import java.io.IOException
import java.net.*
import java.util.concurrent.locks.ReentrantLock

class ProxyPool(
    private val proxyList: List<SOCKSProxyInfo>,
    private val mode: Mode = Mode.AUTO,
) : ProxySelector() {
    private val bannedProxyPerKey = mutableMapOf<String, MutableSet<Proxy>>()
    private val proxies = proxyList.map { it.toProxy() }
    private val lock = ReentrantLock()

    init {
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                val proxy = proxyList.first { it.address == requestingHost }

                return PasswordAuthentication(proxy.userName, proxy.password!!.toCharArray())
            }
        })
    }

    @Volatile
    private var index = 0
        set(value) {
            field = if (value > proxyList.lastIndex) {
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

    fun getProxy(): Proxy = lock.withLock {
        return if (mode == Mode.MANUAL) proxies[index] else next()
    }

    fun next(): Proxy {
        return proxies[++index]
    }

    override fun select(uri: URI?): List<Proxy> = listOf(getProxy())

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
    }

    enum class Mode {
        AUTO, MANUAL
    }
}
