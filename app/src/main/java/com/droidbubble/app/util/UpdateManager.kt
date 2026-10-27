package com.droidbubble.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val tagName: String,
    val version: String,
    val name: String,
    val changelog: String,
    val isBeta: Boolean,
    val downloadUrl: String,
    val apkSize: Long
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: AppReleaseInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object UpdateManager {

    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/Velocity081/droidbubble/releases"

    fun getCurrentVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.1.5"
        } catch (_: Exception) {
            "1.1.5"
        }
    }

    suspend fun checkGithubUpdates(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_RELEASES_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Droidbubble-Android-App")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode !in 200..299) {
                return@withContext UpdateCheckResult.Error("HTTP Error: ${connection.responseCode}")
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val releasesArray = JSONArray(responseText)
            if (releasesArray.length() == 0) {
                return@withContext UpdateCheckResult.UpToDate
            }

            // Find the most relevant latest release
            var targetRelease: AppReleaseInfo? = null

            for (i in 0 until releasesArray.length()) {
                val relObj = releasesArray.getJSONObject(i)
                val tagName = relObj.optString("tag_name", "").trim()
                val name = relObj.optString("name", tagName)
                val body = relObj.optString("body", "No changelog provided.")
                val prerelease = relObj.optBoolean("prerelease", false)
                val isBeta = prerelease || tagName.contains("beta", ignoreCase = true) || name.contains("beta", ignoreCase = true)

                // Locate APK asset
                var apkUrl: String? = null
                var apkSize: Long = 0L
                val assets = relObj.optJSONArray("assets")
                if (assets != null) {
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        val aName = asset.optString("name", "").lowercase()
                        if (aName.endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url")
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                // If no asset is attached, fall back to zipball or release html url
                val finalUrl = apkUrl ?: relObj.optString("html_url")

                val cleanVersion = tagName.removePrefix("v").removePrefix("V").trim()
                targetRelease = AppReleaseInfo(
                    tagName = tagName,
                    version = cleanVersion,
                    name = name,
                    changelog = body,
                    isBeta = isBeta,
                    downloadUrl = finalUrl,
                    apkSize = apkSize
                )
                break // First one is the latest
            }

            val release = targetRelease ?: return@withContext UpdateCheckResult.UpToDate
            val current = getCurrentVersion(context)

            if (isNewerVersion(release.version, current)) {
                UpdateCheckResult.UpdateAvailable(release)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Failed to check updates")
        }
    }

    private fun isNewerVersion(newVer: String, currentVer: String): Boolean {
        // Strip suffixes like -beta, -alpha for numeric comparison
        val newNumeric = newVer.split("-", "_", " ").firstOrNull() ?: newVer
        val curNumeric = currentVer.split("-", "_", " ").firstOrNull() ?: currentVer

        val newParts = newNumeric.split(".").mapNotNull { it.toIntOrNull() }
        val curParts = curNumeric.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(newParts.size, curParts.size)
        for (i in 0 until maxLen) {
            val n = newParts.getOrElse(i) { 0 }
            val c = curParts.getOrElse(i) { 0 }
            if (n > c) return true
            if (n < c) return false
        }

        // If numeric version is identical, check if release has a newer build tag or different string
        return newVer != currentVer && (newVer > currentVer)
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Droidbubble-Android-App")
                connectTimeout = 15000
                readTimeout = 30000
            }

            var actualConn = connection
            if (actualConn.responseCode in 301..308) {
                val newUrl = actualConn.getHeaderField("Location")
                actualConn = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "Droidbubble-Android-App")
                    connectTimeout = 15000
                    readTimeout = 30000
                }
            }

            val totalBytes = actualConn.contentLengthLong.coerceAtLeast(1L)
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outputFile = File(updatesDir, "Droidbubble-update.apk")
            if (outputFile.exists()) outputFile.delete()

            actualConn.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        onProgress((totalDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                    }
                }
            }

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    return
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
