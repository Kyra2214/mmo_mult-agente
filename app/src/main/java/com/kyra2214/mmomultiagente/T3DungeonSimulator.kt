package com.kyra2214.mmomultiagente

import kotlin.random.Random

enum class DungeonOutcome { NOT_READY, DEFEATED, WITHDRAWN, VICTORY }

data class EncounterResult(
    val name: String,
    val enemyHpRemaining: Int,
    val playerHpRemaining: Int,
    val rounds: Int,
    val defeated: Boolean
)

data class T3DungeonResult(
    val outcome: DungeonOutcome,
    val playerHpRemaining: Int,
    val encounters: List<EncounterResult>,
    val chest: DungeonChest?
)

/**
 * First playable T3 dungeon model. The caller supplies combat rolls in tests;
 * production uses random d20-like rolls and a separate d4 chest roll.
 */
object T3DungeonSimulator {
    private val requiredSlots = EquipmentSlot.entries.toSet()

    fun runRandom(equippedGear: Map<EquipmentSlot, Tier>, initialHealth: Int = 100): T3DungeonResult {
        val rolls = List(200) { Random.nextInt(1, 21) }
        return runWithRolls(equippedGear, rolls, Random.nextInt(1, 5), initialHealth)
    }

    fun runWithRolls(
        equippedGear: Map<EquipmentSlot, Tier>,
        combatRolls: List<Int>,
        d4Roll: Int,
        initialHealth: Int = 100
    ): T3DungeonResult {
        require(initialHealth > 0)
        require(combatRolls.all { it in 1..20 })
        require(d4Roll in 1..4)
        val ready = requiredSlots.all { slot -> (equippedGear[slot]?.rank ?: 0) >= Tier.T3.rank }
        if (!ready) return T3DungeonResult(DungeonOutcome.NOT_READY, initialHealth, emptyList(), null)

        var health = initialHealth
        var rollIndex = 0
        fun roll(): Int = if (rollIndex < combatRolls.size) combatRolls[rollIndex++] else 10
        val gearPower = equippedGear.values.sumOf { it.rank }
        val playerDamage = 8 + gearPower / 3
        val encounters = mutableListOf<EncounterResult>()

        data class Enemy(val name: String, val hp: Int, val damage: Int)
        val enemies = listOf(
            Enemy("Mini-chefe T3", hp = 36, damage = 7),
            Enemy("Chefe T3", hp = 60, damage = 12)
        )
        for (enemy in enemies) {
            var enemyHp = enemy.hp
            var rounds = 0
            var consecutiveMisses = 0
            while (enemyHp > 0 && health > 0 && rounds < 20) {
                rounds++
                if (roll() >= 8) {
                    enemyHp -= playerDamage
                    consecutiveMisses = 0
                } else {
                    consecutiveMisses++
                }
                if (enemyHp > 0 && roll() >= 5) health -= enemy.damage

                // Retreat alive if attacks repeatedly fail or HP becomes dangerously low.
                if (enemyHp > 0 && health > 0 &&
                    (consecutiveMisses >= 4 || health <= enemy.damage * 2)) {
                    encounters += EncounterResult(enemy.name, enemyHp.coerceAtLeast(0), health.coerceAtLeast(0), rounds, false)
                    return T3DungeonResult(DungeonOutcome.WITHDRAWN, health.coerceAtLeast(1), encounters, null)
                }
            }
            val defeated = enemyHp <= 0 && health > 0
            encounters += EncounterResult(enemy.name, enemyHp.coerceAtLeast(0), health.coerceAtLeast(0), rounds, defeated)
            if (health <= 0 || enemyHp > 0) {
                return T3DungeonResult(DungeonOutcome.DEFEATED, 0, encounters, null)
            }
        }
        return T3DungeonResult(
            outcome = DungeonOutcome.VICTORY,
            playerHpRemaining = health,
            encounters = encounters,
            chest = T3DungeonLoot.resolveD4(d4Roll)
        )
    }
}
