package com.kyra2214.mmomultiagente

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyLedgerTest {
    @Test fun recordsCreationConsumptionAndCurrencyWithoutConflatingTradeWithMinting() {
        val ledger = EconomyLedger()
        ledger.record(EconomyEvent(agentId = "mira", tick = 3, type = EconomyEventType.RESOURCE_CREATED, itemId = "Peixe T1", quantity = 2, cause = "fishing"))
        ledger.record(EconomyEvent(agentId = "mira", tick = 4, type = EconomyEventType.RESOURCE_CONSUMED, itemId = "Peixe T1", quantity = 1, cause = "sale"))
        ledger.record(EconomyEvent(agentId = "borin", tick = 4, type = EconomyEventType.TRADE_SETTLED, itemId = "Peixe T1", quantity = 1, cause = "market", counterpartyId = "mira"))

        assertEquals(2, ledger.totalCreated("Peixe T1"))
        assertEquals(1, ledger.totalConsumed("Peixe T1"))
        assertEquals(0, ledger.mintedCurrency())
        assertEquals(3, ledger.events.size)
        assertTrue(ledger.forAgent("mira").any { it.type == EconomyEventType.TRADE_SETTLED })
    }

    @Test fun encodedLedgerRestoresEventsAndSkipsCorruptRows() {
        val original = EconomyLedger()
        original.record(EconomyEvent(
            id = "event-1", agentId = "mira", tick = 42,
            type = EconomyEventType.TRADE_SETTLED, itemId = "Peixe T1",
            quantity = 3, cause = "shared market", counterpartyId = "borin"
        ))
        val encoded = original.encode()
        val restored = EconomyLedger.decode(encoded + "\nnot-a-valid-event")

        assertEquals(1, restored.events.size)
        assertEquals("event-1", restored.events.single().id)
        assertEquals("mira", restored.events.single().agentId)
        assertEquals(42L, restored.events.single().tick)
        assertEquals("Peixe T1", restored.events.single().itemId)
        assertEquals("shared market", restored.events.single().cause)
        assertEquals("borin", restored.events.single().counterpartyId)
        assertEquals(3, restored.events.single().quantity)
    }

    @Test fun openingBalanceAndItemTransfersDoNotCountAsMintingOrProduction() {
        val ledger = EconomyLedger()
        ledger.record(EconomyEvent(
            agentId = "aldren", tick = 0, type = EconomyEventType.CURRENCY_INITIALIZED,
            itemId = "alzes", quantity = 100, cause = "ledger-opening-balance-baseline"
        ))
        ledger.record(EconomyEvent(
            agentId = "aldren", tick = 1, type = EconomyEventType.ITEM_TRANSFERRED,
            itemId = "Armadura T1", quantity = 1, cause = "inventory-to-equipment:ARMOR"
        ))

        assertEquals(0, ledger.mintedCurrency())
        assertEquals(0, ledger.burnedCurrency())
        assertEquals(0, ledger.totalCreated("Armadura T1"))
        assertEquals(0, ledger.totalConsumed("Armadura T1"))
    }

    @Test fun rejectsInvalidEconomicEvents() {
        val badQuantity = runCatching {
            EconomyEvent(agentId = "mira", tick = 0, type = EconomyEventType.RESOURCE_CREATED, itemId = "Peixe T1", quantity = 0, cause = "test")
        }
        val badTick = runCatching {
            EconomyEvent(agentId = "mira", tick = -1, type = EconomyEventType.RESOURCE_CREATED, itemId = "Peixe T1", quantity = 1, cause = "test")
        }
        assertTrue(badQuantity.isFailure)
        assertTrue(badTick.isFailure)
    }

    @Test fun duplicateEventIdIsIgnoredForIdempotentRecovery() {
        val ledger = EconomyLedger()
        ledger.record(EconomyEvent(
            id = "same-event", agentId = "mira", tick = 2,
            type = EconomyEventType.RESOURCE_CREATED, itemId = "Peixe T1",
            quantity = 1, cause = "fishing"
        ))
        ledger.record(EconomyEvent(
            id = "same-event", agentId = "mira", tick = 2,
            type = EconomyEventType.RESOURCE_CREATED, itemId = "Peixe T1",
            quantity = 1, cause = "replayed-fishing"
        ))

        assertEquals(1, ledger.events.size)
        assertEquals("fishing", ledger.events.single().cause)
        assertEquals(1, ledger.totalCreated("Peixe T1"))
    }
}
