package com.kyra2214.mmomultiagente

import org.junit.Assert.*
import org.junit.Test

class T3DungeonSimulatorTest {
    private val fullT3Gear = EquipmentSlot.entries.associateWith { Tier.T3 }

    @Test fun dungeonRejectsAgentWithoutCompleteT3Set() {
        val gear = fullT3Gear - EquipmentSlot.MAGIC_ITEM
        val result = T3DungeonSimulator.runWithRolls(gear, emptyList(), d4Roll = 4)
        assertEquals(DungeonOutcome.NOT_READY, result.outcome)
        assertNull(result.chest)
        assertTrue(result.encounters.isEmpty())
    }

    @Test fun preparedAgentMustDefeatMiniBossAndBossBeforeReceivingD4Chest() {
        val result = T3DungeonSimulator.runWithRolls(fullT3Gear, List(100) { 10 }, d4Roll = 3)
        assertEquals(DungeonOutcome.VICTORY, result.outcome)
        assertEquals(2, result.encounters.size)
        assertTrue(result.encounters.all { it.defeated })
        assertEquals(LootRarity.PURPLE, result.chest?.rarity)
        assertTrue((result.playerHpRemaining) > 0)
    }

    @Test fun failedCombatDoesNotGrantDungeonChest() {
        val rolls = List(200) { index -> if (index % 2 == 0) 1 else 20 }
        val result = T3DungeonSimulator.runWithRolls(fullT3Gear, rolls, d4Roll = 4)
        assertEquals(DungeonOutcome.DEFEATED, result.outcome)
        assertNull(result.chest)
        assertEquals(0, result.playerHpRemaining)
    }
}
