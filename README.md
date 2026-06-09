# WireTap Android

A drop-in Android debug inspector for HTTP, BLE, and NFC traffic. Drop it into any debug build to get a live, filterable log of all network calls, Bluetooth GATT events, and NFC tag interactions — with a Compose UI inspector you can open with a single tap.

---

## What it captures

| Signal | Detail |
|--------|--------|
| **HTTP** | Method, URL, headers, request/response body, status code, duration, errors |
| **BLE** | Connect attempts, connected/disconnected, MTU change, services discovered, characteristic reads/writes/notifications |
| **NFC** | Tag discovered (ID + tech list), NDEF records read |

---

## Setup

### 1. Publish the library to mavenLocal

```bash
cd android/
./gradlew :wiretap:publishToMavenLocal
```

This puts the AAR at `~/.m2/repository/com/wiretap/wiretap/1.0.0/`.

### 2. Add mavenLocal to your app's repositories

In your app's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()   // <-- add this first
        google()
        mavenCentral()
    }
}
```

### 3. Add the dependency (debug only)

In `app/build.gradle.kts`:

```kotlin
dependencies {
    debugImplementation("com.wiretap:wiretap:1.0.0")
}
```

WireTap is compiled out of release builds entirely — no code, no overhead.

### 4. Handle the minSdk difference

WireTap targets API 26+. If your app supports API 24/25, add this to your `AndroidManifest.xml` to suppress the merger warning:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-sdk tools:overrideLibrary="com.wiretap" />
    ...
```

---

## Integration

Because `debugImplementation` means WireTap classes don't exist in release builds, all calls into WireTap must go through a source-set-split helper. Create two files with identical signatures:

### `app/src/debug/java/.../debug/WireTapHelper.kt`

```kotlin
package com.yourapp.debug

import android.content.Context
import com.wiretap.WireTap
import com.wiretap.interceptors.WireTapInterceptor
import okhttp3.Interceptor

object WireTapHelper {
    fun init(context: Context) = WireTap.init(context)
    fun newInterceptor(): Interceptor = WireTapInterceptor()
    fun launchInspector(context: Context) = WireTap.launchInspector(context)
}
```

### `app/src/release/java/.../debug/WireTapHelper.kt`

```kotlin
package com.yourapp.debug

import android.content.Context
import okhttp3.Interceptor

object WireTapHelper {
    fun init(context: Context) {}
    fun newInterceptor(): Interceptor = Interceptor { it.proceed(it.request()) }
    fun launchInspector(context: Context) {}
}
```

Both files share the same package and class name. Gradle picks the right one per build variant — no reflection, no runtime checks.

---

## Wiring it up

### Application.onCreate()

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WireTapHelper.init(this)
    }
}
```

### HTTP — OkHttp interceptor

```kotlin
val okHttp = OkHttpClient.Builder()
    .addInterceptor(WireTapHelper.newInterceptor())
    // ... your other interceptors
    .build()
```

Put WireTap **after** auth interceptors and **before** logging so it sees the final request headers and the full response body.

### BLE — GATT callback hooks

Add recording calls inside your existing `BluetoothGattCallback`:

```kotlin
override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
    when (newState) {
        BluetoothProfile.STATE_CONNECTED ->
            WireTapHelper.recordBleConnected(gatt.device.address, gatt.device.name)
        BluetoothProfile.STATE_DISCONNECTED ->
            WireTapHelper.recordBleDisconnected(gatt.device.address, gatt.device.name)
    }
    // your existing logic...
}

override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
    if (status == BluetoothGatt.GATT_SUCCESS)
        WireTapHelper.recordBleServicesDiscovered(gatt.device.address, gatt.device.name)
}

override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
    WireTapHelper.recordBleMtuChanged(gatt.device.address, gatt.device.name, mtu)
}
```

Add the corresponding `record*` no-ops to the release `WireTapHelper`.

### NFC

```kotlin
// When a tag is discovered:
WireTapHelper.recordNfcTagDiscovered(tagId, tag.techList.toList())

// When an NDEF message is read:
WireTapHelper.recordNfcNdefRead(tagId, descriptor, detail)
```

---

## Opening the inspector

### Floating debug button (recommended)

Wrap your root Compose content in a `Box` and add a FAB:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // your existing content

    if (BuildConfig.DEBUG) {
        FloatingActionButton(
            onClick = { WireTapHelper.launchInspector(context) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.BugReport, contentDescription = "WireTap Inspector")
        }
    }
}
```

### Programmatic

```kotlin
WireTapHelper.launchInspector(context)
```

Call this from any button, debug menu, or shake gesture handler.

---

## Team distribution via JitPack

Once your code is pushed to GitHub, teammates can consume WireTap without running `publishToMavenLocal` locally:

```kotlin
// settings.gradle.kts
repositories {
    maven("https://jitpack.io")
    google()
    mavenCentral()
}

// app/build.gradle.kts
debugImplementation("com.github.tribhuwan1123:wiretap-android:main-SNAPSHOT")
```

JitPack builds the AAR from source on first request and caches it. `main-SNAPSHOT` always tracks the latest commit on `main`. For a stable pin, use a specific commit hash or tag instead.

---

## Inspector screens

| Tab | What you see |
|-----|-------------|
| **Timeline** | All HTTP, BLE, and NFC events merged in chronological order |
| **Network** | HTTP request list → tap for full request/response detail |
| **BLE** | GATT event log per device → tap for characteristic hex detail |
| **NFC** | Tag discovery and NDEF record log |

---

## Sample app

The `sample/` module is a self-contained runnable app that exercises all three signals:

```bash
./gradlew :sample:installDebug
```
