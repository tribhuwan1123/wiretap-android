package com.wiretap.core

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class NfcEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val kind: Kind,
    val tagId: String? = null,
    val techList: List<String> = emptyList(),
    val descriptor: String? = null,
    val rawHex: String? = null,
    val detail: String? = null
) {
    @Serializable
    enum class Kind(val label: String) {
        TAG_DISCOVERED("Tag Discovered"),
        NDEF_DISCOVERED("NDEF Discovered"),
        TECH_DISCOVERED("Tech Discovered"),
        NDEF_READ("NDEF Read"),
        NDEF_WRITE("NDEF Write"),
        APDU_COMMAND("APDU Command"),
        APDU_RESPONSE("APDU Response"),
        TAG_LOST("Tag Lost"),
        HCE_COMMAND("HCE Command"),
        HCE_RESPONSE("HCE Response")
    }
}
