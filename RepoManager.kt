package com.example.eclipseandroid.extensions

import android.content.Context
import android.content.pm.PackageManager
import dalvik.system.PathClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class RepoExtensionJson(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Int,
    val version: String,
    val nsfw: Int = 0,
    val sources: List<RepoSourceJson> = emptyList()
)

@Serializable
data class RepoSourceJson(
    val name: String,
    val lang: String,
    val id: String,
    val baseUrl: String
)

class MihonRepoManager(
    private val context: Context,
    private val client: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Fetches and parses a standard Tachiyomi/Mihon index.min.json repository URL.
     */
    suspend fun fetchRepoIndex(repoIndexUrl: String): List<RepoExtensionJson> =
        withContext(Dispatchers.IO) {
            val normalizedUrl = if (repoIndexUrl.endsWith(".json")) {
                repoIndexUrl
            } else {
                "${repoIndexUrl.trimEnd('/')}/index.min.json"
            }
            val request = Request.Builder().url(normalizedUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Repo HTTP ${response.code}")
                val body = response.body?.string().orEmpty()
                json.decodeFromString<List<RepoExtensionJson>>(body)
            }
        }

    /**
     * Discovers installed Mihon/Tachiyomi-compatible extension APKs on the device
     * via the standard "tachiyomi.extension" meta-data flag and loads their Source classes.
     */
    fun loadInstalledExtensions(): List<LoadedExtension> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(
            PackageManager.GET_CONFIGURATIONS or PackageManager.GET_META_DATA
        )
        return packages.mapNotNull { pkgInfo ->
            val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
            val meta = appInfo.metaData ?: return@mapNotNull null
            val sourceClassNames = meta.getString("tachiyomi.extension.class") ?: return@mapNotNull null

            val classLoader = PathClassLoader(appInfo.sourceDir, context.classLoader)
            val instances = sourceClassNames.split(",").mapNotNull { rawName ->
                val fullName = if (rawName.startsWith(".")) "${pkgInfo.packageName}$rawName" else rawName.trim()
                runCatching {
                    classLoader.loadClass(fullName).getDeclaredConstructor().newInstance()
                }.getOrNull()
            }
            LoadedExtension(
                pkgName = pkgInfo.packageName,
                label = pm.getApplicationLabel(appInfo).toString(),
                sourceInstances = instances
            )
        }
    }
}

data class LoadedExtension(
    val pkgName: String,
    val label: String,
    val sourceInstances: List<Any>
)

