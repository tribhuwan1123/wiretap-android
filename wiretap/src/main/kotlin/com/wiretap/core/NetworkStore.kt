package com.wiretap.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkStore(private val maxEntries: Int = 500) {

    private val _entries = MutableStateFlow<List<NetworkEntry>>(emptyList())
    val entries: StateFlow<List<NetworkEntry>> = _entries.asStateFlow()

    internal val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    internal var persistence: PersistenceWriter<NetworkEntry>? = null
    internal var redactionConfig: WireTapRedactionConfig = WireTapRedactionConfig()

    @Synchronized
    fun record(entry: NetworkEntry) {
        val redacted = applyRedaction(entry)
        val current = _entries.value.toMutableList()
        current.add(0, redacted)
        if (current.size > maxEntries) current.subList(maxEntries, current.size).clear()
        _entries.value = current
        persistence?.append(redacted)
    }

    fun clear() {
        _entries.value = emptyList()
        persistence?.clear()
    }

    internal fun loadPersisted(entries: List<NetworkEntry>) {
        _entries.value = entries.take(maxEntries)
    }

    private fun applyRedaction(entry: NetworkEntry): NetworkEntry {
        val config = redactionConfig
        return entry.copy(
            requestHeaders = Redactor.redactHeaders(entry.requestHeaders, config),
            responseHeaders = Redactor.redactHeaders(entry.responseHeaders, config),
            requestBody = Redactor.redactBody(entry.requestBody, config),
            responseBody = Redactor.redactBody(entry.responseBody, config)
        )
    }
}
