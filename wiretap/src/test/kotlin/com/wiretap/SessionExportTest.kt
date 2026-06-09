package com.wiretap

import com.wiretap.core.BleEntry
import com.wiretap.core.NetworkEntry
import com.wiretap.core.NfcEntry
import com.wiretap.core.SessionSerializer
import com.wiretap.core.WireTapSession
import com.wiretap.core.timeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExportTest {

    @Test
    fun `session round-trips through JSON`() {
        val session = WireTapSession(
            appId = "com.example",
            network = listOf(NetworkEntry(method = "GET", url = "https://a.com", responseStatusCode = 200)),
            ble = listOf(BleEntry(kind = BleEntry.Kind.CONNECTED)),
            nfc = listOf(NfcEntry(kind = NfcEntry.Kind.TAG_DISCOVERED))
        )
        val json = SessionSerializer.encode(session)
        val decoded = SessionSerializer.decode(json)
        assertNotNull(decoded)
        assertEquals(1, decoded!!.network.size)
        assertEquals(1, decoded.ble.size)
        assertEquals(1, decoded.nfc.size)
        assertEquals("com.example", decoded.appId)
    }

    @Test
    fun `timeline merges all streams sorted newest-first`() {
        val t1 = 1000L; val t2 = 2000L; val t3 = 3000L
        val session = WireTapSession(
            network = listOf(NetworkEntry(timestamp = t2, method = "GET", url = "https://a.com")),
            ble = listOf(BleEntry(timestamp = t3, kind = BleEntry.Kind.CONNECTED)),
            nfc = listOf(NfcEntry(timestamp = t1, kind = NfcEntry.Kind.TAG_DISCOVERED))
        )
        val tl = session.timeline()
        assertEquals(3, tl.size)
        // newest first
        assertTrue(tl[0] is com.wiretap.core.TimelineEntry.Ble)
    }

    @Test
    fun `decode returns null for invalid JSON`() {
        val result = SessionSerializer.decode("not json")
        assertEquals(null, result)
    }

    @Test
    fun `schema version is 1`() {
        val session = WireTapSession()
        assertEquals(1, session.schemaVersion)
    }
}
