package com.wiretap.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import com.wiretap.WireTap
import com.wiretap.core.NfcEntry

/**
 * Wraps your NfcAdapter.ReaderCallback and auto-records NFC events.
 *
 * Usage:
 *   nfcAdapter.enableReaderMode(
 *       activity,
 *       NfcWireTapReaderCallback(myCallback),
 *       NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
 *       null
 *   )
 */
class NfcWireTapReaderCallback(
    private val delegate: NfcAdapter.ReaderCallback? = null
) : NfcAdapter.ReaderCallback {

    override fun onTagDiscovered(tag: Tag) {
        val tagIdHex = tag.id.joinToString("") { "%02x".format(it) }
        val techs = tag.techList.map { it.substringAfterLast('.') }

        WireTap.nfc.record(
            NfcEntry(
                kind = NfcEntry.Kind.TAG_DISCOVERED,
                tagId = tagIdHex,
                techList = techs,
                detail = "Techs: ${techs.joinToString()}"
            )
        )

        // Try to read NDEF payload
        tryReadNdef(tag, tagIdHex)

        // Try to read IsoDep (APDU)
        tryReadIsoDep(tag, tagIdHex)

        delegate?.onTagDiscovered(tag)
    }

    private fun tryReadNdef(tag: Tag, tagIdHex: String) {
        val ndef = runCatching { Ndef.get(tag) }.getOrNull() ?: return
        runCatching {
            ndef.connect()
            val message = ndef.ndefMessage
            ndef.close()
            message?.records?.forEachIndexed { i, record ->
                val payload = record.payload
                val hex = payload.joinToString("") { "%02x".format(it) }
                val ascii = String(payload, Charsets.ISO_8859_1)
                WireTap.nfc.record(
                    NfcEntry(
                        kind = NfcEntry.Kind.NDEF_READ,
                        tagId = tagIdHex,
                        descriptor = "Record $i: TNF=${record.tnf} Type=${String(record.type)}",
                        rawHex = hex,
                        detail = ascii.take(120)
                    )
                )
            }
        }
    }

    private fun tryReadIsoDep(tag: Tag, tagIdHex: String) {
        val isoDep = runCatching { IsoDep.get(tag) }.getOrNull() ?: return
        runCatching {
            isoDep.connect()
            val historicalBytes = isoDep.historicalBytes
            isoDep.close()
            if (historicalBytes != null) {
                WireTap.nfc.record(
                    NfcEntry(
                        kind = NfcEntry.Kind.APDU_RESPONSE,
                        tagId = tagIdHex,
                        rawHex = historicalBytes.joinToString("") { "%02x".format(it) },
                        descriptor = "IsoDep historical bytes"
                    )
                )
            }
        }
    }
}

/** Direct recording helpers for Host Card Emulation (HCE) services. */
object NfcWireTapHce {
    fun recordCommand(apdu: ByteArray) {
        WireTap.nfc.record(
            NfcEntry(
                kind = NfcEntry.Kind.HCE_COMMAND,
                rawHex = apdu.joinToString("") { "%02x".format(it) },
                detail = "C-APDU len=${apdu.size}"
            )
        )
    }

    fun recordResponse(response: ByteArray) {
        WireTap.nfc.record(
            NfcEntry(
                kind = NfcEntry.Kind.HCE_RESPONSE,
                rawHex = response.joinToString("") { "%02x".format(it) },
                detail = "R-APDU len=${response.size}"
            )
        )
    }
}
