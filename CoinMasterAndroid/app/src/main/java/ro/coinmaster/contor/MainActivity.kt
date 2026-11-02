
package ro.coinmaster.contor

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var spins = 0
    private var attacks = 0
    private var raids = 0
    private var ghosts = 0
    private var running = false

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: LinearLayout? = null
    private var overlayAdded = false

    private lateinit var status: TextView
    private lateinit var spinValue: TextView
    private lateinit var attackValue: TextView
    private lateinit var raidValue: TextView
    private lateinit var ghostValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(20, 19, 38))
        }

        fun label(value: String, size: Float, color: Int = Color.WHITE) =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                setPadding(8, 8, 8, 8)
            }

        root.addView(label("COIN MASTER · CONTOR", 24f))

        status = label("OPRIT", 16f, Color.LTGRAY)
        root.addView(status)

        spinValue = label("0", 42f)
        root.addView(label("ROTIRI TOTALE", 14f, Color.LTGRAY))
        root.addView(spinValue)

        attackValue = label("🔨 Atacuri: 0", 20f)
        raidValue = label("🐷 Raiduri: 0", 20f)
        ghostValue = label("👻 Fantome: 0", 20f)

        root.addView(attackValue)
        root.addView(raidValue)
        root.addView(ghostValue)

        val start = Button(this).apply {
            text = "START"
            setOnClickListener { startOverlay() }
        }

        val pause = Button(this).apply {
            text = "PAUZĂ"
            setOnClickListener {
                running = false
                update()
                updateOverlay()
            }
        }

        val reset = Button(this).apply {
            text = "RESET"
            setOnClickListener {
                spins = 0
                attacks = 0
                raids = 0
                ghosts = 0
                update()
                updateOverlay()
            }
        }

        root.addView(start)
        root.addView(pause)
        root.addView(reset)

        root.addView(label(
            "Fereastra plutitoare este pregătită. " +
            "Detectorul automat al simbolurilor nu este încă implementat.",
            13f, Color.LTGRAY
        ))

        setContentView(root)
        update()
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(
                this,
                "Activează Permite afișarea peste alte aplicații.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        running = true
        update()
        showBubble()

        Toast.makeText(this, "Contorul plutitor este pornit", Toast.LENGTH_SHORT).show()

        moveTaskToBack(true)
    }

    private fun showBubble() {
        if (overlayAdded) return

        val bubbleView = TextView(this).apply {
            text = "CM"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(100, 65, 190))
            setOnClickListener { togglePanel() }
        }

        val params = WindowManager.LayoutParams(
            58,
            58,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 250
        }

        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f
            var moved = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = params.x
                        startY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        moved = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()

                        if (kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5) {
                            moved = true
                        }

                        params.x = startX + dx
                        params.y = startY + dy
                        wm.updateViewLayout(bubbleView, params)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!moved) togglePanel()
                        return true
                    }
                }
                return false
            }
        })

        wm.addView(bubbleView, params)
        bubble = bubbleView
        overlayAdded = true
    }

    private fun togglePanel() {
        if (panel != null) {
            wm.removeView(panel)
            panel = null
            return
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            setBackgroundColor(Color.rgb(30, 28, 55))
        }

        fun item(text: String, size: Float = 17f) =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.WHITE)
                setPadding(8, 8, 8, 8)
            }

        val heading = item("COIN MASTER · CONTOR", 18f)
        heading.setTypeface(null, Typeface.BOLD)
        box.addView(heading)

        box.addView(item("Rotiri: $spins"))
        box.addView(item("🔨 Atacuri: $attacks"))
        box.addView(item("🐷 Raiduri: $raids"))
        box.addView(item("👻 Fantome: $ghosts"))

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val pause = Button(this).apply {
            text = if (running) "PAUZĂ" else "START"
            setOnClickListener {
                running = !running
                update()
                updateOverlay()
            }
        }

        val close = Button(this).apply {
            text = "ÎNCHIDE"
            setOnClickListener {
                wm.removeView(box)
                panel = null
            }
        }

        buttons.addView(pause, LinearLayout.LayoutParams(0, -2, 1f))
        buttons.addView(close, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(buttons)

        val params = WindowManager.LayoutParams(
            290,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 180
        }

        wm.addView(box, params)
        panel = box
    }

    private fun update() {
        spinValue.text = spins.toString()
        attackValue.text = "🔨 Atacuri: $attacks"
        raidValue.text = "🐷 Raiduri: $raids"
        ghostValue.text = "👻 Fantome: $ghosts"
        status.text = if (running) "PORNIT" else "PAUZĂ"
    }

    private fun updateOverlay() {
        if (panel != null) {
            wm.removeView(panel)
            panel = null
            togglePanel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Fereastra plutitoare rămâne atașată cât timp procesul aplicației există.
    }
}
