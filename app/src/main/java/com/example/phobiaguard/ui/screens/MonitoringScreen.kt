package com.example.phobiaguard.ui.screens

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phobiaguard.ScreenMonitoringService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Checks for overlay Permission
 *
 * @param context - Local application context for current screen
 * @return true if Settings.canDrawOverlays(context) is true, otherwise its false
 */
internal fun hasOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

/**
 * Ensures monitoring state is known globally (made using AI)
 */
object MonitoringState{
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    fun updateMonitoring(active: Boolean){
        _isMonitoring.value = active
    }
}

/**
 * Primary display for monitoring service,
 * This is where ScreenMonitoring service is activated and deactivated
 * On first starting, user needs to give permission for app to draw overlays.
 * User has to set activation for screenMonitoring on start
 *
 * @param modifier - modifies screen layout
 */
@Composable
fun MonitoringScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val isMonitoring by MonitoringState.isMonitoring.collectAsState()
    /**
     * media projection uses a screenCaptureIntent over a basic one
     * needs special permissions before intent can be created and launched
     */
    val startMediaProjection = rememberLauncherForActivityResult(   //AI method,
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, ScreenMonitoringService::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            Log.d("Denied", "Denied screen monitoring access")
        }
    }
    val buttonWidth = 180.dp
    val buttonHeight = 80.dp
    val buttonColor = Color(0xFF1CC3AE)

    var hasOverlayPermission by remember { //need to have permission to use overlay
        mutableStateOf(hasOverlayPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasOverlayPermission = granted }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasOverlayPermission(context)) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .padding(horizontal = 20.dp),

                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {


                Button(onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }, modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    ) {
                    Text("Grant Display Over Other Apps Permission")
                }

                Spacer(
                    modifier = Modifier.height(40.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF267F9F))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Steps",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFEFAEF)
                        )

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )


                        Text(
                            text = "Read all before starting\n\n" +
                                    "1. go to app settings on the phone\n" +
                                    "  1.1 click permission\n" +
                                    "  1.1 click allow notifications\n\n" +
                                    "2. click button above\n" +
                                    "3. click on PhobiaGuard and grant permission\n" +
                                    "4. click on back arrow\n" +
                                    "5. click click on any app you want to grant the app access to\n" +
                                    "6. click on back arrow, now the monitoring button will be visible",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFFFEFAEF)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(vertical = 100.dp)
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Monitoring mode",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.W900,
                    color = Color(0xFF011F4B)
                )

                Spacer(
                    modifier = Modifier.height(60.dp)
                )

                Button(
                    onClick = {
                        if(isMonitoring) {
                            val intent = Intent(
                                context,
                                ScreenMonitoringService::class.java
                            )
                            context.stopService(intent)
                            MonitoringState.updateMonitoring(false)
                        }else{
                            val mediaProjectionManager =
                                context.getSystemService(MediaProjectionManager::class.java)
                            startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
                            MonitoringState.updateMonitoring(true)
                        }
                    },
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(

                        containerColor =
                            if(isMonitoring){Color(0xFF15C4B3)}
                            else{Color(0xFFFF6C74)},
                        contentColor = Color(0xFFFEFAEF)
                    )
                ) {
                    Text(
                        text = if (isMonitoring)
                            "Stop Monitoring"
                        else
                            "Activate Monitoring",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(
                    modifier = Modifier.height(60.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF267F9F))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Permissions",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFEFAEF)
                        )

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )


                        Text(
                            text = "- allows screen monitoring\n" +
                                    "- allows app to run in foreground\n" +
                                    "- allows app to display over other apps\n" +
                                    "- allows app to censor/blur images",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFFFEFAEF)
                        )
                    }
                }

            }
        }
    }
}
