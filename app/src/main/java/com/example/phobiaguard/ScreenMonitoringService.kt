
package com.example.phobiaguard


import android.app.Activity.RESULT_OK
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjection.Callback
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import com.example.phobiaguard.ui.components.OverlayView
import com.example.phobiaguard.ui.settings.SpiderDetection
import com.example.phobiaguard.ui.settings.SpiderModelRunner
import com.example.phobiaguard.ui.screens.MonitoringScreen
import com.example.phobiaguard.ui.screens.MonitoringState

const val TAG = "ScreenMonitoringService"

class ScreenMonitoringService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val stopTask = Runnable { stopSelf() }
    private var mediaProjection: MediaProjection?=null
    private var virtualDisplay: VirtualDisplay?=null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private lateinit var spiderModelRunner: SpiderModelRunner
    private lateinit var windowlayout: LayoutParams
    private lateinit var windowmanager: WindowManager
    private var lastDetectionTimestamp = 0L
    private var lastPeekTime = 0L
    private val HOLD_OVERLAY_DURATION_MS = 1500L // 1.5 seconds hold
    private val PEEK_INTERVAL_MS = 1500L         // Peek every 1.5 seconds
    private var isPeeking = false
    private var lastActiveBox = RectF()
    //AI suggested values
    private var overlayView: View? = null
    private var isOverlayAttached = false
    private var lastInferenceTime = 0L
    private val INFERENCE_INTERVAL_MS = 100L // Process 10 frames per second

    /**
     * Throttle function - limits number of frames read per second
     * Skips frame when difference between current and previous frame is less than
     * INFERENCE_INTERVAL_MS
     *
     * @param image current frame being read
     * @see processImage checks if image contains any spiders
     */
    private fun onFrameReceived(image: Image) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastInferenceTime < INFERENCE_INTERVAL_MS) {
            // Skip this frame to throttle execution
            Log.d("PhobiaGuard", "Frame skipped")
            image.close()
            return
        }

        lastInferenceTime = currentTime
        processImage(image) //processes image on valid frame
    }

    /**
     * Initializes virtual display to capture the current frame
     *
     * @param width frame width
     * @param height frame height
     * @param density pixel density in a region
     * @exception Exception onFrameReceived fails to accept image
     * @see onFrameReceived passes latest image from imageReader to be processed or skipped
     */
    private fun setupVirtualDisplay(width: Int, height: Int, density: Int) {

        imageReader = ImageReader.newInstance(width,height, PixelFormat.RGBA_8888,2)
        imageReader?.setOnImageAvailableListener({reader->
            val image = reader.acquireLatestImage()
            if(image != null){
                try{
                    onFrameReceived(image)
                } catch (e: Exception){
                    Log.e(TAG,"Image is invalid, ${e.toString()}")
                } finally {
                    image.close()
                }
            }
        },backgroundHandler)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenMonitor",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,null
        )
    }

    private val mediaProjectionCallback = object : Callback(){
        override  fun onStop(){
            super.onStop()
            Log.d(TAG,"Mediaprojection stopped")
            stopSelf()
        }
    }

    /**
     * Image processing
     * Transforms raw image into a bitmap which ML model decodes and retrieves results.
     * Results are handled in handleSpider function
     *
     * @param image - raw image captured on a valid frame
     */
    private fun processImage(image: Image){ //Made with use of AI
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = createBitmap(image.width + rowPadding / pixelStride, image.height)
        bitmap.copyPixelsFromBuffer(buffer)
        //AI solution to remove padding
        val cleanBitmap = Bitmap.createBitmap(bitmap,0,0,image.width,image.height)
        bitmap.recycle()


        try {
            val preparedImage = SpiderModelRunner.preprocessBitmap(cleanBitmap)
            val inferenceInfo = spiderModelRunner.run(preparedImage.input)
            Log.d("PhobiaGuard", "Decoding")
            val result = SpiderModelRunner.decodeDetections(
                inferenceInfo.output, preparedImage.transform,
                preparedImage.bitmap.width, preparedImage.bitmap.height
            )
            handleSpider(result)
        }finally {
            cleanBitmap.recycle()
        }
    }

    /**
     * Handles detections, responsible for creating censor box if spiders are detected
     *
     * @param detection List containing all results from processImage
     * @see removeOverlay removes overlay when service no longer detects spiders on screen
     * @see OverlayView.setDetections builds overlay boxes based on confirmed detections
     */
    private fun handleSpider(detection: List<SpiderDetection>){
        //AI made scaling

        val metrics =DisplayMetrics()
        val currentTime = System.currentTimeMillis()
        @Suppress("DEPRECATION")
        windowmanager.defaultDisplay.getRealMetrics(metrics)

        val width =metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        val capturedWidth = imageReader?.width?.toFloat() ?: width
        val capturedHeight = imageReader?.height?.toFloat() ?: height

        val scaleX = metrics.widthPixels.toFloat() / capturedWidth
        val scaleY = metrics.heightPixels.toFloat() / capturedHeight
        val padding = 20f
        //AI, peek check to confirm if spider still exists
        if (!isPeeking && currentTime - lastPeekTime > PEEK_INTERVAL_MS) {
            isPeeking = true
            lastPeekTime = currentTime
            handler.post { removeOverlay() }
            return
        }
        if (detection.isNotEmpty()){ //Rewrote based on AI example for proposed improvement
            isPeeking = false
            lastDetectionTimestamp = currentTime

            val rectList= RectF()
            detection.forEachIndexed { index, s->
                val rect = RectF(
                    (s.left*scaleX-padding).coerceAtLeast(0f),
                    (s.top*scaleY-padding).coerceAtLeast(0f),
                    (s.right*scaleX+padding).coerceAtMost(width),
                    (s.bottom*scaleY+padding).coerceAtMost(height)
                )
                if (index==0) rectList.set(rect) else rectList.union(rect)
            }
            lastActiveBox = rectList
            handler.post {
                windowlayout.x = lastActiveBox.left.toInt()  //AI, sets window layout to be equal to detection area
                windowlayout.y=lastActiveBox.top.toInt()
                windowlayout.width=lastActiveBox.width().toInt().coerceAtLeast(1)
                windowlayout.height=lastActiveBox.width().toInt().coerceAtLeast(1)
                val localRect = RectF(0f, 0f, lastActiveBox.width(), rectList.height())
                (overlayView as? OverlayView)?.setDetections(listOf(localRect))

                if (!isOverlayAttached) {
                    windowmanager.addView(overlayView, windowlayout)
                    isOverlayAttached = true
                } else {
                    windowmanager.updateViewLayout(overlayView, windowlayout)
                }
                Log.d("PhobiaGuard", "Spider detected")

            }
        } else {
            //AI solution
            if (isPeeking) {
                isPeeking = false
                handler.post {
                    removeOverlay()
                    lastActiveBox.setEmpty()
                }
            } else if (currentTime - lastDetectionTimestamp < HOLD_OVERLAY_DURATION_MS) {
                // Normal hold: keep box solid in place
                handler.post {
                    if (isOverlayAttached && lastActiveBox.width() > 0f) {
                        windowlayout.x = lastActiveBox.left.toInt()
                        windowlayout.y = lastActiveBox.top.toInt()
                        windowlayout.width = lastActiveBox.width().toInt().coerceAtLeast(1)
                        windowlayout.height = lastActiveBox.height().toInt().coerceAtLeast(1)
                        windowmanager.updateViewLayout(overlayView, windowlayout)
                    }
                }
            }
            else
            handler.post {
                (overlayView as? OverlayView)?.setDetections(emptyList())
                removeOverlay()
                Log.d("PhobiaGuard", "No spiders")
            }
        }
    }

    /**
     * Sets up service when first created
     *
     * @see OverlayView - connects overlay class to the service
     */
    override fun onCreate() {
        super.onCreate()

        spiderModelRunner = SpiderModelRunner(this)
        windowmanager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowlayout = LayoutParams(    //creates default window layout
            400,
            400,
            LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                alpha = 1.0f
        }
        overlayView = OverlayView(this).apply {    //AI suggested
            setBackgroundColor(Color.TRANSPARENT)
        }
        val channel = NotificationChannel(
            "monitoring_channel",
            "PhobiaGuard Monitoring",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(
            NotificationManager::class.java
        )
        manager.createNotificationChannel(channel)
        backgroundThread = HandlerThread("ImageReaderThread").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    /**
     * removes overlay when service finishes or spiders are no longer detected
     *
     * @exception IllegalArgumentException - there is no overlay to remove
     */
    fun removeOverlay(){
        if(isOverlayAttached && overlayView != null){
            try {
                windowmanager.removeView(overlayView)
            }catch (e: IllegalArgumentException){
                Log.w("PhobiaGuard", "Already removed", e)
            }finally {
                isOverlayAttached = false
            }
        }
    }

    /**
     * Triggered on service start, verifies that service has permissions
     * and starts foreground service
     *
     * @param intent How service is to be used and if intent is granted
     * @see startScreenCapture builds the screenCapture components of the service
     */
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val resultCode = intent?.getIntExtra("resultCode",-1) ?: -1
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { //AI
            intent?.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("data")
        }

        // notification gotten on phone, when server is running
        val notification = NotificationCompat.Builder(
            this,
            "monitoring_channel"
        )
            .setContentTitle("PhobiaGuard")
            .setContentText("Foreground Service running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(1, notification)
        }
        if (resultCode == RESULT_OK && data != null) {
            val mediaProjectionManager = getSystemService(MediaProjectionManager::class.java)
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode,data)

            if(mediaProjection != null)
                startScreenCapture()
        }

        handler.removeCallbacks(stopTask)
        handler.postDelayed(stopTask, 360_000L)     //automatically stops service

        return START_NOT_STICKY
    }

    /**
     * Registers media projection callback, allowing application to
     * start capturing the screen
     *
     * @see setupVirtualDisplay - creates the virtual display used to capture the screen
     */
    fun startScreenCapture(){

        mediaProjection?.registerCallback(mediaProjectionCallback,handler)

        val metrics = resources.displayMetrics
        //AI suggested fix on invalid dimensions
        val width = (metrics.widthPixels / 2).coerceAtLeast(1)
        val height = (metrics.heightPixels / 2).coerceAtLeast(1)
        setupVirtualDisplay(width, height, metrics.densityDpi)
    }

    /**
     * Stops service and all active components to prevent memory leaks
     *
     * @exception InterruptedException backroundThread is waiting for frames to finish
     * @see removeOverlay removes stored overlay
     */
    override fun onDestroy() {
        handler.removeCallbacks(stopTask)

        //AI suggested clean up
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(500) // Wait for pending frames to finish
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        spiderModelRunner.close()
        removeOverlay()  //required to prevent memory leak
        MonitoringState.updateMonitoring(false) //ensures monitoring state is false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null


}


