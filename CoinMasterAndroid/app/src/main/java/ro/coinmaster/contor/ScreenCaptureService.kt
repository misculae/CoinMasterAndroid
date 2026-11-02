
package ro.coinmaster.contor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import android.util.DisplayMetrics
import android.os.Handler
import android.os.Looper
import android.util.Log

class ScreenCaptureService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var lastFrame: Bitmap? = null
    private var stableFrames = 0
    private var lastAnalysis = 0L

    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (projection != null) return START_NOT_STICKY

        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0

        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra<Intent>("data")
        }

        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotification()
        startForeground(1, notification())

        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        projection = manager.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()

        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.getRealMetrics(metrics)

        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            android.graphics.PixelFormat.RGBA_8888,
            2
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()

            if (image != null) {
                try {
                    analyzeFrame(image)
                } catch (e: Exception) {
                    Log.e("CoinMaster", "Analiza: ${e.message}")
                } finally {
                    image.close()
                }
            }
        }, null)

        projection?.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    stopSelf()
                }
            },
            handler
        )

        virtualDisplay = projection?.createVirtualDisplay(
            "CoinMasterCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )

        return START_NOT_STICKY
    }

    private fun analyzeFrameCaptureStore.updateFrame(current)(image: Image) {
        val now = System.currentTimeMillis()

        if (now - lastAnalysis < 300) return
        lastAnalysis = now

        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride

        val width = image.width
        val height = image.height

        val rowPadding = rowStride - pixelStride * width
        val bitmapWidth = width + rowPadding / pixelStride

        val full = Bitmap.createBitmap(
            bitmapWidth,
            height,
            Bitmap.Config.ARGB_8888
        )

        full.copyPixelsFromBuffer(buffer)

        val current = Bitmap.createBitmap(
            full, 0, 0, width, height
        )

        full.recycle()

        // Zona centrala: doar pentru analiza si calibrare.
        val left = (width * 0.05f).toInt()
        val top = (height * 0.25f).toInt()
        val cropWidth = (width * 0.90f).toInt()
        val cropHeight = (height * 0.50f).toInt()

        val center = Bitmap.createBitmap(
            current,
            left,
            top,
            cropWidth,
            cropHeight
        )

        current.recycle()

        val previous = lastFrame
        lastFrame = center

        if (previous != null) {
            val same = compareFrames(previous, center)

            if (same) {
                stableFrames++
            } else {
                stableFrames = 0
            }

            previous.recycle()

            if (stableFrames >= 3) {
                Log.d(
                    "CoinMaster",
                    "Imagine stabila. Zona centrala pregatita pentru recunoastere."
                )
            }
        }
    }

    private fun compareFrames(a: Bitmap, b: Bitmap): Boolean {
        if (a.width != b.width || a.height != b.height) {
            return false
        }

        var differences = 0
        val stepX = maxOf(1, a.width / 30)
        val stepY = maxOf(1, a.height / 30)

        var samples = 0

        for (y in 0 until a.height step stepY) {
            for (x in 0 until a.width step stepX) {
                val ca = a.getPixel(x, y)
                val cb = b.getPixel(x, y)

                val diff =
                    kotlin.math.abs(android.graphics.Color.red(ca) -
                        android.graphics.Color.red(cb)) +
                    kotlin.math.abs(android.graphics.Color.green(ca) -
                        android.graphics.Color.green(cb)) +
                    kotlin.math.abs(android.graphics.Color.blue(ca) -
                        android.graphics.Color.blue(cb))

                if (diff > 45) differences++
                samples++
            }
        }

        return samples > 0 && differences < samples * 0.03
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "cm_capture",
                "Coin Master captura",
                NotificationManager.IMPORTANCE_LOW
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun notification(): Notification {
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, "cm_capture")
                .setContentTitle("Coin Master Contor")
                .setContentText("Analiza ecranului este activa")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Coin Master Contor")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .build()
        }
    }

    override fun onDestroy() {
        lastFrame?.recycle()
        lastFrame = null
        virtualDisplay?.release()
        imageReader?.close()
        projection?.stop()
        projection = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
