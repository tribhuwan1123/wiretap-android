package com.wiretap.interceptors

import com.wiretap.WireTap
import com.wiretap.core.NetworkEntry
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * OkHttp interceptor that records all HTTP traffic into WireTap.network.
 *
 * Usage:
 *   val client = OkHttpClient.Builder()
 *       .addInterceptor(WireTapInterceptor())
 *       .build()
 */
class WireTapInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startMs = System.currentTimeMillis()

        val requestBodyStr = runCatching {
            request.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            }
        }.getOrNull()

        val requestHeaders = request.headers.toMap()

        return try {
            val response = chain.proceed(request)
            val durationMs = System.currentTimeMillis() - startMs

            val responseBodyBytes = response.body?.bytes()
            val responseBodyStr = responseBodyBytes?.toString(Charsets.UTF_8)
            val responseHeaders = response.headers.toMap()

            WireTap.network.record(
                NetworkEntry(
                    method = request.method,
                    url = request.url.toString(),
                    requestHeaders = requestHeaders,
                    requestBody = requestBodyStr,
                    responseStatusCode = response.code,
                    responseHeaders = responseHeaders,
                    responseBody = responseBodyStr,
                    durationMs = durationMs
                )
            )

            // Rebuild response since body can only be consumed once
            val newBody = responseBodyBytes?.toResponseBody(response.body?.contentType())
            response.newBuilder().body(newBody).build()

        } catch (e: IOException) {
            val durationMs = System.currentTimeMillis() - startMs
            WireTap.network.record(
                NetworkEntry(
                    method = request.method,
                    url = request.url.toString(),
                    requestHeaders = requestHeaders,
                    requestBody = requestBodyStr,
                    durationMs = durationMs,
                    error = e.message ?: e.javaClass.simpleName
                )
            )
            throw e
        }
    }

    private fun okhttp3.Headers.toMap(): Map<String, String> =
        names().associateWith { name -> values(name).joinToString(", ") }
}
