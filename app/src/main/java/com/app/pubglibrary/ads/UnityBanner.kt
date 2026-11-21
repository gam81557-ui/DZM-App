package com.app.pubglibrary.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.delay

private const val BANNER_RETRY_MS = 30_000L

@Composable
internal fun UnityBanner() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() } ?: return
    if (activity.isFinishing || activity.isDestroyed) return
    val initialized by produceState(false, activity) {
        while (!value) {
            value = UnityDownloadAds.initialize(activity)
            if (!value) delay(BANNER_RETRY_MS)
        }
    }
    if (!initialized) return
    BannerSlot(activity, ::createUnityBanner, ::releaseUnityBanner)
}

private fun createUnityBanner(activity: Activity, onFailure: () -> Unit): View =
    BannerView(activity, UnityDownloadAds.BANNER_PLACEMENT, UnityBannerSize(320, 50)).apply {
        listener = object : BannerView.Listener() {
            override fun onBannerFailedToLoad(bannerView: BannerView, error: BannerErrorInfo) {
                bannerView.post {
                    if (bannerView.listener != null) onFailure()
                }
            }
        }
        load()
    }

private fun releaseUnityBanner(view: View) {
    (view as BannerView).apply {
        listener = null
        destroy()
    }
}

@Composable
internal fun BannerSlot(
    activity: Activity,
    createBanner: (Activity, () -> Unit) -> View,
    releaseBanner: (View) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (maxWidth >= 320.dp) {
            var failed by remember(activity) { mutableStateOf(false) }
            var attempt by remember(activity) { mutableIntStateOf(0) }
            LaunchedEffect(failed) {
                if (failed) {
                    delay(BANNER_RETRY_MS)
                    attempt++
                    failed = false
                }
            }
            if (!failed) {
                key(activity, attempt) {
                    AndroidView(
                        modifier = Modifier.width(320.dp).height(50.dp).testTag("unity_banner"),
                        factory = { createBanner(activity) { failed = true } },
                        onReset = null,
                        onRelease = releaseBanner
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext?.findActivity()
    else -> null
}
