package com.example.phobiaguard.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.phobiaguard.data.UserSettingsRepository
import com.example.phobiaguard.data.local.AppDatabase
import com.example.phobiaguard.data.local.UserSettings
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/** Exposes persisted user settings and handles profile-picture conversion for the settings UI. */
class UserSettingsViewModel(
    private val repository: UserSettingsRepository,
    private val appContext: Context
) : ViewModel() {

    /** Current user settings, collected while this state has active subscribers. */
    val settings: StateFlow<UserSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )

    private val _profilePictureError = MutableStateFlow<String?>(null)
    /** Most recent profile-picture save error, or `null` when there is no error to display. */
    val profilePictureError: StateFlow<String?> = _profilePictureError.asStateFlow()

    /** Saves the supplied user preferences through the repository. */
    fun saveSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }

    /** Encodes the selected image and saves it as the current profile picture. */
    fun saveProfilePicture(uri: Uri) {
        _profilePictureError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profilePicture = encodeProfilePicture(uri)
                val currentSettings = repository.settings.first()
                repository.saveSettings(currentSettings.copy(profilePicture = profilePicture))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _profilePictureError.value = failure.message?.takeIf(String::isNotBlank)
                    ?: "The profile photo could not be saved."
            }
        }
    }

    /** Clears the profile-picture error shown by the settings UI. */
    fun dismissProfilePictureError() {
        _profilePictureError.value = null
    }

    /** Decodes, orients, scales, and JPEG-encodes a selected image for profile storage. */
    private fun encodeProfilePicture(uri: Uri): ByteArray {
        val contentResolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = contentResolver.openInputStream(uri)
            ?: error("The selected photo could not be opened.")
        boundsStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        val sourceMaxDimension = max(bounds.outWidth, bounds.outHeight)
        require(sourceMaxDimension > 0) { "The selected file is not a supported image." }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(sourceMaxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decodedBitmap = contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: error("The selected photo could not be decoded.")

        val orientedBitmap = try {
            orientBitmap(uri, decodedBitmap)
        } catch (failure: Throwable) {
            decodedBitmap.recycle()
            throw failure
        }
        var outputBitmap: Bitmap? = null
        try {
            val scale = minOf(
                1f,
                MAX_PROFILE_PICTURE_DIMENSION.toFloat() /
                    max(orientedBitmap.width, orientedBitmap.height).toFloat()
            )
            outputBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    orientedBitmap,
                    (orientedBitmap.width * scale).roundToInt().coerceAtLeast(1),
                    (orientedBitmap.height * scale).roundToInt().coerceAtLeast(1),
                    true
                )
            } else {
                orientedBitmap
            }

            val bitmapToCompress = checkNotNull(outputBitmap)
            return ByteArrayOutputStream().use { output ->
                check(bitmapToCompress.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "The selected photo could not be compressed."
                }
                output.toByteArray()
            }
        } finally {
            outputBitmap?.takeIf { it !== orientedBitmap }?.recycle()
            orientedBitmap.recycle()
        }
    }

    /** Applies the image orientation recorded in the selected image's EXIF metadata. */
    private fun orientBitmap(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = appContext.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
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
        ) return bitmap

        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (oriented !== bitmap) bitmap.recycle()
        return oriented
    }

    /** Chooses a power-of-two decode sample size that limits the source dimensions. */
    private fun calculateInSampleSize(sourceMaxDimension: Int): Int {
        var sampleSize = 1
        while (sourceMaxDimension / (sampleSize * 2) >= MAX_PROFILE_PICTURE_DIMENSION) {
            sampleSize *= 2
        }
        return sampleSize
    }

    companion object {
        private const val MAX_PROFILE_PICTURE_DIMENSION = 512
        private const val JPEG_QUALITY = 85

        /** Creates a factory that supplies the database-backed repository and application context. */
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext

            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(UserSettingsViewModel::class.java)) {
                        val dao = AppDatabase.getDatabase(applicationContext).userSettingsDao()
                        val repository = UserSettingsRepository(dao)

                        @Suppress("UNCHECKED_CAST")
                        return UserSettingsViewModel(repository, applicationContext) as T
                    }

                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
