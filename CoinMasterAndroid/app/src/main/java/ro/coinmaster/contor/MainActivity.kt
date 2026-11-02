
package ro.coinmaster.contor

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
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
    private var captureStarted = false

    private val CAPTURE_REQUEST = 1001

    private lateinit var wm: WindowManager
    private var panel: LinearLayout? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private lateinit var spinValue: TextView
    private lateinit var attackValue: TextView
    private lateinit var raidValue: TextView
    private lateinit var ghostValue: TextView
    private lateinit var panelStatus: TextView
    private lateinit var pauseButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(20, 19, 38))
        }

        fun label(
            value: String,
            size: Float,
            color: Int = Color.WHITE
        ) = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }

        root.addView(label("COIN MASTER · CONTOR", 24f))

        val start = Button(this).apply {
            text = "START"
            setOnClickListener {
                startOverlay()
            }
        }

        val pause = Button(this).apply {
            text = "PAUZĂ"
            setOnClickListener {
                running = false
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
                updateOverlay()
            }
        }

        val calibrate = Button(this).apply {
            text = "CALIBRARE SIMBOLURI"
            setOnClickListener {
                startActivity(
                    Intent(
                        this@MainActivity,
                        CalibrationActivity::class.java
                    )
                )
            }
        }

        root.addView(start)
        root.addView(pause)
        root.addView(reset)
        root.addView(calibrate)

        root.addView(
            label(
                "Contoarele vor fi actualizate după adăugarea recunoașterii simbolurilor.",
                13f,
                Color.LTGRAY
            )
        )

        setContentView(root)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        showPanel()

        if (captureStarted) {
            running = true
            updateOverlay()
        } else {
            requestScreenCapture()
        }
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        startActivityForResult(
            manager.createScreenCaptureIntent(),
            CAPTURE_REQUEST
        )
    }

    @Deprecated("Legacy activity result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAPTURE_REQUEST) {

            if (resultCode == RESULT_OK && data != null) {

                val serviceIntent = Intent(
                    this,
                    ScreenCaptureService::class.java
                ).apply {
                    putExtra("resultCode", resultCode)
                    putExtra("data", data)
                }

                try {
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }

                    captureStarted = true
                    running = true
                    updateOverlay()

                    Toast.makeText(
                        this,
                        "Capturarea ecranului a pornit!",
                        Toast.LENGTH_LONG
                    ).show()

                    moveTaskToBack(true)

                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Eroare: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } else {
                running = false
                updateOverlay()

                Toast.makeText(
                    this,
                    "Ai refuzat capturarea ecranului.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showPanel() {
        if (panel != null) return

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(5), dp(7), dp(5))
            setBackgroundColor(Color.rgb(30, 28, 55))
        }

        val heading = TextView(this).apply {
            text = "COIN MASTER       ⠿"
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(3))
        }

        box.addView(heading)

        panelStatus = TextView(this).apply {
            text = "OPRIT"
            textSize = 9f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        box.addView(panelStatus)

        spinValue = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        box.addView(spinValue)

        val counters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        fun counterView(): TextView {
            return TextView(this).apply {
                textSize = 11f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(dp(3), 0, dp(3), 0)
            }
        }

        attackValue = counterView()
        raidValue = counterView()
        ghostValue = counterView()

        counters.addView(attackValue)
        counters.addView(raidValue)
        counters.addView(ghostValue)

        box.addView(counters)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        pauseButton = Button(this).apply {
            textSize = 9f
            minHeight = dp(28)
            minimumHeight = dp(28)
            setPadding(dp(5), 0, dp(5), 0)

            setOnClickListener {
                if (captureStarted) {
                    running = !running
                    updateOverlay()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Pornește captura din aplicația principală.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val close = Button(this).apply {
            text = "X"
            textSize = 9f
            minHeight = dp(28)
            minimumHeight = dp(28)
            setPadding(dp(5), 0, dp(5), 0)

            setOnClickListener {
                if (panel != null) {
                    wm.removeView(box)
                    panel = null
                    panelParams = null
                }
            }
        }

        buttons.addView(pauseButton)
        buttons.addView(close)

        box.addView(buttons)

        val params = WindowManager.LayoutParams(
            dp(190),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(20)
            y = dp(180)
        }

        heading.setOnTouchListener(
            object : View.OnTouchListener {

                var startX = 0
                var startY = 0
                var touchX = 0f
                var touchY = 0f

                override fun onTouch(
                    v: View,
                    event: MotionEvent
                ): Boolean {

                    when (event.action) {

                        MotionEvent.ACTION_DOWN -> {
                            startX = params.x
                            startY = params.y
                            touchX = event.rawX
                            touchY = event.rawY
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            params.x = startX +
                                (event.rawX - touchX).toInt()

                            params.y = startY +
                                (event.rawY - touchY).toInt()

                            wm.updateViewLayout(box, params)
                            return true
                        }
                    }

                    return true
                }
            }
        )

        wm.addView(box, params)

        panel = box
        panelParams = params

        updateOverlay()
    }

    private fun updateOverlay() {
        if (panel == null) return

        spinValue.text = "ROTIRI: $spins"
        attackValue.text = "🔨 $attacks"
        raidValue.text = "🐷 $raids"
        ghostValue.text = "🍦 $ghosts"

        panelStatus.text = when {
            running -> "PORNIT"
            captureStarted -> "PAUZĂ"
            else -> "OPRIT"
        }

        pauseButton.text = if (running) "PAUZĂ" else "START"
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
