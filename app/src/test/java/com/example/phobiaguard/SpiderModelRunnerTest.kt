package com.example.phobiaguard

import android.graphics.Bitmap
import android.util.Log
import com.example.phobiaguard.ui.settings.InferenceInfo
import com.example.phobiaguard.ui.settings.LetterboxTransform
import com.example.phobiaguard.ui.settings.PreparedImage
import com.example.phobiaguard.ui.settings.SpiderModelResultDialog
import com.example.phobiaguard.ui.settings.SpiderModelRunner
import com.example.phobiaguard.ui.settings.SpiderModelRunner.Companion.decodeDetections
import com.example.phobiaguard.ui.settings.SpiderModelRunner.Companion.preprocessBitmap
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpiderModelRunnerTest {
    @Test
    fun `prepared image compares tensor arrays by content`() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        try {
            val first = PreparedImage(
                bitmap,
                floatArrayOf(1f, 2f),
                LetterboxTransform(scale = 1f, padX = 0, padY = 0)
            )
            val sameContent = first.copy(input = floatArrayOf(1f, 2f))

            assertEquals(first, sameContent)
            assertEquals(first.hashCode(), sameContent.hashCode())
            assertNotEquals(first, first.copy(input = floatArrayOf(1f, 3f)))
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun `inference info compares tensor arrays by content`() {
        val first = InferenceInfo(
            output = floatArrayOf(0.1f, 0.9f),
            durationMs = 10L,
            modelLoadDurationMs = 5L,
            backend = "CPU",
            fallbackReason = null
        )
        val sameContent = first.copy(output = floatArrayOf(0.1f, 0.9f))

        assertEquals(first, sameContent)
        assertEquals(first.hashCode(), sameContent.hashCode())
        assertNotEquals(first, first.copy(output = floatArrayOf(0.1f, 0.8f)))
    }

    //positive test,
    @Test
    fun `preprocess input array is correct size`(){
        val bitmap: Bitmap = Bitmap.createBitmap(400,400, Bitmap.Config.ARGB_8888)// created with help from AI

        val result = preprocessBitmap(bitmap)
        //the size of preprocessed bitmap should equal to 3*640*640 (according to AI)
        assert(result.input.size == 3*640*640)
        bitmap.recycle()
    }
    @Test
    fun decodeDetections_LowConfidence(){
        SpiderModelRunner.setConfidenceThreshold(5)
        val dummyOutput = FloatArray(SpiderModelRunner.OUTPUT_SIZE) { 0f }  //AI suggestion
        val dummyTransform = LetterboxTransform(scale = 1f, padX = 0, padY = 0) //AI Suggestion

        // 2. Decode detections
        val result = decodeDetections(
            output = dummyOutput,
            transform = dummyTransform,
            originalWidth = 400,
            originalHeight = 400
        )

        assertTrue(result.isEmpty())
    }
    @Test
    fun decodeDetections_HighConfidence(){  //Test made with help from AI
        SpiderModelRunner.setConfidenceThreshold(5)
        val dummyOutput = FloatArray(SpiderModelRunner.OUTPUT_SIZE) { 0f }
        val stride = SpiderModelRunner.CANDIDATE_COUNT // 8400
        dummyOutput[0] = 0.5f             // centerX (normalized 0.0 - 1.0)
        dummyOutput[stride] = 0.5f        // centerY
        dummyOutput[2 * stride] = 0.2f    // width
        dummyOutput[3 * stride] = 0.2f    // height
        dummyOutput[4 * stride] = 0.9f    // confidence score (90%)

        val dummyTransform = LetterboxTransform(scale = 1f, padX = 0, padY = 0)

        // 2. Decode detections
        val result = decodeDetections(
            output = dummyOutput,
            transform = dummyTransform,
            originalWidth = 400,
            originalHeight = 400
        )

        assertEquals(1, result.size)
        assertEquals(0.9f, result[0].confidence, 0.01f)

    }
}
