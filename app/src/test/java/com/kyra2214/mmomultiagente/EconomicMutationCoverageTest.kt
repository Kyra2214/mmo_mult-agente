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

    @Test fun finiteWorldHarvestRecordsBothStockConsumptionAndInventoryCreation() {
        val events = mutableListOf<EconomyEvent>()
        val store = WorldResourceStore(mapOf("Minério T1" to 2))
        val engine = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            inventory = mapOf("Couro Curtido T1" to 2),
            progressionSnapshots = mapOf(
                SkillId.MINING to SkillSnapshot(level = 1, actionsAtCurrentLevel = 0, totalSuccessfulActions = 0)
            )
        )
        engine.worldResourceStore = store
        engine.economyEventSink = events::add

        engine.tick()

        assertEquals(0, store.available("Minério T1"))
        assertEquals(2, engine.inventory["Minério T1"])
        assertTrue(events.any {
            it.type == EconomyEventType.RESOURCE_CONSUMED &&
                it.itemId == "Minério T1" && it.quantity == 2 && it.cause == "world-stock-harvest"
        })
        assertTrue(events.any {
            it.type == EconomyEventType.RESOURCE_CREATED &&
                it.itemId == "Minério T1" && it.quantity == 2 && it.cause == "gathering"
        })
    }

    @Test fun legacyMarketSettlementRecordsCurrencyAndEscrowTransfer() {
        val events = mutableListOf<EconomyEvent>()
        val engine = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            silver = 10,
            marketListing = "Peixe T1",
            marketPrice = 5,
            marketListingAgeTicks = 4,
            legacyMarketBuyerEnabled = true
        )
        engine.economyEventSink = events::add

        engine.tick()

        assertEquals(15, engine.silver)
        assertTrue(events.any {
            it.type == EconomyEventType.CURRENCY_MINTED &&
                it.itemId == "alzes" && it.quantity == 5 && it.cause == "legacy-market-sale"
        })
        assertTrue(events.any {
            it.type == EconomyEventType.ITEM_TRANSFERRED &&
                it.itemId == "Peixe T1" && it.cause == "market-escrow-to-system-buyer"
        })
    }

    @Test fun thirdDungeonDefeatRecordsDestroyedEquipment() {
        val events = mutableListOf<EconomyEvent>()
        val engine = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            equippedGear = mapOf(EquipmentSlot.WEAPON to Tier.T1),
            agentName = "Borin"
        )
        engine.economyEventSink = events::add

        repeat(3) { engine.recordDungeonDefeatWear() }

        assertTrue(events.any {
            it.type == EconomyEventType.RESOURCE_CONSUMED &&
                it.itemId == "Arma T1" && it.quantity == 1 && it.cause == "dungeon-wear-destroyed"
        })
        assertTrue(engine.equippedGear.isEmpty())
    }
}
