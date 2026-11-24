package com.example.comicreader.scraper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class SourceConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val catalogListSelector: String,
    val titleSelector: String,
    val linkSelector: String,
    val coverSelector: String,
    val chapterListSelector: String,
    val pageImageSelector: String
)

data class SeriesItem(val title: String, val url: String, val coverUrl: String, val sourceId: String)
data class ChapterItem(val title: String, val url: String, val chapterNumber: Float)

class WebSourceScraper(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "ComicReaderAndroid/1.0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    suspend fun fetchCatalog(config: SourceConfig, pageUrl: String): List<SeriesItem> =
        withContext(Dispatchers.Default) {
            val doc = Jsoup.parse(fetchHtml(pageUrl), config.baseUrl)
            doc.select(config.catalogListSelector).mapNotNull { el ->
                val title = el.selectFirst(config.titleSelector)?.text()?.trim() ?: return@mapNotNull null
                val url = el.selectFirst(config.linkSelector)?.absUrl("href") ?: return@mapNotNull null
                val cover = el.selectFirst(config.coverSelector)?.absUrl("src").orEmpty()
                SeriesItem(title, url, cover, config.id)
            }
        }

    suspend fun fetchChapters(config: SourceConfig, seriesUrl: String): List<ChapterItem> =
        withContext(Dispatchers.Default) {
            val doc = Jsoup.parse(fetchHtml(seriesUrl), config.baseUrl)
            doc.select(config.chapterListSelector).mapIndexedNotNull { index, el ->
                val title = el.text().trim()
                val url = el.absUrl("href")
                if (url.isBlank()) return@mapIndexedNotNull null
                val num = Regex("""\d+(\.\d+)?""").find(title)?.value?.toFloatOrNull() ?: index.toFloat()
                ChapterItem(title, url, num)
            }
        }

    suspend fun fetchPageUrls(config: SourceConfig, chapterUrl: String): List<String> =
        withContext(Dispatchers.Default) {
            val doc = Jsoup.parse(fetchHtml(chapterUrl), config.baseUrl)
            doc.select(config.pageImageSelector).mapNotNull { img ->
                img.absUrl("src").ifBlank { img.absUrl("data-src") }.ifBlank { null }
            }
        }
}

