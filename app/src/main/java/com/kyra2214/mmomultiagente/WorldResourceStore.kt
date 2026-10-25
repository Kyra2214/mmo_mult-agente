package com.kyra2214.mmomultiagente

/**
 * Shared finite stocks for gatherable world nodes. Harvest is atomic: a failed
 * request never partially removes resources. The host persists snapshot() separately.
 */
class WorldResourceStore(initialStocks: Map<String, Int> = DEFAULT_STOCKS) {
    private val stocks = initialStocks.mapValues { (_, quantity) -> quantity.coerceAtLeast(0) }.toMutableMap()

    @Synchronized
    fun available(resourceId: String): Int = stocks[resourceId] ?: 0

    @Synchronized
    fun harvest(resourceId: String, quantity: Int): Boolean {
        require(resourceId.isNotBlank())
        require(quantity > 0)
        val current = stocks[resourceId] ?: 0
        if (current < quantity) return false
        stocks[resourceId] = current - quantity
        return true
    }

    @Synchronized
    fun snapshot(): Map<String, Int> = stocks.toMap()

    @Synchronized
    fun encode(): String = stocks.entries.sortedBy { it.key }
        .joinToString(";") { (resource, quantity) -> "$resource:$quantity" }

    companion object {
        val DEFAULT_STOCKS = mapOf(
            "Minério T1" to 120, "Minério T2" to 90, "Minério T3" to 60,
            "Pedra T1" to 120, "Pedra T2" to 90, "Pedra T3" to 60,
            "Madeira T1" to 120, "Madeira T2" to 90, "Madeira T3" to 60,
            "Fibra T1" to 120, "Fibra T2" to 90, "Fibra T3" to 60,
            "Fragmento Arcano T1" to 60, "Fragmento Arcano T2" to 45, "Fragmento Arcano T3" to 30,
            "Creature T1" to 80, "Creature T2" to 60, "Creature T3" to 40,
            "Peixe T1" to 100
        )

        fun decode(encoded: String): WorldResourceStore {
            if (encoded.isBlank()) return WorldResourceStore()
            val decoded = encoded.split(";").mapNotNull { entry ->
                val parts = entry.split(":", limit = 2)
                if (parts.size != 2) null
                else {
                    val amount = parts[1].toIntOrNull() ?: return@mapNotNull null
                    if (amount < 0) null else parts[0] to amount
                }
            }.toMap()
            return WorldResourceStore(decoded)
        }
    }
}
