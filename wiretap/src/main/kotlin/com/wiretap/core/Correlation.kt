package com.wiretap.core

data class CorrelatedEpisode(
    val id: Int,
    val startTime: Long,
    val endTime: Long,
    val networkCount: Int,
    val bleCount: Int,
    val nfcCount: Int,
    val trigger: Trigger
) {
    sealed class Trigger {
        object NfcTap : Trigger()
        object BleConnect : Trigger()
        object NetworkCall : Trigger()
        object Unknown : Trigger()
    }

    val label: String get() = when (trigger) {
        is Trigger.NfcTap -> "NFC Tap"
        is Trigger.BleConnect -> "BLE Connect"
        is Trigger.NetworkCall -> "Network Call"
        is Trigger.Unknown -> "Episode ${id + 1}"
    }
}

object Correlator {
    private const val MAX_GAP_MS = 60_000L

    fun correlate(session: WireTapSession): List<CorrelatedEpisode> {
        val all = session.timeline().sortedBy {
            when (it) {
                is TimelineEntry.Network -> it.timestamp
                is TimelineEntry.Ble -> it.timestamp
                is TimelineEntry.Nfc -> it.timestamp
            }
        }
        if (all.isEmpty()) return emptyList()

        val episodes = mutableListOf<MutableList<TimelineEntry>>()
        var current = mutableListOf(all.first())
        for (i in 1 until all.size) {
            val prev = timestampOf(all[i - 1])
            val curr = timestampOf(all[i])
            if (curr - prev > MAX_GAP_MS) {
                episodes.add(current)
                current = mutableListOf()
            }
            current.add(all[i])
        }
        episodes.add(current)

        return episodes.mapIndexed { idx, group ->
            val nets = group.filterIsInstance<TimelineEntry.Network>()
            val bles = group.filterIsInstance<TimelineEntry.Ble>()
            val nfcs = group.filterIsInstance<TimelineEntry.Nfc>()
            val trigger = when {
                nfcs.isNotEmpty() -> CorrelatedEpisode.Trigger.NfcTap
                bles.any { it.entry.kind == BleEntry.Kind.CONNECT_ATTEMPT } -> CorrelatedEpisode.Trigger.BleConnect
                nets.isNotEmpty() -> CorrelatedEpisode.Trigger.NetworkCall
                else -> CorrelatedEpisode.Trigger.Unknown
            }
            CorrelatedEpisode(
                id = idx,
                startTime = timestampOf(group.first()),
                endTime = timestampOf(group.last()),
                networkCount = nets.size,
                bleCount = bles.size,
                nfcCount = nfcs.size,
                trigger = trigger
            )
        }.reversed()
    }

    private fun timestampOf(e: TimelineEntry) = when (e) {
        is TimelineEntry.Network -> e.timestamp
        is TimelineEntry.Ble -> e.timestamp
        is TimelineEntry.Nfc -> e.timestamp
    }
}
