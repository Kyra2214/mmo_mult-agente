package com.kyra2214.mmomultiagente

/** Chooses one useful action from the agent's observed inventory and current opportunities. */
enum class AutonomousAction { GATHER, HUNT, FISH, REFINE, CRAFT, TRADE }

object AutonomousDecisionPolicy {
    fun choose(inventory: Map<String, Int>, silver: Int, marketListing: String?): AutonomousAction {
        fun has(item: String, count: Int = 1) = (inventory[item] ?: 0) >= count

        // Complete any unlocked T1 equipment recipe before gathering more stock.
        val affordableEquipment = TierRecipes.fullSet(Tier.T1).any { recipe ->
            recipe.materials.all { cost ->
                val item = materialDisplayName(cost.itemId)
                has(item, cost.quantity)
            }
        }
        if (affordableEquipment) return AutonomousAction.CRAFT
        if (has("Prancha T1") && has("Fibra T1") && !has("Vara de Pesca T1")) {
            return AutonomousAction.CRAFT
        }

        // Process raw materials when an appropriate refiner can add value.
        if (has("Minério T1") || has("Madeira T1") || has("Fibra T1") || has("Couro T1")) {
            return AutonomousAction.REFINE
        }

        // The fishing branch only becomes viable after a rod exists.
        if (has("Vara de Pesca T1") && !has("Peixe T1") && marketListing != "Peixe T1") {
            return AutonomousAction.FISH
        }

        // Selling is an available goal when there is a real unlisted item.
        if (has("Peixe T1") && marketListing == null) return AutonomousAction.TRADE

        // Avoid gathering if the inventory already has usable processed materials.
        val lacksMetal = !has("Barra T1", 2)
        val lacksWood = !has("Prancha T1")
        val lacksFiber = !has("Fibra T1") && !has("Tecido T1")
        if (lacksMetal || lacksWood || lacksFiber) return AutonomousAction.GATHER

        if ((inventory["Couro Curtido T1"] ?: 0) < 2 && !has("Couro T1")) return AutonomousAction.HUNT
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
