package com.app.pubglibrary

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.lifecycle.viewModelScope
import com.app.pubglibrary.ads.BannerSlot
import com.app.pubglibrary.ads.RewardedAdResult
import com.app.pubglibrary.data.AppLanguage
import com.app.pubglibrary.data.DownloadStage
import com.app.pubglibrary.data.FileRepository
import com.app.pubglibrary.data.LanguagePreferences
import com.app.pubglibrary.data.LibraryFileItem
import com.app.pubglibrary.data.NetworkReachabilityChecker
import com.app.pubglibrary.data.PubgVersion
import com.app.pubglibrary.data.REMOTE_CONTROL_URL
import com.app.pubglibrary.data.RemoteCatalogResult
import com.app.pubglibrary.data.RemoteNotice
import com.app.pubglibrary.data.resolveThumbnailUrl
import com.app.pubglibrary.data.resolveThumbnailUrls
import com.app.pubglibrary.service.FileUserService
import com.app.pubglibrary.shizuku.DownloadFileSource
import com.app.pubglibrary.shizuku.ShizukuStatus
import com.app.pubglibrary.ui.DescriptionLine
import com.app.pubglibrary.ui.DropZoneUiState
import com.app.pubglibrary.ui.DropZoneViewModel
import com.app.pubglibrary.ui.ItemTransferState
import com.app.pubglibrary.ui.LibraryFileCard
import com.app.pubglibrary.ui.MainScreenContent
import com.app.pubglibrary.ui.OverwritePrompt
import com.app.pubglibrary.ui.SnackbarEvent
import com.app.pubglibrary.ui.i18n.EnAppStrings
import com.app.pubglibrary.ui.i18n.MmAppStrings
import com.app.pubglibrary.ui.parseStructuredDescription
import com.app.pubglibrary.ui.theme.DropZoneTheme
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.util.Base64
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class DropZoneRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `read app name from resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DropZone Manager", appName)
    }

    @Test
    fun `remote control url is configured and not in sample mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FileRepository(context)
        assertEquals(
            "https://raw.githubusercontent.com/gam81557-ui/DZM-V3/refs/heads/main/files.json",
            REMOTE_CONTROL_URL
        )
        assertFalse(repo.isSampleModeConfigured())
    }

    @Test
    fun `parse remote control json schema with description and optional category`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FileRepository(context)
        val rawJson = """
            {
              "maintenance": true,
              "maintenanceMessage": "ခဏစောင့်ပေးပါ၊ ဖိုင်အသစ် ပြင်နေပါတယ်။",
              "announcement": "New season config ready",
              "files": [
                {
                  "id": "file_01",
                  "title": "Sample Pack",
                  "description": "120 FPS smooth config & sound fix",
                  "versionKey": "global",
                  "enabled": true,
                  "sizeLabel": "1.2 MB",
                  "downloadUrl": "https://example.com/pack.zip",
                  "targetSubPath": "",
                  "fileName": "pack.zip"
                },
                {
                  "id": "file_02",
                  "title": "Disabled File",
                  "versionKey": "global",
                  "enabled": false,
                  "sizeLabel": "0.5 MB",
                  "downloadUrl": "https://example.com/old.bin",
                  "targetSubPath": "files/example",
                  "fileName": "old.bin"
                }
              ]
            }
        """.trimIndent()

        val parsed = repo.parseRemoteConfigJson(rawJson)
        assertNotNull(parsed)
        assertTrue(parsed!!.maintenance)
        assertEquals("ခဏစောင့်ပေးပါ၊ ဖိုင်အသစ် ပြင်နေပါတယ်။", parsed.maintenanceMessage)
        assertEquals("New season config ready", parsed.announcement)
        assertEquals(2, parsed.files.size)
        assertEquals("120 FPS smooth config & sound fix", parsed.files[0].description)
        assertEquals("General", parsed.files[0].category)
        assertEquals("", parsed.files[1].description)
        assertFalse(parsed.files[1].enabled)
    }

    @Test
    fun `remote config supports optional notices and resolves thumbnails by id then filename then title`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FileRepository(context)
        val config = requireNotNull(repo.parseRemoteConfigJson(
            """
            {
              "notice": {
                "enabled": true, "id": "release-2", "type": "update",
                "title": "New release", "message": "The archive is ready.", "dismissible": true
              },
              "thumbnails": {
                "file_01": "https://github.com/example/project/releases/download/v2/preview.png",
                "files.zip": "https://example.com/file.png",
                "Sample Pack": "https://example.com/title.png"
              },
              "files": [{
                "id": "file_01", "title": "Sample Pack", "sizeLabel": "1 MB",
                "downloadUrl": "https://example.com/files.zip",
                "targetSubPath": "", "fileName": "files.zip"
              }]
            }
            """
        ))
        assertEquals(
            RemoteNotice(true, "release-2", "update", "New release", "The archive is ready.", true),
            config.notice
        )
        val item = config.files.single()
        assertEquals(config.thumbnails.getValue(item.id), item.resolveThumbnailUrl(config.thumbnails))
        assertEquals("https://example.com/file.png", item.resolveThumbnailUrl(config.thumbnails - item.id))
        assertEquals(
            "https://example.com/title.png",
            item.resolveThumbnailUrl(mapOf(item.fileName to "file:///preview.png", item.title to "https://example.com/title.png"))
        )
        assertEquals(null, item.resolveThumbnailUrl(emptyMap()))

        val disabledNoticeConfig = requireNotNull(repo.parseRemoteConfigJson(
            """{"notice":{"enabled":false,"id":"disabled","title":"Hidden","message":"Still parsed"}}"""
        ))
        assertEquals(RemoteNotice(false, "disabled", "info", "Hidden", "Still parsed"), disabledNoticeConfig.notice)
        assertFalse(requireNotNull(disabledNoticeConfig.notice).isVisible(false, emptySet()))

        val legacyConfig = requireNotNull(repo.parseRemoteConfigJson("""{"announcement":"Legacy"}"""))
        assertEquals("Legacy", legacyConfig.announcement)
        assertEquals(null, legacyConfig.notice)
        assertTrue(legacyConfig.thumbnails.isEmpty())
    }

    @Test
    fun `optional thumbnail data never blocks catalog and valid fallback keys are retained`() {
        val repo = FileRepository(ApplicationProvider.getApplicationContext())
        val file = """{"id":"file_01","title":"Pack","sizeLabel":"1 MB",
            "downloadUrl":"https://example.com/files.zip","targetSubPath":"","fileName":"files.zip"}"""
        for (value in listOf("null", "[]", "\"invalid\"")) {
            val config = requireNotNull(repo.parseRemoteConfigJson(
                """{"thumbnails":$value,"requireAdForDownload":true,"files":[$file]}"""
            ))
            assertTrue(config.thumbnails.isEmpty())
            assertTrue(config.requireAdForDownload)
            assertEquals("file_01", config.files.single().id)
        }

        val config = requireNotNull(repo.parseRemoteConfigJson(
            """{"thumbnails":{"file_01":null,"files.zip":"  https://example.com/good.png  ",
                 "Pack":"https://example.com/backup.png","bad":{},"empty":""},"files":[$file]}"""
        ))
        assertEquals(listOf("https://example.com/good.png", "https://example.com/backup.png"),
            config.files.single().resolveThumbnailUrls(config.thumbnails))
        assertEquals(null, config.thumbnails["file_01"])
        assertEquals(null, config.thumbnails["bad"])
        assertEquals(null, config.thumbnails["empty"])
        assertEquals(listOf("https://example.com/good.png"), config.files.single().resolveThumbnailUrls(
            mapOf("file_01" to "https://example.com/good.png", "files.zip" to "https://example.com/good.png")
        ))
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `thumbnail loader decodes redirected images and reports missing assets`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = ImageLoader.Builder(context).build()
        val png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC"
        )
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/release") { exchange ->
            exchange.responseHeaders.add("Location", "/asset")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/asset") { exchange ->
            exchange.responseHeaders.add("Content-Type", "image/png")
            exchange.sendResponseHeaders(200, png.size.toLong())
            exchange.responseBody.use { it.write(png) }
        }
        server.createContext("/missing") { exchange ->
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.start()
        try {
            suspend fun load(path: String) = withTimeout(10_000) {
                val result = async {
                    loader.execute(
                        ImageRequest.Builder(context)
                            .data("http://127.0.0.1:${server.address.port}/$path")
                            .size(36)
                            .build()
                    )
                }
                while (!result.isCompleted) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    delay(10)
                }
                result.await()
            }
            val redirected = load("release")
            assertTrue((redirected as? ErrorResult)?.throwable?.toString(), redirected is SuccessResult)
            assertTrue(load("missing") is ErrorResult)
        } finally {
            loader.shutdown()
            server.stop(0)
        }
    }

    @Test
    fun `banner gets a visible loading area and retries after failure`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        var failedCallback: (() -> Unit)? = null
        var created = 0
        var released = 0
        var show by mutableStateOf(true)
        try {
            composeRule.setContent {
                if (show) BannerSlot(
                    activity = activity,
                    createBanner = { host, onFailure ->
                        assertEquals(activity, host)
                        created++
                        failedCallback = onFailure
                        View(host)
                    },
                    releaseBanner = { released++ }
                )
            }
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            val bounds = composeRule.onNodeWithTag("unity_banner").getUnclippedBoundsInRoot()
            assertEquals(320f, (bounds.right - bounds.left).value, 0.1f)
            assertEquals(50f, (bounds.bottom - bounds.top).value, 0.1f)
            assertEquals(1, created)

            composeRule.runOnIdle { checkNotNull(failedCallback).invoke() }
            composeRule.mainClock.advanceTimeBy(200L)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("unity_banner").assertDoesNotExist()
            assertEquals(1, released)
            composeRule.mainClock.advanceTimeBy(30_100L)
            composeRule.waitForIdle()
            assertEquals(2, created)
            composeRule.onNodeWithTag("unity_banner").assertIsDisplayed()
            composeRule.runOnIdle { show = false }
            composeRule.mainClock.advanceTimeBy(200L)
            composeRule.waitForIdle()
            assertEquals(2, released)
        } finally {
            composeRule.mainClock.autoAdvance = true
            controller.pause().stop().destroy()
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `thumbnail card loads a working fallback after the first image fails`() {
        val png = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC"
        )
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val hits = AtomicInteger()
        server.createContext("/missing") { exchange ->
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.createContext("/image") { exchange ->
            hits.incrementAndGet()
            exchange.responseHeaders.add("Content-Type", "image/png")
            exchange.sendResponseHeaders(200, png.size.toLong())
            exchange.responseBody.use { it.write(png) }
        }
        server.start()
        try {
            val item = LibraryFileItem(
                id = "image_test", title = "Pack", sizeLabel = "1 MB",
                downloadUrl = "https://example.com/files.zip", targetSubPath = "", fileName = "files.zip"
            )
            val urls = listOf(
                "http://127.0.0.1:${server.address.port}/missing",
                "http://127.0.0.1:${server.address.port}/image"
            )
            composeRule.setContent {
                DropZoneTheme {
                    LibraryFileCard(item, urls, "Target", null, false, false, {})
                }
            }
            composeRule.waitUntil(10_000L) {
                composeRule.onAllNodesWithTag("thumbnail_loaded_image_test").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithTag("thumbnail_loaded_image_test").assertIsDisplayed()
            assertTrue(hits.get() > 0)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `remote notice dismissals persist by id separately from startup notice preferences`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = LanguagePreferences(context)
        preferences.dismissRemoteNotice("first-notice")
        preferences.dismissRemoteNotice("second-notice")

        val saved = LanguagePreferences(context).preferencesFlow.first()
        assertTrue(saved.dismissedRemoteNoticeIds.containsAll(listOf("first-notice", "second-notice")))
        val notice = RemoteNotice(enabled = true, id = "first-notice", dismissible = true)
        assertFalse(notice.isVisible(false, saved.dismissedRemoteNoticeIds))
        val newNotice = notice.copy(id = "new-notice")
        assertTrue(newNotice.isVisible(false, saved.dismissedRemoteNoticeIds))
        assertFalse(newNotice.isVisible(true, saved.dismissedRemoteNoticeIds))
        assertFalse(newNotice.copy(enabled = false).isVisible(false, saved.dismissedRemoteNoticeIds))
        assertTrue(notice.copy(dismissible = false).isVisible(false, saved.dismissedRemoteNoticeIds))
        assertFalse(saved.dontShowNoticeAgain)
    }

    @Test
    fun `resolve target directory handles empty targetSubPath for zip extraction into package root`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FileRepository(context)
        val zipItem = LibraryFileItem(
            id = "zip_01",
            title = "Full Pack",
            versionKey = "global",
            enabled = true,
            sizeLabel = "2.5 MB",
            downloadUrl = "https://example.com/pack.zip",
            targetSubPath = "",
            fileName = "pack.zip",
            description = "Extracts directly into package root"
        )
        assertEquals(
            "/storage/emulated/0/Android/data/com.tencent.ig",
            repo.resolveTargetDir("com.tencent.ig", zipItem.targetSubPath)
        )
        assertEquals(
            "/storage/emulated/0/Android/data/com.tencent.ig/ (extracts zip contents)",
            repo.resolveTargetFullPath("com.tencent.ig", zipItem)
        )
    }

    @Test
    fun `download follows redirects and stages a missing archive without replacing it on invalid response`() {
        val tempRoot = createTempDirectory(prefix = "dropzone_download_test").toFile()
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun getFilesDir(): File = tempRoot
        }
        val expectedPayload = "archive fixture"
        val archive = ByteArrayOutputStream()
        ZipOutputStream(archive).use { output ->
            output.putNextEntry(ZipEntry("fixture.txt"))
            output.write(expectedPayload.toByteArray())
            output.closeEntry()
        }
        val archiveBytes = archive.toByteArray()
        val validResponse = AtomicBoolean(true)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/release") { exchange ->
            exchange.responseHeaders.add("Location", "/archive")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        server.createContext("/archive") { exchange ->
            val bytes = if (validResponse.get()) archiveBytes else "<html>Not a ZIP</html>".toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val repository = FileRepository(context)
            val item = LibraryFileItem(
                id = "archive",
                title = "Archive",
                versionKey = "all",
                sizeLabel = "ZIP",
                downloadUrl = "http://127.0.0.1:${server.address.port}/release",
                targetSubPath = "",
                fileName = "files.zip"
            )
            val stages = runBlocking { repository.downloadFile(item).toList() }
            assertTrue(stages.any { it is DownloadStage.Downloading && it.progress > 0f })
            val staged = (stages.last() as DownloadStage.Completed).localFile
            assertEquals("files.zip", staged.name)
            assertTrue(staged.canonicalPath.startsWith(File(tempRoot, "downloads/all").canonicalPath + File.separator))
            val target = File(tempRoot, "target")
            val applied = staged.inputStream().use {
                FileUserService().extractZip(DownloadFileSource(it), target.absolutePath)
            }
            assertEquals("OK", applied)
            assertEquals(expectedPayload, File(target, "fixture.txt").readText())

            validResponse.set(false)
            val failure = runBlocking { repository.downloadFile(item).toList().last() }
            assertEquals(EnAppStrings.errInvalidDownloadedZip, (failure as DownloadStage.Failed).message)
            assertTrue(staged.readBytes().contentEquals(archiveBytes))
            assertEquals(listOf("files.zip"), staged.parentFile?.list()?.toList())
        } finally {
            server.stop(0)
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `items sharing a filename retain their own downloaded content`() {
        val tempRoot = createTempDirectory(prefix = "dropzone_item_staging").toFile()
        val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
            override fun getFilesDir(): File = tempRoot
        }
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        for ((path, content) in listOf("/one" to "FIRST", "/two" to "SECOND")) {
            server.createContext(path) { exchange ->
                val bytes = content.toByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        }
        server.start()
        try {
            val repo = FileRepository(context)
            val item = LibraryFileItem(
                id = "../first", title = "First", versionKey = "all", sizeLabel = "1 KB",
                downloadUrl = "http://127.0.0.1:${server.address.port}/one",
                targetSubPath = "", fileName = "files.pak"
            )
            val second = item.copy(id = "second", downloadUrl = "http://127.0.0.1:${server.address.port}/two")
            val firstFile = (runBlocking { repo.downloadFile(item).toList().last() } as DownloadStage.Completed).localFile
            val secondFile = (runBlocking { repo.downloadFile(second).toList().last() } as DownloadStage.Completed).localFile
            assertTrue(firstFile != secondFile)
            assertTrue(firstFile.canonicalPath.startsWith(File(tempRoot, "downloads").canonicalPath + File.separator))
            assertEquals("FIRST", firstFile.readText())
            assertEquals("SECOND", secondFile.readText())
            assertEquals(firstFile, (runBlocking { repo.downloadFile(item).toList().last() } as DownloadStage.Completed).localFile)
        } finally {
            server.stop(0)
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `extractZip preserves existing folders and only overwrites matching pak files`() {
        val tempRoot = createTempDirectory(prefix = "dropzone_zip_test").toFile()
        try {
            val targetDir = File(tempRoot, "com.tencent.ig")
            val paksDir = File(targetDir, "files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks").apply {
                mkdirs()
            }
            val matchingPak = File(paksDir, "game_patch_match.pak").apply { writeText("OLD_PAK") }
            val unrelatedPak = File(paksDir, "base_game_keep.pak").apply { writeText("KEEP_BASE_PAK") }
            val existingIni = File(targetDir, "files/Config/settings.ini").apply {
                parentFile?.mkdirs()
                writeText("EXISTING_INI")
            }

            val zipFile = File(tempRoot, "pack.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                zos.putNextEntry(ZipEntry("files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/"))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/game_patch_match.pak"))
                zos.write("NEW_PAK".toByteArray())
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("files/Config/settings.ini"))
                zos.write("SHOULD_NOT_OVERWRITE_INI".toByteArray())
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("files/Config/new_added.txt"))
                zos.write("NEW_FILE".toByteArray())
                zos.closeEntry()
            }

            val service = FileUserService()
            val result = zipFile.inputStream().use { input ->
                service.extractZip(DownloadFileSource(input), targetDir.absolutePath)
            }
            assertEquals("OK", result)

            assertEquals("NEW_PAK", matchingPak.readText())
            assertEquals("KEEP_BASE_PAK", unrelatedPak.readText())
            assertEquals("EXISTING_INI", existingIni.readText())
            assertEquals("NEW_FILE", File(targetDir, "files/Config/new_added.txt").readText())
            assertFalse(File(targetDir, "pack").exists())
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `file application preserves adjacent temporary filenames`() {
        val root = createTempDirectory(prefix = "dropzone_temp_collision").toFile()
        try {
            val target = File(root, "target").apply { mkdirs() }
            val existing = File(target, "settings.ini").apply { writeText("OLD_SETTINGS") }
            val neighbor = File(target, "settings.ini.tmp").apply { writeText("KEEP_NEIGHBOR") }
            val source = DownloadFileSource("NEW_SETTINGS".byteInputStream())
            assertEquals("OK", FileUserService().copyFile(source, target.absolutePath, existing.name))
            assertEquals("NEW_SETTINGS", existing.readText())
            assertEquals("KEEP_NEIGHBOR", neighbor.readText())
            assertEquals(setOf(existing.name, neighbor.name), target.list()!!.toSet())

            val pak = File(target, "game.pak").apply { writeText("OLD_PAK") }
            val pakNeighbor = File(target, "game.pak.tmp").apply { writeText("KEEP_PAK_NEIGHBOR") }
            val archive = ByteArrayOutputStream()
            ZipOutputStream(archive).use { zip ->
                zip.putNextEntry(ZipEntry("game.pak"))
                zip.write("NEW_PAK".toByteArray())
                zip.closeEntry()
            }
            val result = archive.toByteArray().inputStream().use {
                FileUserService().extractZip(DownloadFileSource(it), target.absolutePath)
            }
            assertEquals("OK", result)
            assertEquals("NEW_PAK", pak.readText())
            assertEquals("KEEP_PAK_NEIGHBOR", pakNeighbor.readText())
            assertEquals(setOf(existing.name, neighbor.name, pak.name, pakNeighbor.name), target.list()!!.toSet())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `file application preserves a destination directory and cleans up on failure`() {
        val root = createTempDirectory(prefix = "dropzone_temp_failure").toFile()
        try {
            val target = File(root, "target").apply { mkdirs() }
            val destination = File(target, "settings.ini").apply { mkdirs() }
            val contents = File(destination, "keep").apply { writeText("KEEP") }
            val result = FileUserService().copyFile(
                DownloadFileSource("NEW_SETTINGS".byteInputStream()), target.absolutePath, destination.name
            )
            assertTrue(result.startsWith("ERROR:"))
            assertEquals("KEEP", contents.readText())
            assertEquals(setOf(destination.name), target.list()!!.toSet())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `failed file stream leaves existing target untouched and removes staging file`() {
        val root = createTempDirectory(prefix = "dropzone_interrupted_copy").toFile()
        try {
            val destination = File(root, "settings.ini").apply { writeText("ORIGINAL") }
            val source = object : IFileSource.Stub() {
                var calls = 0
                override fun read(maxBytes: Int): ByteArray {
                    if (calls++ == 0) return "INCOMPLETE".toByteArray()
                    throw java.io.IOException("Stream interrupted")
                }
            }

            val result = FileUserService().copyFile(source, root.absolutePath, destination.name)
            assertTrue(result.startsWith("ERROR:"))
            assertEquals("ORIGINAL", destination.readText())
            assertEquals(setOf(destination.name), root.list()!!.toSet())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `extractZip blocks zip slip traversal paths`() {
        val tempRoot = createTempDirectory(prefix = "dropzone_zipslip_test").toFile()
        try {
            val zipFile = File(tempRoot, "evil.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                zos.putNextEntry(ZipEntry("../../escaped.txt"))
                zos.write("malicious".toByteArray())
                zos.closeEntry()
            }

            val targetDir = File(tempRoot, "com.tencent.ig")
            val service = FileUserService()
            val result = zipFile.inputStream().use { input ->
                service.extractZip(DownloadFileSource(input), targetDir.absolutePath)
            }
            assertTrue(result.startsWith("ERROR:"))
            assertFalse(File(tempRoot, "escaped.txt").exists())
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `parseStructuredDescription preserves line breaks across info features and status`() {
        val raw = """
            Bit: 64-Bit
            Updated: 2026-11-20
            Version: 3.5.0

            Features:
            - Ultra Smooth 120 FPS
            - Less Recoil & Clean View

            100% Safe • Main Account Ready
        """.trimIndent()

        val parsed = parseStructuredDescription(raw)
        assertTrue(parsed[0] == DescriptionLine.InfoLine("Bit", "64-Bit"))
        assertTrue(parsed[1] == DescriptionLine.InfoLine("Updated", "2026-11-20"))
        assertTrue(parsed[2] == DescriptionLine.InfoLine("Version", "3.5.0"))
        assertTrue(parsed[3] == DescriptionLine.SpacerLine)
        assertTrue(parsed[4] == DescriptionLine.SectionHeader("Features :"))
        assertTrue(parsed[5] == DescriptionLine.FeatureLine("Ultra Smooth 120 FPS"))
        assertTrue(parsed[6] == DescriptionLine.FeatureLine("Less Recoil & Clean View"))
        assertTrue(parsed[7] == DescriptionLine.SpacerLine)
        assertTrue(parsed[8] == DescriptionLine.StatusLine("100% Safe • Main Account Ready"))
    }

    @Test
    fun `language preferences persists EN and MM selection in local storage`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialPrefs = LanguagePreferences(context)
        assertEquals(AppLanguage.EN, initialPrefs.languageFlow.first())

        initialPrefs.setLanguage(AppLanguage.MM)

        val reloadedPrefs = LanguagePreferences(context)
        assertEquals(AppLanguage.MM, reloadedPrefs.languageFlow.first())

        reloadedPrefs.setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, LanguagePreferences(context).languageFlow.first())
    }

    @Test
    fun `language toggle switches UI labels between English and Myanmar while keeping JSON data unchanged`() {
        val customItem = LibraryFileItem(
            id = "custom_01",
            title = "Custom JSON Config Title",
            versionKey = "global",
            enabled = true,
            sizeLabel = "3.4 MB",
            downloadUrl = "https://example.com/custom.zip",
            targetSubPath = "",
            fileName = "custom.zip",
            description = "Bit: 64-Bit\nFeatures:\n- Keep Raw Description Unchanged\n100% Safe"
        )
        val initialState = DropZoneUiState(
            language = AppLanguage.EN,
            shizukuStatus = ShizukuStatus.NotRunning,
            selectedVersion = PubgVersion.GLOBAL,
            announcement = "Custom JSON Announcement Text",
            visibleItems = listOf(customItem),
            hasLoadedRemoteOnce = true
        )

        var stateHolder by mutableStateOf(initialState)

        composeRule.setContent {
            DropZoneTheme {
                MainScreenContent(
                    uiState = stateHolder,
                    onLanguageChange = { lang -> stateHolder = stateHolder.copy(language = lang) },
                    onRegisterShizuku = {},
                    onUnregisterShizuku = {},
                    onRefreshEnvironment = {},
                    onRequestShizukuPermission = {},
                    onOpenShizukuApp = {},
                    onSelectVersion = {},
                    onClearSelectedVersion = { stateHolder = stateHolder.copy(selectedVersion = null) },
                    onRefreshCatalog = {},
                    onUpdateSearchQuery = {},
                    onSelectCategory = {},
                    onDownloadAndApply = {},
                    onConfirmOverwrite = {},
                    onCancelOverwrite = {},
                    resolveTargetPreview = { version, item, suffix ->
                        "/storage/emulated/0/Android/data/${version.packageName}/ ($suffix)"
                    },
                    snackbarEvents = emptyFlow<SnackbarEvent>(),
                    banner = { Box(Modifier.fillMaxWidth().height(50.dp).testTag("test_banner")) }
                )
            }
        }

        composeRule.waitForIdle()

        // Verify English UI + Custom JSON Data
        composeRule.onNodeWithText(EnAppStrings.shizukuNotRunningTitle).assertIsDisplayed()
        composeRule.onNodeWithText(EnAppStrings.actionOpenShizuku).assertIsDisplayed()
        composeRule.onNodeWithText("Custom JSON Config Title").assertIsDisplayed()
        composeRule.onNodeWithText("Custom JSON Announcement Text").assertIsDisplayed()
        composeRule.onNodeWithText("Keep Raw Description Unchanged").assertIsDisplayed()
        composeRule.onNodeWithTag("download_apply_button_custom_01")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(EnAppStrings.actionDownloadAndApply).assertIsDisplayed()
        val buttonBounds = composeRule.onNodeWithTag("download_apply_button_custom_01").getUnclippedBoundsInRoot()
        val bannerBounds = composeRule.onNodeWithTag("test_banner").getUnclippedBoundsInRoot()
        assertTrue(buttonBounds.bottom <= bannerBounds.top)

        // Toggle to MM
        composeRule.onNodeWithTag("language_toggle_mm").performClick()
        composeRule.waitForIdle()

        // Verify Myanmar UI + Custom JSON Data remains untouched
        composeRule.onNodeWithText(MmAppStrings.shizukuNotRunningTitle).assertIsDisplayed()
        composeRule.onNodeWithText(MmAppStrings.actionOpenShizuku).assertIsDisplayed()
        composeRule.onNodeWithText(MmAppStrings.announcementTitle).assertIsDisplayed()
        composeRule.onNodeWithText("Custom JSON Config Title")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Custom JSON Announcement Text").assertIsDisplayed()
        composeRule.onNodeWithText("Keep Raw Description Unchanged")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("download_apply_button_custom_01")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(MmAppStrings.actionDownloadAndApply).assertIsDisplayed()

        // Navigate back to Version Selector while still in MM
        composeRule.onNodeWithTag("switch_version_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(MmAppStrings.versionSelectorTitle).assertIsDisplayed()

        // Toggle back to EN
        composeRule.onNodeWithTag("language_toggle_en").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(EnAppStrings.versionSelectorTitle).assertIsDisplayed()
    }

    @Test
    fun `versionKey all and empty match every pubg version and resolve to selected package path`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FileRepository(context)

        val parsed = repo.parseRemoteConfigJson(
            """
            {
              "maintenance": false,
              "files": [
                {
                  "id": "universal_all",
                  "title": "Universal All Pack",
                  "versionKey": "all",
                  "enabled": true,
                  "sizeLabel": "2.0 MB",
                  "downloadUrl": "https://example.com/all.zip",
                  "targetSubPath": "files/UE4Game/Paks",
                  "fileName": "all.zip"
                },
                {
                  "id": "universal_empty",
                  "title": "Universal Empty Pack",
                  "versionKey": "",
                  "enabled": true,
                  "sizeLabel": "1.0 MB",
                  "downloadUrl": "https://example.com/empty.pak",
                  "targetSubPath": "",
                  "fileName": "empty.pak"
                },
                {
                  "id": "universal_omitted",
                  "title": "Universal Omitted Key Pack",
                  "enabled": true,
                  "sizeLabel": "1.5 MB",
                  "downloadUrl": "https://example.com/omitted.zip",
                  "targetSubPath": "",
                  "fileName": "omitted.zip"
                },
                {
                  "id": "global_only",
                  "title": "Global Only Pack",
                  "versionKey": "global",
                  "enabled": true,
                  "sizeLabel": "1.2 MB",
                  "downloadUrl": "https://example.com/global.pak",
                  "targetSubPath": "files/Config",
                  "fileName": "global.pak"
                }
              ]
            }
            """.trimIndent()
        )
        assertNotNull(parsed)
        val items = parsed!!.files

        val allMatchesGlobal = items.filter { it.matchesVersion(PubgVersion.GLOBAL) }.map { it.id }
        assertEquals(
            listOf("universal_all", "universal_empty", "universal_omitted", "global_only"),
            allMatchesGlobal
        )

        val allMatchesBgmi = items.filter { it.matchesVersion(PubgVersion.BGMI) }.map { it.id }
        assertEquals(
            listOf("universal_all", "universal_empty", "universal_omitted"),
            allMatchesBgmi
        )

        val allMatchesKr = items.filter { it.matchesVersion(PubgVersion.KR) }.map { it.id }
        assertEquals(
            listOf("universal_all", "universal_empty", "universal_omitted"),
            allMatchesKr
        )

        val allMatchesVn = items.filter { it.matchesVersion(PubgVersion.VN) }.map { it.id }
        assertEquals(
            listOf("universal_all", "universal_empty", "universal_omitted"),
            allMatchesVn
        )

        val universalAll = items.first { it.id == "universal_all" }
        assertEquals(
            "/storage/emulated/0/Android/data/com.pubg.imobile/files/UE4Game/Paks",
            repo.resolveTargetDir(PubgVersion.BGMI.packageName, universalAll.targetSubPath)
        )
        assertEquals(
            "/storage/emulated/0/Android/data/com.pubg.krmobile/files/UE4Game/Paks",
            repo.resolveTargetDir(PubgVersion.KR.packageName, universalAll.targetSubPath)
        )
        assertEquals(
            "/storage/emulated/0/Android/data/com.vng.pubgmobile/files/UE4Game/Paks",
            repo.resolveTargetDir(PubgVersion.VN.packageName, universalAll.targetSubPath)
        )

        val universalOmitted = items.first { it.id == "universal_omitted" }
        assertEquals(
            "/storage/emulated/0/Android/data/com.pubg.imobile",
            repo.resolveTargetDir(PubgVersion.BGMI.packageName, universalOmitted.targetSubPath)
        )
    }

    @Test
    fun `structured details parser and UI match 3-section card layout with VIP badges and status icons`() {
        val sampleDesc = """
            Bit :- 32-64Bit
            Updated :- 4.7
            Version :- V11 Updated Bypass

            Features :
            Real - Esp ( Line Box)
            Wall Hack Customize°
            Upgrade Bypass Add

            Main Accountt 100% Safe
            Online Update Feature
            Termination Ban Fixed
            Observation Ban Fixed
        """.trimIndent()

        val parsed = parseStructuredDescription(sampleDesc)
        assertTrue(parsed.contains(DescriptionLine.InfoLine("Bit", "32-64Bit", ":-")))
        assertTrue(parsed.contains(DescriptionLine.InfoLine("Updated", "4.7", ":-")))
        assertTrue(parsed.contains(DescriptionLine.InfoLine("Version", "V11 Updated Bypass", ":-")))
        assertTrue(parsed.contains(DescriptionLine.SectionHeader("Features :")))
        assertTrue(parsed.contains(DescriptionLine.FeatureLine("Real - Esp ( Line Box)", showVipBadge = false)))
        assertTrue(parsed.contains(DescriptionLine.FeatureLine("Wall Hack Customize°", showVipBadge = true)))
        assertTrue(parsed.contains(DescriptionLine.FeatureLine("Upgrade Bypass Add", showVipBadge = false)))
        assertTrue(parsed.contains(DescriptionLine.StatusLine("Main Accountt 100% Safe")))
        assertTrue(parsed.contains(DescriptionLine.StatusLine("Online Update Feature")))
        assertTrue(parsed.contains(DescriptionLine.StatusLine("Termination Ban Fixed")))
        assertTrue(parsed.contains(DescriptionLine.StatusLine("Observation Ban Fixed")))

        val item = LibraryFileItem(
            id = "vip_pack_01",
            title = "V11 Bypass Pack",
            versionKey = "all",
            enabled = true,
            sizeLabel = "4.2 MB",
            downloadUrl = "https://example.com/v11.zip",
            targetSubPath = "",
            fileName = "v11.zip",
            description = sampleDesc
        )

        composeRule.setContent {
            DropZoneTheme {
                MainScreenContent(
                    uiState = DropZoneUiState(
                        language = AppLanguage.EN,
                        shizukuStatus = ShizukuStatus.Connected,
                        selectedVersion = PubgVersion.GLOBAL,
                        visibleItems = listOf(item),
                        hasLoadedRemoteOnce = true
                    ),
                    onLanguageChange = {},
                    onRegisterShizuku = {},
                    onUnregisterShizuku = {},
                    onRefreshEnvironment = {},
                    onRequestShizukuPermission = {},
                    onOpenShizukuApp = {},
                    onSelectVersion = {},
                    onClearSelectedVersion = {},
                    onRefreshCatalog = {},
                    onUpdateSearchQuery = {},
                    onSelectCategory = {},
                    onDownloadAndApply = {},
                    onConfirmOverwrite = {},
                    onCancelOverwrite = {},
                    resolveTargetPreview = { version, _, suffix ->
                        "/storage/emulated/0/Android/data/${version.packageName}/ ($suffix)"
                    },
                    snackbarEvents = emptyFlow()
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("description_info_box_vip_pack_01").assertIsDisplayed()
        composeRule.onNodeWithTag("description_features_box_vip_pack_01").assertIsDisplayed()
        composeRule.onNodeWithTag("description_status_box_vip_pack_01")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithText("Bit :- 32-64Bit").assertIsDisplayed()
        composeRule.onNodeWithText("Features").assertIsDisplayed()
        composeRule.onNodeWithText("Wall Hack Customize°").assertIsDisplayed()
        composeRule.onNodeWithText("Main Accountt 100% Safe").assertIsDisplayed()
        composeRule.onNodeWithText("Observation Ban Fixed").assertIsDisplayed()
    }

    @Test
    fun `notice modal keeps credit message unchanged across EN and MM while translating controls and persisting preference`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = LanguagePreferences(context)
        prefs.setDontShowNoticeAgain(false)
        assertFalse(prefs.dontShowNoticeAgainFlow.first())

        var stateHolder by mutableStateOf(
            DropZoneUiState(
                language = AppLanguage.EN,
                shizukuStatus = ShizukuStatus.Connected,
                showNoticeDialog = true,
                dontShowNoticeAgain = false
            )
        )

        composeRule.setContent {
            DropZoneTheme {
                MainScreenContent(
                    uiState = stateHolder,
                    onLanguageChange = { lang -> stateHolder = stateHolder.copy(language = lang) },
                    onToggleNoticeDontShowAgain = { checked ->
                        stateHolder = stateHolder.copy(dontShowNoticeAgain = checked)
                        runBlocking { prefs.setDontShowNoticeAgain(checked) }
                    },
                    onDismissNotice = { stateHolder = stateHolder.copy(showNoticeDialog = false) },
                    onOpenNotice = { stateHolder = stateHolder.copy(showNoticeDialog = true) },
                    onRegisterShizuku = {},
                    onUnregisterShizuku = {},
                    onRefreshEnvironment = {},
                    onRequestShizukuPermission = {},
                    onOpenShizukuApp = {},
                    onSelectVersion = {},
                    onClearSelectedVersion = {},
                    onRefreshCatalog = {},
                    onUpdateSearchQuery = {},
                    onSelectCategory = {},
                    onDownloadAndApply = {},
                    onConfirmOverwrite = {},
                    onCancelOverwrite = {},
                    resolveTargetPreview = { version, _, _ -> "/storage/emulated/0/Android/data/${version.packageName}/" },
                    snackbarEvents = emptyFlow()
                )
            }
        }

        composeRule.waitForIdle()

        // Notice modal displays DZM emblem, exact Burmese credit message, and EN controls
        composeRule.onNodeWithTag("notice_popup_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("notice_logo_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("notice_message_text")
            .assertIsDisplayed()
            .assertTextEquals("မူရင်းFilesထုတ်လုပ်သူများကိုCrdပေးပါတယ်")
        composeRule.onNodeWithText(EnAppStrings.noticeDontShowAgain).assertIsDisplayed()
        composeRule.onNodeWithText(EnAppStrings.noticeCloseButton).assertIsDisplayed()

        // Switch to Myanmar and confirm controls translate while credit message stays exact
        stateHolder = stateHolder.copy(language = AppLanguage.MM)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notice_message_text")
            .assertTextEquals("မူရင်းFilesထုတ်လုပ်သူများကိုCrdပေးပါတယ်")
        composeRule.onNodeWithText(MmAppStrings.noticeDontShowAgain).assertIsDisplayed()
        composeRule.onNodeWithText(MmAppStrings.noticeCloseButton).assertIsDisplayed()

        // Toggle "Don't show again" ON and close dialog
        composeRule.onNodeWithTag("notice_dont_show_toggle").performClick()
        composeRule.waitForIdle()
        assertTrue(LanguagePreferences(context).dontShowNoticeAgainFlow.first())

        composeRule.onNodeWithTag("notice_close_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notice_popup_dialog").assertDoesNotExist()

        // Header and selector DZM brand emblems are displayed once modal closes
        composeRule.onNodeWithTag("header_brand_button").assertIsDisplayed()
        composeRule.onNodeWithTag("header_dzm_logo", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("header_dzm_badge", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("selector_dzm_brand_badge").assertIsDisplayed()

        // Tapping header DZM brand reopens the Notice dialog
        composeRule.onNodeWithTag("header_brand_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notice_popup_dialog").assertIsDisplayed()

        // Reset back for test cleanliness
        prefs.setDontShowNoticeAgain(false)
    }

    @Test
    fun `startup network gate blocks main app and modals until verified and switches EN and MM checking text`() {
        var stateHolder by mutableStateOf(
            DropZoneUiState(
                language = AppLanguage.EN,
                isNetworkVerified = false,
                showNoticeDialog = true,
                selectedVersion = PubgVersion.GLOBAL
            )
        )

        composeRule.setContent {
            DropZoneTheme {
                MainScreenContent(
                    uiState = stateHolder,
                    onLanguageChange = {},
                    onRegisterShizuku = {},
                    onUnregisterShizuku = {},
                    onRefreshEnvironment = {},
                    onRequestShizukuPermission = {},
                    onOpenShizukuApp = {},
                    onSelectVersion = {},
                    onClearSelectedVersion = {},
                    onRefreshCatalog = {},
                    onUpdateSearchQuery = {},
                    onSelectCategory = {},
                    onDownloadAndApply = {},
                    onConfirmOverwrite = {},
                    onCancelOverwrite = {},
                    resolveTargetPreview = { version, _, _ -> "/storage/emulated/0/Android/data/${version.packageName}/" },
                    snackbarEvents = emptyFlow()
                )
            }
        }

        composeRule.waitForIdle()

        // Fullscreen checking gate must block all main screens and modals
        composeRule.onNodeWithTag("network_gate_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("network_checking_title")
            .assertIsDisplayed()
            .assertTextEquals("Checking your network...")
        composeRule.onNodeWithTag("notice_popup_dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("version_selector_screen").assertDoesNotExist()
        composeRule.onNodeWithTag("file_library_screen").assertDoesNotExist()
        composeRule.onNodeWithTag("shizuku_status_bar").assertDoesNotExist()

        // Myanmar text when MM is selected
        stateHolder = stateHolder.copy(language = AppLanguage.MM)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("network_checking_title")
            .assertIsDisplayed()
            .assertTextEquals(MmAppStrings.networkCheckingTitle)
        composeRule.onNodeWithTag("notice_popup_dialog").assertDoesNotExist()

        // Once verified, gate clears and main app + notice modal appear
        stateHolder = stateHolder.copy(isNetworkVerified = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("network_gate_screen").assertDoesNotExist()
        composeRule.onNodeWithTag("notice_popup_dialog").assertIsDisplayed()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `required reward blocks the download until completion and omitted flag bypasses ads`() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repository = FileRepository(app)
        val itemJson = """
            {
              "id":"test", "title":"Pack", "sizeLabel":"1 MB",
              "downloadUrl":"https://example.com/files.zip", "fileName":"files.zip",
              "targetSubPath":"files"
            }
        """.trimIndent()
        val network = object : NetworkReachabilityChecker {
            override suspend fun isInternetReachable() = true
            override fun observeNetworkAvailability(): Flow<Unit> = emptyFlow()
        }

        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            for (flag in listOf("", """"requireAdForDownload":true,""")) {
                val config = checkNotNull(repository.parseRemoteConfigJson("""{${flag}"files":[${itemJson}]}"""))
                val gated = flag.isNotEmpty()
                assertEquals(gated, config.requireAdForDownload)
                val item = config.files.single()
                val viewModel = DropZoneViewModel(
                    application = app,
                    networkChecker = network,
                    catalogLoader = { RemoteCatalogResult.Success(config, isSampleMode = false) }
                )
                val messages = mutableListOf<String>()
                val stateJob = backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
                val eventJob = backgroundScope.launch(dispatcher) {
                    viewModel.snackbarEvents.collect { messages += it.message.resolve(EnAppStrings) }
                }

                try {
                    viewModel.selectVersion(PubgVersion.GLOBAL)
                    // DataStore runs on real IO, so use a real timeout for the first combined state.
                    val readyState = withContext(Dispatchers.Default) {
                        withTimeoutOrNull(5_000L) {
                            viewModel.uiState.first {
                                it.isNetworkVerified && it.hasLoadedRemoteOnce && item in it.visibleItems
                            }
                        }
                    }
                    checkNotNull(readyState) { "Timed out waiting for preferences and the test catalog in uiState" }
                    if (gated) {
                        // A pending overwrite must keep its staged file until the user decides.
                        val field = DropZoneViewModel::class.java.getDeclaredField("_pendingOverwrite")
                        field.isAccessible = true
                        @Suppress("UNCHECKED_CAST")
                        val pending = field.get(viewModel) as MutableStateFlow<OverwritePrompt?>
                        val prompt = OverwritePrompt(item, PubgVersion.GLOBAL, "staged", "target", "target/file", false)
                        pending.value = prompt
                        var adRequested = false
                        viewModel.downloadAndApply(item) {
                            adRequested = true
                            RewardedAdResult.COMPLETED
                        }
                        runCurrent()
                        assertFalse(adRequested)
                        assertEquals(prompt, viewModel.uiState.value.pendingOverwrite)
                        assertTrue(viewModel.uiState.value.transferStates.isEmpty())
                        pending.value = null
                        runCurrent()

                        for ((outcome, message) in listOf(
                            RewardedAdResult.SKIPPED to EnAppStrings.msgRewardedAdSkipped,
                            RewardedAdResult.UNAVAILABLE to EnAppStrings.msgRewardedAdUnavailable,
                            RewardedAdResult.COMPLETED to EnAppStrings.msgShizukuStartServiceFirst
                        )) {
                            val reward = CompletableDeferred<RewardedAdResult>()
                            viewModel.downloadAndApply(item) { reward.await() }
                            runCurrent()
                            assertEquals(ItemTransferState.WaitingForAd, viewModel.uiState.value.transferStates[item.id])
                            assertTrue(messages.isEmpty())
                            reward.complete(outcome)
                            runCurrent()
                            assertEquals(listOf(message), messages)
                            assertTrue(viewModel.uiState.value.transferStates.isEmpty())
                            messages.clear()
                        }
                    } else {
                        viewModel.downloadAndApply(item) { error("No ad should be requested") }
                        runCurrent()
                        assertEquals(listOf(EnAppStrings.msgShizukuStartServiceFirst), messages)
                    }
                } finally {
                    eventJob.cancel()
                    stateJob.cancel()
                    viewModel.viewModelScope.cancel()
                    viewModel.unregisterShizukuListeners()
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `viewmodel continuously rechecks reachability and unlocks only when real internet is verified`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val prefs = LanguagePreferences(app)
        prefs.setDontShowNoticeAgain(false)

        val attempts = AtomicInteger(0)
        val allowConnection = AtomicBoolean(false)

        val fakeChecker = object : NetworkReachabilityChecker {
            override suspend fun isInternetReachable(): Boolean {
                attempts.incrementAndGet()
                return allowConnection.get()
            }

            override fun observeNetworkAvailability(): Flow<Unit> = emptyFlow()
        }

        val viewModel = DropZoneViewModel(
            application = app,
            networkChecker = fakeChecker,
            retryIntervalMs = 25L
        )

        val collectJob = launch {
            viewModel.uiState.collect { }
        }

        try {
            // Wait across multiple failed retry cycles and confirm state stays blocked
            withTimeout(3_000L) {
                while (attempts.get() < 3) {
                    Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(35))
                    delay(10L)
                }
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertFalse(viewModel.uiState.value.isNetworkVerified)
            assertFalse(viewModel.uiState.value.showNoticeDialog)

            // Now simulate real internet becoming reachable
            allowConnection.set(true)
            withTimeout(3_000L) {
                while (!viewModel.uiState.value.isNetworkVerified) {
                    Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(35))
                    delay(10L)
                }
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            assertTrue(viewModel.uiState.value.isNetworkVerified)
            assertTrue(viewModel.uiState.value.showNoticeDialog)
        } finally {
            collectJob.cancel()
        }
    }
}
