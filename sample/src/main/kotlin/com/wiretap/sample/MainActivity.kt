package com.wiretap.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wiretap.WireTap
import com.wiretap.core.BleEntry
import com.wiretap.core.NfcEntry
import com.wiretap.interceptors.WireTapInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : ComponentActivity() {

    private val client = OkHttpClient.Builder()
        .addInterceptor(WireTapInterceptor())
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WireTap.init(this)

        setContent {
            val scope = rememberCoroutineScope()
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("WireTap Android Sample",
                            style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(32.dp))

                        Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    client.newCall(
                                        Request.Builder()
                                            .url("https://httpbin.org/get")
                                            .header("Authorization", "Bearer test-token-123")
                                            .build()
                                    ).execute().close()
                                }
                                runCatching {
                                    client.newCall(
                                        Request.Builder()
                                            .url("https://httpbin.org/post")
                                            .post("""{"username":"alice","password":"hunter2"}"""
                                                .toRequestBody("application/json".toMediaType()))
                                            .build()
                                    ).execute().close()
                                }
                                runCatching {
                                    client.newCall(
                                        Request.Builder()
                                            .url("https://httpbin.org/status/404")
                                            .build()
                                    ).execute().close()
                                }
                            }
                        }) { Text("Fire 3 HTTP Requests") }

                        Spacer(Modifier.height(16.dp))

                        Button(onClick = {
                            WireTap.ble.record(BleEntry(
                                kind = BleEntry.Kind.CONNECT_ATTEMPT,
                                peripheralAddress = "AA:BB:CC:DD:EE:FF",
                                peripheralName = "Smart Lock Pro"
                            ))
                            WireTap.ble.record(BleEntry(
                                kind = BleEntry.Kind.CONNECTED,
                                peripheralAddress = "AA:BB:CC:DD:EE:FF",
                                peripheralName = "Smart Lock Pro"
                            ))
                            WireTap.ble.record(BleEntry(
                                kind = BleEntry.Kind.CHARACTERISTIC_NOTIFY,
                                peripheralAddress = "AA:BB:CC:DD:EE:FF",
                                peripheralName = "Smart Lock Pro",
                                characteristicUUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E",
                                rawHex = "0116ff42"
                            ))
                        }) { Text("Simulate BLE Session") }

                        Spacer(Modifier.height(16.dp))

                        Button(onClick = {
                            WireTap.nfc.record(NfcEntry(
                                kind = NfcEntry.Kind.TAG_DISCOVERED,
                                tagId = "04A2B3C4",
                                techList = listOf("NfcA", "Ndef"),
                                detail = "Techs: NfcA, Ndef"
                            ))
                            WireTap.nfc.record(NfcEntry(
                                kind = NfcEntry.Kind.NDEF_READ,
                                tagId = "04A2B3C4",
                                descriptor = "Record 0: TNF=1 Type=T",
                                rawHex = "02656e48656c6c6f",
                                detail = "Hello"
                            ))
                        }) { Text("Simulate NFC Tap") }

                        Spacer(Modifier.height(32.dp))

                        Button(onClick = { WireTap.launchInspector(this@MainActivity) }) {
                            Text("Open WireTap Inspector")
                        }
                    }
                }
            }
        }
    }
}
