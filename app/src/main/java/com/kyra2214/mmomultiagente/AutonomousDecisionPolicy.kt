package com.kyra2214.mmomultiagente

/** Chooses one useful action from the agent's observed inventory and current opportunities. */
enum class AutonomousAction { GATHER, HUNT, FISH, REFINE, CRAFT, TRADE }

object AutonomousDecisionPolicy {
    fun choose(inventory: Map<String, Int>, silver: Int, marketListing: String?, progression: SkillProgression? = null): AutonomousAction {
        fun has(item: String, count: Int = 1) = (inventory[item] ?: 0) >= count

        // Advance one shared equipment tier at a time; all seven specializations
        // must unlock the next tier before the economy moves the whole set forward.
        val targetTier = equipmentTargetTier(progression)
        val affordableEquipment = TierRecipes.fullSet(targetTier).any { recipe ->
            (progression == null || progression.canPerformTier(recipe.skill, targetTier)) &&
                recipe.materials.all { cost -> has(materialDisplayName(cost.itemId), cost.quantity) }
        }
        if (affordableEquipment) return AutonomousAction.CRAFT
        if (has("Prancha T1") && has("Fibra T1") && !has("Vara de Pesca T1")) {
            return AutonomousAction.CRAFT
        }

        // Process raw materials when an appropriate refiner can add value.
        val refiningInputs = listOf(
            "Minério" to SkillId.ORE_REFINING,
            "Pedra" to SkillId.STONE_REFINING,
            "Madeira" to SkillId.WOOD_REFINING,
            "Fibra" to SkillId.FIBER_REFINING,
            "Couro" to SkillId.LEATHER_REFINING,
            "Fragmento Arcano" to SkillId.ARCANE_REFINING
        )
        val refinableRawMaterial = Tier.entries.reversed().any { tier ->
            refiningInputs.any { (raw, skill) ->
                has("$raw ${tier.name}") && (progression == null || progression.canPerformTier(skill, tier))
            }
        }
        if (refinableRawMaterial) return AutonomousAction.REFINE

        // The fishing branch only becomes viable after a rod exists.
        if (has("Vara de Pesca T1") && !has("Peixe T1") && marketListing != "Peixe T1") {
            return AutonomousAction.FISH
        }

        // Selling is an available goal when there is a real unlisted item.
        if (has("Peixe T1") && marketListing == null) return AutonomousAction.TRADE

        // If the target recipe lacks leather, hunt rather than repeatedly gathering unrelated stock.
        val needsLeather = (inventory["Couro Curtido ${targetTier.name}"] ?: 0) < 2
        if (needsLeather && !has("Couro ${targetTier.name}")) return AutonomousAction.HUNT

        val lacksTargetTierResources =
            (!has("Barra ${targetTier.name}", 2) && !has("Minério ${targetTier.name}")) ||
            (!has("Prancha ${targetTier.name}") && !has("Madeira ${targetTier.name}")) ||
            (!has("Tecido ${targetTier.name}") && !has("Fibra ${targetTier.name}")) ||
            (!has("Bloco de Pedra ${targetTier.name}") && !has("Pedra ${targetTier.name}")) ||
            (!has("Componente Arcano ${targetTier.name}") && !has("Fragmento Arcano ${targetTier.name}"))
        if (lacksTargetTierResources) return AutonomousAction.GATHER

        if (silver < 30) return AutonomousAction.HUNT
        return AutonomousAction.TRADE
    }
}

private fun equipmentTargetTier(progression: SkillProgression?): Tier {
    if (progression == null) return Tier.T1
    val craftingSkills = EquipmentSlot.entries.map { TierRecipes.recipe(it, Tier.T1).skill }
    return Tier.fromRank(craftingSkills.minOf { skill ->
        SkillProgression.highestUnlockedTier(progression.snapshot(skill).level).rank
    })
}

private fun materialDisplayName(itemId: String): String {
    val parts = itemId.split("_T")
    val tier = parts.getOrNull(1) ?: "1"
    val name = when (parts.firstOrNull()) {
        "METAL_BAR" -> "Barra"
        "TANNED_LEATHER" -> "Couro Curtido"
        "CLOTH" -> "Tecido"
        "PLANK" -> "Prancha"
        "ARCANE_COMPONENT" -> "Componente Arcano"
        else -> itemId
    }
    return "$name T$tier"
}
