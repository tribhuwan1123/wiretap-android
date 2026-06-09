package com.wiretap.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WireTapSession(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appId: String = "",
    val appVersion: String = "",
    val osVersion: String = android.os.Build.VERSION.RELEASE,
    val device: String = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
    val network: List<NetworkEntry> = emptyList(),
    val ble: List<BleEntry> = emptyList(),
    val nfc: List<NfcEntry> = emptyList()
) {
    val startTime: Long get() = minOf(
        network.minOfOrNull { it.timestamp } ?: Long.MAX_VALUE,
        ble.minOfOrNull { it.timestamp } ?: Long.MAX_VALUE,
        nfc.minOfOrNull { it.timestamp } ?: Long.MAX_VALUE
    ).takeIf { it != Long.MAX_VALUE } ?: exportedAt

    val endTime: Long get() = maxOf(
        network.maxOfOrNull { it.timestamp } ?: Long.MIN_VALUE,
        ble.maxOfOrNull { it.timestamp } ?: Long.MIN_VALUE,
        nfc.maxOfOrNull { it.timestamp } ?: Long.MIN_VALUE
    ).takeIf { it != Long.MIN_VALUE } ?: exportedAt
}

sealed class TimelineEntry {
    data class Network(val entry: NetworkEntry) : TimelineEntry() {
        val timestamp get() = entry.timestamp
    }
    data class Ble(val entry: BleEntry) : TimelineEntry() {
        val timestamp get() = entry.timestamp
    }
    data class Nfc(val entry: NfcEntry) : TimelineEntry() {
        val timestamp get() = entry.timestamp
    }
}

object SessionSerializer {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun encode(session: WireTapSession): String = json.encodeToString(WireTapSession.serializer(), session)

    fun decode(data: String): WireTapSession? =
        runCatching { json.decodeFromString(WireTapSession.serializer(), data) }.getOrNull()
}

fun WireTapSession.timeline(): List<TimelineEntry> {
    val entries = mutableListOf<TimelineEntry>()
    network.mapTo(entries) { TimelineEntry.Network(it) }
    ble.mapTo(entries) { TimelineEntry.Ble(it) }
    nfc.mapTo(entries) { TimelineEntry.Nfc(it) }
    return entries.sortedBy {
        when (it) {
            is TimelineEntry.Network -> it.timestamp
            is TimelineEntry.Ble -> it.timestamp
            is TimelineEntry.Nfc -> it.timestamp
        }
    }.reversed()
}
