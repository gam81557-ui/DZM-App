package com.app.pubglibrary.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Immutable
import com.app.pubglibrary.ui.i18n.EnAppStrings
import com.app.pubglibrary.ui.i18n.UiText
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipException
import java.util.zip.ZipFile

/**
 * Single configurable remote control JSON URL.
 * Set to "REPLACE_WITH_YOUR_JSON_URL" to test built-in sample items locally.
 * When set to a real raw JSON URL, the app strictly uses the remote JSON configuration.
 */
const val REMOTE_CONTROL_URL =
    "https://raw.githubusercontent.com/gam81557-ui/DZM-V3/refs/heads/main/files.json"

private const val PLACEHOLDER_CONTROL_URL = "REPLACE_WITH_YOUR_JSON_URL"

enum class PubgVersion(
    val key: String,
    val displayName: String,
    val shortTag: String,
    val packageName: String,
    val regionLabel: String
) {
    GLOBAL(
        key = "global",
        displayName = "PUBG Global",
        shortTag = "GLOBAL",
        packageName = "com.tencent.ig",
        regionLabel = "International Client"
    ),
    BGMI(
        key = "bgmi",
        displayName = "BGMI",
        shortTag = "INDIA",
        packageName = "com.pubg.imobile",
        regionLabel = "Battlegrounds Mobile India"
    ),
    KR(
        key = "kr",
        displayName = "PUBG Korea",
        shortTag = "KR / JP",
        packageName = "com.pubg.krmobile",
        regionLabel = "Korea & Japan Client"
    ),
    VN(
        key = "vn",
        displayName = "PUBG Vietnam",
        shortTag = "VNG",
        packageName = "com.vng.pubgmobile",
        regionLabel = "VNG Vietnam Client"
    )
}

@Immutable
data class InstalledPackageInfo(
    val isInstalled: Boolean,
    val versionName: String? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class LibraryFileItem(
    val id: String,
    val title: String,
    val versionKey: String = "",
    val enabled: Boolean = true,
    val sizeLabel: String,
    val downloadUrl: String,
    val targetSubPath: String,
    val fileName: String,
    val description: String = "",
    val category: String = "General"
) {
    fun matchesVersion(version: PubgVersion): Boolean {
        val normalized = versionKey.trim()
        return normalized.isEmpty() ||
            normalized.equals("all", ignoreCase = true) ||
            normalized.equals(version.key, ignoreCase = true)
    }
}

@Immutable
@JsonClass(generateAdapter = true)
data class RemoteNotice(
    val enabled: Boolean = false,
    val id: String = "",
    val type: String = "info",
    val title: String = "",
    val message: String = "",
    val dismissible: Boolean = false
) {
    val canDismiss: Boolean get() = dismissible && id.isNotBlank()

    internal fun isVisible(maintenance: Boolean, dismissedIds: Set<String>): Boolean =
        enabled && !maintenance && (!canDismiss || id !in dismissedIds)
}

internal fun LibraryFileItem.resolveThumbnailUrl(thumbnails: Map<String, String>): String? {
    if (thumbnails.isEmpty()) return null
    for (key in listOf(id, fileName, title)) {
        val url = thumbnails[key]?.trim()?.toHttpUrlOrNull()
        if (url != null) return url.toString()
    }
    return null
}

@JsonClass(generateAdapter = true)
data class RemoteControlConfig(
    val maintenance: Boolean = false,
    val maintenanceMessage: String = "",
    val announcement: String = "",
    val files: List<LibraryFileItem> = emptyList(),
    val notice: RemoteNotice? = null,
    val thumbnails: Map<String, String> = emptyMap(),
    val requireAdForDownload: Boolean = false
)

sealed interface RemoteCatalogResult {
    data class Success(
        val config: RemoteControlConfig,
        val isSampleMode: Boolean
    ) : RemoteCatalogResult

    data class Error(val uiText: UiText) : RemoteCatalogResult {
        val message: String
            get() = uiText.resolve(EnAppStrings)
    }
}

sealed interface DownloadStage {
    data class Downloading(val progress: Float) : DownloadStage
    data class Completed(val localFile: File) : DownloadStage
    data class Failed(val uiText: UiText) : DownloadStage {
        val message: String
            get() = uiText.resolve(EnAppStrings)
    }
}

class FileRepository(private val context: Context) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .cache(null)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun isSampleModeConfigured(): Boolean {
        val trimmed = REMOTE_CONTROL_URL.trim()
        return trimmed.isEmpty() || trimmed == PLACEHOLDER_CONTROL_URL
    }

    fun detectInstalledVersions(): Map<PubgVersion, InstalledPackageInfo> {
        val pm = context.packageManager
        return PubgVersion.entries.associateWith { version ->
            try {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(version.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(version.packageName, 0)
                }
                InstalledPackageInfo(
                    isInstalled = true,
                    versionName = info.versionName
                )
            } catch (_: PackageManager.NameNotFoundException) {
                InstalledPackageInfo(isInstalled = false)
            } catch (_: Throwable) {
                InstalledPackageInfo(isInstalled = false)
            }
        }
    }

    suspend fun loadRemoteControlConfig(): RemoteCatalogResult = withContext(Dispatchers.IO) {
        if (isSampleModeConfigured()) {
            return@withContext RemoteCatalogResult.Success(
                config = RemoteControlConfig(
                    maintenance = false,
                    maintenanceMessage = "",
                    announcement = "",
                    files = builtInSampleCatalog()
                ),
                isSampleMode = true
            )
        }

        try {
            val cacheBustedUrl = appendCacheBuster(
                REMOTE_CONTROL_URL.trim(),
                System.currentTimeMillis()
            )
            val request = Request.Builder()
                .url(cacheBustedUrl)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    return@withContext RemoteCatalogResult.Error(
                        UiText.Localized { it.errFetchRemoteHttpFormat(code) }
                    )
                }
                val body = response.body?.string()?.trim()
                if (body.isNullOrEmpty()) {
                    return@withContext RemoteCatalogResult.Error(
                        UiText.Localized { it.errRemoteResponseEmpty }
                    )
                }

                val parsed = parseRemoteConfigJson(body)
                    ?: return@withContext RemoteCatalogResult.Error(
                        UiText.Localized { it.errInvalidRemoteJson }
                    )

                RemoteCatalogResult.Success(
                    config = parsed,
                    isSampleMode = false
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Exception) {
            val detail = t.message?.takeIf { it.isNotBlank() }
            RemoteCatalogResult.Error(
                if (t is java.io.IOException || detail == null) {
                    UiText.Localized { it.errNetworkLoadingConfig }
                } else {
                    UiText.Raw(detail)
                }
            )
        }
    }

    internal fun appendCacheBuster(url: String, timestampMillis: Long): String {
        val parsed = url.toHttpUrlOrNull()
        if (parsed != null) {
            return parsed.newBuilder()
                .setQueryParameter("t", timestampMillis.toString())
                .build()
                .toString()
        }
        val separator = if ('?' in url) '&' else '?'
        return "$url${separator}t=$timestampMillis"
    }

    internal fun parseRemoteConfigJson(rawJson: String): RemoteControlConfig? {
        val adapter = moshi.adapter(RemoteControlConfig::class.java)
        return adapter.fromJson(rawJson.trim())
    }

    fun downloadFile(item: LibraryFileItem): Flow<DownloadStage> = flow {
        val safeVersionFolder = item.versionKey.trim().ifEmpty { "all" }.lowercase()
        emit(DownloadStage.Downloading(0f))
        val stagingDir = File(context.filesDir, "downloads/$safeVersionFolder")
        if (!stagingDir.isDirectory && !stagingDir.mkdirs() && !stagingDir.isDirectory) {
            emit(DownloadStage.Failed(UiText.Localized { it.errCreateStagingDir }))
            return@flow
        }

        val finalFile = File(stagingDir, item.fileName)
        val tempFile = File.createTempFile("download-", ".part", stagingDir)
        try {
            if (item.downloadUrl.startsWith("sample://") && isSampleModeConfigured()) {
                val steps = 8
                for (step in 1..steps) {
                    delay(75L)
                    emit(DownloadStage.Downloading(step / steps.toFloat()))
                }
                tempFile.writeText(buildSamplePayload(item))
            } else if (!downloadToTempFile(item, tempFile) { emit(it) }) {
                return@flow
            }

            if (tempFile.length() == 0L) {
                emit(DownloadStage.Failed(UiText.Localized { it.errEmptyDownloadBody }))
                return@flow
            }
            if (isZipFile(item) && !isReadableZip(tempFile)) {
                emit(DownloadStage.Failed(UiText.Localized { it.errInvalidDownloadedZip }))
                return@flow
            }
            // Publish only a completed download; keep the previous file if publishing fails.
            if (!tempFile.renameTo(finalFile) || !finalFile.isFile) {
                emit(DownloadStage.Failed(UiText.Localized { it.errSaveDownloadedFile }))
                return@flow
            }
            emit(DownloadStage.Completed(finalFile))
        } finally {
            tempFile.delete()
        }
    }.catch { err ->
        val detail = err.message?.takeIf { it.isNotBlank() }
        emit(
            DownloadStage.Failed(
                if (detail != null) UiText.Localized { it.errDownloadFailedFormat(detail) }
                else UiText.Localized { it.errNetworkDownloadingFile }
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun isReadableZip(file: File): Boolean = try {
        ZipFile(file).use { it.size() > 0 }
    } catch (_: ZipException) {
        false
    }

    private suspend fun downloadToTempFile(
        item: LibraryFileItem,
        tempFile: File,
        emit: suspend (DownloadStage) -> Unit
    ): Boolean {
        val request = Request.Builder()
            .url(item.downloadUrl.trim())
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val code = response.code
                emit(DownloadStage.Failed(UiText.Localized { it.errDownloadHttpFormat(code) }))
                return false
            }
            val body = response.body ?: run {
                emit(DownloadStage.Failed(UiText.Localized { it.errEmptyDownloadBody }))
                return false
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile, false).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0L) {
                            val progress = (downloadedBytes.toFloat() / totalBytes.toFloat())
                                .coerceIn(0f, 1f)
                            emit(DownloadStage.Downloading(progress))
                        } else {
                            emit(DownloadStage.Downloading(-1f))
                        }
                    }
                    output.flush()
                }
            }
        }

        return true
    }

    fun isZipFile(item: LibraryFileItem): Boolean {
        return item.fileName.trim().endsWith(".zip", ignoreCase = true)
    }

    fun resolveTargetDir(packageName: String, targetSubPath: String): String {
        val cleanSubPath = targetSubPath.trim().trim('/')
        return if (cleanSubPath.isEmpty()) {
            "/storage/emulated/0/Android/data/$packageName"
        } else {
            "/storage/emulated/0/Android/data/$packageName/$cleanSubPath"
        }
    }

    fun resolveTargetFullPath(
        packageName: String,
        item: LibraryFileItem,
        zipExtractsSuffix: String = "extracts zip contents"
    ): String {
        val targetDir = resolveTargetDir(packageName, item.targetSubPath)
        return if (isZipFile(item)) {
            "$targetDir/ ($zipExtractsSuffix)"
        } else {
            "$targetDir/${item.fileName}"
        }
    }

    private fun buildSamplePayload(item: LibraryFileItem): String = buildString {
        appendLine("; DropZone Manager Sample Asset")
        appendLine("; Title: ${item.title}")
        appendLine("; Edition: ${item.versionKey}")
        appendLine("; Category: ${item.category}")
        appendLine("; File: ${item.fileName}")
        appendLine("[DropZonePreset]")
        appendLine("PresetId=${item.id}")
        appendLine("TargetPath=${item.targetSubPath}/${item.fileName}")
        appendLine("Enabled=${item.enabled}")
    }

    fun builtInSampleCatalog(): List<LibraryFileItem> = listOf(
        LibraryFileItem(
            id = "gl_gfx_120fps",
            title = "Smooth 120 FPS Frame Pacing Config",
            versionKey = "global",
            enabled = true,
            sizeLabel = "14 KB",
            downloadUrl = "sample://global/UserCustom.ini",
            targetSubPath = "files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android",
            fileName = "UserCustom.ini",
            category = "Graphics"
        ),
        LibraryFileItem(
            id = "bgmi_90fps_balanced",
            title = "BGMI 90 FPS Low-Thermal Preset",
            versionKey = "bgmi",
            enabled = true,
            sizeLabel = "15 KB",
            downloadUrl = "sample://bgmi/UserCustom.ini",
            targetSubPath = "files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android",
            fileName = "UserCustom.ini",
            category = "Graphics"
        ),
        LibraryFileItem(
            id = "kr_ultra_hdr",
            title = "KR Ultra-Crisp Anti-Aliasing Profile",
            versionKey = "kr",
            enabled = true,
            sizeLabel = "16 KB",
            downloadUrl = "sample://kr/UserCustom.ini",
            targetSubPath = "files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android",
            fileName = "UserCustom.ini",
            category = "Graphics"
        ),
        LibraryFileItem(
            id = "vn_fps_unlock",
            title = "VNG Extreme Frame Rate Config",
            versionKey = "vn",
            enabled = true,
            sizeLabel = "14 KB",
            downloadUrl = "sample://vn/UserCustom.ini",
            targetSubPath = "files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android",
            fileName = "UserCustom.ini",
            category = "Graphics"
        )
    )
}
