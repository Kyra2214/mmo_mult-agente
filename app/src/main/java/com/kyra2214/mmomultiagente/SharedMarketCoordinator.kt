package com.kyra2214.mmomultiagente

/**
 * Coordinates the legacy per-agent listing slots as one server-visible market.
 * A listing is reserved in the seller's engine; settlement moves the reserved item
 * and currency together. The coordinator never creates a buyer with infinite funds.
 */
data class MarketParticipant(
    val id: String,
    val displayName: String,
    val engine: SimulatorEngine
)

data class SharedMarketOffer(
    val sellerId: String,
    val itemId: String,
    val unitPrice: Int,
    val ageTicks: Int
)

class SharedMarketCoordinator {
    fun currentOffers(participants: List<MarketParticipant>): List<SharedMarketOffer> =
        participants.mapNotNull { seller ->
            val item = seller.engine.marketListing ?: return@mapNotNull null
            val price = seller.engine.marketPrice ?: return@mapNotNull null
            SharedMarketOffer(
                sellerId = seller.id,
                itemId = item,
                unitPrice = price,
                ageTicks = seller.engine.marketListingAgeTicks
            )
        }

    /**
     * One deterministic matching pass per server tick. Buyers must be other agents,
     * have enough currency, and have a simple observable demand for the item.
     */
    fun tick(participants: List<MarketParticipant>, ledger: EconomyLedger? = null): List<String> {
        val byId = participants.associateBy { it.id }
        val events = mutableListOf<String>()
        val offers = currentOffers(participants)
            .sortedWith(compareBy<SharedMarketOffer> { it.ageTicks }.thenBy { it.sellerId })

        for (offer in offers) {
            val seller = byId[offer.sellerId] ?: continue
            if (seller.engine.marketListing != offer.itemId) continue
            if (offer.ageTicks < MIN_LISTING_AGE_TICKS) continue

            val buyer = participants.asSequence()
                .filter { it.id != offer.sellerId }
                .filter { it.engine.silver >= offer.unitPrice }
                .filter { wantsItem(it.engine, offer.itemId) }
                .sortedBy { it.id }
                .firstOrNull() ?: continue

            // Validate every precondition before mutating either participant.
            if (seller.engine.marketListing != offer.itemId ||
                seller.engine.marketPrice != offer.unitPrice ||
                buyer.engine.silver < offer.unitPrice
            ) continue

            buyer.engine.silver -= offer.unitPrice
            seller.engine.silver += offer.unitPrice
            buyer.engine.inventory[offer.itemId] =
                (buyer.engine.inventory[offer.itemId] ?: 0) + 1
            seller.engine.marketListing = null
            seller.engine.marketPrice = null
            seller.engine.marketListingAgeTicks = 0
            ledger?.record(EconomyEvent(
                agentId = buyer.id,
                tick = buyer.engine.worldTick,
                type = EconomyEventType.TRADE_SETTLED,
                itemId = offer.itemId,
                quantity = 1,
                cause = "market-purchase",
                counterpartyId = seller.id
            ))
            ledger?.record(EconomyEvent(
                agentId = seller.id,
                tick = seller.engine.worldTick,
                type = EconomyEventType.TRADE_SETTLED,
                itemId = offer.itemId,
                quantity = 1,
                cause = "market-sale",
                counterpartyId = buyer.id
            ))

            val message = "Mercado compartilhado: ${buyer.displayName} comprou 1 ${offer.itemId} de ${seller.displayName} por ${offer.unitPrice} alzes."
            buyer.engine.log.add("Tick ${buyer.engine.worldTick} — $message")
            seller.engine.log.add("Tick ${seller.engine.worldTick} — $message")
            trimLog(buyer.engine)
            trimLog(seller.engine)
            events += message
        }
        return events
    }

    private fun wantsItem(engine: SimulatorEngine, itemId: String): Boolean {
        if (itemId.startsWith("Peixe ")) {
            return (engine.inventory[itemId] ?: 0) == 0
        }
        if (itemId in setOf("Armadura T1", "Capacete T1", "Luvas T1", "Botas T1", "Arma T1", "Mão Secundária T1", "Item Mágico T1")) {
            return itemId !in engine.inventory && engine.equippedGear.isEmpty()
        }
        return false
    }

    private fun trimLog(engine: SimulatorEngine) {
        while (engine.log.size > MAX_LOG_ENTRIES) engine.log.removeAt(0)
    }

    companion object {
        const val MIN_LISTING_AGE_TICKS = 5
        private const val MAX_LOG_ENTRIES = 120
    }
}
