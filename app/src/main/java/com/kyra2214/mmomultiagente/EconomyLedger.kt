package com.kyra2214.mmomultiagente

import java.util.UUID

enum class EconomyEventType {
    RESOURCE_CREATED,
    RESOURCE_CONSUMED,
    CURRENCY_MINTED,
    CURRENCY_BURNED,
    TRADE_SETTLED
}

data class EconomyEvent(
    val id: String = UUID.randomUUID().toString(),
    val agentId: String,
    val tick: Long,
    val type: EconomyEventType,
    val itemId: String,
    val quantity: Int,
    val cause: String,
    val counterpartyId: String? = null
) {
    init {
        require(agentId.isNotBlank())
        require(tick >= 0)
        require(itemId.isNotBlank())
        require(quantity > 0)
        require(cause.isNotBlank())
    }
}

/**
 * Append-only in-memory ledger for the current simulation session.
 * Persistence and replay reconciliation are intentionally separate follow-up work.
 */
class EconomyLedger {
    private val mutableEvents = mutableListOf<EconomyEvent>()
    val events: List<EconomyEvent> get() = mutableEvents.toList()

    fun record(event: EconomyEvent) {
        mutableEvents.add(event)
    }

    fun recordAll(newEvents: Iterable<EconomyEvent>) {
        newEvents.forEach(::record)
    }

    fun forAgent(agentId: String): List<EconomyEvent> =
        mutableEvents.filter { it.agentId == agentId || it.counterpartyId == agentId }

    fun totalCreated(itemId: String): Int =
        mutableEvents.filter { it.type == EconomyEventType.RESOURCE_CREATED && it.itemId == itemId }
            .sumOf { it.quantity }

    fun totalConsumed(itemId: String): Int =
        mutableEvents.filter { it.type == EconomyEventType.RESOURCE_CONSUMED && it.itemId == itemId }
            .sumOf { it.quantity }

    fun mintedCurrency(): Int =
        mutableEvents.filter { it.type == EconomyEventType.CURRENCY_MINTED }.sumOf { it.quantity }

    fun burnedCurrency(): Int =
        mutableEvents.filter { it.type == EconomyEventType.CURRENCY_BURNED }.sumOf { it.quantity }
}
