package com.example.phobiaguard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Settings Data Access Object
 * Provides Room database operations for persisted [UserSettings].
 */
@Dao
interface UserSettingsDao {
    /**
     * Observes the settings row whose primary key is `1`.
     *
     * @return a flow containing the settings row or `null` when no row exists.
     */
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observeSettings(): Flow<UserSettings?>

    /**
     * Inserts or updates a [UserSettings] row based on its primary key.
     *
     * @param settings settings record to insert or update.
     */
    @Upsert
    suspend fun upsert(settings: UserSettings)
}
