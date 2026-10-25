package com.kyra2214.mmomultiagente

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var engine: SimulatorEngine
    private lateinit var stats: TextView
    private lateinit var objective: TextView
    private lateinit var logView: TextView
    private lateinit var scrollView: ScrollView
    private val handler = Handler(Looper.getMainLooper())
    private val tickIntervalMs = 2_000L
    private val runner = object : Runnable { override fun run() { engine.tick(); persistAndRefresh(); preferences.edit().putLong("last_wall_clock_ms", System.currentTimeMillis()).apply(); handler.postDelayed(this, tickIntervalMs) } }
    private val preferences by lazy { getSharedPreferences("mmo_simulator", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = SimulatorEngine.loadFrom(preferences)
        catchUpWhileClosed()
        buildInterface()
        refresh()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
            setBackgroundColor(Color.rgb(245, 247, 245))
        }
        root.addView(TextView(this).apply {
            text = "MMO MULTIAGENTE"
            textSize = 24f
            setTextColor(Color.rgb(24, 61, 50))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "SIMULADOR TEXTUAL • PROTÓTIPO 0.1.0"
            textSize = 11f
            setTextColor(Color.rgb(83, 104, 94))
            setPadding(0, dp(3), 0, dp(12))
        })
        stats = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(28, 45, 38))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(stats, matchWidth())
        objective = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.rgb(24, 61, 50))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(14), 0, dp(8))
        }
        root.addView(objective)
        root.addView(TextView(this).apply {
            text = "REGISTRO DO MUNDO"
            textSize = 12f
            setTextColor(Color.rgb(83, 104, 94))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(6))
        })
        scrollView = ScrollView(this).apply { setBackgroundColor(Color.WHITE); isFillViewport = true }
        logView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(36, 46, 40))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setTextIsSelectable(true)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
        scrollView.addView(logView)
        root.addView(scrollView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(TextView(this).apply {
            text = "SIMULAÇÃO AUTÔNOMA • 1 tick a cada 2 segundos em primeiro plano"
            textSize = 12f
            setTextColor(Color.rgb(36, 107, 85))
            setPadding(0, dp(10), 0, dp(4))
        }, matchWidth())
        root.addView(TextView(this).apply {
            text = "O progresso é salvo automaticamente neste aparelho."
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(95, 105, 99))
            setPadding(0, dp(8), 0, 0)
        })
        setContentView(root)
    }

    private fun makeButton(label: String, secondary: Boolean = false, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setTextColor(if (secondary) Color.rgb(44, 67, 57) else Color.WHITE)
            setBackgroundColor(if (secondary) Color.rgb(222, 231, 225) else Color.rgb(36, 107, 85))
            setOnClickListener { action() }
            layoutParams = matchWidth().apply { topMargin = dp(5) }
        }

    private fun refresh() {
        val inventoryText = if (engine.inventory.isEmpty()) "vazio"
            else engine.inventory.entries.joinToString { it.value.toString() + "× " + it.key }
        val chestText = if (engine.chest.isEmpty()) "vazio"
            else engine.chest.entries.joinToString { it.value.toString() + "× " + it.key }
        val gearText = if (engine.equippedGear.isEmpty()) "nenhum" else engine.equippedGear.entries.joinToString { (slot, tier) -> slot.name + ": " + tier.name }
        val progressionText = EquipmentSlot.entries.joinToString { slot ->
            val skill = TierRecipes.recipe(slot, Tier.T1).skill
            val snapshot = engine.progression.snapshot(skill)
            val progress = if (snapshot.level >= SkillProgression.MAX_LEVEL) "máx." else "${snapshot.actionsAtCurrentLevel}/${SkillProgression.actionsRequiredForNextLevel(snapshot.level)}"
            "${slot.name}: N${snapshot.level} ($progress)"
        }
        val listing = engine.marketListing?.let { "Oferta: " + it + " (aguardando comprador)" } ?: "Sem ofertas publicadas"
        stats.text = "Agente: Aldren   •   Dia: " + (1 + engine.worldTick / 720) + "   •   Tick: " + engine.worldTick + "\nHP: " + engine.health + "   •   Masmorra T3: " + (if (engine.dungeonCompleted) "concluída" else "tentativas " + engine.dungeonAttempts) + "\nAlzes: " + engine.silver +
            "\nNPCs conhecidos: " + engine.learnedNpcIds.size + "/11" +
            "\nEquipamento: " + gearText + "\nEspecializações: " + progressionText + "\nInventário: " + inventoryText + "\nBaú: " + chestText + "\n" + listing
        objective.text = engine.currentObjective
        logView.text = engine.log.mapIndexed { index, line -> (index + 1).toString() + ". " + line }
            .joinToString("\n\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun persistAndRefresh() {
        engine.saveTo(preferences)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::engine.isInitialized) {
            handler.removeCallbacks(runner)
            handler.postDelayed(runner, tickIntervalMs)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(runner)
        if (::engine.isInitialized) {
            engine.saveTo(preferences)
            preferences.edit().putLong("last_wall_clock_ms", System.currentTimeMillis()).apply()
        }
        super.onPause()
    }

    private fun catchUpWhileClosed() {
        val now = System.currentTimeMillis()
        val last = preferences.getLong("last_wall_clock_ms", now)
        val elapsedTicks = ((now - last).coerceAtLeast(0L) / tickIntervalMs).coerceAtMost(3600L)
        repeat(elapsedTicks.toInt()) { engine.tick() }
        preferences.edit().putLong("last_wall_clock_ms", now).apply()
        if (elapsedTicks > 0) engine.saveTo(preferences)
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
