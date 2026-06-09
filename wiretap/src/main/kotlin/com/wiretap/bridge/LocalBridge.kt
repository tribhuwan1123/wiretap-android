package com.wiretap.bridge

import com.wiretap.WireTap
import com.wiretap.core.LLMExportOptions
import com.wiretap.core.SessionSerializer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

/**
 * Minimal HTTP/1.1 server bound to 127.0.0.1 only.
 * Mirrors the iOS NWListener LocalBridge for MCP integration.
 *
 * Endpoints:
 *   GET /session     — full WireTapSession JSON
 *   GET /overview    — event counts + error summary
 *   GET /timeline    — LLM-ready markdown text
 *   GET /ble         — BLE entries (query: type=, uuid=, device=, limit=)
 *   GET /nfc         — NFC entries (query: limit=)
 *   GET /network/failures — non-2xx / errored requests (query: limit=)
 */
class LocalBridge(private val port: Int = 7777) {

    private var serverSocket: ServerSocket? = null
    private var thread: Thread? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            try {
                val ss = ServerSocket(port, 50, java.net.InetAddress.getByName("127.0.0.1"))
                serverSocket = ss
                while (running) {
                    val client = ss.accept()
                    Thread { handle(client) }.start()
                }
            } catch (_: Exception) { }
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop() {
        running = false
        runCatching { serverSocket?.close() }
        thread?.interrupt()
    }

    private fun handle(socket: Socket) {
        socket.use {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val fullPath = parts[1]
            val path = fullPath.substringBefore("?")
            val query = if ("?" in fullPath) fullPath.substringAfter("?") else ""
            val params = parseQuery(query)

            if (method != "GET") {
                respond(socket, 405, "text/plain", "Method Not Allowed")
                return
            }

            val (status, body) = route(path, params)
            respond(socket, status, "application/json; charset=utf-8", body)
        }
    }

    private fun route(path: String, params: Map<String, String>): Pair<Int, String> = when (path) {
        "/session" -> {
            200 to SessionSerializer.encode(WireTap.makeSession())
        }
        "/overview" -> {
            val net = WireTap.network.entries.value
            val ble = WireTap.ble.entries.value
            val nfc = WireTap.nfc.entries.value
            val failures = net.count { it.isError }
            200 to """{"network":${net.size},"ble":${ble.size},"nfc":${nfc.size},"networkErrors":$failures}"""
        }
        "/timeline" -> {
            200 to """"${WireTap.exportLLMText(LLMExportOptions()).replace("\"", "\\\"")}""""
        }
        "/ble" -> {
            val limit = params["limit"]?.toIntOrNull() ?: 100
            val typeFilter = params["type"]?.uppercase()
            val uuidFilter = params["uuid"]?.uppercase()
            val deviceFilter = params["device"]?.lowercase()
            val entries = WireTap.ble.entries.value
                .filter { e ->
                    (typeFilter == null || e.kind.name == typeFilter) &&
                    (uuidFilter == null || e.characteristicUUID?.uppercase() == uuidFilter) &&
                    (deviceFilter == null || e.peripheralName?.lowercase()?.contains(deviceFilter) == true)
                }
                .take(limit)
            200 to kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(com.wiretap.core.BleEntry.serializer()),
                entries
            )
        }
        "/nfc" -> {
            val limit = params["limit"]?.toIntOrNull() ?: 50
            val entries = WireTap.nfc.entries.value.take(limit)
            200 to kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(com.wiretap.core.NfcEntry.serializer()),
                entries
            )
        }
        "/network/failures" -> {
            val limit = params["limit"]?.toIntOrNull() ?: 50
            val failures = WireTap.network.entries.value.filter { it.isError }.take(limit)
            200 to kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(com.wiretap.core.NetworkEntry.serializer()),
                failures
            )
        }
        else -> 404 to """{"error":"not found","path":"$path"}"""
    }

    private fun respond(socket: Socket, status: Int, contentType: String, body: String) {
        val pw = PrintWriter(socket.getOutputStream(), true)
        pw.println("HTTP/1.1 $status ${statusText(status)}")
        pw.println("Content-Type: $contentType")
        pw.println("Content-Length: ${body.toByteArray().size}")
        pw.println("Connection: close")
        pw.println()
        pw.print(body)
        pw.flush()
    }

    private fun parseQuery(query: String): Map<String, String> =
        if (query.isEmpty()) emptyMap()
        else query.split("&").mapNotNull {
            val (k, v) = it.split("=").let { p -> if (p.size == 2) p[0] to p[1] else return@mapNotNull null }
            k to java.net.URLDecoder.decode(v, "UTF-8")
        }.toMap()

    private fun statusText(code: Int) = when (code) {
        200 -> "OK"; 404 -> "Not Found"; 405 -> "Method Not Allowed"
        else -> "Unknown"
    }
}
