package com.wiretap.core

data class SessionDiff(
    val onlyInA: List<String>,
    val onlyInB: List<String>,
    val common: List<String>
)

object SessionDiffer {
    fun diff(a: WireTapSession, b: WireTapSession): SessionDiff {
        val sigA = signatures(a)
        val sigB = signatures(b)

        val aCount = sigA.groupingBy { it }.eachCount().toMutableMap()
        val bCount = sigB.groupingBy { it }.eachCount().toMutableMap()

        val onlyInA = mutableListOf<String>()
        val onlyInB = mutableListOf<String>()
        val common = mutableListOf<String>()
        val allKeys = (aCount.keys + bCount.keys).toSet()

        allKeys.forEach { key ->
            val ca = aCount[key] ?: 0
            val cb = bCount[key] ?: 0
            val shared = minOf(ca, cb)
            repeat(shared) { common.add(key) }
            repeat(ca - shared) { onlyInA.add(key) }
            repeat(cb - shared) { onlyInB.add(key) }
        }
        return SessionDiff(onlyInA.sorted(), onlyInB.sorted(), common.sorted())
    }

    private fun signatures(session: WireTapSession): List<String> {
        val sigs = mutableListOf<String>()
        session.network.forEach { sigs.add("net:${it.method}:${it.responseStatusCode}") }
        session.ble.forEach { sigs.add("ble:${it.kind.name}:${it.characteristicUUID ?: ""}") }
        session.nfc.forEach { sigs.add("nfc:${it.kind.name}:${it.descriptor ?: ""}") }
        return sigs
    }
}
