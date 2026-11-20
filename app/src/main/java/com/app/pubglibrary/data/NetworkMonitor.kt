package com.app.pubglibrary.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

interface NetworkReachabilityChecker {
    suspend fun isInternetReachable(): Boolean
    fun observeNetworkAvailability(): Flow<Unit>
}

class DefaultNetworkReachabilityChecker(
    private val context: Context
) : NetworkReachabilityChecker {

    private val probeClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .cache(null)
            .build()
    }

    override suspend fun isInternetReachable(): Boolean = withContext(Dispatchers.IO) {
        if (!hasActiveInternetTransport()) {
            return@withContext false
        }

        val remoteUrl = REMOTE_CONTROL_URL.trim()
        if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
            val timestamped = "$remoteUrl${if ('?' in remoteUrl) '&' else '?'}t=${System.currentTimeMillis()}"
            if (probeUrl(timestamped)) {
                return@withContext true
            }
        }

        probeUrl(GOOGLE_GENERATE_204_URL)
    }

    override fun observeNetworkAvailability(): Flow<Unit> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            awaitClose { }
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(Unit)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    trySend(Unit)
                }
            }
        }

        val registered = runCatching {
            cm.registerDefaultNetworkCallback(callback)
            true
        }.getOrDefault(false)

        awaitClose {
            if (registered) {
                runCatching { cm.unregisterNetworkCallback(callback) }
            }
        }
    }.conflate()

    private fun hasActiveInternetTransport(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun probeUrl(url: String): Boolean {
        return try {
            val headRequest = Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .head()
                .build()

            probeClient.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    return true
                }
                if (response.code != 405) {
                    return false
                }
            }

            val getRequest = Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .get()
                .build()

            probeClient.newCall(getRequest).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Throwable) {
            false
        }
    }

    private companion object {
        const val GOOGLE_GENERATE_204_URL = "https://clients3.google.com/generate_204"
    }
}
