package com.kyra2214.mmomultiagente

import org.junit.Assert.assertEquals
import org.junit.Test

class AutonomousDecisionPolicyTest {
    @Test fun prioritizesCraftingWhenARecipeIsAffordable() {
        assertEquals(
            AutonomousAction.CRAFT,
            AutonomousDecisionPolicy.choose(
                mapOf("Barra T1" to 2, "Couro Curtido T1" to 2, "Tecido T1" to 1),
                100,
                null
            )
        )
    }

    @Test fun fishingIsUnavailableUntilTheAgentHasArod() {
        assertEquals(
            AutonomousAction.GATHER,
            AutonomousDecisionPolicy.choose(emptyMap(), 100, null)
        )
        assertEquals(
            AutonomousAction.FISH,
            AutonomousDecisionPolicy.choose(mapOf("Vara de Pesca T1" to 1), 100, null)
        )
    }

    @Test fun listingFishIsASeparateActionAndDoesNotImplySale() {
        assertEquals(
            AutonomousAction.TRADE,
            AutonomousDecisionPolicy.choose(mapOf("Peixe T1" to 2), 100, null)
        )
    }
}


class CoordinatedTierPlanningTest {
    @Test fun agentDoesNotSkipToT2BeforeEveryEquipmentSkillUnlocksIt() {
        val progression = SkillProgression()
        val action = AutonomousDecisionPolicy.choose(
            inventory = mapOf("Barra T2" to 10, "Couro Curtido T2" to 10, "Tecido T2" to 10),
            silver = 100,
            marketListing = null,
            progression = progression
        )
        assertEquals(AutonomousAction.HUNT, action)
    }

    @Test fun sharedTargetTierAdvancesWhenAllSevenCraftingSkillsUnlockT2() {
        val snapshots = EquipmentSlot.entries.associate { slot ->
            TierRecipes.recipe(slot, Tier.T1).skill to SkillSnapshot(2, 0, 1)
        }
        val progression = SkillProgression(snapshots)
        val action = AutonomousDecisionPolicy.choose(
            inventory = mapOf("Barra T2" to 2, "Couro Curtido T2" to 2, "Tecido T2" to 1),
            silver = 100,
            marketListing = null,
            progression = progression
        )
        assertEquals(AutonomousAction.CRAFT, action)
    }
}
