package com.example.phobiaguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Provides the Room database used to persist the app's local user settings.
 * The database exposes [UserSettingsDao] and currently stores [UserSettings].
 */
@Database(
    entities = [UserSettings::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Provides access to the DAO for reading and updating user settings.
     */
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN profilePicture BLOB")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the shared database instance, creating it on first access.
         *
         * @param context context from which the application context is obtained.
         * @return the shared [AppDatabase] instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}
