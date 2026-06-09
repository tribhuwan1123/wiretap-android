package com.wiretap.ui.ble

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
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
import com.wiretap.WireTap
import com.wiretap.core.BleConnectionAttempt
import com.wiretap.core.ConnectionLifecycleGrouper
import com.wiretap.ui.shared.PlaceholderView
import com.wiretap.ui.theme.BleColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionLifecycleScreen() {
    val entries by WireTap.ble.entries.collectAsState()
    val attempts = ConnectionLifecycleGrouper.group(entries)
    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Connection Lifecycle") }) }
    ) { padding ->
        if (attempts.isEmpty()) {
            PlaceholderView(Icons.Default.HourglassEmpty, "No BLE connection attempts recorded")
        } else {
            LazyColumn(contentPadding = padding) {
                items(attempts) { attempt ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (icon, tint) = when (attempt.outcome) {
                                    is BleConnectionAttempt.Outcome.Streaming -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                                    is BleConnectionAttempt.Outcome.Failed -> Icons.Default.Error to Color(0xFFF44336)
                                    is BleConnectionAttempt.Outcome.Disconnected -> Icons.Default.RadioButtonUnchecked to MaterialTheme.colorScheme.outline
                                    is BleConnectionAttempt.Outcome.InProgress -> Icons.Default.HourglassEmpty to BleColor
                                }
                                Icon(icon, contentDescription = null, tint = tint)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(attempt.peripheralName ?: attempt.peripheralAddress,
                                        style = MaterialTheme.typography.titleSmall)
                                    Text(timeFmt.format(Date(attempt.startTime)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row {
                                attempt.phases.forEachIndexed { i, phase ->
                                    if (i > 0) Text(" → ", color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.labelSmall)
                                    Text(phase.name, style = MaterialTheme.typography.labelSmall,
                                        color = BleColor)
                                }
                            }
                            if (attempt.outcome is BleConnectionAttempt.Outcome.Failed) {
                                Spacer(Modifier.height(4.dp))
                                Text("Failed at ${attempt.outcome.phase}: ${attempt.outcome.reason ?: "unknown"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF44336))
                            }
                        }
                    }
                }
            }
        }
    }
}
