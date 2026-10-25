package com.kyra2214.mmomultiagente

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private data class AgentRuntime(
        val id: String,
        val displayName: String,
        val engine: SimulatorEngine,
        val preferences: android.content.SharedPreferences
    )

    private lateinit var agents: List<AgentRuntime>
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var page: Page = Page.SERVER
    private var selectedAgentId: String = "aldren"
    private val handler = Handler(Looper.getMainLooper())
    private val tickIntervalMs = 2_000L
    private val sharedMarket = SharedMarketCoordinator()
    private lateinit var economyLedger: EconomyLedger
    private lateinit var economyLedgerPreferences: android.content.SharedPreferences
    private lateinit var worldResourceStore: WorldResourceStore
    private lateinit var worldResourcePreferences: android.content.SharedPreferences
    private val runner = object : Runnable {
        override fun run() {
            agents.forEach { agent -> agent.engine.tick() }
            sharedMarket.tick(agents.map { MarketParticipant(it.id, it.displayName, it.engine) }, economyLedger)
            worldResourcePreferences.edit().putString("stocks", worldResourceStore.encode()).apply()
            economyLedgerPreferences.edit().putString("events", economyLedger.encode()).apply()
            agents.forEach { agent ->
                agent.engine.saveTo(agent.preferences)
                agent.preferences.edit().putLong("last_wall_clock_ms", System.currentTimeMillis()).apply()
            }
            refresh()
            handler.postDelayed(this, tickIntervalMs)
        }
    }

    private enum class Page { SERVER, EVENTS, AGENTS, AGENT_DETAIL }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        worldResourcePreferences = getSharedPreferences("mmo_world_resources", MODE_PRIVATE)
        worldResourceStore = WorldResourceStore.decode(worldResourcePreferences.getString("stocks", "").orEmpty())
        economyLedgerPreferences = getSharedPreferences("mmo_economy_ledger", MODE_PRIVATE)
        economyLedger = EconomyLedger.decode(economyLedgerPreferences.getString("events", "").orEmpty())
        val profiles = listOf(
            Triple("aldren", "Aldren", "mmo_simulator"),
            Triple("mira", "Mira", "mmo_simulator_mira"),
            Triple("borin", "Borin", "mmo_simulator_borin"),
            Triple("kael", "Kael", "mmo_simulator_kael"),
            Triple("sera", "Sera", "mmo_simulator_sera")
        )
        agents = profiles.map { (id, name, preferenceName) ->
            val prefs = getSharedPreferences(preferenceName, MODE_PRIVATE)
            val engine = SimulatorEngine.loadFrom(prefs, name, legacyMarketBuyerEnabled = false)
            engine.worldResourceStore = worldResourceStore
            engine.economyEventSink = economyLedger::record
            catchUpWhileClosed(engine, prefs)
            AgentRuntime(id, name, engine, prefs)
        }
        buildInterface()
        refresh()
    }

    private fun buildInterface() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(10))
            setBackgroundColor(Color.rgb(245, 247, 245))
        }
        root.addView(TextView(this).apply {
            text = "MMO MULTIAGENTE"
            textSize = 23f
            setTextColor(Color.rgb(24, 61, 50))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "PAINEL DO SERVIDOR • 5 AGENTES"
            textSize = 11f
            setTextColor(Color.rgb(83, 104, 94))
            setPadding(0, dp(3), 0, dp(10))
        })
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        nav.addView(navButton("Servidor", Page.SERVER), weightedButtonParams())
        nav.addView(navButton("Eventos", Page.EVENTS), weightedButtonParams())
        nav.addView(navButton("Agentes", Page.AGENTS), weightedButtonParams())
        root.addView(nav, matchWidth())
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(12))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(TextView(this).apply {
            text = "Simulação autônoma • 1 tick a cada 2 segundos • salvamento local"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(95, 105, 99))
            setPadding(0, dp(8), 0, 0)
        })
        setContentView(root)
    }

    private fun navButton(label: String, target: Page): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 12f
        setOnClickListener { page = target; refresh() }
    }

    private fun refresh() {
        if (!::content.isInitialized) return
        content.removeAllViews()
        when (page) {
            Page.SERVER -> renderServer()
            Page.EVENTS -> renderEvents()
            Page.AGENTS -> renderAgents()
            Page.AGENT_DETAIL -> renderAgentDetail()
        }
    }

    private fun renderServer() {
        val totalTicks = agents.sumOf { it.engine.worldTick }
        val totalSilver = agents.sumOf { it.engine.silver }
        val offers = agents.count { it.engine.marketListing != null }
        addSectionTitle("VISÃO GERAL DO SERVIDOR")
        addInfoCard(
            "Estado: executando\nAgentes ativos: ${agents.size}\nTicks acumulados: $totalTicks\nAlzes em circulação (saldos dos agentes): $totalSilver\nOfertas pendentes: $offers"
        )
        val stockSnapshot = worldResourceStore.snapshot()
        val remainingUnits = stockSnapshot.values.sum()
        val depletedNodes = stockSnapshot.count { it.value == 0 }
        addInfoCard("ESTOQUES COMPARTILHADOS\nUnidades restantes nas fontes: $remainingUnits\nTipos de fonte esgotados: $depletedNodes de ${stockSnapshot.size}\nOs cinco agentes consomem os mesmos estoques finitos.")
        addSectionTitle("AGENTES")
        agents.forEach { agent ->
            val e = agent.engine
            val state = if (e.stage < SimulatorEngine.TOTAL_STAGES) "Tutorial" else "Autônomo"
            addClickableCard(
                "${agent.displayName}  ›",
                "$state • Tick ${e.worldTick} • ${e.silver} alzes\nObjetivo: ${e.currentObjective}\nÚltima atividade: ${e.log.lastOrNull().orEmpty()}"
            ) {
                selectedAgentId = agent.id
                page = Page.AGENT_DETAIL
                refresh()
            }
        }
        addSectionTitle("EVENTOS RECENTES")
        recentEvents(6).forEach { (name, event) -> addPlainText("• $name — $event") }
        addPlainText("Toque no nome de qualquer agente para abrir seus dados individuais e o histórico de ticks.")
    }

    private fun renderEvents() {
        addSectionTitle("EVENTOS DO SERVIDOR")
        addPlainText("Eventos recentes de todos os agentes, agrupados numa única linha do tempo. Os logs continuam pertencendo ao agente que executou a ação.")
        val trades = economyLedger.events.count { it.type == EconomyEventType.TRADE_SETTLED } / 2
        addInfoCard("MERCADO RASTREÁVEL (SESSÃO)\nTransações concluídas: $trades\nEventos causais registrados: ${economyLedger.events.size}\nEscopo atual: liquidações do mercado compartilhado; coleta e recompensas ainda não estão totalmente rastreadas.")
        recentEvents(100).forEach { (name, event) -> addInfoCard("$name\n$event") }
        if (recentEvents(100).isEmpty()) addPlainText("Ainda não há eventos registrados.")
    }

    private fun renderAgents() {
        addSectionTitle("AGENTES DO MUNDO")
        addPlainText("Selecione um agente para consultar saldo, inventário, progressão e log individual.")
        agents.forEach { agent ->
            val e = agent.engine
            addClickableCard(
                agent.displayName,
                "Tick ${e.worldTick} • ${e.silver} alzes • HP ${e.health}\nInventário: ${if (e.inventory.isEmpty()) "vazio" else e.inventory.entries.joinToString { "${it.value}× ${it.key}" }}\nOfertas pendentes: ${if (e.marketListing == null) "nenhuma" else "${e.marketListing} por ${e.marketPrice} alzes"}"
            ) {
                selectedAgentId = agent.id
                page = Page.AGENT_DETAIL
                refresh()
            }
        }
    }

    private fun renderAgentDetail() {
        val agent = agents.firstOrNull { it.id == selectedAgentId } ?: agents.first()
        val e = agent.engine
        addClickableCard("‹ Voltar ao servidor", "Retornar à visão geral") { page = Page.SERVER; refresh() }
        addSectionTitle("AGENTE: ${agent.displayName.uppercase()}")
        addInfoCard(
            "Estado: ${if (e.stage < SimulatorEngine.TOTAL_STAGES) "Tutorial" else "Autônomo"}\nTick: ${e.worldTick}\nDia simulado: ${1 + e.worldTick / 720}\nHP: ${e.health}\nAlzes: ${e.silver}\nNPCs conhecidos: ${e.learnedNpcIds.size}/12\nMasmorra T3: ${if (e.dungeonCompleted) "concluída" else "tentativas ${e.dungeonAttempts}"}\nObjetivo: ${e.currentObjective}"
        )
        addSectionTitle("INVENTÁRIO")
        addInfoCard(if (e.inventory.isEmpty()) "Inventário vazio" else e.inventory.entries.joinToString("\n") { "${it.value}× ${it.key}" })
        addSectionTitle("EQUIPAMENTO")
        addInfoCard(if (e.equippedGear.isEmpty()) "Nenhum equipamento equipado" else e.equippedGear.entries.joinToString("\n") { (slot, tier) -> "${slot.name}: ${tier.name}" })
        addSectionTitle("OFERTA DE MERCADO")
        addInfoCard(e.marketListing?.let { "$it • ${e.marketPrice ?: 0} alzes • aguardando comprador" } ?: "Nenhuma oferta pendente")
        addSectionTitle("LOG DE TICKS DO AGENTE")
        e.log.asReversed().forEachIndexed { index, line -> addInfoCard("Tick/registro ${e.log.size - index}\n$line") }
    }

    private fun recentEvents(limit: Int): List<Pair<String, String>> =
        agents.flatMap { agent -> agent.engine.log.map { line -> Triple(agent.displayName, agent.engine.worldTick, line) } }
            .sortedWith(compareByDescending<Triple<String, Long, String>> { it.second }.thenByDescending { it.third })
            .take(limit)
            .map { it.first to it.third }

    private fun addSectionTitle(text: String) {
        content.addView(TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.rgb(36, 107, 85))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(12), 0, dp(6))
        }, matchWidth())
    }

    private fun addInfoCard(text: String) {
        content.addView(TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.rgb(36, 46, 40))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.WHITE)
            setLineSpacing(dp(2).toFloat(), 1f)
        }, matchWidth().apply { bottomMargin = dp(7) })
    }

    private fun addPlainText(text: String) {
        content.addView(TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.rgb(54, 66, 59))
            setPadding(dp(2), dp(5), dp(2), dp(5))
        }, matchWidth())
    }

    private fun addClickableCard(title: String, details: String, action: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.WHITE)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        card.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(Color.rgb(24, 61, 50))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        card.addView(TextView(this).apply {
            text = details
            textSize = 13f
            setTextColor(Color.rgb(54, 66, 59))
            setPadding(0, dp(5), 0, 0)
            maxLines = 6
        })
        content.addView(card, matchWidth().apply { bottomMargin = dp(8) })
    }

    private fun catchUpWhileClosed(engine: SimulatorEngine, prefs: android.content.SharedPreferences) {
        val now = System.currentTimeMillis()
        val last = prefs.getLong("last_wall_clock_ms", now)
        val elapsedTicks = ((now - last).coerceAtLeast(0L) / tickIntervalMs).coerceAtMost(3600L)
        repeat(elapsedTicks.toInt()) { engine.tick() }
        prefs.edit().putLong("last_wall_clock_ms", now).apply()
        if (elapsedTicks > 0) engine.saveTo(prefs)
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun weightedButtonParams() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        if (::agents.isInitialized) {
            handler.removeCallbacks(runner)
            handler.postDelayed(runner, tickIntervalMs)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(runner)
        if (::agents.isInitialized) agents.forEach { agent ->
            agent.engine.saveTo(agent.preferences)
            agent.preferences.edit().putLong("last_wall_clock_ms", System.currentTimeMillis()).apply()
        }
        super.onPause()
    }
}
