package com.example.phobiaguard.ui.settings

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import androidx.core.graphics.createBitmap
import com.example.phobiaguard.data.UserSettingsRepository
import com.example.phobiaguard.data.local.AppDatabase
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A spider detection box in image coordinates and its confidence score. */
internal data class SpiderDetection(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float
)

/** Scale and padding used to map coordinates between an image and the model input. */
internal data class LetterboxTransform(
    val scale: Float,
    val padX: Int,
    val padY: Int
)

/** An oriented source bitmap and its letterboxed model input. */
internal data class PreparedImage(
    val bitmap: Bitmap,
    val input: FloatArray,
    val transform: LetterboxTransform
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is PreparedImage &&
            bitmap == other.bitmap &&
            input.contentEquals(other.input) &&
            transform == other.transform)

    override fun hashCode(): Int {
        var result = bitmap.hashCode()
        result = 31 * result + input.contentHashCode()
        result = 31 * result + transform.hashCode()
        return result
    }
}

/** Model initialization time and any reason a fallback backend was selected. */
internal data class ModelLoadInfo(
    val durationMs: Long,
    val fallbackReason: String?
)

/** Raw model output, timing, backend, and fallback details for one inference. */
internal data class InferenceInfo(
    val output: FloatArray,
    val durationMs: Long,
    val modelLoadDurationMs: Long,
    val backend: String,
    val fallbackReason: String?
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is InferenceInfo &&
            output.contentEquals(other.output) &&
            durationMs == other.durationMs &&
            modelLoadDurationMs == other.modelLoadDurationMs &&
            backend == other.backend &&
            fallbackReason == other.fallbackReason)

    override fun hashCode(): Int {
        var result = output.contentHashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + modelLoadDurationMs.hashCode()
        result = 31 * result + backend.hashCode()
        result = 31 * result + (fallbackReason?.hashCode() ?: 0)
        return result
    }
}

/** Annotated detections and performance details for a processed image. */
internal data class SpiderModelResult(
    val annotatedImage: Bitmap,
    val detectionCount: Int,
    val backend: String,
    val fallbackReason: String?,
    val modelLoadMs: Long,
    val preprocessingMs: Long,
    val inferenceMs: Long,
    val postprocessingMs: Long,
    val totalMs: Long,
    val inputWidth: Int,
    val inputHeight: Int
)

/** Runs the bundled one-class YOLO model, using GPU inference with CPU fallback when needed.
 *
 * Images are resized and letterboxed to the model input, then detections are mapped back to the
 * source image and returned with an annotated bitmap and timing details.
 *
 * @param context context used to access the model asset and persisted settings.
 */
internal class SpiderModelRunner(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private var session: ModelSession? = null
    private var loadInfo = ModelLoadInfo(durationMs = 0L, fallbackReason = null)

    private val serviceScope = CoroutineScope(SupervisorJob()+ Dispatchers.IO)

    /** Loads the model once, preferring GPU and falling back to CPU if GPU setup fails. */
    fun prepareModel(): ModelLoadInfo {
        if (session != null) return loadInfo.copy(durationMs = 0L)

        val startedAt = System.nanoTime()
        try {
            val dao = AppDatabase.getDatabase(appContext).userSettingsDao()
            val repository = UserSettingsRepository(dao)
            serviceScope.launch {
                repository.settings.collect {
                        userSettings ->
                    setConfidenceThreshold(userSettings.intensity)   //updates confidence threshold
                }
            }

            session = createSession(Accelerator.GPU)
            loadInfo = ModelLoadInfo(elapsedMillis(startedAt), null)
        } catch (gpuFailure: Exception) {
            session?.close()
            session = null
            session = createSession(Accelerator.CPU)
            loadInfo = ModelLoadInfo(
                elapsedMillis(startedAt),
                "GPU setup failed; using CPU (${gpuFailure.message ?: gpuFailure.javaClass.simpleName})."
            )
        } catch (gpuFailure: LinkageError) {
            session?.close()
            session = null
            session = createSession(Accelerator.CPU)
            loadInfo = ModelLoadInfo(
                elapsedMillis(startedAt),
                "GPU setup failed; using CPU (${gpuFailure.message ?: gpuFailure.javaClass.simpleName})."
            )
        }
        return loadInfo
    }
    /** Runs inference on a normalized RGB input tensor, falling back to CPU after GPU failure. */
    fun run(input: FloatArray): InferenceInfo {
        prepareModel()
        val current = checkNotNull(session)
        val startedAt = System.nanoTime()
        return try {
            InferenceInfo(
                output = current.run(input),
                durationMs = elapsedMillis(startedAt),
                modelLoadDurationMs = 0L,
                backend = current.backend,
                fallbackReason = loadInfo.fallbackReason
            )
        } catch (gpuFailure: Exception) {
            if (current.backend != "GPU") throw gpuFailure
            fallbackAndRun(input, elapsedMillis(startedAt), gpuFailure)
        } catch (gpuFailure: LinkageError) {
            if (current.backend != "GPU") throw gpuFailure
            fallbackAndRun(input, elapsedMillis(startedAt), gpuFailure)
        }
    }

    /** Decodes [uri], applies its EXIF orientation, and runs detection on the image. */
    fun runUri(resolver: ContentResolver, uri: Uri): SpiderModelResult =
        runPreparedImage(prepareImage = { decodeAndPreprocess(resolver, uri) })

    /** Takes ownership of [bitmap] and releases it after producing the annotated result. */
    fun runBitmap(bitmap: Bitmap): SpiderModelResult = runPreparedImage(
        prepareImage = { preprocessBitmap(bitmap) },
        bitmapToRecycleOnFailure = bitmap
    )

    private fun runPreparedImage(
        prepareImage: () -> PreparedImage,
        bitmapToRecycleOnFailure: Bitmap? = null
    ): SpiderModelResult {
        val totalStartedAt = System.nanoTime()
        var prepared: PreparedImage? = null
        try {
            val initialLoad = prepareModel()

            val preprocessingStartedAt = System.nanoTime()
            val preparedImage = prepareImage()
            prepared = preparedImage
            val preprocessingMs = elapsedMillis(preprocessingStartedAt)
            val inputWidth = preparedImage.bitmap.width
            val inputHeight = preparedImage.bitmap.height

            val inference = run(preparedImage.input)

            val postprocessingStartedAt = System.nanoTime()
            val detections = decodeDetections(
                output = inference.output,
                transform = preparedImage.transform,
                originalWidth = inputWidth,
                originalHeight = inputHeight
            )
            val annotated = annotate(preparedImage.bitmap, detections)
            val postprocessingMs = elapsedMillis(postprocessingStartedAt)

            return SpiderModelResult(
                annotatedImage = annotated,
                detectionCount = detections.size,
                backend = inference.backend,
                fallbackReason = inference.fallbackReason ?: initialLoad.fallbackReason,
                modelLoadMs = initialLoad.durationMs + inference.modelLoadDurationMs,
                preprocessingMs = preprocessingMs,
                inferenceMs = inference.durationMs,
                postprocessingMs = postprocessingMs,
                totalMs = elapsedMillis(totalStartedAt),
                inputWidth = inputWidth,
                inputHeight = inputHeight
            )
        } finally {
            prepared?.bitmap?.recycle() ?: bitmapToRecycleOnFailure?.recycle()
        }
    }

    private fun fallbackAndRun(
        input: FloatArray,
        failedGpuAttemptMs: Long,
        gpuFailure: Throwable
    ): InferenceInfo {
        val loadStartedAt = System.nanoTime()
        session?.close()
        session = null
        session = createSession(Accelerator.CPU)
        val fallbackLoadMs = elapsedMillis(loadStartedAt)
        val reason = "GPU inference failed; using CPU (${gpuFailure.message ?: gpuFailure.javaClass.simpleName})."
        loadInfo = ModelLoadInfo(loadInfo.durationMs + fallbackLoadMs, reason)

        val cpu = checkNotNull(session)
        val cpuInferenceStartedAt = System.nanoTime()
        return InferenceInfo(
            output = cpu.run(input),
            durationMs = failedGpuAttemptMs + elapsedMillis(cpuInferenceStartedAt),
            modelLoadDurationMs = fallbackLoadMs,
            backend = cpu.backend,
            fallbackReason = reason
        )
    }

    private fun createSession(accelerator: Accelerator): ModelSession {
        val model = CompiledModel.create(
            appContext.assets,
            MODEL_ASSET,
            CompiledModel.Options(accelerator)
        )
        var inputBuffers: List<TensorBuffer>? = null
        var outputBuffers: List<TensorBuffer>? = null
        try {
            inputBuffers = model.createInputBuffers()
            outputBuffers = model.createOutputBuffers()
            return ModelSession(
                model = model,
                inputBuffers = inputBuffers,
                outputBuffers = outputBuffers,
                backend = if (accelerator == Accelerator.GPU) "GPU" else "CPU"
            )
        } catch (failure: Throwable) {
            inputBuffers?.forEach { it.close() }
            outputBuffers?.forEach { it.close() }
            model.close()
            throw failure
        }
    }

    /** Releases the active model session and its input and output buffers. */
    override fun close() {
        session?.close()
        session = null
    }

    private class ModelSession(
        private val model: CompiledModel,
        private val inputBuffers: List<TensorBuffer>,
        private val outputBuffers: List<TensorBuffer>,
        val backend: String
    ) : Closeable {
        fun run(input: FloatArray): FloatArray {
            require(input.size == INPUT_SIZE) { "Expected $INPUT_SIZE model input values." }
            inputBuffers.first().writeFloat(input)
            model.run(inputBuffers, outputBuffers)
            val output = outputBuffers.first().readFloat()
            require(output.size == OUTPUT_SIZE) {
                "Unexpected model output size ${output.size}; expected $OUTPUT_SIZE."
            }
            return output
        }

        override fun close() {
            closeWithUse(
                resources = listOf<AutoCloseable>(model) +
                    outputBuffers.asReversed() +
                    inputBuffers.asReversed()
            ) {}
        }
    }

    companion object {
        const val MODEL_ASSET = "spdr-1.litert"
        const val IMAGE_SIZE = 640
        const val CANDIDATE_COUNT = 8400
        const val INPUT_SIZE = 3 * IMAGE_SIZE * IMAGE_SIZE
        const val OUTPUT_SIZE = 5 * CANDIDATE_COUNT
        private const val MAX_DECODED_SIDE = 2048
        private var CONFIDENCE_THRESHOLD = 0.25f    //threshold is dependent on intensity level
        private const val IOU_THRESHOLD = 0.7f
        private const val MAX_DETECTIONS = 300

        /** Decodes an image [uri], applies its orientation, and prepares it for inference.
         *
         * @param resolver opens the image content and its EXIF metadata.
         * @param uri image content to decode.
         * @return the oriented source bitmap, normalized model input, and coordinate transform.
         */
        fun decodeAndPreprocess(resolver: ContentResolver, uri: Uri): PreparedImage {
            val oriented = decodeOrientedBitmap(resolver, uri)
            try {
                return preprocessBitmap(oriented)
            } catch (failure: Throwable) {
                oriented.recycle()
                throw failure
            }
        }

        /** Letterboxes an oriented bitmap and converts its pixels to normalized RGB model input.
         *
         * The returned [PreparedImage] retains [oriented] as its source bitmap; the caller owns
         * that bitmap and is responsible for recycling it when finished.
         *
         * @param oriented source bitmap with EXIF orientation already applied.
         * @return the source bitmap, model input tensor, and transform used for coordinate mapping.
         */
        fun preprocessBitmap(oriented: Bitmap): PreparedImage {
            val scale = min(
                IMAGE_SIZE.toFloat() / oriented.width,
                IMAGE_SIZE.toFloat() / oriented.height
            )
            val resizedWidth = (oriented.width * scale).roundToInt().coerceIn(1, IMAGE_SIZE)
            val resizedHeight = (oriented.height * scale).roundToInt().coerceIn(1, IMAGE_SIZE)
            val padX = (IMAGE_SIZE - resizedWidth) / 2
            val padY = (IMAGE_SIZE - resizedHeight) / 2

            val processingBitmap = createBitmap(
                IMAGE_SIZE,
                IMAGE_SIZE,
                Bitmap.Config.ARGB_8888
            )
            try {
                val canvas = Canvas(processingBitmap)
                canvas.drawColor(Color.rgb(114, 114, 114))
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
                canvas.drawBitmap(
                    oriented,
                    null,
                    Rect(padX, padY, padX + resizedWidth, padY + resizedHeight),
                    paint
                )
                val pixels = IntArray(INPUT_SIZE / 3)
                processingBitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
                val input = FloatArray(INPUT_SIZE)
                val channelSize = IMAGE_SIZE * IMAGE_SIZE
                for (index in pixels.indices) {
                    val pixel = pixels[index]
                    input[index] = Color.red(pixel) / 255f
                    input[channelSize + index] = Color.green(pixel) / 255f
                    input[2 * channelSize + index] = Color.blue(pixel) / 255f
                }
                return PreparedImage(
                    bitmap = oriented,
                    input = input,
                    transform = LetterboxTransform(scale, padX, padY)
                )
            } finally {
                processingBitmap.recycle()
            }
        }
        /** Sets the minimum detection confidence for the selected intensity from 1 to 5. */
        fun setConfidenceThreshold(intensity: Int){
            val minimum: Float = when (intensity){  //changes minimum filter based on intensity scale
                5 -> 0.25F  //Any
                4 -> 0.35F
                3 -> 0.50F  //most
                2 -> 0.65F
                1 -> 0.80F //Only the most extreme
                else -> 0.25F
            }
            CONFIDENCE_THRESHOLD = minimum
        }
        /** Decodes model output and maps valid detections back to the original image coordinates.
         *
         * @param output flattened model output with [OUTPUT_SIZE] values.
         * @param transform letterbox scale and padding used to prepare the image.
         * @param originalWidth width of the source image in pixels.
         * @param originalHeight height of the source image in pixels.
         * @return confidence-filtered, non-overlapping detections in source image coordinates.
         */
        fun decodeDetections(
            output: FloatArray,
            transform: LetterboxTransform,
            originalWidth: Int,
            originalHeight: Int
        ): List<SpiderDetection> {
            require(output.size == OUTPUT_SIZE) { "Unexpected detector output size." }
            val candidates = ArrayList<SpiderDetection>()
            val stride = CANDIDATE_COUNT
            for (candidate in 0 until CANDIDATE_COUNT) {
                val confidence = output[4 * stride + candidate]
                if (!confidence.isFinite() || confidence < CONFIDENCE_THRESHOLD) continue

                // The exported model stores normalized center-x, center-y, width, and height.
                val centerX = output[candidate] * IMAGE_SIZE
                val centerY = output[stride + candidate] * IMAGE_SIZE
                val width = output[2 * stride + candidate] * IMAGE_SIZE
                val height = output[3 * stride + candidate] * IMAGE_SIZE
                if (!centerX.isFinite() || !centerY.isFinite() ||
                    !width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f
                ) continue

                val left = centerX - width / 2f
                val top = centerY - height / 2f
                val right = centerX + width / 2f
                val bottom = centerY + height / 2f
                candidates += SpiderDetection(left, top, right, bottom, confidence)
            }

            val selected = nonMaximumSuppression(candidates)
            return selected.mapNotNull { detection ->
                val left = ((detection.left - transform.padX) / transform.scale)
                    .coerceIn(0f, originalWidth.toFloat())
                val top = ((detection.top - transform.padY) / transform.scale)
                    .coerceIn(0f, originalHeight.toFloat())
                val right = ((detection.right - transform.padX) / transform.scale)
                    .coerceIn(0f, originalWidth.toFloat())
                val bottom = ((detection.bottom - transform.padY) / transform.scale)
                    .coerceIn(0f, originalHeight.toFloat())
                if (right <= left || bottom <= top) null
                else SpiderDetection(left, top, right, bottom, detection.confidence)
            }
        }

        /** Returns a copy of [bitmap] with a box and confidence label drawn for each detection. */
        fun annotate(bitmap: Bitmap, detections: List<SpiderDetection>): Bitmap {
            val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(50, 220, 90)
                style = Paint.Style.STROKE
                strokeWidth = max(3f, min(bitmap.width, bitmap.height) / 250f)
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = max(24f, min(bitmap.width, bitmap.height) / 24f)
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD
                )
            }
            val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(24, 125, 45)
                style = Paint.Style.FILL
            }
            detections.forEach { detection ->
                canvas.drawRect(
                    detection.left,
                    detection.top,
                    detection.right,
                    detection.bottom,
                    boxPaint
                )
                val label = "spiders ${"%.2f".format(java.util.Locale.US, detection.confidence)}"
                val labelHeight = textPaint.textSize + 12f
                val labelTop = (detection.top - labelHeight).coerceAtLeast(0f)
                val labelRight = detection.left + textPaint.measureText(label) + 20f
                canvas.drawRect(detection.left, labelTop, labelRight, labelTop + labelHeight, backgroundPaint)
                canvas.drawText(label, detection.left + 10f, labelTop + textPaint.textSize, textPaint)
            }
            return result
        }

        private fun nonMaximumSuppression(
            candidates: List<SpiderDetection>
        ): List<SpiderDetection> {
            val sorted = candidates.sortedByDescending { it.confidence }
            val selected = ArrayList<SpiderDetection>(min(sorted.size, MAX_DETECTIONS))
            for (candidate in sorted) {
                if (selected.none { intersectionOverUnion(it, candidate) > IOU_THRESHOLD }) {
                    selected += candidate
                    if (selected.size == MAX_DETECTIONS) break
                }
            }
            return selected
        }

        private fun intersectionOverUnion(a: SpiderDetection, b: SpiderDetection): Float {
            val intersectionWidth = max(0f, min(a.right, b.right) - max(a.left, b.left))
            val intersectionHeight = max(0f, min(a.bottom, b.bottom) - max(a.top, b.top))
            val intersection = intersectionWidth * intersectionHeight
            val areaA = max(0f, a.right - a.left) * max(0f, a.bottom - a.top)
            val areaB = max(0f, b.right - b.left) * max(0f, b.bottom - b.top)
            val union = areaA + areaB - intersection
            return if (union > 0f) intersection / union else 0f
        }

        private fun decodeOrientedBitmap(resolver: ContentResolver, uri: Uri): Bitmap {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsInput = resolver.openInputStream(uri)
                ?: error("Could not open the selected image.")
            boundsInput.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Selected file is not a readable image." }

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > MAX_DECODED_SIDE ||
                bounds.outHeight / sampleSize > MAX_DECODED_SIDE
            ) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: error("Could not decode the selected image.")

            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.setRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.setRotate(-90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            }
            if (orientation == ExifInterface.ORIENTATION_NORMAL ||
                orientation == ExifInterface.ORIENTATION_UNDEFINED
            ) return decoded

            val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (oriented !== decoded) decoded.recycle()
            return oriented
        }

        private fun elapsedMillis(startedAtNanos: Long): Long =
            (System.nanoTime() - startedAtNanos) / 1_000_000L
    }
}

/** Closes resources in reverse scope order, attempting each close even if one fails. */
private fun closeWithUse(
    resources: List<AutoCloseable>,
    index: Int = 0,
    block: () -> Unit
) {
    if (index == resources.size) {
        block()
    } else {
        resources[index].use {
            closeWithUse(resources, index + 1, block)
        }
    }
}
