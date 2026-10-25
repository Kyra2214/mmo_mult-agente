package com.kyra2214.mmomultiagente

import org.junit.Assert.assertEquals
import org.junit.Test

class AutonomousDecisionPolicyTest {
    @Test fun prioritizesCraftingWhenARecipeIsAffordable() {
        assertEquals(
            AutonomousAction.CRAFT,
            AutonomousDecisionPolicy.choose(
                mapOf("Barra T1" to 2, "Couro Curtido T1" to 1, "Prancha T1" to 1),
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
