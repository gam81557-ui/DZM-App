package com.app.pubglibrary.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.pubglibrary.ads.RewardedAdResult
import com.app.pubglibrary.data.AppLanguage
import com.app.pubglibrary.data.AppPreferencesState
import com.app.pubglibrary.data.DefaultNetworkReachabilityChecker
import com.app.pubglibrary.data.DownloadStage
import com.app.pubglibrary.data.FileRepository
import com.app.pubglibrary.data.InstalledPackageInfo
import com.app.pubglibrary.data.LanguagePreferences
import com.app.pubglibrary.data.LibraryFileItem
import com.app.pubglibrary.data.NetworkReachabilityChecker
import com.app.pubglibrary.data.PubgVersion
import com.app.pubglibrary.data.RemoteCatalogResult
import com.app.pubglibrary.data.RemoteNotice
import com.app.pubglibrary.shizuku.ShizukuHelper
import com.app.pubglibrary.shizuku.ShizukuLocalizedException
import com.app.pubglibrary.shizuku.ShizukuStatus
import com.app.pubglibrary.ui.i18n.UiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

@Immutable
sealed interface ItemTransferState {
    data class Downloading(val progress: Float) : ItemTransferState
    data object WaitingForAd : ItemTransferState
    data object WaitingForPermission : ItemTransferState
    data object CheckingTarget : ItemTransferState
    data object Applying : ItemTransferState
}

@Immutable
data class OverwritePrompt(
    val item: LibraryFileItem,
    val version: PubgVersion,
    val stagedFilePath: String,
    val targetDir: String,
    val fullTargetPath: String,
    val isZipArchive: Boolean
)

enum class SnackbarKind {
    SUCCESS,
    INFO,
    ERROR
}

data class SnackbarEvent(
    val message: UiText,
    val kind: SnackbarKind
)

@Immutable
data class DropZoneUiState(
    val language: AppLanguage = AppLanguage.EN,
    val shizukuStatus: ShizukuStatus = ShizukuStatus.NotRunning,
    val installedVersions: Map<PubgVersion, InstalledPackageInfo> = emptyMap(),
    val selectedVersion: PubgVersion? = null,
    val maintenance: Boolean = false,
    val maintenanceMessage: String = "",
    val announcement: String = "",
    val remoteNotice: RemoteNotice? = null,
    val thumbnails: Map<String, String> = emptyMap(),
    val allCatalogItems: List<LibraryFileItem> = emptyList(),
    val visibleItems: List<LibraryFileItem> = emptyList(),
    val availableCategories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isRefreshingCatalog: Boolean = false,
    val isSampleMode: Boolean = false,
    val hasLoadedRemoteOnce: Boolean = false,
    val catalogError: UiText? = null,
    val transferStates: Map<String, ItemTransferState> = emptyMap(),
    val appliedItemIds: Set<String> = emptySet(),
    val pendingOverwrite: OverwritePrompt? = null,
    val isNetworkVerified: Boolean = true,
    val showNoticeDialog: Boolean = false,
    val dontShowNoticeAgain: Boolean = false
)

class DropZoneViewModel(
    application: Application,
    private val networkChecker: NetworkReachabilityChecker,
    private val retryIntervalMs: Long = 1_500L,
    catalogLoader: (suspend () -> RemoteCatalogResult)? = null
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        networkChecker = DefaultNetworkReachabilityChecker(application)
    )

    private val shizukuHelper = ShizukuHelper(application)
    private val repository = FileRepository(application)
    private val loadCatalog = catalogLoader ?: repository::loadRemoteControlConfig
    private val languagePreferences = LanguagePreferences(application)

    private val _isNetworkVerified = MutableStateFlow(false)
    private val _languageOverride = MutableStateFlow<AppLanguage?>(null)
    private val _dontShowNoticeOverride = MutableStateFlow<Boolean?>(null)
    private val _shouldShowStartupNotice = MutableStateFlow<Boolean?>(null)
    private val _noticeDismissedInSession = MutableStateFlow(false)
    private val _forceOpenNotice = MutableStateFlow(false)
    private val _installedVersions = MutableStateFlow(repository.detectInstalledVersions())
    private val _selectedVersion = MutableStateFlow<PubgVersion?>(null)
    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    private val _transferStates = MutableStateFlow<Map<String, ItemTransferState>>(emptyMap())
    private val _appliedItemIds = MutableStateFlow<Set<String>>(emptySet())
    private val _pendingOverwrite = MutableStateFlow<OverwritePrompt?>(null)

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    private data class RemoteMeta(
        val maintenance: Boolean = false,
        val maintenanceMessage: String = "",
        val announcement: String = "",
        val notice: RemoteNotice? = null,
        val thumbnails: Map<String, String> = emptyMap(),
        val requireAdForDownload: Boolean = false,
        val items: List<LibraryFileItem> = emptyList(),
        val refreshing: Boolean = false,
        val isSampleMode: Boolean = false,
        val hasLoadedOnce: Boolean = false,
        val error: UiText? = null
    )

    private data class UiPrefsSnapshot(
        val language: AppLanguage = AppLanguage.EN,
        val dontShowNoticeAgain: Boolean = false,
        val dismissedRemoteNoticeIds: Set<String> = emptySet(),
        val showNoticeDialog: Boolean = false,
        val isNetworkVerified: Boolean = false
    )

    private val _remoteMeta = MutableStateFlow(
        RemoteMeta(isSampleMode = repository.isSampleModeConfigured())
    )

    private val activePrefsFlow = combine(
        languagePreferences.preferencesFlow,
        _languageOverride,
        _dontShowNoticeOverride,
        _shouldShowStartupNotice,
        _noticeDismissedInSession,
        _forceOpenNotice,
        _isNetworkVerified
    ) { values ->
        val saved = values[0] as AppPreferencesState
        val langOverride = values[1] as AppLanguage?
        val dontShowOverride = values[2] as Boolean?
        val showStartupNotice = values[3] as Boolean?
        val dismissedInSession = values[4] as Boolean
        val forceOpen = values[5] as Boolean
        val networkVerified = values[6] as Boolean

        val effectiveLang = langOverride ?: saved.language
        val effectiveDontShow = dontShowOverride ?: saved.dontShowNoticeAgain
        val showNotice = networkVerified &&
            (forceOpen || (showStartupNotice == true && !dismissedInSession))
        UiPrefsSnapshot(
            language = effectiveLang,
            dontShowNoticeAgain = effectiveDontShow,
            dismissedRemoteNoticeIds = saved.dismissedRemoteNoticeIds,
            showNoticeDialog = showNotice,
            isNetworkVerified = networkVerified
        )
    }

    val uiState: StateFlow<DropZoneUiState> = combine(
        activePrefsFlow,
        shizukuHelper.status,
        _installedVersions,
        _selectedVersion,
        _selectedCategory,
        _searchQuery,
        _remoteMeta,
        _transferStates,
        _appliedItemIds,
        _pendingOverwrite
    ) { values ->
        val prefs = values[0] as UiPrefsSnapshot
        val shizukuStatus = values[1] as ShizukuStatus
        @Suppress("UNCHECKED_CAST")
        val installed = values[2] as Map<PubgVersion, InstalledPackageInfo>
        val selectedVersion = values[3] as PubgVersion?
        val selectedCategory = values[4] as String
        val query = values[5] as String
        val meta = values[6] as RemoteMeta
        val allItems = meta.items
        @Suppress("UNCHECKED_CAST")
        val transfers = values[7] as Map<String, ItemTransferState>
        @Suppress("UNCHECKED_CAST")
        val appliedIds = values[8] as Set<String>
        val overwrite = values[9] as OverwritePrompt?

        // Strictly keep only enabled == true items matching the active PUBG version (or "all"/empty)
        val versionItems = if (selectedVersion == null) {
            emptyList()
        } else {
            allItems.filter {
                it.enabled && it.matchesVersion(selectedVersion)
            }
        }

        val distinctCategories = versionItems
            .map { it.category.ifBlank { "General" } }
            .distinct()
            .sorted()
        val categories = listOf("All") + distinctCategories
        val effectiveCategory = if (selectedCategory in categories) selectedCategory else "All"

        val trimmedQuery = query.trim()
        val filtered = versionItems.filter { item ->
            val itemCategory = item.category.ifBlank { "General" }
            val matchesCategory = effectiveCategory == "All" ||
                itemCategory.equals(effectiveCategory, ignoreCase = true)
            val matchesQuery = trimmedQuery.isEmpty() ||
                item.title.contains(trimmedQuery, ignoreCase = true) ||
                item.description.contains(trimmedQuery, ignoreCase = true) ||
                item.fileName.contains(trimmedQuery, ignoreCase = true) ||
                item.targetSubPath.contains(trimmedQuery, ignoreCase = true) ||
                itemCategory.contains(trimmedQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        DropZoneUiState(
            language = prefs.language,
            shizukuStatus = shizukuStatus,
            installedVersions = installed,
            selectedVersion = selectedVersion,
            maintenance = meta.maintenance,
            maintenanceMessage = meta.maintenanceMessage,
            announcement = meta.announcement,
            remoteNotice = meta.notice?.takeIf {
                it.isVisible(meta.maintenance, prefs.dismissedRemoteNoticeIds)
            },
            thumbnails = meta.thumbnails,
            allCatalogItems = allItems,
            visibleItems = filtered,
            availableCategories = categories,
            selectedCategory = effectiveCategory,
            searchQuery = query,
            isRefreshingCatalog = meta.refreshing,
            isSampleMode = meta.isSampleMode,
            hasLoadedRemoteOnce = meta.hasLoadedOnce,
            catalogError = meta.error,
            transferStates = transfers,
            appliedItemIds = appliedIds,
            pendingOverwrite = overwrite,
            isNetworkVerified = prefs.isNetworkVerified,
            showNoticeDialog = prefs.showNoticeDialog,
            dontShowNoticeAgain = prefs.dontShowNoticeAgain
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DropZoneUiState(
            language = _languageOverride.value ?: AppLanguage.EN,
            shizukuStatus = shizukuHelper.status.value,
            installedVersions = _installedVersions.value,
            isSampleMode = repository.isSampleModeConfigured(),
            isNetworkVerified = false,
            showNoticeDialog = false
        )
    )

    init {
        viewModelScope.launch {
            languagePreferences.preferencesFlow.collect { saved ->
                if (_shouldShowStartupNotice.value == null) {
                    _shouldShowStartupNotice.value = !saved.dontShowNoticeAgain
                }
                if (_languageOverride.value == null) {
                    _languageOverride.value = saved.language
                }
                if (_dontShowNoticeOverride.value == null) {
                    _dontShowNoticeOverride.value = saved.dontShowNoticeAgain
                }
            }
        }
        startStartupNetworkVerification()
    }

    private fun startStartupNetworkVerification() {
        viewModelScope.launch {
            val triggerChannel = Channel<Unit>(Channel.CONFLATED)
            val observerJob = launch {
                networkChecker.observeNetworkAvailability().collect {
                    triggerChannel.trySend(Unit)
                }
            }

            try {
                while (isActive && !_isNetworkVerified.value) {
                    val reachable = runCatching {
                        networkChecker.isInternetReachable()
                    }.getOrDefault(false)

                    if (reachable) {
                        _isNetworkVerified.value = true
                        refreshCatalog(showSnackbar = false)
                        break
                    }

                    withTimeoutOrNull(retryIntervalMs) {
                        triggerChannel.receive()
                    }
                }
            } finally {
                observerJob.cancel()
                triggerChannel.close()
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _languageOverride.value = language
        viewModelScope.launch {
            languagePreferences.setLanguage(language)
        }
    }

    fun setNoticeDontShowAgain(dontShow: Boolean) {
        _dontShowNoticeOverride.value = dontShow
        viewModelScope.launch {
            languagePreferences.setDontShowNoticeAgain(dontShow)
        }
    }

    fun dismissNoticeDialog() {
        _noticeDismissedInSession.value = true
        _forceOpenNotice.value = false
    }

    fun dismissRemoteNotice(id: String) {
        val meta = _remoteMeta.value
        val notice = meta.notice ?: return
        if (meta.maintenance || !notice.enabled || !notice.canDismiss || notice.id != id) return
        viewModelScope.launch {
            try {
                languagePreferences.dismissRemoteNotice(id)
            } catch (_: IOException) {
                emitSnackbar(UiText.Localized { it.errSaveNoticeDismissal }, SnackbarKind.ERROR)
            }
        }
    }

    fun openNoticeDialog() {
        _forceOpenNotice.value = true
    }

    fun registerShizukuListeners() {
        shizukuHelper.registerListeners()
    }

    fun unregisterShizukuListeners() {
        shizukuHelper.unregisterListeners()
    }

    fun refreshEnvironmentState() {
        shizukuHelper.refreshStatus()
        _installedVersions.value = repository.detectInstalledVersions()
    }

    fun requestShizukuPermission() {
        val requested = shizukuHelper.requestPermission()
        if (!requested) {
            emitSnackbar(
                UiText.Localized { it.msgShizukuNotRunningShort },
                SnackbarKind.ERROR
            )
        }
    }

    fun openShizukuApp() {
        val launched = shizukuHelper.openShizukuApp()
        if (!launched) {
            emitSnackbar(
                UiText.Localized { it.msgShizukuNotInstalled },
                SnackbarKind.ERROR
            )
        }
    }

    fun selectVersion(version: PubgVersion) {
        _selectedVersion.value = version
        _selectedCategory.value = "All"
        _searchQuery.value = ""
        refreshCatalog(showSnackbar = false)
    }

    fun clearSelectedVersion() {
        _selectedVersion.value = null
        _searchQuery.value = ""
        _selectedCategory.value = "All"
        _pendingOverwrite.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun refreshCatalog(showSnackbar: Boolean = true) {
        if (_remoteMeta.value.refreshing) return
        viewModelScope.launch {
            _remoteMeta.update { it.copy(refreshing = true) }
            when (val result = loadCatalog()) {
                is RemoteCatalogResult.Success -> {
                    val config = result.config
                    _remoteMeta.value = RemoteMeta(
                        maintenance = config.maintenance,
                        maintenanceMessage = config.maintenanceMessage.trim(),
                        announcement = config.announcement.trim(),
                        notice = config.notice,
                        thumbnails = config.thumbnails,
                        requireAdForDownload = config.requireAdForDownload,
                        items = config.files,
                        refreshing = false,
                        isSampleMode = result.isSampleMode,
                        hasLoadedOnce = true,
                        error = null
                    )

                    // Clear pending overwrite if maintenance was enabled or item was removed/disabled
                    val currentPrompt = _pendingOverwrite.value
                    if (currentPrompt != null) {
                        val stillEnabled = !config.maintenance && config.files.any {
                            it.id == currentPrompt.item.id &&
                                it.enabled &&
                                it.matchesVersion(currentPrompt.version)
                        }
                        if (!stillEnabled) {
                        _pendingOverwrite.value = null
                        }
                    }

                    if (showSnackbar) {
                        val enabledCount = config.files.count { it.enabled }
                        if (result.isSampleMode) {
                            emitSnackbar(
                                UiText.Localized { it.msgSampleRefreshedFormat(enabledCount) },
                                SnackbarKind.INFO
                            )
                        } else {
                            emitSnackbar(
                                UiText.Localized { it.msgRemoteRefreshedFormat(enabledCount) },
                                SnackbarKind.SUCCESS
                            )
                        }
                    }
                }
                is RemoteCatalogResult.Error -> {
                    // Strictly rely on remote JSON when configured; never fall back to stale sample items
                    _remoteMeta.value = RemoteMeta(
                        maintenance = false,
                        maintenanceMessage = "",
                        announcement = "",
                        refreshing = false,
                        isSampleMode = false,
                        hasLoadedOnce = true,
                        error = result.uiText
                    )
                    _pendingOverwrite.value = null

                    if (showSnackbar) {
                        emitSnackbar(result.uiText, SnackbarKind.ERROR)
                    }
                }
            }
        }
    }

    fun resolveTargetPreview(
        version: PubgVersion,
        item: LibraryFileItem,
        zipExtractsSuffix: String
    ): String {
        return repository.resolveTargetFullPath(
            packageName = version.packageName,
            item = item,
            zipExtractsSuffix = zipExtractsSuffix
        )
    }

    fun downloadAndApply(
        item: LibraryFileItem,
        showRewardedAd: suspend () -> RewardedAdResult
    ) {
        val meta = _remoteMeta.value
        if (meta.maintenance) {
            val message = if (meta.maintenanceMessage.isNotBlank()) {
                UiText.Raw(meta.maintenanceMessage)
            } else {
                UiText.Localized { it.msgMaintenanceDisabledFallback }
            }
            emitSnackbar(message, SnackbarKind.ERROR)
            return
        }
        if (!item.enabled) {
            emitSnackbar(
                UiText.Localized { it.msgFileDisabled },
                SnackbarKind.ERROR
            )
            return
        }

        val version = _selectedVersion.value ?: return
        if (_transferStates.value.containsKey(item.id)) return

        if (item !in meta.items || !item.matchesVersion(version)) return
        if (meta.requireAdForDownload) {
            if (ItemTransferState.WaitingForAd in _transferStates.value.values) return
            _transferStates.update { it + (item.id to ItemTransferState.WaitingForAd) }
        } else {
            _transferStates.update { it + (item.id to ItemTransferState.WaitingForPermission) }
        }

        viewModelScope.launch {
            try {
                if (meta.requireAdForDownload) {
                    when (showRewardedAd()) {
                        RewardedAdResult.COMPLETED -> Unit
                        RewardedAdResult.SKIPPED -> {
                            emitSnackbar(UiText.Localized { it.msgRewardedAdSkipped }, SnackbarKind.INFO)
                            return@launch
                        }
                        RewardedAdResult.UNAVAILABLE -> {
                            emitSnackbar(UiText.Localized { it.msgRewardedAdUnavailable }, SnackbarKind.ERROR)
                            return@launch
                        }
                    }
                    val current = _remoteMeta.value
                    if (current.maintenance || _selectedVersion.value != version || item !in current.items) {
                        return@launch
                    }
                    _transferStates.update { it + (item.id to ItemTransferState.WaitingForPermission) }
                }
                processDownloadAndApply(item, version)
            } catch (err: ShizukuLocalizedException) {
                emitSnackbar(err.uiText, SnackbarKind.ERROR)
            } finally {
                _transferStates.update { it - item.id }
            }
        }
    }

    private suspend fun processDownloadAndApply(item: LibraryFileItem, version: PubgVersion) {
        shizukuHelper.awaitPermission()

        var completedFilePath: String? = null
        repository.downloadFile(item).collect { stage ->
            when (stage) {
                is DownloadStage.Downloading -> {
                    _transferStates.update {
                        it + (item.id to ItemTransferState.Downloading(stage.progress))
                    }
                }
                is DownloadStage.Failed -> {
                    emitSnackbar(stage.uiText, SnackbarKind.ERROR)
                }
                is DownloadStage.Completed -> {
                    completedFilePath = stage.localFile.absolutePath
                }
            }
        }

        val stagedPath = completedFilePath ?: return
        _transferStates.update { it + (item.id to ItemTransferState.CheckingTarget) }

        val targetDir = repository.resolveTargetDir(version.packageName, item.targetSubPath)
        val isZip = repository.isZipFile(item)

        if (isZip) {
            // Replace matching .pak files automatically without prompting
            performShizukuApply(item, version, stagedPath, targetDir, isZip = true)
        } else {
            val fullTargetPath = "$targetDir/${item.fileName}"
            val existsResult = shizukuHelper.checkTargetFileExists(targetDir, item.fileName)
            existsResult.fold(
                onSuccess = { exists ->
                    if (exists) {
                        _pendingOverwrite.value = OverwritePrompt(
                            item = item,
                            version = version,
                            stagedFilePath = stagedPath,
                            targetDir = targetDir,
                            fullTargetPath = fullTargetPath,
                            isZipArchive = false
                        )
                    } else {
                        performShizukuApply(item, version, stagedPath, targetDir, isZip = false)
                    }
                },
                onFailure = { err ->
                    emitSnackbar(
                        UiText.Localized { strings ->
                            val detail = when (err) {
                                is ShizukuLocalizedException -> err.uiText.resolve(strings)
                                else -> err.message?.takeIf { it.isNotBlank() }
                                    ?: strings.errShizukuBindFailed
                            }
                            strings.msgShizukuCheckFailedFormat(detail)
                        },
                        SnackbarKind.ERROR
                    )
                }
            )
        }
    }

    fun confirmOverwrite() {
        val prompt = _pendingOverwrite.value ?: return
        _pendingOverwrite.value = null
        if (_remoteMeta.value.maintenance) {
            emitSnackbar(
                UiText.Localized { it.msgMaintenanceActiveShort },
                SnackbarKind.ERROR
            )
            return
        }
        viewModelScope.launch {
            performShizukuApply(
                item = prompt.item,
                version = prompt.version,
                stagedPath = prompt.stagedFilePath,
                targetDir = prompt.targetDir,
                isZip = prompt.isZipArchive
            )
        }
    }

    fun cancelOverwrite() {
        val prompt = _pendingOverwrite.value ?: return
        _pendingOverwrite.value = null
        emitSnackbar(
            UiText.Localized { it.msgCancelledOverwriteFormat(prompt.item.fileName) },
            SnackbarKind.INFO
        )
    }

    private suspend fun performShizukuApply(
        item: LibraryFileItem,
        version: PubgVersion,
        stagedPath: String,
        targetDir: String,
        isZip: Boolean
    ) {
        _transferStates.update { it + (item.id to ItemTransferState.Applying) }
        val result = try {
            if (isZip) {
                shizukuHelper.extractZipToTarget(stagedPath, targetDir)
            } else {
                shizukuHelper.copyFileToTarget(stagedPath, targetDir, item.fileName)
            }
        } finally {
            _transferStates.update { it - item.id }
        }
        result.fold(
            onSuccess = {
                _appliedItemIds.update { it + item.id }
                emitSnackbar(
                    UiText.Localized { strings ->
                        if (isZip) {
                            strings.msgExtractedSuccessFormat(item.fileName, version.displayName)
                        } else {
                            strings.msgAppliedSuccessFormat(item.fileName, version.displayName)
                        }
                    },
                    SnackbarKind.SUCCESS
                )
            },
            onFailure = { err ->
                emitSnackbar(
                    UiText.Localized { strings ->
                        val detail = when (err) {
                            is ShizukuLocalizedException -> err.uiText.resolve(strings)
                            else -> err.message?.takeIf { it.isNotBlank() }
                                ?: strings.msgUnknownError
                        }
                        strings.msgApplyFailedFormat(detail)
                    },
                    SnackbarKind.ERROR
                )
            }
        )
    }

    private fun emitSnackbar(message: UiText, kind: SnackbarKind) {
        _snackbarEvents.tryEmit(SnackbarEvent(message, kind))
    }

    override fun onCleared() {
        shizukuHelper.unregisterListeners()
        super.onCleared()
    }
}
