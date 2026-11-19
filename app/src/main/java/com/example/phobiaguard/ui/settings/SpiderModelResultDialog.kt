package com.example.phobiaguard.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** Shows the annotated image, detection summary, and timing and backend details for [result].
 *
 * @param result completed spider model output to display.
 * @param onDismiss callback invoked when the dialog is dismissed.
 */
@Composable
internal fun SpiderModelResultDialog(
    result: SpiderModelResult,
    onDismiss: () -> Unit
) {
    val summary = when (result.detectionCount) {
        0 -> "No spiders detected"
        1 -> "Detected 1 spider"
        else -> "Detected ${result.detectionCount} spiders"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Model result") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    bitmap = result.annotatedImage.asImageBitmap(),
                    contentDescription = "Image with spider detections marked",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    contentScale = ContentScale.Fit
                )
                Text(summary)
                Text("Image: ${result.inputWidth} × ${result.inputHeight}")
                Text("Backend: ${result.backend}")
                result.fallbackReason?.let { Text(it) }
                Text("Model load: ${result.modelLoadMs} ms")
                Text("Preprocessing: ${result.preprocessingMs} ms")
                Text("Inference: ${result.inferenceMs} ms")
                Text("Postprocessing and drawing: ${result.postprocessingMs} ms")
                Text(
                    "Total: ${result.totalMs} ms",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
