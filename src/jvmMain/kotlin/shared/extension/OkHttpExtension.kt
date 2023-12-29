@file:Suppress("NOTHING_TO_INLINE")

package shared.extension

import okhttp3.*
import kotlin.time.toJavaDuration

fun Response.cookies(): List<Cookie> {
    return headers("Set-Cookie").mapNotNull {
        Cookie.parse(request.url, it)
    }
}

fun FormBody.Builder.addAll(parameters: Map<String, String>): FormBody.Builder {
    parameters.forEach { (name, value) ->
        add(name, value)
    }
    return this
}

fun OkHttpClient.Builder.timeout(duration: kotlin.time.Duration) {
    val javaDuration = duration.toJavaDuration()
    connectTimeout(javaDuration)
    readTimeout(javaDuration)
    writeTimeout(javaDuration)
}

inline fun Request.Builder.execute(okHttpClient: OkHttpClient) = okHttpClient.newCall(build()).execute()
inline fun Request.execute(okHttpClient: OkHttpClient) = okHttpClient.newCall(this).execute()
