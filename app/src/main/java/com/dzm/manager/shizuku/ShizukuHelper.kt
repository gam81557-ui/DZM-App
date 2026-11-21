package com.dzm.manager.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.dzm.manager.BuildConfig
import com.dzm.manager.IFileService
import com.dzm.manager.IFileSource
import com.dzm.manager.service.FileUserService
import com.dzm.manager.ui.i18n.EnAppStrings
import com.dzm.manager.ui.i18n.UiText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.File
import java.io.FileNotFoundException
import kotlin.coroutines.resume

sealed interface ShizukuStatus {
    data object Connected : ShizukuStatus
    data object PermissionRequired : ShizukuStatus
    data object NotRunning : ShizukuStatus
}

class ShizukuLocalizedException(
    val uiText: UiText
) : IllegalStateException(uiText.resolve(EnAppStrings))

class ShizukuHelper(private val context: Context) {

    companion object {
        const val REQUEST_CODE_PERMISSION = 1001
        const val SHIZUKU_PACKAGE_NAME = "rikka.shizuku"
        // Bump when the Binder interface changes so Shizuku replaces the old service.
        private const val FILE_SERVICE_VERSION = 2
        private const val SERVICE_BIND_TIMEOUT_MS = 10_000L
    }

    private var permissionRejectedByServer = false
    private var permissionRequest: CompletableDeferred<Boolean>? = null

    private val _status = MutableStateFlow(computeStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val serviceMutex = Mutex()
    private var listenersRegistered = false
    private var boundService: IFileService? = null
    private var activeConnection: ServiceConnection? = null

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, FileUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("file_service")
            .debuggable(BuildConfig.DEBUG)
            .version(FILE_SERVICE_VERSION)
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        permissionRejectedByServer = false
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        boundService = null
        activeConnection = null
        finishPermissionRequest(false)
        refreshStatus()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_PERMISSION) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                if (granted) permissionRejectedByServer = false
                refreshStatus()
                finishPermissionRequest(granted)
            }
        }

    fun registerListeners() {
        if (listenersRegistered) return
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        listenersRegistered = true
        refreshStatus()
    }

    fun unregisterListeners() {
        if (!listenersRegistered) return
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        listenersRegistered = false
        finishPermissionRequest(false)
        unbindUserServiceQuietly()
    }

    fun refreshStatus() {
        _status.value = computeStatus()
    }

    private fun computeStatus(): ShizukuStatus {
        val binderAlive = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
        if (!binderAlive) {
            return ShizukuStatus.NotRunning
        }
        val granted = try {
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED &&
                    !permissionRejectedByServer
            }
        } catch (_: Throwable) {
            false
        }
        return if (granted) ShizukuStatus.Connected else ShizukuStatus.PermissionRequired
    }

    // Permission requests and their callbacks run on the main thread.
    private fun finishPermissionRequest(granted: Boolean) {
        val request = permissionRequest
        permissionRequest = null
        request?.complete(granted)
    }

    private fun startPermissionRequest(): CompletableDeferred<Boolean>? {
        permissionRequest?.let { return it }
        val request = CompletableDeferred<Boolean>()
        permissionRequest = request
        return try {
            if (!Shizuku.pingBinder() || Shizuku.isPreV11()) {
                finishPermissionRequest(false)
                return null
            }
            Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
            request
        } catch (_: Exception) {
            finishPermissionRequest(false)
            refreshStatus()
            null
        }
    }

    fun requestPermission(): Boolean {
        registerListeners()
        refreshStatus()
        return when (_status.value) {
            ShizukuStatus.Connected -> true
            ShizukuStatus.NotRunning -> false
            ShizukuStatus.PermissionRequired -> startPermissionRequest() != null
        }
    }

    suspend fun awaitPermission() = withContext(Dispatchers.Main.immediate) {
        registerListeners()
        refreshStatus()
        if (_status.value == ShizukuStatus.PermissionRequired) {
            val granted = startPermissionRequest()?.await() == true
            refreshStatus()
            if (!granted && _status.value != ShizukuStatus.NotRunning) {
                throw permissionRequired()
            }
        }
        requirePermission()
    }

    private fun permissionRequired() = ShizukuLocalizedException(
        UiText.Localized { it.errShizukuPermissionRequiredService }
    )

    private fun requirePermission() {
        refreshStatus()
        when (_status.value) {
            ShizukuStatus.Connected -> Unit
            ShizukuStatus.PermissionRequired -> throw permissionRequired()
            ShizukuStatus.NotRunning -> throw ShizukuLocalizedException(
                UiText.Localized { it.msgShizukuStartServiceFirst }
            )
        }
    }

    fun openShizukuApp(): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun checkTargetFileExists(targetDir: String, fileName: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val service = acquireUserService()
                service.fileExists(targetDir, fileName)
            }
        }

    suspend fun copyFileToTarget(
        sourcePath: String,
        targetDir: String,
        fileName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val service = acquireUserService()
            val response = withDownloadedFile(sourcePath) { service.copyFile(it, targetDir, fileName) }
            if (response != "OK") {
                val rawMessage = response?.removePrefix("ERROR:")?.takeIf { it.isNotBlank() }
                throw ShizukuLocalizedException(
                    if (rawMessage != null) {
                        UiText.Raw(rawMessage)
                    } else {
                        UiText.Localized { it.errShizukuUnknownCopy }
                    }
                )
            }
        }
    }

    suspend fun checkZipTargetsExist(sourcePath: String, targetDir: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val service = acquireUserService()
                withDownloadedFile(sourcePath) { service.zipHasExistingFiles(it, targetDir) }
            }
        }

    suspend fun extractZipToTarget(sourcePath: String, targetDir: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val service = acquireUserService()
                val response = withDownloadedFile(sourcePath) { service.extractZip(it, targetDir) }
                if (response != "OK") {
                    val rawMessage = response?.removePrefix("ERROR:")?.takeIf { it.isNotBlank() }
                    throw ShizukuLocalizedException(
                        if (rawMessage != null) {
                            UiText.Raw(rawMessage)
                        } else {
                            UiText.Localized { it.errShizukuUnknownZipExtract }
                        }
                    )
                }
            }
        }

    private inline fun <T> withDownloadedFile(sourcePath: String, read: (IFileSource) -> T): T {
        val input = try {
            val file = File(sourcePath)
            if (!file.isFile || file.length() == 0L) throw FileNotFoundException()
            file.inputStream()
        } catch (_: FileNotFoundException) {
            throw ShizukuLocalizedException(UiText.Localized { it.errOpenDownloadedFile })
        }
        return input.use { read(DownloadFileSource(it)) }
    }

    private suspend fun acquireUserService(): IFileService = withContext(Dispatchers.Main.immediate) {
        awaitPermission()
        try {
            serviceMutex.withLock { bindUserService() }
        } catch (_: SecurityException) {
            // The SDK can cache a grant that the server has since revoked.
            permissionRejectedByServer = true
            awaitPermission()
            try {
                serviceMutex.withLock { bindUserService() }
            } catch (_: SecurityException) {
                permissionRejectedByServer = true
                refreshStatus()
                throw permissionRequired()
            }
        }
    }

    private suspend fun bindUserService(): IFileService {
        requirePermission()
        val existing = boundService
        if (existing != null && existing.asBinder().pingBinder()) {
            return existing
        }

        unbindUserServiceQuietly()

        return try {
            awaitServiceConnection()
        } catch (t: Exception) {
            unbindUserServiceQuietly()
            throw t
        }
    }

    private suspend fun awaitServiceConnection(): IFileService =
        withTimeout(SERVICE_BIND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        if (!continuation.isActive || activeConnection !== this) return
                        val fileService = IFileService.Stub.asInterface(service)
                        if (fileService != null) {
                            boundService = fileService
                            continuation.resume(fileService)
                        } else {
                            activeConnection = null
                            continuation.resumeWith(
                                Result.failure(
                                    ShizukuLocalizedException(
                                        UiText.Localized { it.errShizukuBindFailed }
                                    )
                                )
                            )
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        if (activeConnection === this) {
                            boundService = null
                            activeConnection = null
                        }
                    }
                }

                activeConnection = connection
                try {
                    requirePermission()
                    Shizuku.bindUserService(userServiceArgs, connection)
                } catch (t: Exception) {
                    activeConnection = null
                    continuation.resumeWith(Result.failure(t))
                }
            }
        }

    private fun unbindUserServiceQuietly() {
        val connection = activeConnection
        boundService = null
        activeConnection = null
        if (connection != null && computeStatus() == ShizukuStatus.Connected) {
            runCatching {
                Shizuku.unbindUserService(userServiceArgs, connection, true)
            }
        }
    }
}
