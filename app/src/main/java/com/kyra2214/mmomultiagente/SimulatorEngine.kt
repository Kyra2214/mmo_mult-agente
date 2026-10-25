package com.kyra2214.mmomultiagente

/** Deterministic simulation core without Android UI dependencies. */
class SimulatorEngine(
    var stage: Int = 0,
    var silver: Int = 100,
    inventory: Map<String, Int> = emptyMap(),
    chest: Map<String, Int> = emptyMap(),
    learnedNpcIds: Set<Int> = emptySet(),
    var marketListing: String? = null,
    var marketPrice: Int? = null,
    var marketListingAgeTicks: Int = 0,
    var worldTick: Long = 0L,
    var autonomousActionIndex: Int = 0,
    skillCraftCounts: Map<String, Int> = emptyMap(),
    progressionSnapshots: Map<SkillId, SkillSnapshot> = emptyMap(),
    equippedGear: Map<EquipmentSlot, Tier> = emptyMap(),
    var dungeonCompleted: Boolean = false,
    var dungeonAttempts: Int = 0,
    var health: Int = 100,
    gearDamage: Map<EquipmentSlot, Int> = emptyMap(),
    logEntries: List<String> = listOf(
        "Dia 1 começou. Aldren recebeu a quest: Aprendendo a sobreviver.",
        "Objetivo: conhecer a vila e aprender as mecânicas básicas."
    ),
    var agentName: String = "Aldren",
    var legacyMarketBuyerEnabled: Boolean = true
) {
    val inventory = inventory.toMutableMap()
    val chest = chest.toMutableMap()
    val learnedNpcIds = learnedNpcIds.toMutableSet()
    val skillCraftCounts = skillCraftCounts.toMutableMap()
    var progression = SkillProgression(progressionSnapshots)
    val equippedGear = equippedGear.toMutableMap()
    val gearDamage = gearDamage.toMutableMap()
    val log = logEntries.toMutableList().apply { indices.forEach { i -> this[i] = this[i].replace("Aldren", agentName) } }

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
    val repairNpc = Npc(12, "Mestre de Reparos", "Conserta equipamentos mediante pagamento em alzes. O agente deve visitar este NPC antes de acumular desgaste excessivo.")

    val isComplete: Boolean get() = stage >= TOTAL_STAGES

    val currentObjective: String
        get() = when {
            stage < npcs.size -> "Quest 1/9 — Conhecer a vila (" + (stage + 1) + "/12 NPCs)"
            stage == 11 -> "Quest 1/9 — Conhecer a vila (12/12 NPCs)"
            stage == 12 -> "Quest 2/9 — Aprender a coletar"
            stage == 13 -> "Quest 3/9 — Aprender a caçar"
            stage == 14 -> "Quest 4/9 — Fabricar o primeiro item"
            stage == 15 -> "Quest 5/9 — Conhecer o baú"
            stage == 16 -> "Quest 6/9 — Publicar uma oferta"
            stage == 17 -> "Quest 7/9 — Avaliar o teleporte"
            stage == 18 -> "Quest 8/9 — Conhecer a masmorra"
            else -> "Tutorial concluído — vida autônoma desbloqueada"
        }

    /** One unattended world tick. The tutorial runs automatically; afterward a policy chooses by needs. */
    fun tick(): String {
        if (stage < TOTAL_STAGES) return step()
        worldTick += 1
        if (marketListing != null) marketListingAgeTicks += 1
        (if (legacyMarketBuyerEnabled) resolveMarketBuyer() else null)?.let { message ->
            addLog("Tick $worldTick — $message")
            return message
        }
        repairDamagedEquipment()?.let { message ->
            addLog("Tick $worldTick — $message")
            return message
        }
        val hasUnrepairedWear = gearDamage.values.any { it > 0 }
        if (isReadyForT3Dungeon() && !hasUnrepairedWear && !dungeonCompleted && (dungeonAttempts == 0 || worldTick % 50L == 0L)) {
            val result = T3DungeonSimulator.runRandom(equippedGear, health.coerceAtLeast(1))
            dungeonAttempts += 1
            health = result.playerHpRemaining
            if (result.outcome == DungeonOutcome.VICTORY) {
                dungeonCompleted = true
                result.chest?.lootItemIds?.forEach { inventory.addItem(it, 1) }
                val message = "${agentName} vence o mini-chefe e o chefe T3. Abre o baú ${result.chest?.rarity}; itens recebidos: ${result.chest?.lootItemIds?.joinToString()}. A vida continua com ${health} HP."
                addLog("Tick $worldTick — $message")
                return message
            }
            if (result.outcome == DungeonOutcome.WITHDRAWN) {
                health = 100
                val message = "${agentName} percebe que o combate está difícil, recua para uma área segura, sai da masmorra e descansa antes de voltar. Não perde itens nem prata; tentará novamente quando estiver melhor preparado."
                addLog("Tick $worldTick — $message")
                return message
            }
            val wornOut = recordDungeonDefeatWear()
            health = 100
            val brokenText = if (wornOut.isEmpty()) "" else " Itens que precisam ser refeitos: " + wornOut.joinToString() + "."
            val message = "${agentName} é derrotado na masmorra T3 após ${result.encounters.lastOrNull()?.name ?: "o combate"}. Não perde itens nem prata pela morte. O desgaste do equipamento aumentou; precisa pagar ao Mestre de Reparos antes de chegar à terceira derrota sem conserto." + brokenText
            addLog("Tick $worldTick — $message")
            return message
        }
        val message = when (AutonomousDecisionPolicy.choose(inventory.toMap(), silver, marketListing, progression)) {
            AutonomousAction.GATHER -> gatherNeededMaterials()
            AutonomousAction.HUNT -> huntForHide()
            AutonomousAction.FISH -> fishIfUseful()
            AutonomousAction.REFINE -> refineOneMaterial()
            AutonomousAction.CRAFT -> craftOneItem()
            AutonomousAction.CRAFT_TOOL -> craftFishingRod()
            AutonomousAction.TRADE -> listOrReviewMarket()
        }
        autonomousActionIndex += 1
        addLog("Tick $worldTick — $message")
        return message
    }

    private fun resolveMarketBuyer(): String? {
        val item = marketListing ?: return null
        // A buyer visits the market every five world ticks. Offers remain reserved
        // until that visit; listing alone never grants silver.
        if (marketListingAgeTicks < 5 || worldTick % 5L != 0L) return null
        val price = marketPrice ?: when {
            item == "Armadura T1" -> 25
            item.startsWith("Peixe ") -> 5
            else -> 10
        }
        silver += price
        marketListing = null
        marketPrice = null
        marketListingAgeTicks = 0
        return "Comprador confirma a compra de $item por $price alzes. A oferta foi liquidada e o saldo foi creditado."
    }

    private fun isReadyForT3Dungeon(): Boolean =
        EquipmentSlot.entries.all { slot -> (equippedGear[slot]?.rank ?: 0) >= Tier.T3.rank }

    internal fun recordDungeonDefeatWear(): List<String> {
        val broken = mutableListOf<String>()
        equippedGear.toMap().forEach { (slot, tier) ->
            val damage = (gearDamage[slot] ?: 0) + 1
            if (damage >= 3) {
                equippedGear.remove(slot)
                gearDamage.remove(slot)
                broken.add(slot.name + " " + tier.name)
            } else {
                gearDamage[slot] = damage
            }
        }
        return broken
    }

    private fun repairDamagedEquipment(): String? {
        val entry = gearDamage.entries.filter { it.value > 0 && equippedGear.containsKey(it.key) }
            .sortedBy { (slot, _) -> equippedGear[slot]?.rank ?: 1 }.firstOrNull() ?: return null
        val (slot, damage) = entry
        val tier = equippedGear[slot] ?: return null
        val cost = 10 * tier.rank * damage
        if (silver < cost) return null
        silver -= cost
        gearDamage[slot] = 0
        return "${agentName} visita ${repairNpc.name} e conserta ${slot.name} ${tier.name} por $cost alzes. Desgaste zerado; saldo: $silver alzes."
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
        val targetTier = equipmentTargetTier()
        needs.forEach { need ->
            val skillTier = SkillProgression.highestUnlockedTier(progression.snapshot(need.skill).level)
            val refiningSkill = when (need.raw) {
                "Minério" -> SkillId.ORE_REFINING
                "Pedra" -> SkillId.STONE_REFINING
                "Madeira" -> SkillId.WOOD_REFINING
                "Fibra" -> SkillId.FIBER_REFINING
                else -> SkillId.ARCANE_REFINING
            }
            val refiningTier = SkillProgression.highestUnlockedTier(progression.snapshot(refiningSkill).level)
            val tier = Tier.fromRank(minOf(targetTier.rank, skillTier.rank, refiningTier.rank))
            val rawName = "${need.raw} ${tier.name}"
            val processedName = "${need.processed} ${tier.name}"
            val hasRaw = (inventory[rawName] ?: 0) > 0
            val requiredProcessed = when (need.raw) {
                "Minério" -> 3
                "Madeira", "Fibra", "Fragmento Arcano" -> 2
                else -> 1
            }
            val hasProcessed = (inventory[processedName] ?: 0) >= requiredProcessed
            if (!hasRaw && !hasProcessed) {
                val sourceAvailable = worldResourceStore?.harvest(rawName, need.batchSize) ?: (worldResourceStore == null)
                if (sourceAvailable) {
                    inventory.addItem(rawName, need.batchSize)
                    recordEconomy(EconomyEventType.RESOURCE_CREATED, rawName, need.batchSize, "gathering")
                    progression.recordSuccessfulAction(need.skill, tier)
                    gathered.add("${need.batchSize} $rawName")
                } else {
                    gathered.add("fonte esgotada: $rawName")
                }
            }
        }
        return if (gathered.isNotEmpty()) {
            "${agentName} consulta os estoques e coleta recursos compatíveis com suas especializações: ${gathered.joinToString(", ")}."
        } else {
            "${agentName} verifica as fontes disponíveis; não coleta porque já tem matéria-prima ou material processado para as necessidades imediatas."
        }
    }

    private fun huntForHide(): String {
        val targetTier = equipmentTargetTier()
        val unlockedTier = SkillProgression.highestUnlockedTier(progression.snapshot(SkillId.SKINNING).level)
        val refiningTier = SkillProgression.highestUnlockedTier(progression.snapshot(SkillId.LEATHER_REFINING).level)
        val tier = Tier.fromRank(minOf(targetTier.rank, unlockedTier.rank, refiningTier.rank))
        if (worldResourceStore != null && !worldResourceStore!!.harvest("Creature ${tier.name}", 1)) {
            return "${agentName} procura uma criatura ${tier.name}, mas o estoque local está esgotado. Não recebe couro nem alzes."
        }
        inventory.addItem("Couro ${tier.name}", 1)
        recordEconomy(EconomyEventType.RESOURCE_CREATED, "Couro ${tier.name}", 1, "hunt")
        progression.recordSuccessfulAction(SkillId.SKINNING, tier)
        silver += 5
        recordEconomy(EconomyEventType.CURRENCY_MINTED, "alzes", 5, "hunt-reward")
        return "${agentName} caça uma criatura ${tier.name} compatível com seu preparo; obtém 1 Couro ${tier.name} e 5 alzes."
    }

    private fun fishIfUseful(): String {
        // Fishing requires a rod. Without one, the agent must gather/refine/craft first.
        if ((inventory["Vara de Pesca T1"] ?: 0) <= 0) {
            return "${agentName} avalia a pesca, mas ainda não tem vara. Adia a atividade e prioriza conseguir os materiais e fabricar a ferramenta."
        }
        if (worldResourceStore != null && !worldResourceStore!!.harvest("Peixe T1", 2)) {
            return "${agentName} encontra o ponto de pesca sem estoque suficiente. Não recebe peixes."
        }
        inventory.addItem("Peixe T1", 2)
        recordEconomy(EconomyEventType.RESOURCE_CREATED, "Peixe T1", 2, "fishing")
        progression.recordSuccessfulAction(SkillId.FISHING, Tier.T1)
        return "${agentName} usa sua vara em um ponto de pesca e obtém 2 Peixes T1."
    }

    private fun listOrReviewMarket(): String {
        if (marketListing == null && inventory.removeItem("Peixe T1", 1)) {
            marketListing = "Peixe T1"
            marketPrice = 5
            marketListingAgeTicks = 0
            return "${agentName} publica uma oferta de 1 Peixe T1 por 5 alzes. O peixe fica reservado; nenhum alze é recebido antes da venda."
        }
        return "${agentName} revisa ofertas e saldo. Sem comprador confirmado, mantém o item reservado e não contabiliza uma venda fictícia."
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
        } ?: return "${agentName} consulta os refinadores, mas não encontra matéria-prima refinável de um tier liberado."
        inventory.removeItem(recipe.raw, 1)
        recordEconomy(EconomyEventType.RESOURCE_CONSUMED, recipe.raw, 1, "refining")
        inventory.addItem(recipe.output, 1)
        recordEconomy(EconomyEventType.RESOURCE_CREATED, recipe.output, 1, "refining")
        progression.recordSuccessfulAction(recipe.skill, recipe.tier)
        return "${agentName} realiza ${recipe.label} ${recipe.tier.name}: consome 1 ${recipe.raw} e produz 1 ${recipe.output}."
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

        localizedCosts.forEach { (cost, item) ->
            inventory.removeItem(item, cost.quantity)
            recordEconomy(EconomyEventType.RESOURCE_CONSUMED, item, cost.quantity, "crafting")
        }
        val itemName = equipmentName(slot, tier)
        inventory.addItem(itemName, 1)
        recordEconomy(EconomyEventType.RESOURCE_CREATED, itemName, 1, "crafting)
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
            // New equipment starts intact; wear belongs to the replaced piece.
            gearDamage.remove(slot)
        }
        return "${agentName} fabrica $itemName com a receita ${tier.name}; materiais consumidos e especialização ${recipe.skill} agora está no nível ${result.afterLevel}."
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

    private fun craftFishingRod(): String {
        if ((inventory["Vara de Pesca T1"] ?: 0) > 0) {
            return "${agentName} já possui uma Vara de Pesca T1; não fabrica outra sem necessidade."
        }
        if ((inventory["Prancha T1"] ?: 0) < 1 || (inventory["Tecido T1"] ?: 0) < 1) {
            return "Fabricação da vara adiada: faltam 1 Prancha T1 e 1 Tecido T1."
        }
        inventory.removeItem("Prancha T1", 1)
        inventory.removeItem("Tecido T1", 1)
        inventory.addItem("Vara de Pesca T1", 1)
        skillCraftCounts["ferramentas"] = (skillCraftCounts["ferramentas"] ?: 0) + 1
        progression.recordSuccessfulAction(SkillId.TOOL_CRAFTING, Tier.T1)
        return "${agentName} fabrica uma Vara de Pesca T1 consumindo 1 Prancha T1 e 1 Tecido T1."
    }

    private fun craftOneItem(): String {
        val targetTier = equipmentTargetTier()
        // Spread tier-specific crafting across the least-developed specialization,
        // so one slot cannot monopolize materials while the other six lag behind.
        val orderedSlots = EquipmentSlot.entries.sortedWith(
            compareBy<EquipmentSlot> { slot ->
                progression.snapshot(TierRecipes.recipe(slot, Tier.T1).skill).level
            }.thenBy { slot ->
                progression.snapshot(TierRecipes.recipe(slot, Tier.T1).skill).actionsAtCurrentLevel
            }.thenBy { slot ->
                progression.snapshot(TierRecipes.recipe(slot, Tier.T1).skill).totalSuccessfulActions
            }
        )
        for (slot in orderedSlots) {
            val recipe = TierRecipes.recipe(slot, targetTier)
            if (!progression.canPerformTier(recipe.skill, targetTier)) continue
            val affordable = recipe.materials.all { cost ->
                (inventory[materialDisplayName(cost.itemId)] ?: 0) >= cost.quantity
            }
            if (affordable) return craftEquipment(slot, targetTier)
        }

        // Tool recipe is separate from combat equipment and requires its own materials.
        if ((inventory["Prancha T1"] ?: 0) >= 1 && (inventory["Fibra T1"] ?: 0) >= 1 &&
            (inventory["Vara de Pesca T1"] ?: 0) <= 0) {
            inventory.removeItem("Prancha T1", 1)
            inventory.removeItem("Fibra T1", 1)
            inventory.addItem("Vara de Pesca T1", 1)
            skillCraftCounts["ferramentas"] = (skillCraftCounts["ferramentas"] ?: 0) + 1
            progression.recordSuccessfulAction(SkillId.TOOL_CRAFTING, Tier.T1)
            return "${agentName} fabrica uma Vara de Pesca T1 consumindo 1 Prancha T1 e 1 Fibra T1."
        }
        return "${agentName} compara as receitas e os requisitos de especialização, mas não fabrica: faltam materiais para o tier-alvo ${targetTier.name}."
    }

    private fun equipmentTargetTier(): Tier {
        return EquipmentProgressPlanner.targetTier(progression)
    }

    fun step(): String {
        if (isComplete) {
            val message = "Tutorial concluído. ${agentName} agora pode escolher seus próprios objetivos."
            addLog(message)
            return message
        }
        val message = when (stage) {
            in 0..10 -> {
                val npc = npcs[stage]
                learnedNpcIds.add(npc.id)
                "${agentName} conversa com " + npc.name + ": “" + npc.explanation + "”"
            }
            11 -> {
                learnedNpcIds.add(repairNpc.id)
                "${agentName} conversa com ${repairNpc.name}: “${repairNpc.explanation}”"
            }
            12 -> {
                inventory.addItem("Minério T1", 3)
                inventory.addItem("Madeira T1", 3)
                inventory.addItem("Fibra T1", 2)
                recordEconomy(EconomyEventType.RESOURCE_CREATED, "Minério T1", 3, "tutorial-starter")
                recordEconomy(EconomyEventType.RESOURCE_CREATED, "Madeira T1", 3, "tutorial-starter")
                recordEconomy(EconomyEventType.RESOURCE_CREATED, "Fibra T1", 2, "tutorial-starter")
                "Coleta concluída: +3 Minério T1, +3 Madeira T1 e +2 Fibra T1. Os recursos vieram do pacote inicial do tutorial."
            }
            13 -> {
                inventory.addItem("Couro T1", 2)
                recordEconomy(EconomyEventType.RESOURCE_CREATED, "Couro T1", 2, "tutorial-reward")
                silver += 10
                recordEconomy(EconomyEventType.CURRENCY_MINTED, "alzes", 10, "tutorial-reward")
                "Caçada concluída: criatura T1 derrotada. ${agentName} recebeu 2 Couros T1 e 10 alzes como recompensa."
            }
            14 -> craftStarterArmor()
            15 -> {
                if (inventory.removeItem("Fibra T1", 1)) {
                    chest.addItem("Fibra T1", 1)
                    "${agentName} guarda 1 Fibra T1 no baú. O item está armazenado, não no inventário carregado."
                } else {
                    "O baú foi apresentado. Não havia Fibra T1 sobrando; nenhuma quantidade foi criada."
                }
            }
            16 -> {
                if (inventory.removeItem("Armadura T1", 1)) {
                    marketListing = "Armadura T1"
                    marketPrice = 25
                    marketListingAgeTicks = 0
                    "Oferta publicada: Armadura T1 por 25 alzes. O item está reservado na oferta e continua pertencendo a ${agentName}. Ainda não foi vendido; nenhum dinheiro foi recebido."
                } else {
                    "Oferta não publicada: ${agentName} não possui Armadura T1 no inventário."
                }
            }
            17 -> {
                val cost = 30
                if (silver - cost < 20) {
                    "Decisão de transporte: ir a pé. O teleporte custa " + cost + " alzes e deixaria pouca reserva. Caminhar leva cerca de 25 minutos simulados; teleporte levaria 1."
                } else {
                    silver -= cost
                    "Decisão de transporte: teleportar por " + cost + " alzes. O custo foi descontado do saldo."
                }
            }
            18 -> "${agentName} conversa com o NPC da masmorra e aprende a verificar requisitos, riscos e recompensas. A entrada não é forçada."
            else -> "Etapa sem ação definida."
        }
        addLog(message)
        stage += 1
        if (isComplete) addLog("Quest inicial concluída. A partir de agora, ${agentName} poderá definir objetivos próprios.")
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
        requirements.forEach { (item, amount) ->
            inventory.removeItem(item, amount)
            recordEconomy(EconomyEventType.RESOURCE_CONSUMED, item, amount, "tutorial-crafting")
        }
        inventory.addItem("Armadura T1", 1)
        recordEconomy(EconomyEventType.RESOURCE_CREATED, "Armadura T1", 1, "tutorial-crafting")
        skillCraftCounts["armaduras"] = (skillCraftCounts["armaduras"] ?: 0) + 1
        progression.recordSuccessfulAction(SkillId.ARMOR_CRAFTING, Tier.T1)
        return "Fabricação concluída: 2 Minérios T1 + 1 Couro T1 + 1 Madeira T1 foram consumidos. ${agentName} fabricou 1 Armadura T1; a especialização de armaduras ganhou progresso."
    }

    private fun recordEconomy(type: EconomyEventType, itemId: String, quantity: Int, cause: String) {
        economyEventSink?.invoke(
            EconomyEvent(
                agentId = agentName.lowercase(),
                tick = worldTick,
                type = type,
                itemId = itemId,
                quantity = quantity,
                cause = cause
            )
        )
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
        marketPrice = null
        marketListingAgeTicks = 0
        worldTick = 0L
        autonomousActionIndex = 0
        skillCraftCounts.clear()
        progression = SkillProgression()
        equippedGear.clear()
        gearDamage.clear()
        dungeonCompleted = false
        dungeonAttempts = 0
        health = 100
        log.clear()
        log.add("Dia 1 começou. ${agentName} recebeu a quest: Aprendendo a sobreviver.")
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
            .putInt("market_price", marketPrice ?: -1)
            .putInt("market_listing_age_ticks", marketListingAgeTicks)
            .putLong("world_tick", worldTick)
            .putInt("autonomous_action_index", autonomousActionIndex)
            .putString("skill_craft_counts", skillCraftCounts.encode())
            .putString("skill_progression", progression.export().entries.joinToString(";") { (skill, state) ->
                "${skill.name},${state.level},${state.actionsAtCurrentLevel},${state.totalSuccessfulActions}"
            })
            .putString("equipped_gear", equippedGear.entries.joinToString(";") { (slot, tier) -> "${slot.name},${tier.name}" })
            .putBoolean("dungeon_completed", dungeonCompleted)
            .putInt("dungeon_attempts", dungeonAttempts)
            .putInt("health", health)
            .putString("gear_damage", gearDamage.entries.joinToString(";") { (slot, damage) -> "${slot.name},$damage" })
            .putString("log", log.joinToString("\u001e"))
            .apply()
    }

    companion object {
        const val TOTAL_STAGES = 19
        private const val MAX_LOG_ENTRIES = 120

        fun loadFrom(preferences: android.content.SharedPreferences, agentName: String = "Aldren", legacyMarketBuyerEnabled: Boolean = true): SimulatorEngine {
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
                marketPrice = preferences.getInt("market_price", -1).takeIf { it >= 0 },
                marketListingAgeTicks = preferences.getInt("market_listing_age_ticks", 0),
                worldTick = preferences.getLong("world_tick", 0L),
                autonomousActionIndex = preferences.getInt("autonomous_action_index", 0),
                skillCraftCounts = preferences.getString("skill_craft_counts", "").orEmpty().decodeInventory(),
                dungeonCompleted = preferences.getBoolean("dungeon_completed", false),
                dungeonAttempts = preferences.getInt("dungeon_attempts", 0),
                health = preferences.getInt("health", 100),
                gearDamage = preferences.getString("gear_damage", "").orEmpty().split(";").mapNotNull { entry ->
                    val parts = entry.split(",")
                    val slot = parts.getOrNull(0)?.let { runCatching { EquipmentSlot.valueOf(it) }.getOrNull() }
                    val damage = parts.getOrNull(1)?.toIntOrNull()
                    if (slot == null || damage == null || damage <= 0) null else slot to damage
                }.toMap(),
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
                    "Dia 1 começou. ${agentName} recebeu a quest: Aprendendo a sobreviver.",
                    "Objetivo: conhecer a vila e aprender as mecânicas básicas."
                ),
                agentName = agentName,
                legacyMarketBuyerEnabled = legacyMarketBuyerEnabled
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
