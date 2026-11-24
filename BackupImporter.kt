package com.example.eclipseandroid.backup

import android.content.Context
import android.net.Uri
import com.example.comicreader.data.LibraryDao
import com.example.comicreader.data.SavedChapter
import com.example.comicreader.data.SavedSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

// --- Mihon / Tachiyomi Protobuf Schema (.tachibk / .proto.gz) ---
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MihonBackup(
    @ProtoNumber(1) val backupManga: List<MihonBackupManga> = emptyList()
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MihonBackupManga(
    @ProtoNumber(1) val source: Long = 0L,
    @ProtoNumber(2) val url: String = "",
    @ProtoNumber(3) val title: String = "",
    @ProtoNumber(13) val thumbnailUrl: String = "",
    @ProtoNumber(16) val chapters: List<MihonBackupChapter> = emptyList()
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MihonBackupChapter(
    @ProtoNumber(1) val url: String = "",
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(5) val read: Boolean = false,
    @ProtoNumber(7) val lastPageRead: Long = 0L,
    @ProtoNumber(9) val chapterNumber: Float = 0f
)

// --- Kotatsu JSON Schema inside .bk.zip ---
@Serializable
data class KotatsuBackupEntry(
    val manga: KotatsuManga,
    val history: KotatsuHistory? = null
)

@Serializable
data class KotatsuManga(
    val id: Long,
    val title: String,
    val url: String,
    val coverUrl: String = "",
    val source: String = ""
)

@Serializable
data class KotatsuHistory(
    val chapterId: Long = 0L,
    val page: Int = 0
)

class BackupImporter(
    private val context: Context,
    private val dao: LibraryDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun importBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = uri.lastPathSegment.orEmpty().lowercase()
            when {
                fileName.endsWith(".zip") || fileName.endsWith(".bk.zip") -> importKotatsuZip(uri)
                else -> importMihonProtoGz(uri) // handles .tachibk and .proto.gz
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun importMihonProtoGz(uri: Uri): Int {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { input ->
            GZIPInputStream(input).use { gzip -> gzip.readBytes() }
        } ?: error("Unable to open Mihon backup stream")

        val backup = ProtoBuf.decodeFromByteArray<MihonBackup>(rawBytes)
        var importedCount = 0

        for (manga in backup.backupManga) {
            dao.upsertSeries(
                SavedSeries(
                    url = manga.url,
                    sourceId = manga.source.toString(),
                    title = manga.title,
                    coverUrl = manga.thumbnailUrl
                )
            )
            val chapters = manga.chapters.map { ch ->
                SavedChapter(
                    url = ch.url,
                    seriesUrl = manga.url,
                    title = ch.name,
                    chapterNumber = ch.chapterNumber,
                    isRead = ch.read,
                    lastReadPage = ch.lastPageRead.toInt()
                )
            }
            if (chapters.isNotEmpty()) dao.upsertChapters(chapters)
            importedCount++
        }
        return importedCount
    }

    private suspend fun importKotatsuZip(uri: Uri): Int {
        var importedCount = 0
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "favourites" || entry.name == "history") {
                        val content = zip.bufferedReader().readText()
                        val entries = json.decodeFromString<List<KotatsuBackupEntry>>(content)
                        for (item in entries) {
                            dao.upsertSeries(
                                SavedSeries(
                                    url = item.manga.url,
                                    sourceId = item.manga.source,
                                    title = item.manga.title,
                                    coverUrl = item.manga.coverUrl
                                )
                            )
                            importedCount++
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("Unable to open Kotatsu backup stream")
        return importedCount
    }
}
