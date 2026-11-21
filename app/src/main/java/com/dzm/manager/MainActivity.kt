package com.dzm.manager

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.dzm.manager.ads.RewardedAdResult
import com.dzm.manager.ads.UnityDownloadAds
import com.dzm.manager.ui.DropZoneViewModel
import com.dzm.manager.ui.MainScreen
import com.dzm.manager.ui.theme.DropZoneTheme
import kotlinx.coroutines.async

class MainActivity : ComponentActivity() {

    private val viewModel: DropZoneViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            DropZoneTheme {
                MainScreen(viewModel = viewModel, showRewardedAd = ::showRewardedAd)
            }
        }
    }

    private suspend fun showRewardedAd(): RewardedAdResult {
        // The ad must not outlive the Activity it is shown from.
        val ad = lifecycleScope.async { UnityDownloadAds.showRewarded(this@MainActivity) }
        return try {
            ad.await()
        } finally {
            ad.cancel()
        }
    }
}
