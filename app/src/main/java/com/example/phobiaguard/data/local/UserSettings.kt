package com.example.phobiaguard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the user's locally persisted settings in the `user_settings` Room table.
 * Default values are used when no saved settings row exists.
 *
 * @property id primary-key value for the settings row. It defaults to `1`.
 * @property name stored user name. Defaults to `"Guest"`.
 * @property selectedPhobia selected phobia name. Defaults to `"Spiders"`.
 * @property intensity selected phobia intensity (accepts values from `1`-`5`).
 * @property profilePicture profile image bytes, or `null` when no photo is selected.
 */
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "Guest",
    val selectedPhobia: String = "Spiders",
    val intensity: Int = 1,
    val profilePicture: ByteArray? = null
)
