package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- Bookmarked Articles Entity & DAO ---
@Entity(tableName = "bookmarked_articles")
data class BookmarkedArticle(
    @PrimaryKey val url: String,
    val title: String,
    val description: String,
    val source: String,
    val publishedAt: String,
    val imageUrl: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarked_articles ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(article: BookmarkedArticle)

    @Delete
    suspend fun deleteBookmark(article: BookmarkedArticle)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_articles WHERE url = :url)")
    suspend fun isBookmarked(url: String): Boolean
}

// --- Custom News Feeds Preferences ---
@Entity(tableName = "news_preferences")
data class NewsPreference(
    @PrimaryKey val category: String,
    val isEnabled: Boolean
)

@Dao
interface NewsPreferenceDao {
    @Query("SELECT * FROM news_preferences")
    fun getAllPreferences(): Flow<List<NewsPreference>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreference(preference: NewsPreference)

    @Query("SELECT isEnabled FROM news_preferences WHERE category = :category LIMIT 1")
    suspend fun isCategoryEnabled(category: String): Boolean?
}

// --- Severe Weather Alerts Preference & Monitor ---
@Entity(tableName = "severe_alert_settings")
data class SevereAlertSetting(
    @PrimaryKey val city: String,
    val isEnabled: Boolean = true,
    val tempMaxThreshold: Float = 40f,
    val tempMinThreshold: Float = -10f,
    val windThreshold: Float = 50f, // km/h
    val notificationEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SevereAlertDao {
    @Query("SELECT * FROM severe_alert_settings ORDER BY timestamp DESC")
    fun getAllAlertSettings(): Flow<List<SevereAlertSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAlertSetting(setting: SevereAlertSetting)

    @Delete
    suspend fun deleteAlertSetting(setting: SevereAlertSetting)
}

// --- App Database Configuration ---
@Database(
    entities = [BookmarkedArticle::class, NewsPreference::class, SevereAlertSetting::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun newsPreferenceDao(): NewsPreferenceDao
    abstract fun severeAlertDao(): SevereAlertDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atmosphere_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Repository Implementation ---
class AtmosphereRepository(private val db: AppDatabase) {
    val allBookmarks: Flow<List<BookmarkedArticle>> = db.bookmarkDao().getAllBookmarks()
    val allPreferences: Flow<List<NewsPreference>> = db.newsPreferenceDao().getAllPreferences()
    val allAlertSettings: Flow<List<SevereAlertSetting>> = db.severeAlertDao().getAllAlertSettings()

    suspend fun addBookmark(article: BookmarkedArticle) = db.bookmarkDao().insertBookmark(article)
    suspend fun removeBookmark(article: BookmarkedArticle) = db.bookmarkDao().deleteBookmark(article)
    suspend fun isBookmarked(url: String): Boolean = db.bookmarkDao().isBookmarked(url)

    suspend fun savePreference(preference: NewsPreference) = db.newsPreferenceDao().savePreference(preference)
    suspend fun isCategoryEnabled(category: String): Boolean = db.newsPreferenceDao().isCategoryEnabled(category) ?: true

    suspend fun saveAlertSetting(setting: SevereAlertSetting) = db.severeAlertDao().saveAlertSetting(setting)
    suspend fun deleteAlertSetting(setting: SevereAlertSetting) = db.severeAlertDao().deleteAlertSetting(setting)
}
