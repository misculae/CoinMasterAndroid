
package ro.coinmaster.contor

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.*
import android.widget.LinearLayout.LayoutParams

class CalibrationActivity : Activity() {

    private lateinit var imageView: SelectionView
    private var selectedName = "ciocan"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        val title = TextView(this).apply {
            text = "CALIBRARE SIMBOLURI"
            textSize = 20f
            gravity = android.view.Gravity.CENTER
        }

        root.addView(title)

        val choices = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun choice(text: String, value: String) {
            choices.addView(
                Button(this).apply {
                    this.text = text
                    setOnClickListener {
                        selectedName = value
                        title.text = "Selectat: $text"
                    }
                },
                LayoutParams(
                    0,
                    LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }

        choice("🔨 Ciocan", "ciocan")
        choice("🐷 Porc", "porc")
        choice("🍦 Înghețată", "inghetata")

        root.addView(choices)

        val frame = CaptureStore.latestFrame

        if (frame == null || frame.isRecycled) {

            root.addView(TextView(this).apply {
                text = "Nu există încă o captură. Pornește jocul și captura, apoi încearcă din nou."
                textSize = 16f
            })

        } else {

            imageView = SelectionView(this, frame)

            root.addView(
                imageView,
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )

            root.addView(Button(this).apply {
                text = "SALVEAZĂ SIMBOLUL ÎNCADRAT"

                setOnClickListener {
                    val crop = imageView.getSelection()

                    if (crop == null) {

                        Toast.makeText(
                            this@CalibrationActivity,
                            "Încadrează mai întâi simbolul cu degetul.",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        CaptureStore.saveTemplate(
                            selectedName,
                            crop
                        )

                        crop.recycle()

                        Toast.makeText(
                            this@CalibrationActivity,
                            "Modelul $selectedName a fost memorat.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        }

        root.addView(Button(this).apply {
            text = "ÎNCHIDE"
            setOnClickListener {
                finish()
            }
        })

        setContentView(root)
    }

    private class SelectionView(
        context: android.content.Context,
        bitmap: Bitmap
    ) : View(context) {

        private val source = bitmap
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        private var startX = 0f
        private var startY = 0f
        private var endX = 0f
        private var endY = 0f

        private var selected = false
        private var scale = 1f
        private var offsetX = 0f
        private var offsetY = 0f

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val sx = width.toFloat() / source.width
            val sy = height.toFloat() / source.height

            scale = minOf(sx, sy)

            val drawW = source.width * scale
            val drawH = source.height * scale

            offsetX = (width - drawW) / 2f
            offsetY = (height - drawH) / 2f

            canvas.drawBitmap(
                source,
                null,
                RectF(
                    offsetX,
                    offsetY,
                    offsetX + drawW,
                    offsetY + drawH
                ),
                paint
            )

            if (selected) {
                paint.color = Color.GREEN
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f

                canvas.drawRect(
                    startX,
                    startY,
                    endX,
                    endY,
                    paint
                )

                paint.style = Paint.Style.FILL
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    endX = event.x
                    endY = event.y
                    selected = true
                    invalidate()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    endX = event.x
                    endY = event.y
                    invalidate()
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    endX = event.x
                    endY = event.y
                    invalidate()
                    return true
                }
            }

            return true
        }

        fun getSelection(): Bitmap? {
            if (!selected || scale <= 0f) return null

            val left = (
                (minOf(startX, endX) - offsetX) / scale
            ).toInt().coerceIn(0, source.width - 1)

            val top = (
                (minOf(startY, endY) - offsetY) / scale
            ).toInt().coerceIn(0, source.height - 1)

            val right = (
                (maxOf(startX, endX) - offsetX) / scale
            ).toInt().coerceIn(0, source.width)

            val bottom = (
                (maxOf(startY, endY) - offsetY) / scale
            ).toInt().coerceIn(0, source.height)

            val w = right - left
            val h = bottom - top

            if (w < 8 || h < 8) return null

            return Bitmap.createBitmap(
                source,
                left,
                top,
                w,
                h
            )
        }
    }
}
