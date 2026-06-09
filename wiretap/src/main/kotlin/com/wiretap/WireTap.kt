package com.wiretap

import android.content.Context
import android.content.Intent
import com.wiretap.bridge.LocalBridge
import com.wiretap.core.BleEntry
import com.wiretap.core.BleStore
import com.wiretap.core.Correlator
import com.wiretap.core.LLMExportOptions
import com.wiretap.core.LLMExporter
import com.wiretap.core.NetworkEntry
import com.wiretap.core.NetworkStore
import com.wiretap.core.NfcEntry
import com.wiretap.core.NfcStore
import com.wiretap.core.PersistenceWriter
import com.wiretap.core.SessionDiffer
import com.wiretap.core.SessionSerializer
import com.wiretap.core.StorageConfig
import com.wiretap.core.WireTapRedactionConfig
import com.wiretap.core.WireTapSession
import com.wiretap.core.timeline
import com.wiretap.overlay.WireTapOverlayService
import com.wiretap.ui.WireTapActivity
import java.io.File

object WireTap {

    val network = NetworkStore()
    val ble = BleStore()
    val nfc = NfcStore()

    var redaction: WireTapRedactionConfig = WireTapRedactionConfig()
        set(value) { field = value; network.redactionConfig = value }

    private var bridge: LocalBridge? = null
    private var appContext: Context? = null

    fun init(context: Context, storage: StorageConfig = StorageConfig.InMemory) {
        appContext = context.applicationContext
        if (storage is StorageConfig.Disk) {
            val dir = storage.dir ?: context.filesDir.resolve("wiretap").also { it.mkdirs() }
            network.persistence = PersistenceWriter(
                File(dir, "network.jsonl"), NetworkEntry.serializer(), scope = network.scope
            )
            ble.persistence = PersistenceWriter(
                File(dir, "ble.jsonl"), BleEntry.serializer(), scope = ble.scope
            )
            nfc.persistence = PersistenceWriter(
                File(dir, "nfc.jsonl"), NfcEntry.serializer(), scope = nfc.scope
            )
            network.loadPersisted(network.persistence!!.readAll())
            ble.loadPersisted(ble.persistence!!.readAll())
            nfc.loadPersisted(nfc.persistence!!.readAll())
        }
    }

    fun makeSession(): WireTapSession {
        val ctx = appContext
        val pkgInfo = ctx?.let { runCatching { it.packageManager.getPackageInfo(it.packageName, 0) }.getOrNull() }
        return WireTapSession(
            appId = ctx?.packageName ?: "",
            appVersion = pkgInfo?.versionName ?: "",
            network = network.entries.value,
            ble = ble.entries.value,
            nfc = nfc.entries.value
        )
    }

    fun exportSessionJson(): String = SessionSerializer.encode(makeSession())

    fun importSession(json: String): WireTapSession? = SessionSerializer.decode(json)

    fun exportLLMText(options: LLMExportOptions = LLMExportOptions()): String =
        LLMExporter.export(makeSession(), options)

    fun timeline() = makeSession().timeline()

    fun correlatedEpisodes() = Correlator.correlate(makeSession())

    fun diff(sessionJson: String): com.wiretap.core.SessionDiff? {
        val imported = importSession(sessionJson) ?: return null
        return SessionDiffer.diff(makeSession(), imported)
    }

    fun startLocalBridge(port: Int = 7777) {
        bridge = LocalBridge(port).also { it.start() }
    }

    fun stopLocalBridge() {
        bridge?.stop()
        bridge = null
    }

    fun launchInspector(context: Context) {
        context.startActivity(Intent(context, WireTapActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun installFloatingButton(context: Context) {
        context.startService(Intent(context, WireTapOverlayService::class.java))
    }

    fun removeFloatingButton(context: Context) {
        context.stopService(Intent(context, WireTapOverlayService::class.java))
    }
}
