package com.wiretap.ui.network

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
import androidx.compose.material.icons.filled.Wifi
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
import com.wiretap.core.NetworkEntry
import com.wiretap.ui.shared.PlaceholderView
import com.wiretap.ui.theme.statusColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HTTP_METHODS = listOf("ALL", "GET", "POST", "PUT", "PATCH", "DELETE")
private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkListScreen(navController: NavController) {
    val entries by WireTap.network.entries.collectAsState()
    var search by remember { mutableStateOf("") }
    var methodFilter by remember { mutableStateOf("ALL") }

    val filtered = entries.filter { e ->
        (methodFilter == "ALL" || e.method == methodFilter) &&
        (search.isEmpty() || e.url.contains(search, ignoreCase = true) ||
         e.responseStatusCode?.toString()?.contains(search) == true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network") },
                actions = {
                    IconButton(onClick = { WireTap.network.clear() }) {
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
                placeholder = { Text("Search URL or status…") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {}

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(HTTP_METHODS) { method ->
                    FilterChip(
                        selected = methodFilter == method,
                        onClick = { methodFilter = method },
                        label = { Text(method) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                PlaceholderView(Icons.Default.Wifi, "No network requests captured")
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { entry ->
                        NetworkRow(entry) { navController.navigate("network/${entry.id}") }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkRow(entry: NetworkEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    entry.method,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    entry.statusLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(entry.responseStatusCode)
                )
                entry.durationMs?.let {
                    Text("${it}ms", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
            Text(entry.url, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(timeFmt.format(Date(entry.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}
