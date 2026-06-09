package com.wiretap.core

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class NetworkEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val responseStatusCode: Int? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val durationMs: Long? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = responseStatusCode?.let { it in 200..299 } ?: false
    val isError: Boolean get() = error != null || (responseStatusCode?.let { it >= 400 } ?: false)

    fun toCurl(): String {
        val sb = StringBuilder("curl -X $method")
        requestHeaders.forEach { (k, v) -> sb.append(" \\\n  -H '${k}: ${v}'") }
        requestBody?.let { sb.append(" \\\n  -d '${it.replace("'", "\\'")}'") }
        sb.append(" \\\n  '$url'")
        return sb.toString()
    }

    fun statusLabel(): String = responseStatusCode?.toString() ?: (if (error != null) "ERR" else "…")
}
