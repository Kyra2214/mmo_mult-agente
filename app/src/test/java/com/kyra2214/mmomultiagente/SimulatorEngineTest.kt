package com.kyra2214.mmomultiagente

import org.junit.Assert.*
import org.junit.Test

class SimulatorEngineTest {
    @Test fun firstElevenStepsTeachAllNpcFunctions() {
        val engine = SimulatorEngine()
        repeat(11) { engine.step() }
        assertEquals(11, engine.learnedNpcIds.size)
        assertEquals((1..11).toSet(), engine.learnedNpcIds)
        assertTrue(engine.log.any { it.contains("Tier 1, T2 e T3") || it.contains("T1, T2 e T3") })
    }

    @Test fun gatheringAndHuntingCreateResourcesAndRewardSilver() {
        val engine = SimulatorEngine()
        repeat(13) { engine.step() }
        assertEquals(3, engine.inventory["Minério T1"])
        assertEquals(3, engine.inventory["Madeira T1"])
        assertEquals(2, engine.inventory["Couro T1"])
        assertEquals(110, engine.silver)
    }

    @Test fun craftingConsumesMaterialsAndCreatesOneArmor() {
        val engine = SimulatorEngine()
        repeat(14) { engine.step() }
        assertEquals(1, engine.inventory["Armadura T1"])
        assertEquals(1, engine.inventory["Minério T1"])
        assertEquals(2, engine.inventory["Madeira T1"])
        assertEquals(1, engine.inventory["Couro T1"])
    }

    @Test fun marketListingDoesNotCountAsSaleOrAddSilver() {
        val engine = SimulatorEngine()
        repeat(16) { engine.step() }
        assertEquals("Armadura T1", engine.marketListing)
        assertEquals(110, engine.silver)
        assertFalse(engine.inventory.containsKey("Armadura T1"))
        assertTrue(engine.log.any { it.contains("Ainda não foi vendido") })
    }

    @Test fun tutorialCanFinishWithoutDuplicatingItems() {
        val engine = SimulatorEngine()
        repeat(SimulatorEngine.TOTAL_STAGES) { engine.step() }
        assertTrue(engine.isComplete)
        val savedInventory = engine.inventory.toMap()
        engine.step()
        assertEquals(savedInventory, engine.inventory)
    }

    @Test fun freshAgentHasNoMarketListing() {
        assertNull(SimulatorEngine().marketListing)
    }
}


class AutonomousTickTest {
    @Test fun tickAutomaticallyRunsTutorialAndThenContinuesWorld() {
        val engine = SimulatorEngine()
        repeat(SimulatorEngine.TOTAL_STAGES) { engine.tick() }
        assertTrue(engine.isComplete)
        assertEquals(0L, engine.worldTick)
        repeat(6) { engine.tick() }
        assertEquals(6L, engine.worldTick)
        assertTrue(engine.log.any { it.contains("pescar") })
        assertTrue(engine.log.any { it.contains("refina") || it.contains("refino") || it.contains("curtume") })
    }

    @Test fun autonomousCraftingConsumesMaterialsAndTracksCategorySeparately() {
        val engine = SimulatorEngine(inventory = mapOf("Barra T1" to 2, "Couro Curtido T1" to 1, "Prancha T1" to 1))
        repeat(SimulatorEngine.TOTAL_STAGES) { engine.step() }
        repeat(5) { engine.tick() }
        assertEquals(1, engine.inventory["Armadura T1"])
        assertEquals(1, engine.skillCraftCounts["armaduras"])
        assertFalse(engine.inventory.containsKey("Barra T1"))
    }
}
