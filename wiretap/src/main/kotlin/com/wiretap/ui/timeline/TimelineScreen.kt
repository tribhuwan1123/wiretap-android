package com.wiretap.ui.timeline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wiretap.WireTap
import com.wiretap.core.BleEntry
import com.wiretap.core.NetworkEntry
import com.wiretap.core.NfcEntry
import com.wiretap.core.TimelineEntry
import com.wiretap.core.timeline
import com.wiretap.ui.shared.PlaceholderView
import com.wiretap.ui.theme.BleColor
import com.wiretap.ui.theme.NetworkColor
import com.wiretap.ui.theme.NfcColor
import com.wiretap.ui.theme.statusColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(navController: NavController) {
    val networkEntries by WireTap.network.entries.collectAsState()
    val bleEntries by WireTap.ble.entries.collectAsState()
    val nfcEntries by WireTap.nfc.entries.collectAsState()

    val session = com.wiretap.core.WireTapSession(
        network = networkEntries,
        ble = bleEntries,
        nfc = nfcEntries
    )
    val entries = session.timeline()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Timeline") }) }
    ) { padding ->
        if (entries.isEmpty()) {
            PlaceholderView(Icons.Default.List, "No events captured yet")
        } else {
            LazyColumn(contentPadding = padding) {
                items(entries, key = { e ->
                    when (e) {
                        is TimelineEntry.Network -> e.entry.id
                        is TimelineEntry.Ble -> e.entry.id
                        is TimelineEntry.Nfc -> e.entry.id
                    }
                }) { entry ->
                    TimelineRow(entry) {
                        when (entry) {
                            is TimelineEntry.Network -> navController.navigate("network/${entry.entry.id}")
                            is TimelineEntry.Ble -> navController.navigate("ble/${entry.entry.id}")
                            is TimelineEntry.Nfc -> {}
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (entry) {
            is TimelineEntry.Network -> {
                Icon(Icons.Default.Wifi, contentDescription = "Network",
                    tint = statusColor(entry.entry.responseStatusCode))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("${entry.entry.method} ${entry.entry.statusLabel()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor(entry.entry.responseStatusCode))
                    Text(entry.entry.url, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(timeFmt.format(Date(entry.entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            is TimelineEntry.Ble -> {
                Icon(Icons.Default.Bluetooth, contentDescription = "BLE", tint = BleColor)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(entry.entry.kind.label, style = MaterialTheme.typography.labelMedium, color = BleColor)
                    entry.entry.peripheralName?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(timeFmt.format(Date(entry.entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            is TimelineEntry.Nfc -> {
                Icon(Icons.Default.Nfc, contentDescription = "NFC", tint = NfcColor)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(entry.entry.kind.label, style = MaterialTheme.typography.labelMedium, color = NfcColor)
                    entry.entry.tagId?.let {
                        Text("Tag: $it", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(timeFmt.format(Date(entry.entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
