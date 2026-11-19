package com.example.phobiaguard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.example.phobiaguard.ui.screens.CameraScreen
import com.example.phobiaguard.ui.screens.CameraViewModel
import com.example.phobiaguard.ui.screens.MonitoringScreen
import com.example.phobiaguard.ui.screens.hasCameraPermission
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class CameraScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA
    )

    private val viewModel: CameraViewModel =CameraViewModel(ApplicationProvider.getApplicationContext())
    @Test
    fun takePictureTest(){
        composeTestRule.setContent {
            CameraScreen(viewModel)
        }
        composeTestRule.onNodeWithText("Capture and analyze").assertIsEnabled()
    }

    /**
     * Tests that clicking on grant camera permission updates permission
     *
     * @see hasCameraPermission
     */
    @Test
    fun cameraPermissionGranted(){
        composeTestRule.setContent {
            CameraScreen(viewModel)
            grantPermissionRule
        }
        assertTrue(hasCameraPermission(ApplicationProvider.getApplicationContext()))
    }

    /**
     *Tests if camera does not have camera permission
     */
    @Test
    fun cameraPermissionDenied(){
        composeTestRule.setContent {
            CameraScreen(viewModel)
        }
        assertFalse(hasCameraPermission(ApplicationProvider.getApplicationContext()))
    }
}