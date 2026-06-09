package com.wiretap.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BleStore(private val maxEntries: Int = 1000) {

    private val _entries = MutableStateFlow<List<BleEntry>>(emptyList())
    val entries: StateFlow<List<BleEntry>> = _entries.asStateFlow()

    internal val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    internal var persistence: PersistenceWriter<BleEntry>? = null

    private val decoders = mutableMapOf<String, (ByteArray) -> Map<String, String>>()

    fun registerDecoder(
        characteristicUUID: String,
        decoder: (ByteArray) -> Map<String, String>
    ) {
        decoders[characteristicUUID.uppercase()] = decoder
    }

    @Synchronized
    fun record(entry: BleEntry) {
        val decoded = applyDecoder(entry)
        val current = _entries.value.toMutableList()
        current.add(0, decoded)
        if (current.size > maxEntries) current.subList(maxEntries, current.size).clear()
        _entries.value = current
        persistence?.append(decoded)
    }

    fun clear() {
        _entries.value = emptyList()
        persistence?.clear()
    }

    internal fun loadPersisted(entries: List<BleEntry>) {
        _entries.value = entries.take(maxEntries)
    }

    private fun applyDecoder(entry: BleEntry): BleEntry {
        val uuid = entry.characteristicUUID?.uppercase() ?: return entry
        val decoder = decoders[uuid] ?: return entry
        val bytes = entry.rawBytes ?: return entry
        return try {
            entry.copy(decodedFields = decoder(bytes))
        } catch (_: Exception) {
            entry
        }
    }
}
