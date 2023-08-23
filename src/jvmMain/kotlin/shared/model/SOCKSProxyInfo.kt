package shared.model

import java.net.InetSocketAddress
import java.net.Proxy

data class SOCKSProxyInfo(
    val version: Version,
    val address: String,
    val port: Int,
    val userName: String? = null,
    val password: String? = null
) {
    companion object {
        /**
         * Parse this type of string: socks5://exmpl:test@94.198.40.26:8128
         */
        fun parse(text: String): SOCKSProxyInfo? = runCatching {
            val version = if (text.startsWith("socks5")) {
                Version.V5
            } else if (text.startsWith("socks4")) {
                Version.V4
            } else {
                return@runCatching null
            }

            //Substring from original text: exmpl:test@94.198.40.26:8128
            val text = text.substring(text.lastIndexOf("/") + 1)

            var userName: String? = null
            var password: String? = null

            val address: String
            val port: Int
            if (text.contains("@")) {
                val userDataAndAddress = text.split("@")
                val userData = userDataAndAddress[0].split(":")
                userName = userData[0]
                password = userData.getOrNull(1)

                val addressAndPort = userDataAndAddress[1].split(":")

                address = addressAndPort[0]
                port = addressAndPort[1].toInt()
            } else {
                val addressAndPort = text.split(":")

                address = addressAndPort[0]
                port = addressAndPort[1].toInt()
            }


            SOCKSProxyInfo(version = version, address = address, port = port, userName = userName, password = password)
        }.getOrNull()
    }

    private val string = buildString {
        append(when (version) {
            Version.V4 -> "socks4://"
            Version.V5 -> "socks5://"
        })

        if (userName != null) {
            append(userName)
        }
        if (password != null) {
            append(":$password")
        }

        if (userName != null) {
            append("@")
        }

        append("$address:$port")
    }

    fun toProxy(): Proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(address, port))

    override fun toString(): String = string

    enum class Version {
        V4, V5
    }
}
