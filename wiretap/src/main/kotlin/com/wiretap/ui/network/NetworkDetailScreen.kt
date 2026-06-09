package com.wiretap.ui.network

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wiretap.WireTap
import com.wiretap.ui.shared.InfoBlock
import com.wiretap.ui.shared.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDetailScreen(entryId: String) {
    val entries by WireTap.network.entries.collectAsState()
    val entry = entries.firstOrNull { it.id == entryId } ?: return
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${entry.method} ${entry.statusLabel()}") },
                actions = {
                    IconButton(onClick = {
                        val clip = ClipData.newPlainText("cURL", entry.toCurl())
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                    }) { Icon(Icons.Default.ContentCopy, "Copy cURL") }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, entry.toCurl())
                        }
                        context.startActivity(Intent.createChooser(intent, "Share"))
                    }) { Icon(Icons.Default.Share, "Share") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            SectionHeader("Request")
            InfoBlock("URL", entry.url, monospace = true)
            InfoBlock("Method", entry.method)
            if (entry.requestHeaders.isNotEmpty()) {
                SectionHeader("Request Headers")
                entry.requestHeaders.forEach { (k, v) -> InfoBlock(k, v, monospace = true) }
            }
            entry.requestBody?.let {
                SectionHeader("Request Body")
                InfoBlock("Body", it, monospace = true)
            }

            SectionHeader("Response")
            entry.responseStatusCode?.let { InfoBlock("Status", it.toString()) }
            entry.durationMs?.let { InfoBlock("Duration", "${it}ms") }
            if (entry.responseHeaders.isNotEmpty()) {
                SectionHeader("Response Headers")
                entry.responseHeaders.forEach { (k, v) -> InfoBlock(k, v, monospace = true) }
            }
            entry.responseBody?.let {
                SectionHeader("Response Body")
                InfoBlock("Body", it, monospace = true)
            }
            entry.error?.let {
                SectionHeader("Error")
                InfoBlock("Error", it)
            }

            SectionHeader("cURL")
            InfoBlock("Command", entry.toCurl(), monospace = true)
        }
    }
}
