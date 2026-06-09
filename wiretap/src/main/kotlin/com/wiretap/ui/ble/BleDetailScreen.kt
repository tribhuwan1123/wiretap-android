package com.wiretap.ui.ble

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wiretap.WireTap
import com.wiretap.ui.shared.InfoBlock
import com.wiretap.ui.shared.SectionHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleDetailScreen(entryId: String) {
    val entries by WireTap.ble.entries.collectAsState()
    val entry = entries.firstOrNull { it.id == entryId } ?: return

    Scaffold(
        topBar = { TopAppBar(title = { Text(entry.kind.label) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            SectionHeader("Event")
            InfoBlock("Kind", entry.kind.label)
            InfoBlock("Time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(entry.timestamp)))
            entry.gattStatus?.let { InfoBlock("GATT Status", it.toString()) }
            entry.detail?.let { InfoBlock("Detail", it) }

            SectionHeader("Device")
            entry.peripheralName?.let { InfoBlock("Name", it) }
            entry.peripheralAddress?.let { InfoBlock("Address", it, monospace = true) }

            if (entry.characteristicUUID != null || entry.serviceUUID != null) {
                SectionHeader("UUIDs")
                entry.serviceUUID?.let { InfoBlock("Service", it, monospace = true) }
                entry.characteristicUUID?.let { InfoBlock("Characteristic", it, monospace = true) }
            }

            if (entry.rawHex != null) {
                SectionHeader("Payload")
                InfoBlock("Hex", entry.rawHex, monospace = true)
                entry.rawAscii?.let { InfoBlock("ASCII", it, monospace = true) }
                InfoBlock("Bytes", "${entry.rawHex.length / 2}")
            }

            if (entry.decodedFields.isNotEmpty()) {
                SectionHeader("Decoded Fields")
                entry.decodedFields.forEach { (k, v) -> InfoBlock(k, v) }
            }
        }
    }
}
