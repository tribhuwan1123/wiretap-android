package com.wiretap.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import com.wiretap.WireTap
import com.wiretap.core.BleEntry

/**
 * Wraps your existing BluetoothGattCallback and auto-records all events.
 *
 * Usage:
 *   val wireTapCallback = BleWireTapGattCallback(myCallback)
 *   device.connectGatt(context, false, wireTapCallback)
 */
class BleWireTapGattCallback(
    private val delegate: BluetoothGattCallback? = null
) : BluetoothGattCallback() {

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        val kind = when (newState) {
            BluetoothProfile.STATE_CONNECTING -> BleEntry.Kind.CONNECT_ATTEMPT
            BluetoothProfile.STATE_CONNECTED -> BleEntry.Kind.CONNECTED
            BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED -> BleEntry.Kind.DISCONNECTED
            else -> BleEntry.Kind.CONNECTION_STATE_CHANGED
        }
        record(gatt, kind, gattStatus = status, detail = stateLabel(newState))
        delegate?.onConnectionStateChange(gatt, status, newState)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        val detail = gatt.services.joinToString { it.uuid.toString() }
        record(gatt, BleEntry.Kind.SERVICES_DISCOVERED, gattStatus = status, detail = detail)
        delegate?.onServicesDiscovered(gatt, status)
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        record(gatt, BleEntry.Kind.CHARACTERISTIC_READ,
            characteristicUUID = characteristic.uuid.toString(),
            data = value, gattStatus = status)
        delegate?.onCharacteristicRead(gatt, characteristic, value, status)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in API 33")
    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        val value = characteristic.value ?: byteArrayOf()
        record(gatt, BleEntry.Kind.CHARACTERISTIC_READ,
            characteristicUUID = characteristic.uuid.toString(),
            data = value, gattStatus = status)
        @Suppress("DEPRECATION")
        delegate?.onCharacteristicRead(gatt, characteristic, status)
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        record(gatt, BleEntry.Kind.CHARACTERISTIC_WRITE,
            characteristicUUID = characteristic.uuid.toString(),
            gattStatus = status)
        delegate?.onCharacteristicWrite(gatt, characteristic, status)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        record(gatt, BleEntry.Kind.CHARACTERISTIC_CHANGED,
            characteristicUUID = characteristic.uuid.toString(),
            data = value)
        delegate?.onCharacteristicChanged(gatt, characteristic, value)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in API 33")
    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.value ?: byteArrayOf()
        record(gatt, BleEntry.Kind.CHARACTERISTIC_CHANGED,
            characteristicUUID = characteristic.uuid.toString(),
            data = value)
        @Suppress("DEPRECATION")
        delegate?.onCharacteristicChanged(gatt, characteristic)
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int,
        value: ByteArray
    ) {
        record(gatt, BleEntry.Kind.DESCRIPTOR_READ,
            characteristicUUID = descriptor.characteristic?.uuid?.toString(),
            data = value, gattStatus = status,
            detail = "Descriptor: ${descriptor.uuid}")
        delegate?.onDescriptorRead(gatt, descriptor, status, value)
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        record(gatt, BleEntry.Kind.DESCRIPTOR_WRITE,
            characteristicUUID = descriptor.characteristic?.uuid?.toString(),
            gattStatus = status, detail = "Descriptor: ${descriptor.uuid}")
        delegate?.onDescriptorWrite(gatt, descriptor, status)
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        record(gatt, BleEntry.Kind.MTU_CHANGED, gattStatus = status, detail = "MTU: $mtu")
        delegate?.onMtuChanged(gatt, mtu, status)
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        record(gatt, BleEntry.Kind.RSSI_READ, gattStatus = status, detail = "RSSI: $rssi dBm")
        delegate?.onReadRemoteRssi(gatt, rssi, status)
    }

    override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
        record(gatt, BleEntry.Kind.RELIABLE_WRITE_COMPLETED, gattStatus = status)
        delegate?.onReliableWriteCompleted(gatt, status)
    }

    override fun onPhyUpdate(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
        record(gatt, BleEntry.Kind.PHY_UPDATE, gattStatus = status, detail = "TX=$txPhy RX=$rxPhy")
        delegate?.onPhyUpdate(gatt, txPhy, rxPhy, status)
    }

    override fun onPhyRead(gatt: BluetoothGatt, txPhy: Int, rxPhy: Int, status: Int) {
        record(gatt, BleEntry.Kind.PHY_READ, gattStatus = status, detail = "TX=$txPhy RX=$rxPhy")
        delegate?.onPhyRead(gatt, txPhy, rxPhy, status)
    }

    override fun onServiceChanged(gatt: BluetoothGatt) {
        record(gatt, BleEntry.Kind.SERVICE_CHANGED)
        delegate?.onServiceChanged(gatt)
    }

    private fun record(
        gatt: BluetoothGatt,
        kind: BleEntry.Kind,
        characteristicUUID: String? = null,
        data: ByteArray? = null,
        gattStatus: Int? = null,
        detail: String? = null
    ) {
        WireTap.ble.record(
            BleEntry(
                kind = kind,
                peripheralAddress = gatt.device?.address,
                peripheralName = runCatching { gatt.device?.name }.getOrNull(),
                characteristicUUID = characteristicUUID,
                rawHex = data?.joinToString("") { "%02x".format(it) },
                rawAscii = data?.let { String(it, Charsets.ISO_8859_1) },
                gattStatus = gattStatus,
                detail = detail
            )
        )
    }

    private fun stateLabel(state: Int) = when (state) {
        BluetoothProfile.STATE_CONNECTING -> "connecting"
        BluetoothProfile.STATE_CONNECTED -> "connected"
        BluetoothProfile.STATE_DISCONNECTING -> "disconnecting"
        BluetoothProfile.STATE_DISCONNECTED -> "disconnected"
        else -> "state=$state"
    }
}
