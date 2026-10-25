package com.kyra2214.mmomultiagente

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedMarketCoordinatorTest {
    @Test fun purchaseTransfersItemAndCurrencyBetweenDistinctAgentsAtomically() {
        val seller = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            silver = 40,
            marketListing = "Peixe T1",
            marketPrice = 5,
            marketListingAgeTicks = SharedMarketCoordinator.MIN_LISTING_AGE_TICKS
        )
        val buyer = SimulatorEngine(stage = SimulatorEngine.TOTAL_STAGES, silver = 20)
        val participants = listOf(
            MarketParticipant("seller", "Aldren", seller),
            MarketParticipant("buyer", "Mira", buyer)
        )

        val events = SharedMarketCoordinator().tick(participants)

        assertEquals(1, events.size)
        assertEquals(45, seller.silver)
        assertEquals(15, buyer.silver)
        assertEquals(1, buyer.inventory["Peixe T1"])
        assertNull(seller.marketListing)
        assertNull(seller.marketPrice)
        assertEquals(0, seller.marketListingAgeTicks)
        assertTrue(seller.log.any { it.contains("Mira comprou") })
        assertTrue(buyer.log.any { it.contains("Aldren") })
    }

    @Test fun sameAgentCannotBuyOwnOfferAndInsufficientBuyerCannotSettle() {
        val seller = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            silver = 100,
            marketListing = "Peixe T1",
            marketPrice = 5,
            marketListingAgeTicks = 5
        )
        val poorBuyer = SimulatorEngine(stage = SimulatorEngine.TOTAL_STAGES, silver = 0)
        val coordinator = SharedMarketCoordinator()

        val events = coordinator.tick(listOf(
            MarketParticipant("seller", "Aldren", seller),
            MarketParticipant("buyer", "Mira", poorBuyer)
        ))

        assertTrue(events.isEmpty())
        assertEquals("Peixe T1", seller.marketListing)
        assertEquals(100, seller.silver)
        assertEquals(0, poorBuyer.silver)
        assertFalse(poorBuyer.inventory.containsKey("Peixe T1"))
    }

    @Test fun settledOfferCannotBeSoldTwice() {
        val seller = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            silver = 10,
            marketListing = "Peixe T1",
            marketPrice = 5,
            marketListingAgeTicks = 5
        )
        val buyer = SimulatorEngine(stage = SimulatorEngine.TOTAL_STAGES, silver = 20)
        val participants = listOf(
            MarketParticipant("seller", "Aldren", seller),
            MarketParticipant("buyer", "Mira", buyer)
        )
        val coordinator = SharedMarketCoordinator()

        assertEquals(1, coordinator.tick(participants).size)
        assertTrue(coordinator.tick(participants).isEmpty())
        assertEquals(15, seller.silver)
        assertEquals(15, buyer.silver)
        assertEquals(1, buyer.inventory["Peixe T1"])
    }

    @Test fun offersStayOpenBeforeMinimumAge() {
        val seller = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            silver = 10,
            marketListing = "Peixe T1",
            marketPrice = 5,
            marketListingAgeTicks = SharedMarketCoordinator.MIN_LISTING_AGE_TICKS - 1
        )
        val buyer = SimulatorEngine(stage = SimulatorEngine.TOTAL_STAGES, silver = 20)

        val events = SharedMarketCoordinator().tick(listOf(
            MarketParticipant("seller", "Aldren", seller),
            MarketParticipant("buyer", "Mira", buyer)
        ))

        assertTrue(events.isEmpty())
        assertEquals("Peixe T1", seller.marketListing)
        assertEquals(20, buyer.silver)
    }
}
