package shared.model

import java.net.InetSocketAddress
import java.net.Proxy

data class ProxyInfo(
    val type: Type,
    val address: String,
    val port: Int,
    val userName: String? = null,
    val password: String? = null,
) {
    companion object {
        /**
         * Parse this type of string: socks5://exmpl:test@94.198.40.26:8128
         */
        fun parse(text: String): ProxyInfo? = runCatching {
            val type = if (text.startsWith("socks")) {
                Type.SOCKS
            } else if (text.startsWith("http")) {
                Type.HTTP
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

                address = addressAndPort.subList(0, addressAndPort.size - 1).joinToString(":")
                port = addressAndPort[addressAndPort.lastIndex].toInt()
            } else {
                val addressAndPort = text.split(":")

                address = addressAndPort.subList(0, addressAndPort.size - 1).joinToString(":")
                port = addressAndPort[addressAndPort.lastIndex].toInt()
            }


            ProxyInfo(type = type, address = address, port = port, userName = userName, password = password)
        }.getOrNull()
    }

    private val string = buildString {
        append(when (type) {
            Type.SOCKS -> "socks://"
            Type.HTTP -> "http://"
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

    fun toProxy(): Proxy = Proxy(when (type) {
        Type.SOCKS -> Proxy.Type.SOCKS
        Type.HTTP -> Proxy.Type.HTTP
    }, InetSocketAddress(address, port))

    override fun toString(): String = string

    enum class Type {
        SOCKS, HTTP
    }
}
