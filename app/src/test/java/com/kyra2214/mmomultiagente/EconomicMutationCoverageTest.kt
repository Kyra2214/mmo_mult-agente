package com.kyra2214.mmomultiagente

import org.junit.Assert.assertEquals
import org.junit.Test

class EconomicMutationCoverageTest {
    @Test fun teleportCostIsRecordedAsCurrencyBurned() {
        val events = mutableListOf<EconomyEvent>()
        val engine = SimulatorEngine(stage = 17, silver = 100, agentName = "Mira")
        engine.economyEventSink = events::add

        engine.step()

        assertEquals(70, engine.silver)
        assertEquals(1, events.size)
        assertEquals(EconomyEventType.CURRENCY_BURNED, events.single().type)
        assertEquals(30, events.single().quantity)
        assertEquals("teleport", events.single().cause)
    }

    @Test fun repairCostIsRecordedAsCurrencyBurned() {
        val events = mutableListOf<EconomyEvent>()
        val engine = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            silver = 100,
            equippedGear = mapOf(EquipmentSlot.WEAPON to Tier.T1),
            gearDamage = mapOf(EquipmentSlot.WEAPON to 1),
            agentName = "Borin"
        )
        engine.economyEventSink = events::add

        engine.tick()

        assertEquals(90, engine.silver)
        assertEquals(1, events.size)
        assertEquals(EconomyEventType.CURRENCY_BURNED, events.single().type)
        assertEquals(10, events.single().quantity)
        assertEquals("equipment-repair", events.single().cause)
    }
}
