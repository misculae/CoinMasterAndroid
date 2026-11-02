
package ro.coinmaster.contor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import android.graphics.Bitmap
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val savedFirstImage = AtomicBoolean(false)

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
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
                    if (savedFirstImage.compareAndSet(false, true)) {
                        saveFirstImage(image)
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "CoinMaster",
                        "Eroare captură: ${e.message}"
                    )
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
            null
        )

        virtualDisplay = projection?.createVirtualDisplay(
            "CoinMasterCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        return START_NOT_STICKY
    }

    private fun saveFirstImage(image: Image) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val width = image.width
        val height = image.height

        val rowPadding = rowStride - pixelStride * width
        val bitmapWidth = width + rowPadding / pixelStride

        val fullBitmap = Bitmap.createBitmap(
            bitmapWidth,
            height,
            Bitmap.Config.ARGB_8888
        )

        fullBitmap.copyPixelsFromBuffer(buffer)

        val bitmap = Bitmap.createBitmap(
            fullBitmap,
            0,
            0,
            width,
            height
        )

        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "CoinMaster_calibrare.jpg"
            )
            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            )
            if (Build.VERSION.SDK_INT >= 29) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/CoinMaster"
                )
            }
        }

        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        if (uri != null) {
            val output: OutputStream? =
                contentResolver.openOutputStream(uri)

            output?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }

            android.util.Log.i(
                "CoinMaster",
                "Captură salvată: $uri"
            )
        }

        fullBitmap.recycle()
        bitmap.recycle()
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "cm_capture",
                "Coin Master captură",
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
                .setContentText("Capturarea ecranului este activă")
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
        virtualDisplay?.release()
        imageReader?.close()
        projection?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
