package com.example.phobiaguard.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Represents the current status of a spider model test. */
internal sealed interface SpiderModelTestState {
    data object Idle : SpiderModelTestState
    data object Running : SpiderModelTestState
    data class Success(val result: SpiderModelResult) : SpiderModelTestState
    data class Error(val message: String) : SpiderModelTestState
}

/** Runs image tests and exposes their progress and result to the settings UI. */
internal class SpiderModelTestViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val runner = SpiderModelRunner(appContext)
    private val inferenceDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val _state = MutableStateFlow<SpiderModelTestState>(SpiderModelTestState.Idle)
    val state: StateFlow<SpiderModelTestState> = _state.asStateFlow()

    /** Starts inference for [uri] unless another test is already running. */
    fun runModel(uri: Uri) {
        if (_state.value == SpiderModelTestState.Running) return
        _state.value = SpiderModelTestState.Running
        viewModelScope.launch(inferenceDispatcher) {
            try {
                _state.value = SpiderModelTestState.Success(
                    runner.runUri(appContext.contentResolver, uri)
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.value = SpiderModelTestState.Error(
                    failure.message?.takeIf(String::isNotBlank)
                        ?: "The model could not process this image."
                )
            } catch (failure: LinkageError) {
                _state.value = SpiderModelTestState.Error(
                    failure.message?.takeIf(String::isNotBlank)
                        ?: "The on-device inference runtime could not be started."
                )
            }
        }
    }

    /** Clears a completed or failed result and releases its annotated image, if present. */
    fun dismissResult() {
        if (_state.value != SpiderModelTestState.Running) {
            (_state.value as? SpiderModelTestState.Success)
                ?.result
                ?.annotatedImage
                ?.recycle()
            _state.value = SpiderModelTestState.Idle
        }
    }

    override fun onCleared() {
        (_state.value as? SpiderModelTestState.Success)?.result?.annotatedImage?.recycle()
        CoroutineScope(SupervisorJob() + inferenceDispatcher).launch {
            runner.close()
        }
    }

    companion object {
        /** Creates a factory that supplies the application context to this ViewModel. */
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SpiderModelTestViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return SpiderModelTestViewModel(applicationContext) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

    }
}
