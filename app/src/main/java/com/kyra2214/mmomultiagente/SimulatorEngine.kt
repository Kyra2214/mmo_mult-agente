package com.kyra2214.mmomultiagente

/** Deterministic simulation core without Android UI dependencies. */
class SimulatorEngine(
    var stage: Int = 0,
    var silver: Int = 100,
    inventory: Map<String, Int> = emptyMap(),
    chest: Map<String, Int> = emptyMap(),
    learnedNpcIds: Set<Int> = emptySet(),
    var marketListing: String? = null,
    var worldTick: Long = 0L,
    var autonomousActionIndex: Int = 0,
    skillCraftCounts: Map<String, Int> = emptyMap(),
    progressionSnapshots: Map<SkillId, SkillSnapshot> = emptyMap(),
    equippedGear: Map<EquipmentSlot, Tier> = emptyMap(),
    logEntries: List<String> = listOf(
        "Dia 1 começou. Aldren recebeu a quest: Aprendendo a sobreviver.",
        "Objetivo: conhecer a vila e aprender as mecânicas básicas."
    )
) {
    val inventory = inventory.toMutableMap()
    val chest = chest.toMutableMap()
    val learnedNpcIds = learnedNpcIds.toMutableSet()
    val skillCraftCounts = skillCraftCounts.toMutableMap()
    var progression = SkillProgression(progressionSnapshots)
    val equippedGear = equippedGear.toMutableMap()
    val log = logEntries.toMutableList()

    data class Npc(val id: Int, val name: String, val explanation: String)

    val npcs = listOf(
        Npc(1, "Criador de armaduras", "Fabrica armaduras. Apresenta receitas T1, T2 e T3, materiais e requisitos."),
        Npc(2, "Criador de capacetes", "Fabrica capacetes T1, T2 e T3; cada receita exige materiais e progressão adequados."),
        Npc(3, "Criador de luvas", "Fabrica luvas T1, T2 e T3. Conhecer uma receita não libera a fabricação automaticamente."),
        Npc(4, "Criador de botas", "Fabrica botas T1, T2 e T3. Ferramentas, habilidade e materiais precisam atender à receita."),
        Npc(5, "Criador de itens de mão secundária", "Fabrica escudos, livros, orbes e outros itens secundários T1, T2 e T3."),
        Npc(6, "Criador de itens mágicos", "Fabrica artefatos e itens mágicos T1, T2 e T3 usando componentes específicos."),
        Npc(7, "Criador de armas", "Fabrica armas T1, T2 e T3. Materiais e requisitos variam conforme a receita."),
        Npc(8, "Mercado", "Permite consultar ofertas e publicar itens. Uma oferta pode esperar por comprador; publicar não significa vender."),
        Npc(9, "Teleportador", "Transporta entre destinos por alzes. Compare preço, tempo, distância, risco e carga."),
        Npc(10, "Baú e armazenamento", "Guarda itens fora do inventário carregado. Itens armazenados precisam ser retirados para fabricar."),
        Npc(11, "Masmorra", "Apresenta desafios, requisitos, riscos e recompensas. Entre apenas quando estiver preparado.")
    )

    val isComplete: Boolean get() = stage >= TOTAL_STAGES

    val currentObjective: String
        get() = when {
            stage < npcs.size -> "Quest 1/8 — Conhecer a vila (" + (stage + 1) + "/11 NPCs)"
            stage == 11 -> "Quest 2/8 — Aprender a coletar"
            stage == 12 -> "Quest 3/8 — Aprender a caçar"
            stage == 13 -> "Quest 4/8 — Fabricar o primeiro item"
            stage == 14 -> "Quest 5/8 — Conhecer o baú"
            stage == 15 -> "Quest 6/8 — Publicar uma oferta"
            stage == 16 -> "Quest 7/8 — Avaliar o teleporte"
            stage == 17 -> "Quest 8/8 — Conhecer a masmorra"
            else -> "Tutorial concluído — vida autônoma desbloqueada"
        }

    /** One unattended world tick. The tutorial runs automatically; afterward a policy chooses by needs. */
    fun tick(): String {
        if (stage < TOTAL_STAGES) return step()
        worldTick += 1
        val message = when (AutonomousDecisionPolicy.choose(inventory.toMap(), silver, marketListing, progression)) {
            AutonomousAction.GATHER -> gatherNeededMaterials()
            AutonomousAction.HUNT -> huntForHide()
            AutonomousAction.FISH -> fishIfUseful()
            AutonomousAction.REFINE -> refineOneMaterial()
            AutonomousAction.CRAFT -> craftOneItem()
            AutonomousAction.TRADE -> listOrReviewMarket()
        }
        autonomousActionIndex += 1
        addLog("Tick $worldTick — $message")
        return message
    }

    private fun gatherNeededMaterials(): String {
        data class GatherNeed(val raw: String, val processed: String, val skill: SkillId, val label: String, val batchSize: Int = 1)
        val needs = listOf(
            GatherNeed("Minério", "Barra", SkillId.MINING, "minério", 2),
            GatherNeed("Pedra", "Bloco de Pedra", SkillId.STONE_GATHERING, "pedra"),
            GatherNeed("Madeira", "Prancha", SkillId.WOODCUTTING, "madeira"),
            GatherNeed("Fibra", "Tecido", SkillId.FIBER_GATHERING, "fibra"),
            GatherNeed("Fragmento Arcano", "Componente Arcano", SkillId.ARCANE_GATHERING, "fragmentos arcanos")
        )
        val gathered = mutableListOf<String>()
        needs.forEach { need ->
            val tier = SkillProgression.highestUnlockedTier(progression.snapshot(need.skill).level)
            val rawName = "${need.raw} ${tier.name}"
            val processedName = "${need.processed} ${tier.name}"
            val hasRaw = (inventory[rawName] ?: 0) > 0
            val hasProcessed = (inventory[processedName] ?: 0) >= if (need.raw == "Minério") 2 else 1
            if (!hasRaw && !hasProcessed) {
                inventory.addItem(rawName, need.batchSize)
                progression.recordSuccessfulAction(need.skill, tier)
                gathered.add("${need.batchSize} $rawName")
            }
        }
        return if (gathered.isNotEmpty()) {
            "Aldren consulta os estoques e coleta recursos compatíveis com suas especializações: ${gathered.joinToString(", ")}."
        } else {
            "Aldren verifica as fontes disponíveis; não coleta porque já tem matéria-prima ou material processado para as necessidades imediatas."
        }
    }

    private fun huntForHide(): String {
        val tier = SkillProgression.highestUnlockedTier(progression.snapshot(SkillId.SKINNING).level)
        inventory.addItem("Couro ${tier.name}", 1)
        progression.recordSuccessfulAction(SkillId.SKINNING, tier)
        silver += 5
        return "Aldren caça uma criatura ${tier.name} compatível com seu preparo; obtém 1 Couro ${tier.name} e 5 alzes."
    }

    private fun fishIfUseful(): String {
        // Fishing requires a rod. Without one, the agent must gather/refine/craft first.
        if ((inventory["Vara de Pesca T1"] ?: 0) <= 0) {
            return "Aldren avalia a pesca, mas ainda não tem vara. Adia a atividade e prioriza conseguir os materiais e fabricar a ferramenta."
        }
        inventory.addItem("Peixe T1", 2)
        progression.recordSuccessfulAction(SkillId.FISHING, Tier.T1)
        return "Aldren usa sua vara em um ponto de pesca e obtém 2 Peixes T1."
    }

    private fun listOrReviewMarket(): String {
        if (marketListing == null && inventory.removeItem("Peixe T1", 1)) {
            marketListing = "Peixe T1"
            return "Aldren publica uma oferta de 1 Peixe T1. O peixe fica reservado; nenhum alze é recebido antes da venda."
        }
        return "Aldren revisa ofertas e saldo. Sem comprador confirmado, mantém o item reservado e não contabiliza uma venda fictícia."
    }

    private fun refineOneMaterial(): String {
        data class RefiningRecipe(val raw: String, val output: String, val label: String, val skill: SkillId, val tier: Tier)
        val recipes = Tier.entries.reversed().flatMap { tier ->
            listOf(
                RefiningRecipe("Minério ${tier.name}", "Barra ${tier.name}", "refino de minério", SkillId.ORE_REFINING, tier),
                RefiningRecipe("Madeira ${tier.name}", "Prancha ${tier.name}", "refino de madeira", SkillId.WOOD_REFINING, tier),
                RefiningRecipe("Couro ${tier.name}", "Couro Curtido ${tier.name}", "curtume", SkillId.LEATHER_REFINING, tier),
                RefiningRecipe("Fibra ${tier.name}", "Tecido ${tier.name}", "refino de fibras", SkillId.FIBER_REFINING, tier),
                RefiningRecipe("Pedra ${tier.name}", "Bloco de Pedra ${tier.name}", "refino de pedra", SkillId.STONE_REFINING, tier),
                RefiningRecipe("Fragmento Arcano ${tier.name}", "Componente Arcano ${tier.name}", "refino arcano", SkillId.ARCANE_REFINING, tier)
            )
        }
        val recipe = recipes.firstOrNull { r ->
            progression.canPerformTier(r.skill, r.tier) && (inventory[r.raw] ?: 0) > 0
        } ?: return "Aldren consulta os refinadores, mas não encontra matéria-prima refinável de um tier liberado."
        inventory.removeItem(recipe.raw, 1)
        inventory.addItem(recipe.output, 1)
        progression.recordSuccessfulAction(recipe.skill, recipe.tier)
        return "Aldren realiza ${recipe.label} ${recipe.tier.name}: consome 1 ${recipe.raw} e produz 1 ${recipe.output}."
    }

    /**
     * Craft one item from the tier recipe, requiring the matching specialization
     * unlock and every listed material. No inventory changes happen on failure.
     */
    fun craftEquipment(slot: EquipmentSlot, tier: Tier): String {
        val recipe = TierRecipes.recipe(slot, tier)
        if (!progression.canPerformTier(recipe.skill, tier)) {
            return "Fabricação bloqueada: a especialização ${recipe.skill} ainda não liberou ${tier.name} para ${slot.name}."
        }
        val localizedCosts = recipe.materials.map { cost -> cost to materialDisplayName(cost.itemId) }
        val missing = localizedCosts.filter { (cost, item) -> (inventory[item] ?: 0) < cost.quantity }
        if (missing.isNotEmpty()) {
            return "Fabricação bloqueada: faltam " + missing.joinToString { (cost, item) ->
                "${(cost.quantity - (inventory[item] ?: 0))}× $item"
            } + ". Nenhum material foi consumido."
        }

        localizedCosts.forEach { (cost, item) -> inventory.removeItem(item, cost.quantity) }
        val itemName = equipmentName(slot, tier)
        inventory.addItem(itemName, 1)
        val countKey = when (slot) {
            EquipmentSlot.ARMOR -> "armaduras"
            EquipmentSlot.HELMET -> "capacetes"
            EquipmentSlot.GLOVES -> "luvas"
            EquipmentSlot.BOOTS -> "botas"
            EquipmentSlot.WEAPON -> "armas"
            EquipmentSlot.OFF_HAND -> "mao_secundaria"
            EquipmentSlot.MAGIC_ITEM -> "itens_magicos"
        }
        skillCraftCounts[countKey] = (skillCraftCounts[countKey] ?: 0) + 1
        val result = progression.recordSuccessfulAction(recipe.skill, tier)
        val oldTier = equippedGear[slot]
        if (oldTier == null || tier.rank > oldTier.rank) {
            if (oldTier != null) inventory.addItem(equipmentName(slot, oldTier), 1)
            inventory.removeItem(itemName, 1)
            equippedGear[slot] = tier
        }
        return "Aldren fabrica $itemName com a receita ${tier.name}; materiais consumidos e especialização ${recipe.skill} agora está no nível ${result.afterLevel}."
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

    private fun equipmentName(slot: EquipmentSlot, tier: Tier): String {
        val name = when (slot) {
            EquipmentSlot.ARMOR -> "Armadura"
            EquipmentSlot.HELMET -> "Capacete"
            EquipmentSlot.GLOVES -> "Luvas"
            EquipmentSlot.BOOTS -> "Botas"
            EquipmentSlot.WEAPON -> "Arma"
            EquipmentSlot.OFF_HAND -> "Mão Secundária"
            EquipmentSlot.MAGIC_ITEM -> "Item Mágico"
        }
        return "$name ${tier.name}"
    }

    private fun craftOneItem(): String {
        // Prefer an equipment recipe that is unlocked and fully affordable.
        for (slot in EquipmentSlot.entries) {
            val skill = TierRecipes.recipe(slot, Tier.T1).skill
            val affordableTier = Tier.entries.reversed().firstOrNull { tier ->
                progression.canPerformTier(skill, tier) &&
                    TierRecipes.recipe(slot, tier).materials.all { cost ->
                        (inventory[materialDisplayName(cost.itemId)] ?: 0) >= cost.quantity
                    }
            }
            if (affordableTier != null) {
                val result = craftEquipment(slot, affordableTier)
                if (result.startsWith("Aldren fabrica")) return result
            }
        }

        // Tool recipe is separate from combat equipment and requires its own materials.
        if ((inventory["Prancha T1"] ?: 0) >= 1 && (inventory["Fibra T1"] ?: 0) >= 1 &&
            (inventory["Vara de Pesca T1"] ?: 0) <= 0) {
            inventory.removeItem("Prancha T1", 1)
            inventory.removeItem("Fibra T1", 1)
            inventory.addItem("Vara de Pesca T1", 1)
            skillCraftCounts["ferramentas"] = (skillCraftCounts["ferramentas"] ?: 0) + 1
            progression.recordSuccessfulAction(SkillId.TOOL_CRAFTING, Tier.T1)
            return "Aldren fabrica uma Vara de Pesca T1 consumindo 1 Prancha T1 e 1 Fibra T1."
        }
        return "Aldren compara as receitas e os requisitos de especialização, mas não fabrica: faltam materiais ou a receita ainda não foi liberada."
    }
    fun step(): String {
        if (isComplete) {
            val message = "Tutorial concluído. Aldren agora pode escolher seus próprios objetivos."
            addLog(message)
            return message
        }
        val message = when (stage) {
            in 0..10 -> {
                val npc = npcs[stage]
                learnedNpcIds.add(npc.id)
                "Aldren conversa com " + npc.name + ": “" + npc.explanation + "”"
            }
            11 -> {
                inventory.addItem("Minério T1", 3)
                inventory.addItem("Madeira T1", 3)
                inventory.addItem("Fibra T1", 2)
                "Coleta concluída: +3 Minério T1, +3 Madeira T1 e +2 Fibra T1. Os recursos vieram de uma fonte disponível."
            }
            12 -> {
                inventory.addItem("Couro T1", 2)
                silver += 10
                "Caçada concluída: criatura T1 derrotada. Aldren recebeu 2 Couros T1 e 10 alzes como recompensa."
            }
            13 -> craftStarterArmor()
            14 -> {
                if (inventory.removeItem("Fibra T1", 1)) {
                    chest.addItem("Fibra T1", 1)
                    "Aldren guarda 1 Fibra T1 no baú. O item está armazenado, não no inventário carregado."
                } else {
                    "O baú foi apresentado. Não havia Fibra T1 sobrando; nenhuma quantidade foi criada."
                }
            }
            15 -> {
                if (inventory.removeItem("Armadura T1", 1)) {
                    marketListing = "Armadura T1"
                    "Oferta publicada: Armadura T1 por 25 alzes. O item está reservado na oferta e continua pertencendo a Aldren. Ainda não foi vendido; nenhum dinheiro foi recebido."
                } else {
                    "Oferta não publicada: Aldren não possui Armadura T1 no inventário."
                }
            }
            16 -> {
                val cost = 30
                if (silver - cost < 20) {
                    "Decisão de transporte: ir a pé. O teleporte custa " + cost + " alzes e deixaria pouca reserva. Caminhar leva cerca de 25 minutos simulados; teleporte levaria 1."
                } else {
                    silver -= cost
                    "Decisão de transporte: teleportar por " + cost + " alzes. O custo foi descontado do saldo."
                }
            }
            17 -> "Aldren conversa com o NPC da masmorra e aprende a verificar requisitos, riscos e recompensas. A entrada não é forçada."
            else -> "Etapa sem ação definida."
        }
        addLog(message)
        stage += 1
        if (isComplete) addLog("Quest inicial concluída. A partir de agora, Aldren poderá definir objetivos próprios.")
        return message
    }

    private fun craftStarterArmor(): String {
        val requirements = linkedMapOf("Minério T1" to 2, "Couro T1" to 1, "Madeira T1" to 1)
        val missing = requirements.filter { (item, amount) -> (inventory[item] ?: 0) < amount }
        if (missing.isNotEmpty()) {
            return "Fabricação bloqueada: faltam " + missing.entries.joinToString { entry ->
                (entry.value - (inventory[entry.key] ?: 0)).toString() + " " + entry.key
            } + ". O motor não criou o item."
        }
        requirements.forEach { (item, amount) -> inventory.removeItem(item, amount) }
        inventory.addItem("Armadura T1", 1)
        return "Fabricação concluída: 2 Minérios T1 + 1 Couro T1 + 1 Madeira T1 foram consumidos. Aldren fabricou 1 Armadura T1."
    }

    private fun addLog(message: String) {
        log.add(message)
        if (log.size > MAX_LOG_ENTRIES) log.removeAt(0)
    }

    fun reset() {
        stage = 0
        silver = 100
        inventory.clear()
        chest.clear()
        learnedNpcIds.clear()
        marketListing = null
        worldTick = 0L
        autonomousActionIndex = 0
        skillCraftCounts.clear()
        progression = SkillProgression()
        equippedGear.clear()
        log.clear()
        log.add("Dia 1 começou. Aldren recebeu a quest: Aprendendo a sobreviver.")
        log.add("Objetivo: conhecer a vila e aprender as mecânicas básicas.")
    }

    fun saveTo(preferences: android.content.SharedPreferences) {
        preferences.edit()
            .putInt("stage", stage)
            .putInt("silver", silver)
            .putString("inventory", inventory.encode())
            .putString("chest", chest.encode())
            .putString("learned_npcs", learnedNpcIds.sorted().joinToString(","))
            .putString("market_listing", marketListing)
            .putLong("world_tick", worldTick)
            .putInt("autonomous_action_index", autonomousActionIndex)
            .putString("skill_craft_counts", skillCraftCounts.encode())
            .putString("skill_progression", progression.export().entries.joinToString(";") { (skill, state) ->
                "${skill.name},${state.level},${state.actionsAtCurrentLevel},${state.totalSuccessfulActions}"
            })
            .putString("equipped_gear", equippedGear.entries.joinToString(";") { (slot, tier) -> "${slot.name},${tier.name}" })
            .putString("log", log.joinToString("\u001e"))
            .apply()
    }

    companion object {
        const val TOTAL_STAGES = 18
        private const val MAX_LOG_ENTRIES = 120

        fun loadFrom(preferences: android.content.SharedPreferences): SimulatorEngine {
            val learned = preferences.getString("learned_npcs", "").orEmpty()
                .split(",").mapNotNull { it.toIntOrNull() }.toSet()
            val savedLog = preferences.getString("log", null)?.split("\u001e")?.filter { it.isNotBlank() }
            return SimulatorEngine(
                stage = preferences.getInt("stage", 0),
                silver = preferences.getInt("silver", 100),
                inventory = preferences.getString("inventory", "").orEmpty().decodeInventory(),
                chest = preferences.getString("chest", "").orEmpty().decodeInventory(),
                learnedNpcIds = learned,
                marketListing = preferences.getString("market_listing", null),
                worldTick = preferences.getLong("world_tick", 0L),
                autonomousActionIndex = preferences.getInt("autonomous_action_index", 0),
                skillCraftCounts = preferences.getString("skill_craft_counts", "").orEmpty().decodeInventory(),
                equippedGear = preferences.getString("equipped_gear", "").orEmpty().split(";").mapNotNull { entry ->
                    val parts = entry.split(",")
                    if (parts.size != 2) null else {
                        val slot = runCatching { EquipmentSlot.valueOf(parts[0]) }.getOrNull()
                        val tier = runCatching { Tier.valueOf(parts[1]) }.getOrNull()
                        if (slot == null || tier == null) null else slot to tier
                    }
                }.toMap(),
                progressionSnapshots = preferences.getString("skill_progression", "").orEmpty().split(";").mapNotNull { entry ->
                    val parts = entry.split(",")
                    if (parts.size != 4) null else {
                        val skill = runCatching { SkillId.valueOf(parts[0]) }.getOrNull()
                        val level = parts[1].toIntOrNull()
                        val count = parts[2].toIntOrNull()
                        val total = parts[3].toIntOrNull()
                        if (skill == null || level == null || count == null || total == null) null
                        else skill to SkillSnapshot(level, count, total)
                    }
                }.toMap(),
                logEntries = savedLog ?: listOf(
                    "Dia 1 começou. Aldren recebeu a quest: Aprendendo a sobreviver.",
                    "Objetivo: conhecer a vila e aprender as mecânicas básicas."
                )
            )
        }

        private fun Map<String, Int>.encode(): String =
            entries.joinToString(";") { it.key + ":" + it.value }

        private fun String.decodeInventory(): Map<String, Int> {
            if (isBlank()) return emptyMap()
            return split(";").mapNotNull { entry ->
                val parts = entry.split(":", limit = 2)
                if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
            }.filter { it.second > 0 }.toMap()
        }
    }
}

private fun MutableMap<String, Int>.addItem(item: String, amount: Int) {
    require(amount > 0)
    this[item] = (this[item] ?: 0) + amount
}

private fun MutableMap<String, Int>.removeItem(item: String, amount: Int): Boolean {
    require(amount > 0)
    val current = this[item] ?: return false
    if (current < amount) return false
    val remaining = current - amount
    if (remaining == 0) remove(item) else this[item] = remaining
    return true
}
