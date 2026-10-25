package com.kyra2214.mmomultiagente

import org.junit.Assert.*
import org.junit.Test

class MarketSettlementTest {
    @Test fun listingRemainsReservedUntilBuyerVisit() {
        val engine = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            silver = 100,
            inventory = mapOf("Barra T1" to 2, "Couro Curtido T1" to 2, "Tecido T1" to 1),
            marketListing = "Armadura T1",
            marketPrice = 25,
            marketListingAgeTicks = 3,
            worldTick = 3
        )
        engine.tick()
        assertEquals("Armadura T1", engine.marketListing)
        assertEquals(25, engine.marketPrice)
        assertEquals(100, engine.silver)
        assertEquals(4, engine.marketListingAgeTicks)
    }

    @Test fun buyerCreditsSilverOnlyWhenOfferSettles() {
        val engine = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            silver = 100,
            marketListing = "Armadura T1",
            marketPrice = 25,
            marketListingAgeTicks = 4,
            worldTick = 4
        )
        val message = engine.tick()
        assertEquals(125, engine.silver)
        assertNull(engine.marketListing)
        assertNull(engine.marketPrice)
        assertEquals(0, engine.marketListingAgeTicks)
        assertTrue(message.contains("Comprador confirma"))
    }
}
