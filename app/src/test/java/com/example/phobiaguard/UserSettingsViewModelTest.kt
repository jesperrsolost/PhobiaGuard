package com.example.phobiaguard

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.impl.Scheduler
import com.example.phobiaguard.data.UserSettingsRepository
import com.example.phobiaguard.data.local.UserSettings
import com.example.phobiaguard.data.local.UserSettingsDao
import com.example.phobiaguard.ui.settings.InferenceInfo
import com.example.phobiaguard.ui.settings.LetterboxTransform
import com.example.phobiaguard.ui.settings.PreparedImage
import com.example.phobiaguard.ui.settings.SettingsScreen

import com.example.phobiaguard.ui.viewmodel.UserSettingsViewModel
import com.google.common.util.concurrent.AbstractScheduledService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*
import org.junit.Before

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserSettingsViewModelTest {   //Initially tried manually, AI generated a functional test

    private class FakeUserSettingsDao : UserSettingsDao {
        private val _settings = MutableStateFlow<UserSettings?>(null)
        override fun observeSettings(): Flow<UserSettings?> = _settings
        override suspend fun upsert(settings: UserSettings) {
            _settings.value = settings
        }
    }

    private lateinit var dao: FakeUserSettingsDao
    private lateinit var repository: UserSettingsRepository
    private lateinit var viewModel: UserSettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp(){
        Dispatchers.setMain(testDispatcher)
        dao = FakeUserSettingsDao()
        repository = UserSettingsRepository(dao)
        viewModel = UserSettingsViewModel(repository,ApplicationProvider.getApplicationContext<Context>())
    }
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    @Test
    fun userSettingsUpdated() = runTest(testDispatcher){
        val settings =UserSettings(name = "Tom", selectedPhobia = "Spiders", intensity = 5)
        backgroundScope.launch(testDispatcher) {
            viewModel.settings.collect {}
        }

        viewModel.saveSettings(settings)
        testDispatcher.scheduler.advanceUntilIdle()
        val currentSettings = viewModel.settings.value

        assertEquals("Tom", currentSettings.name)
        assertTrue(currentSettings.intensity == 5)
    }

}