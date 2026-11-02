package ro.coinmaster.contor

import android.graphics.Bitmap

object CaptureStore {
    @Volatile
    var latestFrame: Bitmap? = null

    private val templates = mutableMapOf<String, Bitmap>()

    @Synchronized
    fun updateFrame(bitmap: Bitmap) {
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val old = latestFrame
        latestFrame = copy
        old?.recycle()
    }

    @Synchronized
    fun saveTemplate(name: String, bitmap: Bitmap) {
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        templates[name]?.recycle()
        templates[name] = copy
    }

    @Synchronized
    fun hasTemplate(name: String): Boolean = templates.containsKey(name)

    @Synchronized
    fun getTemplate(name: String): Bitmap? = templates[name]

    @Synchronized
    fun clear() {
        latestFrame?.recycle()
        latestFrame = null
        templates.values.forEach { it.recycle() }
        templates.clear()
    }
}
