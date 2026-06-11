package com.wiretap.core

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class BleEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val kind: Kind,
    val peripheralAddress: String? = null,
    val peripheralName: String? = null,
    val serviceUUID: String? = null,
    val characteristicUUID: String? = null,
    val rawHex: String? = null,
    val rawAscii: String? = null,
    val gattStatus: Int? = null,
    val detail: String? = null,
    val decodedFields: Map<String, String> = emptyMap()
) {
    @Serializable
    enum class Kind(val label: String) {
        CONNECT_ATTEMPT("Connect Attempt"),
        CONNECTED("Connected"),
        DISCONNECTED("Disconnected"),
        SERVICES_DISCOVERED("Services Discovered"),
        SERVICE_CHANGED("Service Changed"),
        CHARACTERISTIC_READ("Characteristic Read"),
        CHARACTERISTIC_WRITE("Characteristic Write"),
        CHARACTERISTIC_NOTIFY("Characteristic Notify"),
        CHARACTERISTIC_CHANGED("Characteristic Changed"),
        DESCRIPTOR_READ("Descriptor Read"),
        DESCRIPTOR_WRITE("Descriptor Write"),
        MTU_CHANGED("MTU Changed"),
        RSSI_READ("RSSI Read"),
        BOND_CREATED("Bond Created"),
        BOND_FAILED("Bond Failed"),
        PHY_UPDATE("PHY Update"),
        PHY_READ("PHY Read"),
        CONNECTION_STATE_CHANGED("Connection State Changed"),
        RELIABLE_WRITE_COMPLETED("Reliable Write Completed")
    }
    val rawBytes: ByteArray?
        get() = rawHex?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()
}
