package com.kyra2214.mmomultiagente

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
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
    private lateinit var nextButton: Button
    private val preferences by lazy { getSharedPreferences("mmo_simulator", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = SimulatorEngine.loadFrom(preferences)
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
        nextButton = makeButton("Executar próximo passo") {
            engine.step()
            persistAndRefresh()
        }
        root.addView(nextButton, matchWidth())
        root.addView(makeButton("Avançar 10 passos") {
            repeat(10) { if (!engine.isComplete) engine.step() }
            persistAndRefresh()
        }, matchWidth())
        root.addView(makeButton("Reiniciar simulação", secondary = true) {
            engine.reset()
            persistAndRefresh()
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
        val listing = engine.marketListing?.let { "Oferta: " + it + " (aguardando comprador)" } ?: "Sem ofertas publicadas"
        stats.text = "Agente: Aldren   •   Dia: 1\nAlzes: " + engine.silver +
            "\nNPCs conhecidos: " + engine.learnedNpcIds.size + "/11" +
            "\nInventário: " + inventoryText + "\nBaú: " + chestText + "\n" + listing
        objective.text = engine.currentObjective
        logView.text = engine.log.mapIndexed { index, line -> (index + 1).toString() + ". " + line }
            .joinToString("\n\n")
        nextButton.isEnabled = !engine.isComplete
        nextButton.text = if (engine.isComplete) "Tutorial concluído" else "Executar próximo passo"
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun persistAndRefresh() {
        engine.saveTo(preferences)
        refresh()
    }

    override fun onPause() {
        if (::engine.isInitialized) engine.saveTo(preferences)
        super.onPause()
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
