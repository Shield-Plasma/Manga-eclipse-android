package com.example.comicreader.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "series")
data class SavedSeries(
    @PrimaryKey val url: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String,
    val lastUpdatedEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "chapters")
data class SavedChapter(
    @PrimaryKey val url: String,
    val seriesUrl: String,
    val title: String,
    val chapterNumber: Float,
    val isRead: Boolean = false,
    val lastReadPage: Int = 0,
    val isDownloaded: Boolean = false,
    val localFolderPath: String? = null
)

enum class SortOrder { TITLE_ASC, RECENTLY_UPDATED, UNREAD_DESC }

@Dao
interface LibraryDao {
    @Query("SELECT * FROM series ORDER BY title ASC")
    fun observeByTitle(): Flow<List<SavedSeries>>

    @Query("SELECT * FROM series ORDER BY lastUpdatedEpoch DESC")
    fun observeByRecent(): Flow<List<SavedSeries>>

    @Query("SELECT * FROM chapters WHERE seriesUrl = :seriesUrl ORDER BY chapterNumber DESC")
    fun observeChapters(seriesUrl: String): Flow<List<SavedChapter>>

    @Upsert
    suspend fun upsertSeries(series: SavedSeries)

    @Upsert
    suspend fun upsertChapters(chapters: List<SavedChapter>)

    @Query("UPDATE chapters SET isRead = :read, lastReadPage = :page WHERE url = :chapterUrl")
    suspend fun updateProgress(chapterUrl: String, read: Boolean, page: Int)
}

