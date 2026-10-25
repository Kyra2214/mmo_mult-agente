package com.kyra2214.mmomultiagente

import org.junit.Assert.*
import org.junit.Test

class AutonomousDungeonIntegrationTest {
    @Test fun dungeonIsNotEnteredBeforeCompleteT3Equipment() {
        val engine = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            equippedGear = mapOf(EquipmentSlot.ARMOR to Tier.T3)
        )
        engine.tick()
        assertEquals(0, engine.dungeonAttempts)
        assertFalse(engine.dungeonCompleted)
    }

    @Test fun completeT3GearTriggersAutonomousDungeonAttempt() {
        val engine = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            equippedGear = EquipmentSlot.entries.associateWith { Tier.T3 }
        )
        val message = engine.tick()
        assertEquals(1, engine.dungeonAttempts)
        assertTrue(message.contains("masmorra") || message.contains("baú"))
        assertTrue(engine.log.last().contains("Tick 1"))
        if (engine.dungeonCompleted) {
            assertTrue(engine.inventory.keys.any { it.startsWith("T3_") })
        }
    }
}
