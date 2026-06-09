package com.wiretap.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NfcStore(private val maxEntries: Int = 200) {

    private val _entries = MutableStateFlow<List<NfcEntry>>(emptyList())
    val entries: StateFlow<List<NfcEntry>> = _entries.asStateFlow()

    internal val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    internal var persistence: PersistenceWriter<NfcEntry>? = null

    @Synchronized
    fun record(entry: NfcEntry) {
        val current = _entries.value.toMutableList()
        current.add(0, entry)
        if (current.size > maxEntries) current.subList(maxEntries, current.size).clear()
        _entries.value = current
        persistence?.append(entry)
    }

    fun clear() {
        _entries.value = emptyList()
        persistence?.clear()
    }

    internal fun loadPersisted(entries: List<NfcEntry>) {
        _entries.value = entries.take(maxEntries)
    }
}
