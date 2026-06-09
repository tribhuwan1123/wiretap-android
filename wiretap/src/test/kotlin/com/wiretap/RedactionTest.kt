package com.wiretap

import com.wiretap.core.Redactor
import com.wiretap.core.WireTapRedactionConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class RedactionTest {

    private val config = WireTapRedactionConfig()

    @Test
    fun `authorization header is redacted`() {
        val headers = mapOf("Authorization" to "Bearer secret123")
        val result = Redactor.redactHeaders(headers, config)
        assertEquals("[redacted]", result["Authorization"])
    }

    @Test
    fun `non-sensitive header is preserved`() {
        val headers = mapOf("Content-Type" to "application/json")
        val result = Redactor.redactHeaders(headers, config)
        assertEquals("application/json", result["Content-Type"])
    }

    @Test
    fun `password key in JSON body is redacted`() {
        val body = """{"username":"alice","password":"hunter2"}"""
        val result = Redactor.redactBody(body, config)
        assertEquals("""{"username":"alice","password":"[redacted]"}""", result)
    }

    @Test
    fun `null body returns null`() {
        val result = Redactor.redactBody(null, config)
        assertEquals(null, result)
    }

    @Test
    fun `custom rule is applied`() {
        val custom = config.copy(customRules = listOf(
            com.wiretap.core.RedactionRule(Regex("\\d{16}"), "****-****-****-****")
        ))
        val body = "Card: 1234567812345678"
        val result = Redactor.redactBody(body, custom)
        assertEquals("Card: ****-****-****-****", result)
    }
}
