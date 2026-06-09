package com.wiretap.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LLMExportFormat { MARKDOWN, JSONL }

data class LLMExportOptions(
    val format: LLMExportFormat = LLMExportFormat.MARKDOWN,
    val includeNetwork: Boolean = true,
    val includeBle: Boolean = true,
    val includeNfc: Boolean = true,
    val filter: String? = null,
    val maxBodyLength: Int = 2000
)

object LLMExporter {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    fun export(session: WireTapSession, options: LLMExportOptions = LLMExportOptions()): String {
        return when (options.format) {
            LLMExportFormat.MARKDOWN -> toMarkdown(session, options)
            LLMExportFormat.JSONL -> toJsonl(session, options)
        }
    }

    private fun toMarkdown(session: WireTapSession, opts: LLMExportOptions): String {
        val sb = StringBuilder()
        sb.appendLine("# WireTap Session")
        sb.appendLine("- **Device**: ${session.device}")
        sb.appendLine("- **OS**: Android ${session.osVersion}")
        sb.appendLine("- **App**: ${session.appId} ${session.appVersion}")
        sb.appendLine("- **Exported**: ${dateFmt.format(Date(session.exportedAt))}")
        sb.appendLine()

        val entries = session.timeline()
        val filter = opts.filter?.lowercase()

        entries.forEach { entry ->
            when (entry) {
                is TimelineEntry.Network -> if (opts.includeNetwork) {
                    val n = entry.entry
                    if (filter == null || n.url.lowercase().contains(filter) ||
                        n.method.lowercase().contains(filter)) {
                        sb.appendLine("## [${dateFmt.format(Date(n.timestamp))}] HTTP ${n.method} ${n.statusLabel()}")
                        sb.appendLine("**URL**: `${n.url}`")
                        if (n.requestBody != null) sb.appendLine("**Request Body**:\n```\n${n.requestBody.take(opts.maxBodyLength)}\n```")
                        if (n.responseBody != null) sb.appendLine("**Response Body**:\n```\n${n.responseBody.take(opts.maxBodyLength)}\n```")
                        n.error?.let { sb.appendLine("**Error**: $it") }
                        n.durationMs?.let { sb.appendLine("**Duration**: ${it}ms") }
                        sb.appendLine()
                    }
                }
                is TimelineEntry.Ble -> if (opts.includeBle) {
                    val b = entry.entry
                    if (filter == null || b.kind.label.lowercase().contains(filter) ||
                        b.peripheralName?.lowercase()?.contains(filter) == true) {
                        sb.appendLine("## [${dateFmt.format(Date(b.timestamp))}] BLE ${b.kind.label}")
                        b.peripheralName?.let { sb.appendLine("**Device**: $it (${b.peripheralAddress})") }
                        b.characteristicUUID?.let { sb.appendLine("**Characteristic**: $it") }
                        b.rawHex?.let { sb.appendLine("**Data**: `$it`") }
                        if (b.decodedFields.isNotEmpty()) {
                            sb.appendLine("**Decoded**:")
                            b.decodedFields.forEach { (k, v) -> sb.appendLine("  - $k: $v") }
                        }
                        sb.appendLine()
                    }
                }
                is TimelineEntry.Nfc -> if (opts.includeNfc) {
                    val n = entry.entry
                    if (filter == null || n.kind.label.lowercase().contains(filter) ||
                        n.descriptor?.lowercase()?.contains(filter) == true) {
                        sb.appendLine("## [${dateFmt.format(Date(n.timestamp))}] NFC ${n.kind.label}")
                        n.tagId?.let { sb.appendLine("**Tag ID**: $it") }
                        n.descriptor?.let { sb.appendLine("**Descriptor**: $it") }
                        n.rawHex?.let { sb.appendLine("**Data**: `$it`") }
                        sb.appendLine()
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun toJsonl(session: WireTapSession, opts: LLMExportOptions): String {
        val sb = StringBuilder()
        val metaJson = """{"type":"meta","device":"${session.device}","os":"Android ${session.osVersion}","app":"${session.appId} ${session.appVersion}"}"""
        sb.appendLine(metaJson)
        val filter = opts.filter?.lowercase()
        session.timeline().forEach { entry ->
            when (entry) {
                is TimelineEntry.Network -> if (opts.includeNetwork) {
                    val n = entry.entry
                    if (filter == null || n.url.lowercase().contains(filter)) {
                        sb.appendLine("""{"type":"network","ts":${n.timestamp},"method":"${n.method}","url":"${n.url}","status":${n.responseStatusCode ?: "null"},"durationMs":${n.durationMs ?: "null"}}""")
                    }
                }
                is TimelineEntry.Ble -> if (opts.includeBle) {
                    val b = entry.entry
                    if (filter == null || b.kind.name.lowercase().contains(filter)) {
                        sb.appendLine("""{"type":"ble","ts":${b.timestamp},"kind":"${b.kind.name}","device":"${b.peripheralName ?: ""}","address":"${b.peripheralAddress ?: ""}"}""")
                    }
                }
                is TimelineEntry.Nfc -> if (opts.includeNfc) {
                    val n = entry.entry
                    if (filter == null || n.kind.name.lowercase().contains(filter)) {
                        sb.appendLine("""{"type":"nfc","ts":${n.timestamp},"kind":"${n.kind.name}","tagId":"${n.tagId ?: ""}"}""")
                    }
                }
            }
        }
        return sb.toString()
    }
}
