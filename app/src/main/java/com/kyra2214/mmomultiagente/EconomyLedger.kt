package com.kyra2214.mmomultiagente

import java.util.UUID
import java.nio.charset.StandardCharsets
import java.util.Base64

enum class EconomyEventType {
    RESOURCE_CREATED,
    RESOURCE_CONSUMED,
    CURRENCY_MINTED,
    CURRENCY_BURNED,
    CURRENCY_INITIALIZED,
    ITEM_TRANSFERRED,
    CURRENCY_TRANSFERRED,
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
 * Append-only economic event ledger. The host persists encode() in server preferences.
 * Malformed rows are skipped during recovery so one damaged row cannot discard the full history.
 */
class EconomyLedger(initialEvents: Iterable<EconomyEvent> = emptyList()) {
    private val mutableEvents = initialEvents.toMutableList()
    private val eventIds = mutableEvents.mapTo(mutableSetOf()) { it.id }
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()
    private fun pack(value: String): String = encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun unpack(value: String): String = String(decoder.decode(value), StandardCharsets.UTF_8)
    val events: List<EconomyEvent> get() = mutableEvents.toList()

    @Synchronized
    fun record(event: EconomyEvent) {
        if (eventIds.add(event.id)) mutableEvents.add(event)
    }

    /** Stable text format suitable for SharedPreferences; string fields are URL-safe Base64. */
    @Synchronized
    fun encode(): String = mutableEvents.joinToString("\n") { event ->
        listOf(
            pack(event.id), pack(event.agentId), event.tick.toString(), event.type.name,
            pack(event.itemId), event.quantity.toString(), pack(event.cause),
            event.counterpartyId?.let(::pack).orEmpty()
        ).joinToString("|")
    }

    companion object {
        fun decode(encoded: String): EconomyLedger {
            if (encoded.isBlank()) return EconomyLedger()
            val recovered = encoded.lineSequence().mapNotNull { line ->
                runCatching {
                    val fields = line.split("|")
                    require(fields.size == 8)
                    EconomyEvent(
                        id = String(Base64.getUrlDecoder().decode(fields[0]), StandardCharsets.UTF_8),
                        agentId = String(Base64.getUrlDecoder().decode(fields[1]), StandardCharsets.UTF_8),
                        tick = fields[2].toLong(),
                        type = EconomyEventType.valueOf(fields[3]),
                        itemId = String(Base64.getUrlDecoder().decode(fields[4]), StandardCharsets.UTF_8),
                        quantity = fields[5].toInt(),
                        cause = String(Base64.getUrlDecoder().decode(fields[6]), StandardCharsets.UTF_8),
                        counterpartyId = fields[7].takeIf { it.isNotEmpty() }?.let {
                            String(Base64.getUrlDecoder().decode(it), StandardCharsets.UTF_8)
                        }
                    )
                }.getOrNull()
            }.toList()
            return EconomyLedger(recovered)
        }
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
