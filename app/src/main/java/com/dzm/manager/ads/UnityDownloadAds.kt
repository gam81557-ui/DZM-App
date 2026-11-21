package com.dzm.manager.ads

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class RewardedAdResult { COMPLETED, SKIPPED, UNAVAILABLE }

internal object UnityDownloadAds {
    private const val GAME_ID = "800447271"
    const val BANNER_PLACEMENT = "BP_Banner_Android"
    private const val REWARDED_PLACEMENT = "BP_Rewarded_Android"
    private const val LOAD_TIMEOUT_MS = 20_000L
    private const val SHOW_START_TIMEOUT_MS = 180_000L
    private val initialization = Mutex()

    suspend fun initialize(context: Context): Boolean = withContext(Dispatchers.IO) {
        withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            initialization.withLock {
                if (UnityAdsInitialization.isInitialized()) return@withLock true
                val result = CompletableDeferred<Boolean>()
                UnityAds.initialize(context.applicationContext, GAME_ID, false,
                    object : IUnityAdsInitializationListener {
                        override fun onInitializationComplete() {
                            result.complete(true)
                        }

                        override fun onInitializationFailed(
                            error: UnityAds.UnityAdsInitializationError,
                            message: String
                        ) {
                            result.complete(false)
                        }
                    }
                )
                result.await()
            }
        } ?: false
    }

    suspend fun showRewarded(activity: ComponentActivity): RewardedAdResult {
        val loaded = withTimeoutOrNull(LOAD_TIMEOUT_MS) {
            initialize(activity.application) && loadRewarded()
        } == true
        if (!loaded || activity.isFinishing ||
            !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) return RewardedAdResult.UNAVAILABLE

        val startedOrFinished = CompletableDeferred<Unit>()
        val result = CompletableDeferred<RewardedAdResult>()
        UnityAds.show(activity, REWARDED_PLACEMENT, object : IUnityAdsShowListener {
            override fun onUnityAdsShowStart(placementId: String) {
                if (placementId == REWARDED_PLACEMENT) startedOrFinished.complete(Unit)
            }
            override fun onUnityAdsShowClick(placementId: String) = Unit

            override fun onUnityAdsShowFailure(
                placementId: String,
                error: UnityAds.UnityAdsShowError,
                message: String
            ) {
                result.complete(RewardedAdResult.UNAVAILABLE)
                startedOrFinished.complete(Unit)
            }

            override fun onUnityAdsShowComplete(
                placementId: String,
                state: UnityAds.UnityAdsShowCompletionState
            ) {
                result.complete(
                    when {
                        placementId != REWARDED_PLACEMENT -> RewardedAdResult.UNAVAILABLE
                        state == UnityAds.UnityAdsShowCompletionState.COMPLETED -> RewardedAdResult.COMPLETED
                        else -> RewardedAdResult.SKIPPED
                    }
                )
                startedOrFinished.complete(Unit)
            }
        })
        if (withTimeoutOrNull(SHOW_START_TIMEOUT_MS) { startedOrFinished.await() } == null &&
            !startedOrFinished.isCompleted
        ) return RewardedAdResult.UNAVAILABLE

        // MainActivity's lifecycle scope cancels this wait when the Activity is destroyed.
        return result.await()
    }

    private suspend fun loadRewarded(): Boolean {
        val result = CompletableDeferred<Boolean>()
        UnityAds.load(REWARDED_PLACEMENT, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                result.complete(placementId == REWARDED_PLACEMENT)
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                result.complete(false)
            }
        })
        return result.await()
    }
}
