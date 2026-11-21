package com.app.pubglibrary.ui.i18n

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import com.app.pubglibrary.data.AppLanguage
import com.app.pubglibrary.data.PubgVersion

const val NOTICE_CREDIT_MESSAGE = "မူရင်းFilesထုတ်လုပ်သူများကိုCrdပေးပါတယ်"

@Immutable
data class AppStrings(
    val language: AppLanguage,
    val appBrandTitle: String,
    val logoContentDescription: String,
    val languageToggleDescription: String,
    val languageEnglishLabel: String,
    val languageMyanmarLabel: String,

    // Shizuku Connection Bar
    val shizukuConnectedTitle: String,
    val shizukuConnectedSubtitle: String,
    val shizukuConnectedBadge: String,
    val shizukuPermissionTitle: String,
    val shizukuPermissionSubtitle: String,
    val shizukuNotRunningTitle: String,
    val shizukuNotRunningSubtitle: String,
    val actionGrant: String,
    val actionOpenShizuku: String,

    // PUBG Version Selector Screen
    val versionSelectorTitle: String,
    val versionSelectorSubtitle: String,
    val detectedCountFormat: (installed: Int, total: Int) -> String,
    val badgeInstalled: String,
    val badgeInstalledVersionFormat: (version: String) -> String,
    val badgeNotInstalled: String,
    val openVersionDescriptionFormat: (editionName: String) -> String,
    val regionLabelGlobal: String,
    val regionLabelBgmi: String,
    val regionLabelKr: String,
    val regionLabelVn: String,

    // File Library Top Bar & Status
    val backToVersionsDescription: String,
    val actionSwitchVersion: String,
    val actionRefresh: String,
    val sourceError: String,
    val sourceFetching: String,
    val sourceSample: String,
    val sourceLiveRemote: String,

    // Banners & Search & Filters
    val announcementTitle: String,
    val maintenanceTitle: String,
    val maintenanceDefaultMessage: String,
    val searchPlaceholder: String,
    val searchDescription: String,
    val clearSearchDescription: String,
    val categoryAll: String,

    // Loading / Error / Empty States
    val loadingRemoteFiles: String,
    val catalogErrorTitle: String,
    val actionRetry: String,
    val actionRetrying: String,
    val emptyFilteredTitle: String,
    val emptyNoFilesTitle: String,
    val emptyFilteredSubtitle: String,
    val emptyNoFilesSubtitle: String,
    val actionClearFilters: String,

    // File Card
    val badgeZipExtract: String,
    val badgeApplied: String,
    val targetPathHeader: String,
    val targetZipExtractsSuffix: String,
    val actionDownloadAndApply: String,
    val statusMaintenanceActiveButton: String,
    val statusDownloadingPercentFormat: (percent: Int) -> String,
    val statusDownloadingIndeterminate: String,
    val statusCheckingTarget: String,
    val statusExtractingViaShizuku: String,
    val statusApplyingViaShizuku: String,

    // Startup Network Verification Gate
    val networkCheckingTitle: String,
    val networkCheckingSubtitle: String,

    // Notice Popup Dialog
    val noticeDontShowAgain: String,
    val noticeCloseButton: String,

    // Overwrite Dialog
    val overwriteDialogTitle: String,
    val overwriteDialogMessageSingle: String,
    val overwriteDialogMessageZipFormat: (fileName: String) -> String,
    val actionOverwrite: String,
    val actionCancel: String,

    // Alerts, Errors & Snackbars
    val msgShizukuNotRunningShort: String,
    val msgShizukuNotInstalled: String,
    val msgSampleRefreshedFormat: (count: Int) -> String,
    val msgRemoteRefreshedFormat: (count: Int) -> String,
    val msgMaintenanceDisabledFallback: String,
    val msgMaintenanceActiveShort: String,
    val msgFileDisabled: String,
    val msgShizukuStartServiceFirst: String,
    val msgShizukuPermissionPromptHint: String,
    val msgShizukuCheckFailedFormat: (detail: String) -> String,
    val msgCancelledOverwriteFormat: (fileName: String) -> String,
    val msgExtractedSuccessFormat: (fileName: String, edition: String) -> String,
    val msgAppliedSuccessFormat: (fileName: String, edition: String) -> String,
    val msgApplyFailedFormat: (detail: String) -> String,
    val msgUnknownError: String,

    // Repository & Shizuku Service Errors
    val errFetchRemoteHttpFormat: (code: Int) -> String,
    val errRemoteResponseEmpty: String,
    val errInvalidRemoteJson: String,
    val errNetworkLoadingConfig: String,
    val errCreateStagingDir: String,
    val errDownloadHttpFormat: (code: Int) -> String,
    val errEmptyDownloadBody: String,
    val errNetworkDownloadingFile: String,
    val errDownloadFailedFormat: (detail: String) -> String,
    val errInvalidDownloadedZip: String,
    val errSaveDownloadedFile: String,
    val errOpenDownloadedFile: String,
    val errShizukuPermissionRequiredService: String,
    val errShizukuServiceNotRunning: String,
    val errShizukuBindFailed: String,
    val errShizukuUnknownCopy: String,
    val errShizukuUnknownZipExtract: String
) {
    fun regionLabelFor(version: PubgVersion): String = when (version) {
        PubgVersion.GLOBAL -> regionLabelGlobal
        PubgVersion.BGMI -> regionLabelBgmi
        PubgVersion.KR -> regionLabelKr
        PubgVersion.VN -> regionLabelVn
    }
}

val EnAppStrings = AppStrings(
    language = AppLanguage.EN,
    appBrandTitle = "DropZone Manager",
    logoContentDescription = "DZM Logo",
    languageToggleDescription = "Switch app language between English and Myanmar",
    languageEnglishLabel = "EN",
    languageMyanmarLabel = "MM",

    shizukuConnectedTitle = "Shizuku Connected",
    shizukuConnectedSubtitle = "Binder active • Privileged IPC ready",
    shizukuConnectedBadge = "LIVE",
    shizukuPermissionTitle = "Permission Required",
    shizukuPermissionSubtitle = "Authorize DropZone Manager in Shizuku",
    shizukuNotRunningTitle = "Shizuku Not Running",
    shizukuNotRunningSubtitle = "Start Shizuku service to enable apply",
    actionGrant = "Grant",
    actionOpenShizuku = "Open Shizuku",

    versionSelectorTitle = "Select PUBG Edition",
    versionSelectorSubtitle = "Choose the target client package for Android/data deployment",
    detectedCountFormat = { installed, total -> "$installed / $total Detected" },
    badgeInstalled = "Installed",
    badgeInstalledVersionFormat = { ver -> "Installed • v$ver" },
    badgeNotInstalled = "Not Installed",
    openVersionDescriptionFormat = { edition -> "Open $edition" },
    regionLabelGlobal = "International Client",
    regionLabelBgmi = "Battlegrounds Mobile India",
    regionLabelKr = "Korea & Japan Client",
    regionLabelVn = "VNG Vietnam Client",

    backToVersionsDescription = "Back to PUBG versions",
    actionSwitchVersion = "Switch",
    actionRefresh = "Refresh",
    sourceError = "Remote sync error",
    sourceFetching = "Fetching live remote catalog…",
    sourceSample = "Built-in sample catalog",
    sourceLiveRemote = "Live remote control active",

    announcementTitle = "Notice",
    maintenanceTitle = "Maintenance Mode",
    maintenanceDefaultMessage = "Downloads are temporarily paused for updates. Please check back shortly.",
    searchPlaceholder = "Search configs, packs, or paths…",
    searchDescription = "Search",
    clearSearchDescription = "Clear search",
    categoryAll = "All",

    loadingRemoteFiles = "Loading remote files…",
    catalogErrorTitle = "Failed to Load Remote Config",
    actionRetry = "Retry",
    actionRetrying = "Retrying…",
    emptyFilteredTitle = "No matching files",
    emptyNoFilesTitle = "No files for this edition",
    emptyFilteredSubtitle = "Try clearing your search query or switching category chips.",
    emptyNoFilesSubtitle = "Tap Refresh to check for newly published files.",
    actionClearFilters = "Clear Filters",

    badgeZipExtract = "ZIP EXTRACT",
    badgeApplied = "Applied",
    targetPathHeader = "TARGET PATH",
    targetZipExtractsSuffix = "extracts zip contents",
    actionDownloadAndApply = "Download & Apply",
    statusMaintenanceActiveButton = "Maintenance Active",
    statusDownloadingPercentFormat = { pct -> "Downloading $pct%" },
    statusDownloadingIndeterminate = "Downloading…",
    statusCheckingTarget = "Checking target…",
    statusExtractingViaShizuku = "Extracting via Shizuku…",
    statusApplyingViaShizuku = "Applying via Shizuku…",

    networkCheckingTitle = "Checking your network...",
    networkCheckingSubtitle = "Verifying internet reachability…",

    noticeDontShowAgain = "Don't show again",
    noticeCloseButton = "Close",

    overwriteDialogTitle = "File Already Exists",
    overwriteDialogMessageSingle = "A file already exists at the target location. Overwrite it with the downloaded file?",
    overwriteDialogMessageZipFormat = { fileName ->
        "Some files inside $fileName already exist in the target directory. Extract and overwrite them?"
    },
    actionOverwrite = "Overwrite",
    actionCancel = "Cancel",

    msgShizukuNotRunningShort = "Shizuku is not running. Start Shizuku first.",
    msgShizukuNotInstalled = "Shizuku app (rikka.shizuku) is not installed on this device.",
    msgSampleRefreshedFormat = { count -> "Sample catalog refreshed ($count items)." },
    msgRemoteRefreshedFormat = { count -> "Remote config updated ($count active files)." },
    msgMaintenanceDisabledFallback = "Maintenance mode is active. Downloads are temporarily disabled.",
    msgMaintenanceActiveShort = "Maintenance mode is active.",
    msgFileDisabled = "This file is currently disabled.",
    msgShizukuStartServiceFirst = "Shizuku is not running. Open Shizuku and start the service first.",
    msgShizukuPermissionPromptHint = "Shizuku permission required. Approve the prompt and try again.",
    msgShizukuCheckFailedFormat = { detail -> "Shizuku check failed: $detail" },
    msgCancelledOverwriteFormat = { fileName -> "Cancelled overwrite for $fileName." },
    msgExtractedSuccessFormat = { fileName, edition -> "Extracted $fileName to $edition." },
    msgAppliedSuccessFormat = { fileName, edition -> "Applied $fileName to $edition." },
    msgApplyFailedFormat = { detail -> "Apply failed: $detail" },
    msgUnknownError = "Unknown error.",

    errFetchRemoteHttpFormat = { code -> "Failed to fetch remote config (HTTP $code)." },
    errRemoteResponseEmpty = "Remote config response was empty.",
    errInvalidRemoteJson = "Invalid remote config JSON format.",
    errNetworkLoadingConfig = "Network error while loading remote configuration.",
    errCreateStagingDir = "Could not create staging directory.",
    errDownloadHttpFormat = { code -> "Download failed (HTTP $code)." },
    errEmptyDownloadBody = "Empty download response body.",
    errNetworkDownloadingFile = "Network error while downloading file.",
    errDownloadFailedFormat = { detail -> "Download failed: $detail" },
    errInvalidDownloadedZip = "The download is not a valid ZIP archive.",
    errSaveDownloadedFile = "Could not save the downloaded file.",
    errOpenDownloadedFile = "The downloaded file is missing or unreadable. Download it again.",
    errShizukuPermissionRequiredService = "Shizuku permission is required before applying files.",
    errShizukuServiceNotRunning = "Shizuku service is not running.",
    errShizukuBindFailed = "Failed to bind IFileService interface.",
    errShizukuUnknownCopy = "Unknown Shizuku copy error.",
    errShizukuUnknownZipExtract = "Unknown Shizuku zip extract error."
)

val MmAppStrings = AppStrings(
    language = AppLanguage.MM,
    appBrandTitle = "DropZone မန်နေဂျာ",
    logoContentDescription = "DZM လိုဂို",
    languageToggleDescription = "အက်ပ်ဘာသာစကားကို အင်္ဂလိပ်နှင့် မြန်မာ ပြောင်းလဲရန်",
    languageEnglishLabel = "EN",
    languageMyanmarLabel = "MM",

    shizukuConnectedTitle = "Shizuku ချိတ်ဆက်ပြီးပါပြီ",
    shizukuConnectedSubtitle = "Binder ချိတ်ဆက်ပြီး • ဖိုင်ထည့်ရန် အသင့်ဖြစ်ပါပြီ",
    shizukuConnectedBadge = "ချိတ်ဆက်ပြီး",
    shizukuPermissionTitle = "ခွင့်ပြုချက် လိုအပ်ပါသည်",
    shizukuPermissionSubtitle = "Shizuku ထဲတွင် DropZone Manager ကို ခွင့်ပြုပေးပါ",
    shizukuNotRunningTitle = "Shizuku မဖွင့်ရသေးပါ",
    shizukuNotRunningSubtitle = "ဖိုင်များထည့်သွင်းရန် Shizuku ဝန်ဆောင်မှုကို အရင်ဖွင့်ပါ",
    actionGrant = "ခွင့်ပြုမည်",
    actionOpenShizuku = "Shizuku ဖွင့်မည်",

    versionSelectorTitle = "PUBG ဗားရှင်း ရွေးချယ်ပါ",
    versionSelectorSubtitle = "Android/data ထဲသို့ ဖိုင်ထည့်သွင်းမည့် ဂိမ်းဗားရှင်းကို ရွေးချယ်ပါ",
    detectedCountFormat = { installed, total -> "$installed / $total တွေ့ရှိသည်" },
    badgeInstalled = "ထည့်သွင်းပြီး",
    badgeInstalledVersionFormat = { ver -> "ထည့်သွင်းပြီး • v$ver" },
    badgeNotInstalled = "မထည့်သွင်းရသေးပါ",
    openVersionDescriptionFormat = { edition -> "$edition ကို ဖွင့်မည်" },
    regionLabelGlobal = "နိုင်ငံတကာ ဗားရှင်း",
    regionLabelBgmi = "အိန္ဒိယ ဗားရှင်း (BGMI)",
    regionLabelKr = "ကိုရီးယားနှင့် ဂျပန် ဗားရှင်း",
    regionLabelVn = "VNG ဗီယက်နမ် ဗားရှင်း",

    backToVersionsDescription = "PUBG ဗားရှင်းများသို့ ပြန်သွားမည်",
    actionSwitchVersion = "ပြောင်းမည်",
    actionRefresh = "ပြန်လည်စစ်ဆေးမည်",
    sourceError = "အွန်လိုင်း ချိတ်ဆက်မှု မအောင်မြင်ပါ",
    sourceFetching = "အွန်လိုင်း ဖိုင်စာရင်း ရယူနေပါသည်…",
    sourceSample = "နမူနာ ဖိုင်စာရင်း အသုံးပြုနေသည်",
    sourceLiveRemote = "အွန်လိုင်း ထိန်းချုပ်စနစ် ချိတ်ဆက်ပြီး",

    announcementTitle = "အသိပေးချက်",
    maintenanceTitle = "ပြုပြင်ထိန်းသိမ်းမှု စနစ်",
    maintenanceDefaultMessage = "ခဏစောင့်ပေးပါ၊ ဖိုင်အသစ် ပြင်ဆင်နေပါသည်။",
    searchPlaceholder = "ဖိုင်အမည်၊ အမျိုးအစား သို့မဟုတ် လမ်းကြောင်း ရှာဖွေပါ…",
    searchDescription = "ရှာဖွေရန်",
    clearSearchDescription = "ရှာဖွေမှု ရှင်းလင်းမည်",
    categoryAll = "အားလုံး",

    loadingRemoteFiles = "အွန်လိုင်း ဖိုင်များ ရယူနေပါသည်…",
    catalogErrorTitle = "အွန်လိုင်း ဖိုင်စာရင်း ဖွင့်၍မရပါ",
    actionRetry = "ပြန်ကြိုးစားမည်",
    actionRetrying = "ပြန်ကြိုးစားနေသည်…",
    emptyFilteredTitle = "ကိုက်ညီသော ဖိုင် မတွေ့ပါ",
    emptyNoFilesTitle = "ဤဗားရှင်းအတွက် ဖိုင်မရှိသေးပါ",
    emptyFilteredSubtitle = "ရှာဖွေမှု စာသားကို ရှင်းလင်းပြီး သို့မဟုတ် အမျိုးအစား ပြောင်း၍ ထပ်မံကြည့်ပါ။",
    emptyNoFilesSubtitle = "ဖိုင်အသစ်များ စစ်ဆေးရန် ပြန်လည်စစ်ဆေးခလုတ်ကို နှိပ်ပါ။",
    actionClearFilters = "စစ်ထုတ်မှု ရှင်းမည်",

    badgeZipExtract = "ZIP ဖြည်ထည့်မည်",
    badgeApplied = "ထည့်သွင်းပြီး",
    targetPathHeader = "ထည့်သွင်းမည့် လမ်းကြောင်း",
    targetZipExtractsSuffix = "Zip ဖိုင်အတွင်းမှ ဖိုင်များကို ဖြည်ထည့်မည်",
    actionDownloadAndApply = "ဒေါင်းလုဒ်ဆွဲ၍ ထည့်သွင်းမည်",
    statusMaintenanceActiveButton = "ပြုပြင်ထိန်းသိမ်းနေဆဲ ဖြစ်ပါသည်",
    statusDownloadingPercentFormat = { pct -> "ဒေါင်းလုဒ်ဆွဲနေသည် $pct%" },
    statusDownloadingIndeterminate = "ဒေါင်းလုဒ်ဆွဲနေသည်…",
    statusCheckingTarget = "ဖိုင်လမ်းကြောင်း စစ်ဆေးနေသည်…",
    statusExtractingViaShizuku = "Shizuku ဖြင့် ဖြည်ထည့်နေသည်…",
    statusApplyingViaShizuku = "Shizuku ဖြင့် ထည့်သွင်းနေသည်…",

    networkCheckingTitle = "အင်တာနက် ချိတ်ဆက်မှုကို စစ်ဆေးနေပါသည်...",
    networkCheckingSubtitle = "အင်တာနက် ချိတ်ဆက်မှု အတည်ပြုပြီးသည်အထိ စောင့်ဆိုင်းနေပါသည်…",

    noticeDontShowAgain = "နောက်ထပ် မပြတော့ပါ",
    noticeCloseButton = "ပိတ်မည်",

    overwriteDialogTitle = "ဖိုင် ရှိနှင့်ပြီးဖြစ်နေပါသည်",
    overwriteDialogMessageSingle = "သတ်မှတ်ထားသော လမ်းကြောင်းတွင် ဖိုင်ရှိနှင့်ပြီးဖြစ်ပါသည်။ ဒေါင်းလုဒ်ဆွဲထားသော ဖိုင်ဖြင့် အစားထိုးမည်လား?",
    overwriteDialogMessageZipFormat = { fileName ->
        "$fileName အတွင်းရှိ ဖိုင်အချို့သည် ဤနေရာတွင် ရှိနှင့်ပြီးဖြစ်ပါသည်။ ဖြည်၍ အစားထိုးမည်လား?"
    },
    actionOverwrite = "အစားထိုးမည်",
    actionCancel = "မလုပ်တော့ပါ",

    msgShizukuNotRunningShort = "Shizuku မဖွင့်ရသေးပါ။ Shizuku ကို အရင်ဖွင့်ပါ။",
    msgShizukuNotInstalled = "ဤစက်တွင် Shizuku အက်ပ် (rikka.shizuku) မထည့်သွင်းရသေးပါ။",
    msgSampleRefreshedFormat = { count -> "နမူနာ ဖိုင်စာရင်း ပြန်လည်စစ်ဆေးပြီးပါပြီ (ဖိုင် $count ခု)။" },
    msgRemoteRefreshedFormat = { count -> "အွန်လိုင်း ဖိုင်စာရင်း အသစ်ရရှိပြီးပါပြီ (ဖိုင် $count ခု)။" },
    msgMaintenanceDisabledFallback = "ပြုပြင်ထိန်းသိမ်းမှု စနစ် ဖွင့်ထားပါသည်။ ဒေါင်းလုဒ်ဆွဲခြင်းကို ခေတ္တပိတ်ထားပါသည်။",
    msgMaintenanceActiveShort = "ပြုပြင်ထိန်းသိမ်းမှု စနစ် ဖွင့်ထားပါသည်။",
    msgFileDisabled = "ဤဖိုင်ကို ယာယီပိတ်ထားပါသည်။",
    msgShizukuStartServiceFirst = "Shizuku မဖွင့်ရသေးပါ။ Shizuku ကိုဖွင့်ပြီး ဝန်ဆောင်မှုကို အရင်စတင်ပါ။",
    msgShizukuPermissionPromptHint = "Shizuku ခွင့်ပြုချက် လိုအပ်ပါသည်။ ခွင့်ပြုပြီး ထပ်မံကြိုးစားပါ။",
    msgShizukuCheckFailedFormat = { detail -> "Shizuku စစ်ဆေးမှု မအောင်မြင်ပါ: $detail" },
    msgCancelledOverwriteFormat = { fileName -> "$fileName အစားထိုးခြင်းကို ပယ်ဖျက်လိုက်ပါသည်။" },
    msgExtractedSuccessFormat = { fileName, edition -> "$fileName ကို $edition ထဲသို့ ဖြည်ထည့်ပြီးပါပြီ။" },
    msgAppliedSuccessFormat = { fileName, edition -> "$fileName ကို $edition ထဲသို့ ထည့်သွင်းပြီးပါပြီ။" },
    msgApplyFailedFormat = { detail -> "ထည့်သွင်းမှု မအောင်မြင်ပါ: $detail" },
    msgUnknownError = "အမည်မသိ အမှားဖြစ်ပွားပါသည်။",

    errFetchRemoteHttpFormat = { code -> "အွန်လိုင်း ဖိုင်စာရင်း ရယူ၍မရပါ (HTTP $code)။" },
    errRemoteResponseEmpty = "အွန်လိုင်း ဖိုင်စာရင်း အလွတ်ဖြစ်နေပါသည်။",
    errInvalidRemoteJson = "အွန်လိုင်း ဖိုင်စာရင်း JSON ပုံစံ မှားယွင်းနေပါသည်။",
    errNetworkLoadingConfig = "အွန်လိုင်း ဖိုင်စာရင်း ရယူစဉ် အင်တာနက်ချိတ်ဆက်မှု အမှားဖြစ်ပွားပါသည်။",
    errCreateStagingDir = "ယာယီဖိုင်သိမ်းဆည်းရန် ဖိုလ်ဒါ ဖန်တီး၍မရပါ။",
    errDownloadHttpFormat = { code -> "ဒေါင်းလုဒ်ဆွဲခြင်း မအောင်မြင်ပါ (HTTP $code)။" },
    errEmptyDownloadBody = "ဒေါင်းလုဒ်ဖိုင် အလွတ်ဖြစ်နေပါသည်။",
    errNetworkDownloadingFile = "ဖိုင်ဒေါင်းလုဒ်ဆွဲစဉ် အင်တာနက်ချိတ်ဆက်မှု အမှားဖြစ်ပွားပါသည်။",
    errDownloadFailedFormat = { detail -> "ဒေါင်းလုဒ်ဆွဲ၍ မရပါ။ $detail" },
    errInvalidDownloadedZip = "ဒေါင်းလုဒ်ဖိုင်သည် မှန်ကန်သော ZIP ဖိုင် မဟုတ်ပါ။",
    errSaveDownloadedFile = "ဒေါင်းလုဒ်ဖိုင်ကို သိမ်းဆည်း၍ မရပါ။",
    errOpenDownloadedFile = "ဒေါင်းလုဒ်ဖိုင် မရှိပါ သို့မဟုတ် ဖတ်၍မရပါ။ ထပ်မံ ဒေါင်းလုဒ်ဆွဲပါ။",
    errShizukuPermissionRequiredService = "ဖိုင်များထည့်သွင်းရန် Shizuku ခွင့်ပြုချက် အရင်လိုအပ်ပါသည်။",
    errShizukuServiceNotRunning = "Shizuku ဝန်ဆောင်မှု မဖွင့်ရသေးပါ။",
    errShizukuBindFailed = "Shizuku ဖိုင်ဝန်ဆောင်မှု (IFileService) ချိတ်ဆက်၍မရပါ။",
    errShizukuUnknownCopy = "Shizuku ဖိုင်ကူးယူမှုတွင် အမည်မသိ အမှားဖြစ်ပွားပါသည်။",
    errShizukuUnknownZipExtract = "Shizuku Zip ဖြည်ထည့်မှုတွင် အမည်မသိ အမှားဖြစ်ပွားပါသည်။"
)

fun stringsFor(language: AppLanguage): AppStrings = when (language) {
    AppLanguage.EN -> EnAppStrings
    AppLanguage.MM -> MmAppStrings
}

@Immutable
sealed interface UiText {
    data class Raw(val text: String) : UiText
    data class Localized(val resolver: (AppStrings) -> String) : UiText

    fun resolve(strings: AppStrings): String = when (this) {
        is Raw -> text
        is Localized -> resolver(strings)
    }
}

val LocalAppStrings = compositionLocalOf { EnAppStrings }
