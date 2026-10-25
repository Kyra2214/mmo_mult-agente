package com.kyra2214.mmomultiagente

/** Chooses one useful action from the agent's observed inventory and current opportunities. */
enum class AutonomousAction { GATHER, HUNT, FISH, REFINE, CRAFT, TRADE }

object AutonomousDecisionPolicy {
    fun choose(inventory: Map<String, Int>, silver: Int, marketListing: String?): AutonomousAction {
        fun has(item: String, count: Int = 1) = (inventory[item] ?: 0) >= count

        // Complete a viable recipe before gathering more stock.
        if (has("Barra T1", 2) && has("Couro Curtido T1") && has("Prancha T1")) {
            return AutonomousAction.CRAFT
        }
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

        // Avoid gathering if the inventory already has usable processed materials.
        val lacksMetal = !has("Barra T1", 2)
        val lacksWood = !has("Prancha T1")
        val lacksFiber = !has("Fibra T1") && !has("Tecido T1")
        if (lacksMetal || lacksWood || lacksFiber) return AutonomousAction.GATHER

        if (!has("Couro T1") && !has("Couro Curtido T1")) return AutonomousAction.HUNT
        if (has("Peixe T1") && marketListing == null) return AutonomousAction.TRADE
        if (silver < 30) return AutonomousAction.HUNT
        return AutonomousAction.TRADE
    }
}
