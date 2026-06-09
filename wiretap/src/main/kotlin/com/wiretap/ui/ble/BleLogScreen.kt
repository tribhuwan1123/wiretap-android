package com.wiretap.ui.ble

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wiretap.WireTap
import com.wiretap.core.BleEntry
import com.wiretap.ui.shared.PlaceholderView
import com.wiretap.ui.theme.BleColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleLogScreen(navController: NavController) {
    val entries by WireTap.ble.entries.collectAsState()
    var search by remember { mutableStateOf("") }
    var kindFilter by remember { mutableStateOf<BleEntry.Kind?>(null) }

    val filtered = entries.filter { e ->
        (kindFilter == null || e.kind == kindFilter) &&
        (search.isEmpty() ||
         e.peripheralName?.contains(search, ignoreCase = true) == true ||
         e.peripheralAddress?.contains(search, ignoreCase = true) == true ||
         e.characteristicUUID?.contains(search, ignoreCase = true) == true ||
         e.kind.label.contains(search, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BLE") },
                actions = {
                    IconButton(onClick = { navController.navigate("ble/lifecycle") }) {
                        Text("Lifecycle", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { WireTap.ble.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchBar(
                query = search,
                onQueryChange = { search = it },
                onSearch = {},
                active = false,
                onActiveChange = {},
                placeholder = { Text("Search device, UUID, event…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {}

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                item {
                    FilterChip(selected = kindFilter == null, onClick = { kindFilter = null },
                        label = { Text("All") })
                }
                items(BleEntry.Kind.entries) { kind ->
                    FilterChip(selected = kindFilter == kind, onClick = { kindFilter = kind },
                        label = { Text(kind.label) })
                }
            }

            if (filtered.isEmpty()) {
                PlaceholderView(Icons.Default.Bluetooth, "No BLE events captured")
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { entry ->
                        BleRow(entry) { navController.navigate("ble/${entry.id}") }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BleRow(entry: BleEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.kind.label, style = MaterialTheme.typography.labelMedium, color = BleColor)
            entry.peripheralName?.let {
                Text("$it · ${entry.peripheralAddress ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            entry.characteristicUUID?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline, maxLines = 1)
            }
            Text(timeFmt.format(Date(entry.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }
        entry.rawHex?.let {
            Text(it.take(8) + if (it.length > 8) "…" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}
