package com.wiretap.core

data class BleConnectionAttempt(
    val peripheralAddress: String,
    val peripheralName: String?,
    val startTime: Long,
    val events: List<BleEntry>,
    val outcome: Outcome
) {
    sealed class Outcome {
        object InProgress : Outcome()
        object Streaming : Outcome()
        object Disconnected : Outcome()
        data class Failed(val phase: String, val reason: String?) : Outcome()
    }

    val phases: List<Phase> get() {
        return events.mapNotNull { entry ->
            when (entry.kind) {
                BleEntry.Kind.CONNECT_ATTEMPT -> Phase("Connecting", entry.timestamp)
                BleEntry.Kind.CONNECTED -> Phase("Connected", entry.timestamp)
                BleEntry.Kind.SERVICES_DISCOVERED -> Phase("Services", entry.timestamp)
                BleEntry.Kind.MTU_CHANGED -> Phase("MTU", entry.timestamp)
                BleEntry.Kind.BOND_CREATED -> Phase("Paired", entry.timestamp)
                BleEntry.Kind.CHARACTERISTIC_NOTIFY, BleEntry.Kind.CHARACTERISTIC_CHANGED -> Phase("Streaming", entry.timestamp)
                BleEntry.Kind.DISCONNECTED -> Phase("Disconnected", entry.timestamp)
                else -> null
            }
        }.distinctBy { it.name }
    }
}

data class Phase(val name: String, val timestamp: Long)

object ConnectionLifecycleGrouper {
    private const val SESSION_GAP_MS = 30_000L

    fun group(entries: List<BleEntry>): List<BleConnectionAttempt> {
        val attempts = mutableListOf<BleConnectionAttempt>()
        val byDevice = entries
            .filter { it.peripheralAddress != null }
            .sortedBy { it.timestamp }
            .groupBy { it.peripheralAddress!! }

        byDevice.forEach { (address, deviceEntries) ->
            var sessionStart = 0
            deviceEntries.forEachIndexed { i, entry ->
                val isNewSession = i == 0 ||
                    (entry.timestamp - deviceEntries[i - 1].timestamp > SESSION_GAP_MS) ||
                    entry.kind == BleEntry.Kind.CONNECT_ATTEMPT
                if (isNewSession && i > sessionStart) {
                    val sessionEvents = deviceEntries.subList(sessionStart, i)
                    attempts.add(buildAttempt(address, sessionEvents))
                    sessionStart = i
                }
            }
            if (sessionStart < deviceEntries.size) {
                attempts.add(buildAttempt(address, deviceEntries.subList(sessionStart, deviceEntries.size)))
            }
        }
        return attempts.sortedByDescending { it.startTime }
    }

    private fun buildAttempt(address: String, events: List<BleEntry>): BleConnectionAttempt {
        val name = events.firstNotNullOfOrNull { it.peripheralName }
        val outcome = when {
            events.any { it.kind == BleEntry.Kind.BOND_FAILED } ->
                BleConnectionAttempt.Outcome.Failed("pairing", events.first { it.kind == BleEntry.Kind.BOND_FAILED }.detail)
            events.last().kind == BleEntry.Kind.DISCONNECTED -> BleConnectionAttempt.Outcome.Disconnected
            events.any { it.kind == BleEntry.Kind.CHARACTERISTIC_NOTIFY || it.kind == BleEntry.Kind.CHARACTERISTIC_CHANGED } ->
                BleConnectionAttempt.Outcome.Streaming
            else -> BleConnectionAttempt.Outcome.InProgress
        }
        return BleConnectionAttempt(address, name, events.first().timestamp, events, outcome)
    }
}
