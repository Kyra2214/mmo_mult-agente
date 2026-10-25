package com.kyra2214.mmomultiagente

/** Deterministic simulation core without Android UI dependencies. */
class SimulatorEngine(
    var stage: Int = 0,
    var silver: Int = 100,
    inventory: Map<String, Int> = emptyMap(),
    chest: Map<String, Int> = emptyMap(),
    learnedNpcIds: Set<Int> = emptySet(),
    var marketListing: String? = null,
    logEntries: List<String> = listOf(
        "Dia 1 começou. Aldren recebeu a quest: Aprendendo a sobreviver.",
        "Objetivo: conhecer a vila e aprender as mecânicas básicas."
    )
) {
    val inventory = inventory.toMutableMap()
    val chest = chest.toMutableMap()
    val learnedNpcIds = learnedNpcIds.toMutableSet()
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
