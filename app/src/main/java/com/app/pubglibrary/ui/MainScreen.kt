package com.app.pubglibrary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pubglibrary.R
import com.app.pubglibrary.data.AppLanguage
import com.app.pubglibrary.data.InstalledPackageInfo
import com.app.pubglibrary.data.LibraryFileItem
import com.app.pubglibrary.data.PubgVersion
import com.app.pubglibrary.shizuku.ShizukuStatus
import com.app.pubglibrary.ui.i18n.LocalAppStrings
import com.app.pubglibrary.ui.i18n.NOTICE_CREDIT_MESSAGE
import com.app.pubglibrary.ui.i18n.UiText
import com.app.pubglibrary.ui.i18n.stringsFor
import com.app.pubglibrary.ui.theme.AmberContainer
import com.app.pubglibrary.ui.theme.AmberSignal
import com.app.pubglibrary.ui.theme.AmberText
import com.app.pubglibrary.ui.theme.CrimsonAlert
import com.app.pubglibrary.ui.theme.CrimsonContainer
import com.app.pubglibrary.ui.theme.CrimsonText
import com.app.pubglibrary.ui.theme.EmeraldContainer
import com.app.pubglibrary.ui.theme.EmeraldHud
import com.app.pubglibrary.ui.theme.EmeraldText
import com.app.pubglibrary.ui.theme.HairlineBorder
import com.app.pubglibrary.ui.theme.JetBrainsMonoFamily
import com.app.pubglibrary.ui.theme.ObsidianBg
import com.app.pubglibrary.ui.theme.SlateElevated
import com.app.pubglibrary.ui.theme.SlateInput
import com.app.pubglibrary.ui.theme.SlateSurface
import com.app.pubglibrary.ui.theme.TextMuted
import com.app.pubglibrary.ui.theme.TextPrimary
import com.app.pubglibrary.ui.theme.TextSecondary

@Composable
fun MainScreen(
    viewModel: DropZoneViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainScreenContent(
        uiState = uiState,
        onLanguageChange = { viewModel.setLanguage(it) },
        onToggleNoticeDontShowAgain = { viewModel.setNoticeDontShowAgain(it) },
        onDismissNotice = { viewModel.dismissNoticeDialog() },
        onOpenNotice = { viewModel.openNoticeDialog() },
        onRegisterShizuku = { viewModel.registerShizukuListeners() },
        onUnregisterShizuku = { viewModel.unregisterShizukuListeners() },
        onRefreshEnvironment = { viewModel.refreshEnvironmentState() },
        onRequestShizukuPermission = { viewModel.requestShizukuPermission() },
        onOpenShizukuApp = { viewModel.openShizukuApp() },
        onSelectVersion = { viewModel.selectVersion(it) },
        onClearSelectedVersion = { viewModel.clearSelectedVersion() },
        onRefreshCatalog = { showSnackbar -> viewModel.refreshCatalog(showSnackbar) },
        onUpdateSearchQuery = { viewModel.updateSearchQuery(it) },
        onSelectCategory = { viewModel.selectCategory(it) },
        onDownloadAndApply = { viewModel.downloadAndApply(it) },
        onConfirmOverwrite = { viewModel.confirmOverwrite() },
        onCancelOverwrite = { viewModel.cancelOverwrite() },
        resolveTargetPreview = { version, item, suffix ->
            viewModel.resolveTargetPreview(version, item, suffix)
        },
        snackbarEvents = viewModel.snackbarEvents,
        modifier = modifier
    )
}

@Composable
internal fun MainScreenContent(
    uiState: DropZoneUiState,
    onLanguageChange: (AppLanguage) -> Unit,
    onRegisterShizuku: () -> Unit,
    onUnregisterShizuku: () -> Unit,
    onRefreshEnvironment: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onSelectVersion: (PubgVersion) -> Unit,
    onClearSelectedVersion: () -> Unit,
    onRefreshCatalog: (Boolean) -> Unit,
    onUpdateSearchQuery: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onDownloadAndApply: (LibraryFileItem) -> Unit,
    onConfirmOverwrite: () -> Unit,
    onCancelOverwrite: () -> Unit,
    resolveTargetPreview: (PubgVersion, LibraryFileItem, String) -> String,
    snackbarEvents: kotlinx.coroutines.flow.Flow<SnackbarEvent>,
    modifier: Modifier = Modifier,
    onToggleNoticeDontShowAgain: (Boolean) -> Unit = {},
    onDismissNotice: () -> Unit = {},
    onOpenNotice: () -> Unit = {}
) {
    val appStrings = remember(uiState.language) { stringsFor(uiState.language) }
    val latestStrings by rememberUpdatedState(appStrings)
    val snackbarHostState = remember { SnackbarHostState() }
    var lastSnackbarKind by remember { mutableStateOf(SnackbarKind.INFO) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        onRegisterShizuku()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshEnvironment()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onUnregisterShizuku()
        }
    }

    LaunchedEffect(snackbarEvents) {
        snackbarEvents.collect { event ->
            lastSnackbarKind = event.kind
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = event.message.resolve(latestStrings),
                duration = SnackbarDuration.Short
            )
        }
    }

    if (uiState.isNetworkVerified && uiState.selectedVersion != null) {
        BackHandler {
            onClearSelectedVersion()
        }
    }

    CompositionLocalProvider(LocalAppStrings provides appStrings) {
        AnimatedContent(
            targetState = uiState.isNetworkVerified,
            modifier = modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn(tween(240)) togetherWith fadeOut(tween(180))
            },
            label = "startup_network_gate"
        ) { isVerified ->
            if (!isVerified) {
                StartupNetworkGateScreen()
            } else {
                if (uiState.showNoticeDialog) {
                    NoticePopupDialog(
                        dontShowAgain = uiState.dontShowNoticeAgain,
                        onToggleDontShowAgain = onToggleNoticeDontShowAgain,
                        onDismiss = onDismissNotice
                    )
                }

                uiState.pendingOverwrite?.let { prompt ->
                    OverwriteConfirmDialog(
                        prompt = prompt,
                        onConfirm = onConfirmOverwrite,
                        onDismiss = onCancelOverwrite
                    )
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBg)
                        .drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x22F59E0B),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.82f, 0f),
                                    radius = size.width * 0.9f
                                )
                            )
                        },
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) { data ->
                            val containerColor = when (lastSnackbarKind) {
                                SnackbarKind.SUCCESS -> EmeraldContainer
                                SnackbarKind.ERROR -> CrimsonContainer
                                SnackbarKind.INFO -> SlateElevated
                            }
                            val contentColor = when (lastSnackbarKind) {
                                SnackbarKind.SUCCESS -> EmeraldText
                                SnackbarKind.ERROR -> CrimsonText
                                SnackbarKind.INFO -> TextPrimary
                            }
                            val borderColor = when (lastSnackbarKind) {
                                SnackbarKind.SUCCESS -> EmeraldHud.copy(alpha = 0.5f)
                                SnackbarKind.ERROR -> CrimsonAlert.copy(alpha = 0.5f)
                                SnackbarKind.INFO -> HairlineBorder
                            }
                            Snackbar(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                                containerColor = containerColor,
                                contentColor = contentColor,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = data.visuals.message,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        TopUtilityHeaderBar(
                            language = uiState.language,
                            onLanguageChange = onLanguageChange,
                            onOpenNotice = onOpenNotice
                        )

                        ShizukuConnectionBar(
                            status = uiState.shizukuStatus,
                            onGrantClick = onRequestShizukuPermission,
                            onOpenShizukuClick = onOpenShizukuApp
                        )

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            AnimatedContent(
                                targetState = uiState.selectedVersion,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 860.dp),
                                transitionSpec = {
                                    if (targetState != null) {
                                        (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 5 } +
                                            fadeIn(tween(180)))
                                            .togetherWith(
                                                slideOutHorizontally(tween(150)) { -it / 6 } + fadeOut(tween(120))
                                            )
                                    } else {
                                        (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it / 5 } +
                                            fadeIn(tween(180)))
                                            .togetherWith(
                                                slideOutHorizontally(tween(150)) { it / 6 } + fadeOut(tween(120))
                                            )
                                    }
                                },
                                label = "screen_transition"
                            ) { selectedVersion ->
                                if (selectedVersion == null) {
                                    VersionSelectorScreen(
                                        installedVersions = uiState.installedVersions,
                                        onSelectVersion = onSelectVersion
                                    )
                                } else {
                                    val stableResolveTargetPreview = remember(
                                        selectedVersion,
                                        appStrings.targetZipExtractsSuffix,
                                        resolveTargetPreview
                                    ) {
                                        { item: LibraryFileItem ->
                                            resolveTargetPreview(
                                                selectedVersion,
                                                item,
                                                appStrings.targetZipExtractsSuffix
                                            )
                                        }
                                    }
                                    FileLibraryScreen(
                                        version = selectedVersion,
                                        installedInfo = uiState.installedVersions[selectedVersion],
                                        maintenance = uiState.maintenance,
                                        maintenanceMessage = uiState.maintenanceMessage,
                                        announcement = uiState.announcement,
                                        items = uiState.visibleItems,
                                        availableCategories = uiState.availableCategories,
                                        selectedCategory = uiState.selectedCategory,
                                        searchQuery = uiState.searchQuery,
                                        isRefreshing = uiState.isRefreshingCatalog,
                                        isSampleMode = uiState.isSampleMode,
                                        hasLoadedRemoteOnce = uiState.hasLoadedRemoteOnce,
                                        catalogError = uiState.catalogError,
                                        transferStates = uiState.transferStates,
                                        appliedItemIds = uiState.appliedItemIds,
                                        resolveTargetPreview = stableResolveTargetPreview,
                                        onSwitchVersion = onClearSelectedVersion,
                                        onRefresh = { onRefreshCatalog(true) },
                                        onRetry = { onRefreshCatalog(false) },
                                        onSearchChange = onUpdateSearchQuery,
                                        onCategorySelect = onSelectCategory,
                                        onDownloadAndApply = onDownloadAndApply
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartupNetworkGateScreen(
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x28F59E0B),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.35f),
                        radius = size.width * 0.85f
                    )
                )
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp)
            .testTag("network_gate_screen"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
            color = SlateSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, HairlineBorder),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AmberSignal.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DzmLogoEmblem(
                        size = 80.dp,
                        cornerRadius = 22.dp,
                        borderWidth = 1.5.dp,
                        modifier = Modifier.testTag("network_gate_dzm_logo")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DzmMonogramBadge()

                Spacer(modifier = Modifier.height(24.dp))

                CircularProgressIndicator(
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("network_checking_spinner"),
                    color = AmberSignal,
                    trackColor = SlateElevated,
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = strings.networkCheckingTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("network_checking_title")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = strings.networkCheckingSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("network_checking_subtitle")
                )
            }
        }
    }
}

@Composable
private fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
private fun DzmLogoEmblem(
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 1.dp
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(ObsidianBg)
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        AmberSignal.copy(alpha = 0.85f),
                        EmeraldHud.copy(alpha = 0.55f),
                        AmberSignal.copy(alpha = 0.85f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_dzm_logo),
            contentDescription = strings.logoContentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DzmMonogramBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = AmberContainer,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, AmberSignal.copy(alpha = 0.55f))
    ) {
        Text(
            text = "DZM",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = AmberText,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TopUtilityHeaderBar(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onOpenNotice: () -> Unit
) {
    val strings = LocalAppStrings.current
    val logoInteraction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .pressScale(logoInteraction, pressedScale = 0.98f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = logoInteraction,
                    indication = androidx.compose.material3.ripple(),
                    onClick = onOpenNotice
                )
                .padding(vertical = 2.dp, horizontal = 2.dp)
                .testTag("header_brand_button")
        ) {
            DzmLogoEmblem(
                size = 30.dp,
                cornerRadius = 8.dp,
                modifier = Modifier.testTag("header_dzm_logo")
            )
            DzmMonogramBadge(modifier = Modifier.testTag("header_dzm_badge"))
            AnimatedContent(
                targetState = strings.appBrandTitle,
                transitionSpec = {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(100))
                },
                label = "brand_title_fade"
            ) { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = AmberSignal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("app_brand_title")
                )
            }
        }

        LanguageTogglePill(
            selectedLanguage = language,
            onSelectLanguage = onLanguageChange
        )
    }
}

@Composable
private fun LanguageTogglePill(
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    val strings = LocalAppStrings.current
    val segmentWidth = 52.dp
    val segmentGap = 2.dp
    val indicatorOffset by animateDpAsState(
        targetValue = if (selectedLanguage == AppLanguage.EN) {
            0.dp
        } else {
            segmentWidth + segmentGap
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "lang_slider_offset"
    )

    Surface(
        modifier = Modifier
            .testTag("language_toggle")
            .semantics { contentDescription = strings.languageToggleDescription },
        color = SlateSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier
                    .padding(start = 6.dp, end = 6.dp)
                    .size(16.dp)
            )
            Box {
                // Smooth sliding active pill background with zero width jump
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(segmentWidth)
                        .height(48.dp)
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(AmberSignal)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(segmentGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppLanguage.entries.forEach { option ->
                        val isSelected = option == selectedLanguage
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) Color(0xFF1C1300) else TextSecondary,
                            animationSpec = tween(160),
                            label = "lang_pill_fg_${option.code}"
                        )
                        val label = when (option) {
                            AppLanguage.EN -> strings.languageEnglishLabel
                            AppLanguage.MM -> strings.languageMyanmarLabel
                        }
                        val interactionSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .width(segmentWidth)
                                .minimumInteractiveComponentSize()
                                .pressScale(interactionSource, pressedScale = 0.94f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.RadioButton,
                                    onClick = { onSelectLanguage(option) }
                                )
                                .semantics { selected = isSelected }
                                .testTag("language_toggle_${option.code.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShizukuConnectionBar(
    status: ShizukuStatus,
    onGrantClick: () -> Unit,
    onOpenShizukuClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val bgColor by animateColorAsState(
        targetValue = when (status) {
            ShizukuStatus.Connected -> EmeraldContainer.copy(alpha = 0.85f)
            ShizukuStatus.PermissionRequired -> AmberContainer.copy(alpha = 0.9f)
            ShizukuStatus.NotRunning -> CrimsonContainer.copy(alpha = 0.9f)
        },
        label = "shizuku_bar_bg"
    )
    val accentColor by animateColorAsState(
        targetValue = when (status) {
            ShizukuStatus.Connected -> EmeraldHud
            ShizukuStatus.PermissionRequired -> AmberSignal
            ShizukuStatus.NotRunning -> CrimsonAlert
        },
        label = "shizuku_bar_accent"
    )
    val textColor = when (status) {
        ShizukuStatus.Connected -> EmeraldText
        ShizukuStatus.PermissionRequired -> AmberText
        ShizukuStatus.NotRunning -> CrimsonText
    }
    val statusLabel = when (status) {
        ShizukuStatus.Connected -> strings.shizukuConnectedTitle
        ShizukuStatus.PermissionRequired -> strings.shizukuPermissionTitle
        ShizukuStatus.NotRunning -> strings.shizukuNotRunningTitle
    }
    val statusSubtitle = when (status) {
        ShizukuStatus.Connected -> strings.shizukuConnectedSubtitle
        ShizukuStatus.PermissionRequired -> strings.shizukuPermissionSubtitle
        ShizukuStatus.NotRunning -> strings.shizukuNotRunningSubtitle
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            .testTag("shizuku_status_bar"),
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = when (status) {
                        ShizukuStatus.Connected -> Icons.Filled.VerifiedUser
                        ShizukuStatus.PermissionRequired -> Icons.Filled.Key
                        ShizukuStatus.NotRunning -> Icons.Filled.ErrorOutline
                    },
                    contentDescription = statusLabel,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    (fadeIn(tween(160)) + scaleIn(initialScale = 0.94f, animationSpec = tween(160)))
                        .togetherWith(fadeOut(tween(110)))
                },
                label = "shizuku_action_transition"
            ) { currentStatus ->
                when (currentStatus) {
                    ShizukuStatus.Connected -> {
                        Surface(
                            color = EmeraldHud.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = strings.shizukuConnectedBadge,
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    ShizukuStatus.PermissionRequired -> {
                        val grantInteraction = remember { MutableInteractionSource() }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onGrantClick,
                                interactionSource = grantInteraction,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .pressScale(grantInteraction)
                                    .testTag("shizuku_grant_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberSignal,
                                    contentColor = Color(0xFF1C1300)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = strings.actionGrant,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    ShizukuStatus.NotRunning -> {
                        val openInteraction = remember { MutableInteractionSource() }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onOpenShizukuClick,
                                interactionSource = openInteraction,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .pressScale(openInteraction)
                                    .testTag("open_shizuku_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonAlert,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Launch,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.actionOpenShizuku,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionSelectorScreen(
    installedVersions: Map<PubgVersion, InstalledPackageInfo>,
    onSelectVersion: (PubgVersion) -> Unit
) {
    val strings = LocalAppStrings.current
    val installedCount = installedVersions.values.count { it.isInstalled }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("version_selector_screen")
    ) {
        val columns = if (maxWidth >= 600.dp) 2 else 1

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.versionSelectorTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("version_selector_title")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DzmBrandPillChip(modifier = Modifier.testTag("selector_dzm_brand_badge"))
                    Surface(
                        color = SlateElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Text(
                            text = strings.detectedCountFormat(installedCount, PubgVersion.entries.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (installedCount > 0) EmeraldText else TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.versionSelectorSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = PubgVersion.entries,
                    key = { it.key },
                    contentType = { "pubg_version_card" }
                ) { version ->
                    val info = installedVersions[version] ?: InstalledPackageInfo(isInstalled = false)
                    PubgVersionCard(
                        version = version,
                        installedInfo = info,
                        onClick = { onSelectVersion(version) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DzmBrandPillChip(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SlateElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AmberSignal.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(start = 5.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DzmLogoEmblem(
                size = 20.dp,
                cornerRadius = 5.dp,
                borderWidth = 0.8.dp
            )
            Text(
                text = "DZM",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AmberText
            )
        }
    }
}

@Composable
private fun PubgVersionCard(
    version: PubgVersion,
    installedInfo: InstalledPackageInfo,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor = if (installedInfo.isInstalled) {
        EmeraldHud.copy(alpha = 0.4f)
    } else {
        HairlineBorder
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .pressScale(interactionSource, pressedScale = 0.98f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .testTag("version_card_${version.key}"),
        color = SlateSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberContainer)
                    .border(1.dp, AmberSignal.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SportsEsports,
                    contentDescription = version.displayName,
                    tint = AmberSignal,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = SlateElevated,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = version.shortTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberText,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    InstalledStatusBadge(installedInfo = installedInfo)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = version.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = strings.regionLabelFor(version),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = version.packageName,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    fontFamily = JetBrainsMonoFamily
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = strings.openVersionDescriptionFormat(version.displayName),
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun InstalledStatusBadge(installedInfo: InstalledPackageInfo) {
    val strings = LocalAppStrings.current
    val bg = if (installedInfo.isInstalled) EmeraldContainer else SlateInput
    val border = if (installedInfo.isInstalled) EmeraldHud.copy(alpha = 0.45f) else HairlineBorder
    val fg = if (installedInfo.isInstalled) EmeraldText else TextMuted
    val text = if (installedInfo.isInstalled) {
        val ver = installedInfo.versionName?.takeIf { it.isNotBlank() }
        if (ver != null) strings.badgeInstalledVersionFormat(ver) else strings.badgeInstalled
    } else {
        strings.badgeNotInstalled
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun FileLibraryScreen(
    version: PubgVersion,
    installedInfo: InstalledPackageInfo?,
    maintenance: Boolean,
    maintenanceMessage: String,
    announcement: String,
    items: List<LibraryFileItem>,
    availableCategories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    isRefreshing: Boolean,
    isSampleMode: Boolean,
    hasLoadedRemoteOnce: Boolean,
    catalogError: UiText?,
    transferStates: Map<String, ItemTransferState>,
    appliedItemIds: Set<String>,
    resolveTargetPreview: (LibraryFileItem) -> String,
    onSwitchVersion: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onDownloadAndApply: (LibraryFileItem) -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("file_library_screen")
    ) {
        // Top Bar for Active Edition + Switch + Refresh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onSwitchVersion,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("back_to_versions_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backToVersionsDescription,
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = version.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (installedInfo != null) {
                            InstalledStatusBadge(installedInfo = installedInfo)
                        }
                    }
                    Text(
                        text = version.packageName,
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onSwitchVersion,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("switch_version_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        tint = AmberSignal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.actionSwitchVersion,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }

                Surface(
                    color = SlateSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HairlineBorder)
                ) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_catalog_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = AmberSignal
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = strings.actionRefresh,
                                tint = AmberSignal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Catalog Source Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = when {
                    catalogError != null -> Icons.Filled.CloudOff
                    isSampleMode -> Icons.Filled.FolderCopy
                    else -> Icons.Filled.CloudDone
                },
                contentDescription = null,
                tint = when {
                    catalogError != null -> CrimsonText
                    isSampleMode -> TextSecondary
                    else -> EmeraldText
                },
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = when {
                    catalogError != null -> strings.sourceError
                    isRefreshing && !hasLoadedRemoteOnce -> strings.sourceFetching
                    isSampleMode -> strings.sourceSample
                    else -> strings.sourceLiveRemote
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (catalogError != null) CrimsonText else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Announcement Banner (when non-empty; preserves custom JSON message verbatim)
        AnimatedVisibility(
            visible = announcement.isNotBlank() && catalogError == null,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(140)) + fadeOut(tween(120))
        ) {
            AnnouncementBanner(
                message = announcement,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        // Maintenance Banner (when maintenance == true; preserves custom JSON message verbatim)
        AnimatedVisibility(
            visible = maintenance && catalogError == null,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(140)) + fadeOut(tween(120))
        ) {
            MaintenanceBanner(
                message = maintenanceMessage.ifBlank {
                    strings.maintenanceDefaultMessage
                },
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input"),
            placeholder = {
                Text(
                    text = strings.searchPlaceholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = strings.searchDescription,
                    tint = TextSecondary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = strings.clearSearchDescription,
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateSurface,
                unfocusedContainerColor = SlateSurface,
                focusedBorderColor = AmberSignal,
                unfocusedBorderColor = HairlineBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AmberSignal
            )
        )

        val showCategoryChips = availableCategories.size > 2 ||
            (availableCategories.size == 2 && !availableCategories.last().equals("General", ignoreCase = true))

        if (showCategoryChips) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(availableCategories, key = { it }) { category ->
                    val selected = category == selectedCategory
                    val chipLabel = if (category == "All") strings.categoryAll else category
                    FilterChip(
                        selected = selected,
                        onClick = { onCategorySelect(category) },
                        modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ", "_")}"),
                        label = {
                            Text(
                                text = chipLabel,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SlateSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = AmberContainer,
                            selectedLabelColor = AmberText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = HairlineBorder,
                            selectedBorderColor = AmberSignal.copy(alpha = 0.65f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            catalogError != null -> {
                RemoteErrorState(
                    errorMessage = catalogError.resolve(strings),
                    isRetrying = isRefreshing,
                    onRetry = onRetry
                )
            }
            isRefreshing && items.isEmpty() && !hasLoadedRemoteOnce -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AmberSignal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.loadingRemoteFiles,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            items.isEmpty() -> {
                EmptyLibraryState(
                    hasActiveFilter = searchQuery.isNotBlank() || selectedCategory != "All",
                    onResetFilters = {
                        onSearchChange("")
                        onCategorySelect("All")
                    }
                )
            }
            else -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = items,
                        key = { it.id },
                        contentType = { "library_file_card" }
                    ) { item ->
                        LibraryFileCard(
                            item = item,
                            targetPathPreview = resolveTargetPreview(item),
                            transferState = transferStates[item.id],
                            wasApplied = item.id in appliedItemIds,
                            maintenanceActive = maintenance,
                            onDownloadAndApply = { onDownloadAndApply(item) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(180),
                                fadeOutSpec = tween(140),
                                placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("announcement_banner"),
        color = SlateElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AmberSignal.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Campaign,
                contentDescription = strings.announcementTitle,
                tint = AmberSignal,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.announcementTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun MaintenanceBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("maintenance_banner"),
        color = CrimsonContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = strings.maintenanceTitle,
                tint = CrimsonText,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.maintenanceTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = CrimsonText
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun RemoteErrorState(
    errorMessage: String,
    isRetrying: Boolean,
    onRetry: () -> Unit
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("catalog_error_card"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = SlateSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = CrimsonText,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = strings.catalogErrorTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    enabled = !isRetrying,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("retry_catalog_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberSignal,
                        contentColor = Color(0xFF1C1300)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRetrying) strings.actionRetrying else strings.actionRetry,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryFileCard(
    item: LibraryFileItem,
    targetPathPreview: String,
    transferState: ItemTransferState?,
    wasApplied: Boolean,
    maintenanceActive: Boolean,
    onDownloadAndApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isBusy = transferState != null
    val isZip = remember(item.fileName) { item.fileName.trim().endsWith(".zip", ignoreCase = true) }
    val buttonEnabled = !isBusy && !maintenanceActive
    val buttonInteraction = remember { MutableInteractionSource() }
    val animatedBorderColor by animateColorAsState(
        targetValue = if (wasApplied) EmeraldHud.copy(alpha = 0.45f) else HairlineBorder,
        animationSpec = tween(200),
        label = "card_applied_border"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = when (transferState) {
            is ItemTransferState.Downloading -> transferState.progress.coerceIn(0f, 1f)
            ItemTransferState.CheckingTarget, ItemTransferState.Applying -> 1f
            ItemTransferState.WaitingForPermission, null -> 0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "transfer_progress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_card_${item.id}"),
        color = SlateSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, animatedBorderColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = AmberContainer,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, AmberSignal.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = item.category.ifBlank { "General" },
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = SlateElevated,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.sizeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (isZip) {
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, EmeraldHud.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = strings.badgeZipExtract,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (wasApplied) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = strings.badgeApplied,
                            tint = EmeraldHud,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.badgeApplied,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberContainer)
                        .border(1.dp, AmberSignal.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SportsEsports,
                        contentDescription = null,
                        tint = AmberSignal,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                StructuredGreenDescription(
                    description = item.description,
                    itemId = item.id
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Monospace target path preview
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateInput,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, HairlineBorder.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        text = strings.targetPathHeader,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = targetPathPreview,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontFamily = JetBrainsMonoFamily
                    )
                }
            }

            AnimatedVisibility(
                visible = isBusy,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(140)) + fadeOut(tween(120))
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (transferState is ItemTransferState.Downloading && transferState.progress < 0f) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AmberSignal,
                            trackColor = SlateElevated
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (transferState is ItemTransferState.Applying) EmeraldHud else AmberSignal,
                            trackColor = SlateElevated
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val buttonContainerColor by animateColorAsState(
                targetValue = if (buttonEnabled) AmberSignal else SlateElevated,
                animationSpec = tween(180),
                label = "apply_btn_bg"
            )
            val buttonContentColor by animateColorAsState(
                targetValue = if (buttonEnabled) Color(0xFF1C1300) else TextMuted,
                animationSpec = tween(180),
                label = "apply_btn_fg"
            )

            Button(
                onClick = onDownloadAndApply,
                enabled = buttonEnabled,
                interactionSource = buttonInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .pressScale(buttonInteraction, pressedScale = 0.98f)
                    .testTag("download_apply_button_${item.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor,
                    disabledContainerColor = buttonContainerColor,
                    disabledContentColor = buttonContentColor
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = when {
                        maintenanceActive -> Icons.Filled.Build
                        transferState is ItemTransferState.Applying -> Icons.Filled.Security
                        else -> Icons.Filled.Download
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val buttonText = when {
                    maintenanceActive -> strings.statusMaintenanceActiveButton
                    transferState is ItemTransferState.Downloading -> {
                        if (transferState.progress >= 0f) {
                            strings.statusDownloadingPercentFormat((transferState.progress * 100).toInt())
                        } else {
                            strings.statusDownloadingIndeterminate
                        }
                    }
                    transferState is ItemTransferState.WaitingForPermission -> strings.shizukuPermissionTitle
                    transferState is ItemTransferState.CheckingTarget -> strings.statusCheckingTarget
                    transferState is ItemTransferState.Applying -> {
                        if (isZip) strings.statusExtractingViaShizuku else strings.statusApplyingViaShizuku
                    }
                    else -> strings.actionDownloadAndApply
                }
                AnimatedContent(
                    targetState = buttonText,
                    transitionSpec = {
                        fadeIn(tween(140)) togetherWith fadeOut(tween(100))
                    },
                    label = "apply_btn_text"
                ) { animatedText ->
                    Text(
                        text = animatedText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    hasActiveFilter: Boolean,
    onResetFilters: () -> Unit
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.FolderCopy,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasActiveFilter) strings.emptyFilteredTitle else strings.emptyNoFilesTitle,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasActiveFilter) {
                    strings.emptyFilteredSubtitle
                } else {
                    strings.emptyNoFilesSubtitle
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (hasActiveFilter) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onResetFilters,
                    border = BorderStroke(1.dp, AmberSignal.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = strings.actionClearFilters,
                        color = AmberText
                    )
                }
            }
        }
    }
}

@Composable
private fun OverwriteConfirmDialog(
    prompt: OverwritePrompt,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    val confirmInteraction = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(180)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.95f)
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .testTag("overwrite_confirm_dialog"),
                color = SlateSurface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, HairlineBorder),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = AmberSignal,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.overwriteDialogTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (prompt.isZipArchive) {
                            strings.overwriteDialogMessageZipFormat(prompt.item.fileName)
                        } else {
                            strings.overwriteDialogMessageSingle
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SlateInput,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Text(
                            text = prompt.fullTargetPath,
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberText,
                            fontFamily = JetBrainsMonoFamily,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("cancel_overwrite_button")
                        ) {
                            Text(
                                text = strings.actionCancel,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onConfirm,
                            interactionSource = confirmInteraction,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .pressScale(confirmInteraction)
                                .testTag("confirm_overwrite_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberSignal,
                                contentColor = Color(0xFF1C1300)
                            )
                        ) {
                            Text(
                                text = strings.actionOverwrite,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticePopupDialog(
    dontShowAgain: Boolean,
    onToggleDontShowAgain: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    val closeInteraction = remember { MutableInteractionSource() }
    val toggleInteraction = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(180)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.95f)
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .testTag("notice_popup_dialog"),
                color = SlateSurface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, HairlineBorder),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Stylish Custom DZM Logo Emblem Badge
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        AmberSignal.copy(alpha = 0.22f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                            .padding(6.dp)
                            .testTag("notice_logo_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        DzmLogoEmblem(
                            size = 76.dp,
                            cornerRadius = 20.dp,
                            borderWidth = 1.5.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    DzmMonogramBadge(modifier = Modifier.testTag("notice_dzm_pill"))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Clean Dark Inner Card showing strictly the credit notice message
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SlateInput,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, HairlineBorder.copy(alpha = 0.85f))
                    ) {
                        Text(
                            text = NOTICE_CREDIT_MESSAGE,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp)
                                .testTag("notice_message_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Don't Show Again Toggle Row
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = toggleInteraction,
                                indication = androidx.compose.material3.ripple(),
                                role = Role.Switch,
                                onClick = { onToggleDontShowAgain(!dontShowAgain) }
                            )
                            .testTag("notice_dont_show_toggle"),
                        color = SlateElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HairlineBorder.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.noticeDontShowAgain,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            )
                            Switch(
                                checked = dontShowAgain,
                                onCheckedChange = onToggleDontShowAgain,
                                modifier = Modifier.testTag("notice_dont_show_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF1C1300),
                                    checkedTrackColor = AmberSignal,
                                    checkedBorderColor = AmberSignal,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SlateInput,
                                    uncheckedBorderColor = HairlineBorder
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Close / OK Action Button
                    Button(
                        onClick = onDismiss,
                        interactionSource = closeInteraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .pressScale(closeInteraction, pressedScale = 0.98f)
                            .testTag("notice_close_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberSignal,
                            contentColor = Color(0xFF1C1300)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            text = strings.noticeCloseButton,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
