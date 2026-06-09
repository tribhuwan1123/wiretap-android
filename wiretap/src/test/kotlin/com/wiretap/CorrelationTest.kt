package com.wiretap

import com.wiretap.core.BleEntry
import com.wiretap.core.CorrelatedEpisode
import com.wiretap.core.Correlator
import com.wiretap.core.NetworkEntry
import com.wiretap.core.NfcEntry
import com.wiretap.core.WireTapSession
import org.junit.Assert.assertEquals
import org.junit.Test

class CorrelationTest {

    @Test
    fun `events within gap form single episode`() {
        val base = System.currentTimeMillis()
        val session = WireTapSession(
            network = listOf(NetworkEntry(timestamp = base, method = "GET", url = "https://a.com")),
            ble = listOf(BleEntry(timestamp = base + 1000, kind = BleEntry.Kind.CONNECTED)),
            nfc = listOf(NfcEntry(timestamp = base + 2000, kind = NfcEntry.Kind.TAG_DISCOVERED))
        )
        val episodes = Correlator.correlate(session)
        assertEquals(1, episodes.size)
    }

    @Test
    fun `events separated by large gap form two episodes`() {
        val base = System.currentTimeMillis()
        val session = WireTapSession(
            network = listOf(
                NetworkEntry(timestamp = base, method = "GET", url = "https://a.com"),
                NetworkEntry(timestamp = base + 120_000, method = "POST", url = "https://b.com")
            )
        )
        val episodes = Correlator.correlate(session)
        assertEquals(2, episodes.size)
    }

    @Test
    fun `nfc trigger identified correctly`() {
        val base = System.currentTimeMillis()
        val session = WireTapSession(
            nfc = listOf(NfcEntry(timestamp = base, kind = NfcEntry.Kind.TAG_DISCOVERED)),
            network = listOf(NetworkEntry(timestamp = base + 500, method = "GET", url = "https://a.com"))
        )
        val episodes = Correlator.correlate(session)
        assertEquals(CorrelatedEpisode.Trigger.NfcTap, episodes.first().trigger)
    }
}
