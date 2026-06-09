package com.wiretap.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

class PersistenceWriter<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val maxLines: Int = 5000,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun append(entry: T) {
        scope.launch {
            try {
                val line = json.encodeToString(serializer, entry)
                file.appendText(line + "\n")
                if (file.readLines().size > maxLines) compact()
            } catch (_: Exception) { }
        }
    }

    fun clear() {
        scope.launch { runCatching { file.writeText("") } }
    }

    fun readAll(): List<T> = try {
        file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
            .reversed()
    } catch (_: Exception) {
        emptyList()
    }

    private fun compact() {
        val lines = file.readLines().filter { it.isNotBlank() }
        val kept = lines.takeLast(maxLines)
        file.writeText(kept.joinToString("\n") + "\n")
    }
}

sealed class StorageConfig {
    object InMemory : StorageConfig()
    data class Disk(val dir: File? = null) : StorageConfig()
}
