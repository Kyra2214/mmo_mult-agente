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
        assertEquals(2, ledger.events.size)
        assertTrue(ledger.forAgent("mira").any { it.type == EconomyEventType.TRADE_SETTLED })
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
}
