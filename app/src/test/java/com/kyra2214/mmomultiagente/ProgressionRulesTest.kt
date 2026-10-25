package com.kyra2214.mmomultiagente

import org.junit.Assert.*
import org.junit.Test

class ProgressionRulesTest {
    @Test fun eachCraftingCategoryProgressesIndependently() {
        val progression = SkillProgression()
        progression.recordSuccessfulAction(SkillId.HELMET_CRAFTING, Tier.T1)
        assertEquals(2, progression.snapshot(SkillId.HELMET_CRAFTING).level)
        assertEquals(1, progression.snapshot(SkillId.ARMOR_CRAFTING).level)
        assertEquals(1, progression.snapshot(SkillId.WEAPON_CRAFTING).level)
    }

    @Test fun higherLevelsRequireTheAppropriateTierAndRealSuccessfulActions() {
        val progression = SkillProgression()
        progression.recordSuccessfulAction(SkillId.HELMET_CRAFTING, Tier.T1)
        assertFalse(progression.canPerformTier(SkillId.HELMET_CRAFTING, Tier.T3))
        repeat(9) { progression.recordSuccessfulAction(SkillId.HELMET_CRAFTING, Tier.T2) }
        assertEquals(2, progression.snapshot(SkillId.HELMET_CRAFTING).level)
        progression.recordSuccessfulAction(SkillId.HELMET_CRAFTING, Tier.T2)
        assertEquals(3, progression.snapshot(SkillId.HELMET_CRAFTING).level)
        assertTrue(progression.canPerformTier(SkillId.HELMET_CRAFTING, Tier.T3))
        assertEquals(0, progression.snapshot(SkillId.HELMET_CRAFTING).actionsAtCurrentLevel)
    }

    @Test fun cheapTierOneCraftsCannotAdvanceLevelTwoToThree() {
        val progression = SkillProgression()
        progression.recordSuccessfulAction(SkillId.HELMET_CRAFTING, Tier.T1)
        repeat(5) { progression.recordSuccessfulAction(SkillId.HELMET_CRAFTING, Tier.T1) }
        assertEquals(2, progression.snapshot(SkillId.HELMET_CRAFTING).level)
        assertEquals(0, progression.snapshot(SkillId.HELMET_CRAFTING).actionsAtCurrentLevel)
        assertEquals(6, progression.snapshot(SkillId.HELMET_CRAFTING).totalSuccessfulActions)
    }

    @Test fun everyEquipmentSlotHasARecipeForTiersOneThroughFour() {
        Tier.entries.forEach { tier ->
            val recipes = TierRecipes.fullSet(tier)
            assertEquals(7, recipes.size)
            assertEquals(EquipmentSlot.entries.toSet(), recipes.map { it.slot }.toSet())
            assertTrue(recipes.all { it.tier == tier && it.materials.isNotEmpty() })
        }
    }

    @Test fun dungeonD4MapsToFourExplicitRarityTables() {
        assertEquals(LootRarity.GREEN, T3DungeonLoot.resolveD4(1).rarity)
        assertEquals(LootRarity.BLUE, T3DungeonLoot.resolveD4(2).rarity)
        assertEquals(LootRarity.PURPLE, T3DungeonLoot.resolveD4(3).rarity)
        assertEquals(LootRarity.GOLD, T3DungeonLoot.resolveD4(4).rarity)
        assertTrue((1..4).all { T3DungeonLoot.resolveD4(it).lootItemIds.isNotEmpty() })
    }
}
