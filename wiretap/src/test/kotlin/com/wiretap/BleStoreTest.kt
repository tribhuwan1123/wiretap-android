package com.wiretap

import com.wiretap.core.BleEntry
import com.wiretap.core.BleStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BleStoreTest {

    private lateinit var store: BleStore

    @Before
    fun setUp() { store = BleStore(maxEntries = 10) }

    @Test
    fun `record adds BLE entry`() = runTest {
        store.record(BleEntry(kind = BleEntry.Kind.CONNECTED, peripheralAddress = "AA:BB:CC"))
        assertEquals(1, store.entries.value.size)
        assertEquals(BleEntry.Kind.CONNECTED, store.entries.value.first().kind)
    }

    @Test
    fun `decoder is applied on record`() = runTest {
        store.registerDecoder("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") { _ ->
            mapOf("temperature" to "22.5")
        }
        store.record(
            BleEntry(
                kind = BleEntry.Kind.CHARACTERISTIC_NOTIFY,
                characteristicUUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E",
                rawHex = "ff"
            )
        )
        assertEquals("22.5", store.entries.value.first().decodedFields["temperature"])
    }

    @Test
    fun `rawBytes derived from rawHex`() {
        val entry = BleEntry(kind = BleEntry.Kind.CHARACTERISTIC_READ, rawHex = "deadbeef")
        assertEquals(4, entry.rawBytes?.size)
        assertEquals(0xde.toByte(), entry.rawBytes?.get(0))
    }

    @Test
    fun `clear resets store`() = runTest {
        store.record(BleEntry(kind = BleEntry.Kind.DISCONNECTED))
        store.clear()
        assertTrue(store.entries.value.isEmpty())
    }
}
