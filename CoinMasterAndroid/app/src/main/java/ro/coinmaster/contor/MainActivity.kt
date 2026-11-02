package ro.coinmaster.contor

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private var spins = 0
    private var attacks = 0
    private var raids = 0
    private var ghosts = 0
    private var running = false
    private lateinit var status: TextView
    private lateinit var spinValue: TextView
    private lateinit var attackValue: TextView
    private lateinit var raidValue: TextView
    private lateinit var ghostValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Color.rgb(20, 19, 38))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
        }
        fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
            text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(8, 8, 8, 8)
        }
        root.addView(text("COIN MASTER · CONTOR", 24f, Color.WHITE, true))
        status = text("OPRIT", 14f, Color.LTGRAY)
        root.addView(status)
        spinValue = text("0", 42f, Color.WHITE, true)
        root.addView(text("ROTIRI TOTALE", 14f, Color.LTGRAY, true))
        root.addView(spinValue)
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 18, 0, 18) }
        fun counter(label: String, value: TextView) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            line.addView(text(label, 18f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(value, LinearLayout.LayoutParams(70, -2))
            grid.addView(line)
        }
        attackValue = text("0", 24f, Color.rgb(255,190,70), true)
        raidValue = text("0", 24f, Color.rgb(255,130,170), true)
        ghostValue = text("0", 24f, Color.rgb(80,210,255), true)
        counter("🔨  ATACURI", attackValue)
        counter("🐷  RAIDURI", raidValue)
        counter("👻  FANTOME", ghostValue)
        root.addView(grid)
        val start = Button(this).apply { text = "START"; setOnClickListener { running = true; status.text = "PORNIT · detectorul automat nu este încă activ" } }
        val pause = Button(this).apply { text = "PAUZĂ"; setOnClickListener { running = false; status.text = "PAUZĂ" } }
        val reset = Button(this).apply { text = "RESET"; setOnClickListener { spins = 0; attacks = 0; raids = 0; ghosts = 0; update() } }
        root.addView(start, LinearLayout.LayoutParams(-1, -2))
        root.addView(pause, LinearLayout.LayoutParams(-1, -2))
        root.addView(reset, LinearLayout.LayoutParams(-1, -2))
        val note = text("PROTOTIP: interfața și contoarele sunt pregătite. Capturarea ecranului și recunoașterea automată a celor 3 simboluri din rândul din mijloc nu sunt încă implementate.", 13f, Color.LTGRAY)
        note.setPadding(8, 22, 8, 8)
        root.addView(note)
        setContentView(root)
        update()
    }

    private fun update() {
        spinValue.text = spins.toString()
        attackValue.text = attacks.toString()
        raidValue.text = raids.toString()
        ghostValue.text = ghosts.toString()
        status.text = if (running) "PORNIT" else "OPRIT"
    }
}
