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
        assertEquals(2, engine.progression.snapshot(SkillId.ARMOR_CRAFTING).level)
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
        repeat(80) { engine.tick() }
        assertEquals(80L, engine.worldTick)
        assertTrue(engine.log.any { it.contains("refina") || it.contains("refino") || it.contains("curtume") })
        assertTrue(engine.log.any { it.contains("Vara de Pesca") || it.contains("pesca") || it.contains("Pesca") })
        assertTrue(engine.progression.snapshot(SkillId.FISHING).totalSuccessfulActions > 0)
        assertTrue(engine.progression.snapshot(SkillId.ORE_REFINING).totalSuccessfulActions > 0)
    }

    @Test fun autonomousCraftingConsumesMaterialsAndTracksCategorySeparately() {
        val engine = SimulatorEngine(inventory = mapOf("Barra T1" to 2, "Couro Curtido T1" to 2, "Tecido T1" to 1))
        repeat(SimulatorEngine.TOTAL_STAGES) { engine.step() }
        engine.tick()
        assertEquals(Tier.T1, engine.equippedGear[EquipmentSlot.HELMET])
        assertEquals(1, engine.skillCraftCounts["armaduras"])
        assertEquals(1, engine.skillCraftCounts["capacetes"])
        assertFalse(engine.inventory.containsKey("Barra T1"))
        assertFalse(engine.inventory.containsKey("Couro Curtido T1"))
        assertFalse(engine.inventory.containsKey("Tecido T1"))
    }
}


class TierRecipeIntegrationTest {
    @Test fun craftingTierOneEquipmentConsumesRecipeAndEquipsIt() {
        val engine = SimulatorEngine(inventory = mapOf(
            "Barra T1" to 2,
            "Couro Curtido T1" to 2,
            "Tecido T1" to 1
        ))
        val result = engine.craftEquipment(EquipmentSlot.ARMOR, Tier.T1)
        assertTrue(result.contains("fabrica"))
        assertEquals(Tier.T1, engine.equippedGear[EquipmentSlot.ARMOR])
        assertFalse(engine.inventory.containsKey("Barra T1"))
        assertFalse(engine.inventory.containsKey("Couro Curtido T1"))
        assertFalse(engine.inventory.containsKey("Tecido T1"))
        assertEquals(2, engine.progression.snapshot(SkillId.ARMOR_CRAFTING).level)
    }

    @Test fun lockedTierRecipeDoesNotConsumeAnyMaterials() {
        val engine = SimulatorEngine(inventory = mapOf(
            "Barra T3" to 20,
            "Couro Curtido T3" to 20,
            "Tecido T3" to 20
        ))
        val before = engine.inventory.toMap()
        val result = engine.craftEquipment(EquipmentSlot.ARMOR, Tier.T3)
        assertTrue(result.contains("bloqueada"))
        assertEquals(before, engine.inventory)
        assertTrue(engine.equippedGear.isEmpty())
    }

    @Test fun insufficientRecipeMaterialsAreNotPartiallyConsumed() {
        val engine = SimulatorEngine(inventory = mapOf(
            "Barra T1" to 2,
            "Couro Curtido T1" to 1,
            "Tecido T1" to 1
        ))
        val before = engine.inventory.toMap()
        val result = engine.craftEquipment(EquipmentSlot.ARMOR, Tier.T1)
        assertTrue(result.contains("faltam"))
        assertEquals(before, engine.inventory)
    }
}


class TierAwareActivityTest {
    @Test fun gatheringDoesNotExceedTheSharedEquipmentTargetTier() {
        val engine = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            inventory = mapOf("Couro Curtido T1" to 2),
            progressionSnapshots = mapOf(
                SkillId.MINING to SkillSnapshot(level = 2, actionsAtCurrentLevel = 0, totalSuccessfulActions = 1)
            )
        )
        engine.tick()
        assertTrue((engine.inventory["Minério T1"] ?: 0) > 0)
        assertEquals(null, engine.inventory["Minério T2"])
        assertTrue((engine.inventory["Madeira T1"] ?: 0) > 0)
        assertEquals(2, engine.progression.snapshot(SkillId.MINING).level)
    }

    @Test fun refiningConsumesRawMaterialAndPreservesItsTier() {
        val engine = SimulatorEngine(
            stage = SimulatorEngine.TOTAL_STAGES,
            inventory = mapOf("Minério T2" to 1),
            progressionSnapshots = mapOf(
                SkillId.ORE_REFINING to SkillSnapshot(level = 2, actionsAtCurrentLevel = 0, totalSuccessfulActions = 1)
            )
        )
        engine.tick()
        assertEquals(1, engine.inventory["Barra T2"])
        assertFalse(engine.inventory.containsKey("Minério T2"))
    }
}


class LongRunSimulationTest {
    @Test fun unattendedAgentReachesTheT3DungeonGateAndKeepsRunning() {
        val engine = SimulatorEngine(stage = SimulatorEngine.TOTAL_STAGES)
        repeat(20_000) { engine.tick() }
        assertEquals(20_000L, engine.worldTick)
        assertTrue(engine.progression.export().values.any { it.totalSuccessfulActions > 0 })
        assertTrue("No equipment after long run. Gear=${engine.equippedGear}; inventory=${engine.inventory}; skills=${engine.progression.export()}",
            engine.equippedGear.isNotEmpty())
        assertTrue(engine.inventory.values.all { it > 0 })
        assertTrue(engine.log.size <= 120)
        if (engine.dungeonAttempts == 0) {
            println("LONG_RUN_DIAGNOSTIC gear=${engine.equippedGear}; skills=${engine.progression.export()}; inventory=${engine.inventory}; lastLogs=${engine.log.takeLast(20)}")
        }
        assertTrue("armor skill level=${engine.progression.snapshot(SkillId.ARMOR_CRAFTING)}", engine.progression.snapshot(SkillId.ARMOR_CRAFTING).level >= 3)
        assertTrue("helmet skill level=${engine.progression.snapshot(SkillId.HELMET_CRAFTING)}", engine.progression.snapshot(SkillId.HELMET_CRAFTING).level >= 3)
        assertTrue("glove skill level=${engine.progression.snapshot(SkillId.GLOVE_CRAFTING)}", engine.progression.snapshot(SkillId.GLOVE_CRAFTING).level >= 3)
        assertTrue("boot skill level=${engine.progression.snapshot(SkillId.BOOT_CRAFTING)}", engine.progression.snapshot(SkillId.BOOT_CRAFTING).level >= 3)
        assertTrue("weapon skill level=${engine.progression.snapshot(SkillId.WEAPON_CRAFTING)}", engine.progression.snapshot(SkillId.WEAPON_CRAFTING).level >= 3)
        assertTrue("off-hand skill level=${engine.progression.snapshot(SkillId.OFF_HAND_CRAFTING)}", engine.progression.snapshot(SkillId.OFF_HAND_CRAFTING).level >= 3)
        assertTrue("magic skill level=${engine.progression.snapshot(SkillId.MAGIC_ITEM_CRAFTING)}", engine.progression.snapshot(SkillId.MAGIC_ITEM_CRAFTING).level >= 3)
        assertTrue("Agent never reached T3 dungeon gate. Gear=${engine.equippedGear}; skills=${engine.progression.export()}; inventory=${engine.inventory}",
            engine.dungeonAttempts > 0)
        assertTrue("Agent did not reach full T4 equipment. Gear=${engine.equippedGear}; skills=${engine.progression.export()}; inventory=${engine.inventory}",
            EquipmentSlot.entries.all { (engine.equippedGear[it]?.rank ?: 0) >= Tier.T4.rank })
        assertTrue(engine.progression.snapshot(SkillId.ARMOR_CRAFTING).level >= 4)
        assertTrue(engine.progression.snapshot(SkillId.HELMET_CRAFTING).level >= 4)
        assertTrue(engine.progression.snapshot(SkillId.GLOVE_CRAFTING).level >= 4)
        assertTrue(engine.progression.snapshot(SkillId.BOOT_CRAFTING).level >= 4)
        assertTrue(engine.progression.snapshot(SkillId.WEAPON_CRAFTING).level >= 4)
        assertTrue(engine.progression.snapshot(SkillId.OFF_HAND_CRAFTING).level >= 4)
        assertTrue(engine.progression.snapshot(SkillId.MAGIC_ITEM_CRAFTING).level >= 4)
    }
}
