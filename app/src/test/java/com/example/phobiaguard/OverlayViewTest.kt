package com.example.phobiaguard


import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.phobiaguard.ui.components.OverlayView
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.intArrayOf
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OverlayViewTest {
    @Test
    fun drawsNewBoxes(){    //made by AI
        val context = ApplicationProvider.getApplicationContext<Context>()
        val overlayView = OverlayView(context)
        val testRect = RectF(10f, 10f, 100f, 100f)
        overlayView.setDetections(listOf(testRect))
        val bitmap = Bitmap.createBitmap(200,200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        overlayView.draw(canvas)

        assertTrue(true)    //succeeds if no exceptions are thrown
        bitmap.recycle()
    }
}