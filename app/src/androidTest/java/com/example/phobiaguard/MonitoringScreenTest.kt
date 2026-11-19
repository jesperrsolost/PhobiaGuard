package com.example.phobiaguard

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.phobiaguard.ui.screens.MonitoringScreen
import org.junit.Rule
import org.junit.Test
import org.junit.Before

class MonitoringScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.SYSTEM_ALERT_WINDOW
    )

    @Before //generated using AI, grants overlay permission
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow"
        )
        instrumentation.uiAutomation.executeShellCommand(
            "appops set ${context.packageName} PROJECT_MEDIA allow"
        )
    }


    @Test
    fun startMonitoringTest() {
        composeTestRule.setContent {
            MonitoringScreen()
        }

        // Confirm that clicking Activate Monitoring updates the state and button text
        composeTestRule.onNodeWithText("Activate Monitoring").performClick()
        composeTestRule.onNodeWithText("Stop Monitoring").assertIsDisplayed()
    }

    @Test
    fun stopMonitoringTest() {
        composeTestRule.setContent {
            MonitoringScreen()
        }
        // Activate monitoring first
        composeTestRule.onNodeWithText("Activate Monitoring").performClick()
        composeTestRule.onNodeWithText("Stop Monitoring").assertIsDisplayed()

        // Then click Stop Monitoring to revert state
        composeTestRule.onNodeWithText("Stop Monitoring").performClick()
        composeTestRule.onNodeWithText("Activate Monitoring").assertIsDisplayed()
    }
}