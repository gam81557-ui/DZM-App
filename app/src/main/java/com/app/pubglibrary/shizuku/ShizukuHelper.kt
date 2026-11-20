package com.app.pubglibrary.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.app.pubglibrary.BuildConfig
import com.app.pubglibrary.IFileService
import com.app.pubglibrary.service.FileUserService
import com.app.pubglibrary.ui.i18n.EnAppStrings
import com.app.pubglibrary.ui.i18n.UiText
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
        private const val SERVICE_BIND_TIMEOUT_MS = 10_000L
    }

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
            .version(BuildConfig.VERSION_CODE)
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        boundService = null
        activeConnection = null
        refreshStatus()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == REQUEST_CODE_PERMISSION) {
                refreshStatus()
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
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (_: Throwable) {
            false
        }
        return if (granted) ShizukuStatus.Connected else ShizukuStatus.PermissionRequired
    }

    fun requestPermission(): Boolean {
        val binderAlive = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
        if (!binderAlive) {
            refreshStatus()
            return false
        }
        return try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                refreshStatus()
                true
            } else {
                Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
                true
            }
        } catch (_: Throwable) {
            refreshStatus()
            false
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
            val response = service.copyFile(sourcePath, targetDir, fileName)
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
                service.zipHasExistingFiles(sourcePath, targetDir)
            }
        }

    suspend fun extractZipToTarget(sourcePath: String, targetDir: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val service = acquireUserService()
                val response = service.extractZip(sourcePath, targetDir)
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

    private suspend fun acquireUserService(): IFileService = serviceMutex.withLock {
        val currentStatus = computeStatus()
        _status.value = currentStatus
        if (currentStatus != ShizukuStatus.Connected) {
            throw ShizukuLocalizedException(
                when (currentStatus) {
                    ShizukuStatus.PermissionRequired ->
                        UiText.Localized { it.errShizukuPermissionRequiredService }
                    ShizukuStatus.NotRunning ->
                        UiText.Localized { it.errShizukuServiceNotRunning }
                    ShizukuStatus.Connected -> UiText.Raw("")
                }
            )
        }

        val existing = boundService
        if (existing != null && existing.asBinder().pingBinder()) {
            return@withLock existing
        }

        unbindUserServiceQuietly()

        withTimeout(SERVICE_BIND_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        val fileService = IFileService.Stub.asInterface(service)
                        if (fileService != null) {
                            boundService = fileService
                            activeConnection = this
                            if (continuation.isActive) {
                                continuation.resume(fileService)
                            }
                        } else if (continuation.isActive) {
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
                        boundService = null
                        activeConnection = null
                    }
                }

                continuation.invokeOnCancellation {
                    runCatching { Shizuku.unbindUserService(userServiceArgs, connection, true) }
                }

                try {
                    Shizuku.bindUserService(userServiceArgs, connection)
                } catch (t: Throwable) {
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(t))
                    }
                }
            }
        }
    }

    private fun unbindUserServiceQuietly() {
        val connection = activeConnection
        boundService = null
        activeConnection = null
        if (connection != null) {
            runCatching {
                Shizuku.unbindUserService(userServiceArgs, connection, true)
            }
        }
    }
}
