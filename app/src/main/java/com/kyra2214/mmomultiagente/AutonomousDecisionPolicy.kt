package com.kyra2214.mmomultiagente

/** Chooses one useful action from the agent's observed inventory and current opportunities. */
enum class AutonomousAction { GATHER, HUNT, FISH, REFINE, CRAFT, TRADE }

object AutonomousDecisionPolicy {
    fun choose(inventory: Map<String, Int>, silver: Int, marketListing: String?, progression: SkillProgression? = null): AutonomousAction {
        fun has(item: String, count: Int = 1) = (inventory[item] ?: 0) >= count

        // Complete any unlocked T1 equipment recipe before gathering more stock.
        val affordableEquipment = Tier.entries.reversed().any { tier ->
            TierRecipes.fullSet(tier).any { recipe ->
                (progression == null || progression.canPerformTier(recipe.skill, tier)) &&
                    recipe.materials.all { cost -> has(materialDisplayName(cost.itemId), cost.quantity) }
            }
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

        // Avoid gathering if the inventory already has usable processed materials.
        val tieredCraftingNeed = Tier.entries.reversed().any { tier ->
            (inventory["Barra ${tier.name}"] ?: 0) < 2 ||
                (inventory["Couro Curtido ${tier.name}"] ?: 0) < 2 ||
                (inventory["Tecido ${tier.name}"] ?: 0) < 1
        }
        if (tieredCraftingNeed && Tier.entries.any { tier -> has("Couro Curtido ${tier.name}", 2) || has("Couro ${tier.name}") }) {
            val hideTier = Tier.entries.reversed().firstOrNull { tier ->
                (inventory["Couro Curtido ${tier.name}"] ?: 0) < 2 && !has("Couro ${tier.name}")
            }
            if (hideTier != null) return AutonomousAction.HUNT
        }

        val lacksTieredResource = Tier.entries.reversed().any { tier ->
            val canGatherTier = progression == null || listOf(
                SkillId.MINING, SkillId.WOODCUTTING, SkillId.FIBER_GATHERING
            ).any { progression.canPerformTier(it, tier) }
            canGatherTier && (
                (!has("Barra ${tier.name}", 2) && !has("Minério ${tier.name}")) ||
                (!has("Prancha ${tier.name}") && !has("Madeira ${tier.name}")) ||
                (!has("Tecido ${tier.name}") && !has("Fibra ${tier.name}")) ||
                (!has("Bloco de Pedra ${tier.name}") && !has("Pedra ${tier.name}")) ||
                (!has("Componente Arcano ${tier.name}") && !has("Fragmento Arcano ${tier.name}"))
            )
        }
        if (lacksTieredResource) return AutonomousAction.GATHER
        if (silver < 30) return AutonomousAction.HUNT
        return AutonomousAction.TRADE
    }
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
