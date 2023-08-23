@file:Suppress("NOTHING_TO_INLINE")

package shared.extension

import okhttp3.Cookie
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.time.toJavaDuration

fun Response.cookies(): List<Cookie> {
    return headers("Set-Cookie").mapNotNull {
        Cookie.parse(request.url, it)
    }
}

fun OkHttpClient.Builder.timeout(duration: kotlin.time.Duration) {
    val javaDuration = duration.toJavaDuration()
    connectTimeout(javaDuration)
    readTimeout(javaDuration)
    writeTimeout(javaDuration)
}

inline fun Request.Builder.execute(okHttpClient: OkHttpClient) = okHttpClient.newCall(build()).execute()
inline fun Request.execute(okHttpClient: OkHttpClient) = okHttpClient.newCall(this).execute()
