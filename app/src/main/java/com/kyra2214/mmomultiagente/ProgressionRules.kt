package com.kyra2214.mmomultiagente

/**
 * Deterministic progression rules for the first playable tiers.
 * Every skill is independent; only successful, paid-for actions advance it.
 */
enum class Tier(val rank: Int) {
    T1(1), T2(2), T3(3), T4(4);

    companion object {
        fun fromRank(rank: Int): Tier = entries.first { it.rank == rank }
    }
}

enum class SkillId {
    MINING, WOODCUTTING, FIBER_GATHERING, SKINNING, FISHING,
    ORE_REFINING, STONE_REFINING, WOOD_REFINING, FIBER_REFINING, LEATHER_REFINING,
    ARMOR_CRAFTING, HELMET_CRAFTING, GLOVE_CRAFTING, BOOT_CRAFTING,
    WEAPON_CRAFTING, OFF_HAND_CRAFTING, MAGIC_ITEM_CRAFTING
}

enum class EquipmentSlot {
    ARMOR, HELMET, GLOVES, BOOTS, WEAPON, OFF_HAND, MAGIC_ITEM
}

enum class LootRarity { GREEN, BLUE, PURPLE, GOLD }

data class SkillSnapshot(
    val level: Int,
    val actionsAtCurrentLevel: Int,
    val totalSuccessfulActions: Int
)

data class ProgressionResult(
    val beforeLevel: Int,
    val afterLevel: Int,
    val actionsAtCurrentLevel: Int,
    val unlockedTier: Tier
)

class SkillProgression(
    initial: Map<SkillId, SkillSnapshot> = emptyMap()
) {
    private val skills = SkillId.entries.associateWith { skill ->
        initial[skill] ?: SkillSnapshot(level = 1, actionsAtCurrentLevel = 0, totalSuccessfulActions = 0)
    }.toMutableMap()

    fun snapshot(skill: SkillId): SkillSnapshot = skills.getValue(skill)

    /**
     * A successful action only counts if its tier is sufficient for the current
     * transition. Requirements are per skill: T1 for L1→L2, T2 for L2→L3,
     * T3 for L3→L4, and T4 for L4→L5. No failed action grants progress.
     */
    fun recordSuccessfulAction(skill: SkillId, actionTier: Tier): ProgressionResult {
        val before = skills.getValue(skill)
        val neededTier = Tier.fromRank(before.level.coerceIn(1, 4))
        require(actionTier.rank >= neededTier.rank) {
            "Ação ${actionTier.name} insuficiente para evoluir ${skill.name} no nível ${before.level}"
        }
        val neededActions = actionsRequiredForNextLevel(before.level)
        val count = before.actionsAtCurrentLevel + 1
        val level = if (count >= neededActions && before.level < MAX_LEVEL) before.level + 1 else before.level
        val remainingCount = if (level > before.level) 0 else count
        val after = SkillSnapshot(level, remainingCount, before.totalSuccessfulActions + 1)
        skills[skill] = after
        return ProgressionResult(before.level, after.level, after.actionsAtCurrentLevel, highestUnlockedTier(after.level))
    }

    fun canPerformTier(skill: SkillId, tier: Tier): Boolean =
        tier.rank <= highestUnlockedTier(snapshot(skill).level).rank

    fun export(): Map<SkillId, SkillSnapshot> = skills.toMap()

    companion object {
        const val MAX_LEVEL = 5

        fun actionsRequiredForNextLevel(level: Int): Int = when (level) {
            1 -> 1
            2 -> 10
            3 -> 20
            4 -> 30
            else -> Int.MAX_VALUE
        }

        fun highestUnlockedTier(level: Int): Tier = when {
            level >= 4 -> Tier.T4
            level >= 3 -> Tier.T3
            level >= 2 -> Tier.T2
            else -> Tier.T1
        }
    }
}

data class MaterialCost(val itemId: String, val quantity: Int) {
    init { require(itemId.isNotBlank()); require(quantity > 0) }
}

data class EquipmentRecipe(
    val slot: EquipmentSlot,
    val tier: Tier,
    val skill: SkillId,
    val materials: List<MaterialCost>
)

object TierRecipes {
    private val slotSkills = mapOf(
        EquipmentSlot.ARMOR to SkillId.ARMOR_CRAFTING,
        EquipmentSlot.HELMET to SkillId.HELMET_CRAFTING,
        EquipmentSlot.GLOVES to SkillId.GLOVE_CRAFTING,
        EquipmentSlot.BOOTS to SkillId.BOOT_CRAFTING,
        EquipmentSlot.WEAPON to SkillId.WEAPON_CRAFTING,
        EquipmentSlot.OFF_HAND to SkillId.OFF_HAND_CRAFTING,
        EquipmentSlot.MAGIC_ITEM to SkillId.MAGIC_ITEM_CRAFTING
    )

    fun recipe(slot: EquipmentSlot, tier: Tier): EquipmentRecipe {
        val t = tier.rank
        val primary = when (slot) {
            EquipmentSlot.ARMOR, EquipmentSlot.HELMET, EquipmentSlot.GLOVES, EquipmentSlot.BOOTS ->
                listOf(MaterialCost("METAL_BAR_T$t", 2), MaterialCost("TANNED_LEATHER_T$t", 2), MaterialCost("CLOTH_T$t", 1))
            EquipmentSlot.WEAPON ->
                listOf(MaterialCost("METAL_BAR_T$t", 3), MaterialCost("PLANK_T$t", 2), MaterialCost("TANNED_LEATHER_T$t", 1))
            EquipmentSlot.OFF_HAND ->
                listOf(MaterialCost("PLANK_T$t", 2), MaterialCost("METAL_BAR_T$t", 1), MaterialCost("CLOTH_T$t", 1))
            EquipmentSlot.MAGIC_ITEM ->
                listOf(MaterialCost("ARCANE_COMPONENT_T$t", 2), MaterialCost("CLOTH_T$t", 2), MaterialCost("METAL_BAR_T$t", 1))
        }
        return EquipmentRecipe(slot, tier, slotSkills.getValue(slot), primary)
    }

    fun fullSet(tier: Tier): List<EquipmentRecipe> = EquipmentSlot.entries.map { recipe(it, tier) }
}

data class DungeonChest(val rarity: LootRarity, val lootItemIds: List<String>)

object T3DungeonLoot {
    private val tables = mapOf(
        1 to DungeonChest(LootRarity.GREEN, listOf("T3_COIN_POUCH", "T3_COMMON_MATERIALS")),
        2 to DungeonChest(LootRarity.BLUE, listOf("T3_REFINED_MATERIAL_BUNDLE", "T3_UNCOMMON_RELIC")),
        3 to DungeonChest(LootRarity.PURPLE, listOf("T3_RARE_RELIC", "T3_ENCHANTED_COMPONENT")),
        4 to DungeonChest(LootRarity.GOLD, listOf("T3_LEGENDARY_RELIC"))
    )

    /** Pass an injected d4 result in tests; production callers provide a random result from 1..4. */
    fun resolveD4(roll: Int): DungeonChest {
        require(roll in 1..4) { "D4 result must be between 1 and 4" }
        return tables.getValue(roll)
    }
}
