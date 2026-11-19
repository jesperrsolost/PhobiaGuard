package com.example.phobiaguard.data

import com.example.phobiaguard.data.local.UserSettings
import com.example.phobiaguard.data.local.UserSettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Coordinates reading and saving [UserSettings] through [UserSettingsDao].
 */
class UserSettingsRepository(
    private val dao: UserSettingsDao
) {
    /**
     * Emits persisted settings, replacing a missing row with default values.
     */
    val settings: Flow<UserSettings> =
        dao.observeSettings().map { settings ->
            settings ?: UserSettings()
        }

    /**
     * Validates and persists the supplied settings.
     *
     * @param settings settings to persist.
     * @throws IllegalArgumentException if the intensity is outside that range.
     */
    suspend fun saveSettings(settings: UserSettings) {
        require(settings.intensity in 1..5)
        dao.upsert(settings)
    }

}
