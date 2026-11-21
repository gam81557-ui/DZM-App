package com.app.pubglibrary.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

@Composable
internal fun UnityBanner() {
    val context = LocalContext.current
    val initialized by produceState(false, context) { value = UnityDownloadAds.initialize(context) }
    if (!initialized) return

    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (maxWidth >= 320.dp) {
            var loaded by remember { mutableStateOf(false) }
            var failed by remember { mutableStateOf(false) }
            if (!failed) {
                AndroidView(
                    modifier = Modifier.width(320.dp).height(if (loaded) 50.dp else 0.dp),
                    factory = { viewContext ->
                        BannerView(viewContext, UnityDownloadAds.BANNER_PLACEMENT, UnityBannerSize(320, 50))
                            .apply {
                                listener = object : BannerView.Listener() {
                                    override fun onBannerLoaded(bannerView: BannerView) {
                                        bannerView.post { loaded = true }
                                    }

                                    override fun onBannerFailedToLoad(
                                        bannerView: BannerView,
                                        error: BannerErrorInfo
                                    ) {
                                        bannerView.post { failed = true }
                                    }
                                }
                                load()
                            }
                    },
                    onReset = null,
                    onRelease = { it.destroy() }
                )
            }
        }
    }
}
