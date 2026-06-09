package com.wiretap.ui.nfc

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wiretap.WireTap
import com.wiretap.core.NfcEntry
import com.wiretap.ui.shared.InfoBlock
import com.wiretap.ui.shared.PlaceholderView
import com.wiretap.ui.shared.SectionHeader
import com.wiretap.ui.theme.NfcColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcLogScreen() {
    val entries by WireTap.nfc.entries.collectAsState()
    var search by remember { mutableStateOf("") }
    var kindFilter by remember { mutableStateOf<NfcEntry.Kind?>(null) }
    var selectedEntry by remember { mutableStateOf<NfcEntry?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val filtered = entries.filter { e ->
        (kindFilter == null || e.kind == kindFilter) &&
        (search.isEmpty() ||
         e.kind.label.contains(search, ignoreCase = true) ||
         e.tagId?.contains(search, ignoreCase = true) == true ||
         e.descriptor?.contains(search, ignoreCase = true) == true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFC") },
                actions = {
                    IconButton(onClick = { WireTap.nfc.clear() }) {
                        Icon(Icons.Default.Delete, "Clear")
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
                placeholder = { Text("Search kind, tag, descriptor…") },
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
                items(NfcEntry.Kind.entries) { kind ->
                    FilterChip(selected = kindFilter == kind, onClick = { kindFilter = kind },
                        label = { Text(kind.label) })
                }
            }

            if (filtered.isEmpty()) {
                PlaceholderView(Icons.Default.Nfc, "No NFC events captured")
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { entry ->
                        NfcRow(entry) {
                            selectedEntry = entry
                            scope.launch { sheetState.show() }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        selectedEntry?.let { entry ->
            ModalBottomSheet(onDismissRequest = { selectedEntry = null }, sheetState = sheetState) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    SectionHeader("NFC Event")
                    InfoBlock("Kind", entry.kind.label)
                    InfoBlock("Time", timeFmt.format(Date(entry.timestamp)))
                    entry.tagId?.let { InfoBlock("Tag ID", it, monospace = true) }
                    if (entry.techList.isNotEmpty()) InfoBlock("Technologies", entry.techList.joinToString())
                    entry.descriptor?.let { InfoBlock("Descriptor", it) }
                    entry.rawHex?.let { InfoBlock("Data (Hex)", it, monospace = true) }
                    entry.detail?.let { InfoBlock("Detail", it) }
                }
            }
        }
    }
}

@Composable
private fun NfcRow(entry: NfcEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.kind.label, style = MaterialTheme.typography.labelMedium, color = NfcColor)
            entry.tagId?.let {
                Text("Tag: $it", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            entry.descriptor?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline, maxLines = 1)
            }
            Text(timeFmt.format(Date(entry.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}
