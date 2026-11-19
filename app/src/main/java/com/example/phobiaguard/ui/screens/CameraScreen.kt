package com.example.phobiaguard.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.phobiaguard.ui.settings.SpiderModelResult
import com.example.phobiaguard.ui.settings.SpiderModelResultDialog
import com.example.phobiaguard.ui.settings.SpiderModelRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Checks if application has permission to use camera
 *
 * @param context  current application context
 * @return true if manifiest permission is granted, false otherwise
 */
internal fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

internal sealed interface CameraCaptureState {
    data object Idle : CameraCaptureState
    data object Running : CameraCaptureState
    data class Success(val result: SpiderModelResult) : CameraCaptureState
    data class Error(val message: String) : CameraCaptureState
}

/**
 * CameraViewModel
 *
 */
internal class CameraViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val runner = SpiderModelRunner(appContext)
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private val inferenceDispatcher = Dispatchers.IO.limitedParallelism(1)
    private var imageCapture: ImageCapture? = null

    private val _captureState = MutableStateFlow<CameraCaptureState>(CameraCaptureState.Idle)
    val captureState: StateFlow<CameraCaptureState> = _captureState.asStateFlow()

    /**
     * Binds camera to lifecycle allowing viewmodel to take pictures
     *
     * @param context used to bind camera to current application
     * @param lifecycleOwner the owner of the lifecycle which controls the camera
     * @param previewView the live preview for camera
     */
    fun bindCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context.applicationContext)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )
                imageCapture = capture
            } catch (failure: Exception) {
                _captureState.value = CameraCaptureState.Error(
                    failure.message?.takeIf(String::isNotBlank)
                        ?: "The camera could not be started."
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Takes a picture with the camera and decodes it into a bitmap
     *
     * @see rotateBitmap changes orientation of the bitmap
     * @see runModel processes the bitmap in the ML model and identifies any spiders
     */
    fun takePhoto() {
        if (_captureState.value == CameraCaptureState.Running) return
        val capture = imageCapture
        if (capture == null) {
            _captureState.value = CameraCaptureState.Error("The camera is not ready yet.")
            return
        }

        _captureState.value = CameraCaptureState.Running
        capture.takePicture(
            captureExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    var bitmap: Bitmap? = null
                    var failure: Throwable? = null
                    try {
                        val rotationDegrees = image.imageInfo.rotationDegrees
                        val decodedBitmap = image.toBitmap()
                        bitmap = decodedBitmap
                        bitmap = rotateBitmap(decodedBitmap, rotationDegrees)
                    } catch (captureFailure: Exception) {
                        failure = captureFailure
                    } catch (captureFailure: LinkageError) {
                        failure = captureFailure
                    } finally {
                        image.close()
                    }

                    if (failure != null) {
                        bitmap?.recycle()
                        _captureState.value = CameraCaptureState.Error(
                            failure.message?.takeIf(String::isNotBlank)
                                ?: "The captured image could not be read."
                        )
                    } else {
                        bitmap?.let(::runModel)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    _captureState.value = CameraCaptureState.Error(
                        exception.message?.takeIf(String::isNotBlank)
                            ?: "The camera could not capture an image."
                    )
                }
            }
        )
    }

    /**
     * Runs ML on captured image, trying to identify any spiders
     *
     * @param bitmap decoded image that is processed by model
     * @exception CancellationException catches if model stops running while processing
     * @exception Exception catches if the model fails to process the image
     * @exception LinkageError if camera screen cannot link with model
     * @see SpiderModelRunner.runBitmap processes bitmap and updates CameraCapture state
     */
    private fun runModel(bitmap: Bitmap) {
        viewModelScope.launch(inferenceDispatcher) {
            try {
                _captureState.value = CameraCaptureState.Success(runner.runBitmap(bitmap))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _captureState.value = CameraCaptureState.Error(
                    failure.message?.takeIf(String::isNotBlank)
                        ?: "The model could not process this image."
                )
            } catch (failure: LinkageError) {
                _captureState.value = CameraCaptureState.Error(
                    failure.message?.takeIf(String::isNotBlank)
                        ?: "The on-device inference runtime could not be started."
                )
            }
        }
    }

    /**
     * Discards result of capture state when dismiss request is sent
     */
    fun dismissResult() {
        when (val state = _captureState.value) {
            is CameraCaptureState.Success -> state.result.annotatedImage.recycle()
            CameraCaptureState.Idle,
            CameraCaptureState.Running,
            is CameraCaptureState.Error -> Unit
        }
        if (_captureState.value != CameraCaptureState.Running) {
            _captureState.value = CameraCaptureState.Idle
        }
    }

    override fun onCleared() {
        (_captureState.value as? CameraCaptureState.Success)
            ?.result
            ?.annotatedImage
            ?.recycle()
        captureExecutor.shutdown()
        CoroutineScope(SupervisorJob() + inferenceDispatcher).launch {
            runner.close()
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return CameraViewModel(applicationContext) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

        private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
            if (rotationDegrees % 360 == 0) return bitmap
            //AI suggested fix for illegal argument
            val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                safeBitmap.width,
                safeBitmap.height,
                matrix,
                true
            )
            if (rotated !== bitmap) bitmap.recycle()
            return rotated
        }
    }
}

/**
 * Main camera screen visible in application
 *
 *@param viewModel CameraViewmodel which stores captured image and processes it
 *@see hasCameraPermission checks if camera permission has been granted
 */
@Composable
internal fun CameraScreen(viewModel: CameraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val captureState by viewModel.captureState.collectAsState()
    var hasCameraPerm by remember {
        mutableStateOf(hasCameraPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPerm = granted }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPerm) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also {
                        viewModel.bindCamera(ctx, lifecycleOwner, it)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = viewModel::takePhoto,
                    enabled = captureState != CameraCaptureState.Running
                ) {
                    Text("Capture and analyze")
                }
                if (captureState == CameraCaptureState.Running) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Running spider detection…")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Camera permission is needed to capture an image.")
                Button(onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Grant Camera Permission")
                }
            }
        }
    }

    when (val state = captureState) {
        CameraCaptureState.Idle,
        CameraCaptureState.Running -> Unit
        is CameraCaptureState.Success -> SpiderModelResultDialog(
            result = state.result,
            onDismiss = viewModel::dismissResult
        )
        is CameraCaptureState.Error -> AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            title = { Text("Camera processing failed") },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissResult) {
                    Text("Close")
                }
            }
        )
    }
}
