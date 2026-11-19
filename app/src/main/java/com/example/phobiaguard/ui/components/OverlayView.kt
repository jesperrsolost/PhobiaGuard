package com.example.phobiaguard.ui.components
//AI generated overlayView example
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boxPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.FILL

    }

    private val boxes = mutableListOf<RectF>()

    /**
     * Sets up the overlay boxes that are drawn
     *
     * @param newBoxes - boxes that should be drawn on the next onDraw
     */
    fun setDetections(newBoxes: List<RectF>) {
        boxes.clear()
        boxes.addAll(newBoxes)
        invalidate() // Trigger redraw on UI thread
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (rect in boxes) {
            canvas.drawRect(rect, boxPaint)
        }
    }
}