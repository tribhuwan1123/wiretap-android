package com.wiretap

import com.wiretap.core.NetworkEntry
import com.wiretap.core.NetworkStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkStoreTest {

    private lateinit var store: NetworkStore

    @Before
    fun setUp() {
        store = NetworkStore(maxEntries = 10)
    }

    @Test
    fun `record adds entry at front`() = runTest {
        store.record(entry("GET", "https://a.com"))
        store.record(entry("POST", "https://b.com"))
        assertEquals("POST", store.entries.value.first().method)
    }

    @Test
    fun `record enforces maxEntries`() = runTest {
        repeat(15) { store.record(entry("GET", "https://example.com/$it")) }
        assertEquals(10, store.entries.value.size)
    }

    @Test
    fun `clear empties entries`() = runTest {
        store.record(entry("GET", "https://a.com"))
        store.clear()
        assertTrue(store.entries.value.isEmpty())
    }

    @Test
    fun `toCurl generates valid curl command`() {
        val e = NetworkEntry(
            method = "POST",
            url = "https://api.example.com/login",
            requestHeaders = mapOf("Content-Type" to "application/json"),
            requestBody = """{"username":"alice"}"""
        )
        val curl = e.toCurl()
        assertTrue(curl.contains("curl -X POST"))
        assertTrue(curl.contains("Content-Type"))
        assertTrue(curl.contains("https://api.example.com/login"))
    }

    @Test
    fun `isSuccess true for 2xx`() {
        val e = entry("GET", "https://a.com").copy(responseStatusCode = 200)
        assertTrue(e.isSuccess)
    }

    @Test
    fun `isError true for 4xx and error`() {
        val withStatus = entry("GET", "https://a.com").copy(responseStatusCode = 404)
        val withError = entry("GET", "https://a.com").copy(error = "timeout")
        assertTrue(withStatus.isError)
        assertTrue(withError.isError)
    }

    private fun entry(method: String, url: String) = NetworkEntry(method = method, url = url)
}
