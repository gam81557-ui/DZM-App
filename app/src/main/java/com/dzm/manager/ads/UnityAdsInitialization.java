package com.dzm.manager.ads;

import com.unity3d.ads.UnityAds;

final class UnityAdsInitialization {
    private UnityAdsInitialization() {}

    // The SDK's Java method is exposed as a property by Kotlin metadata.
    static boolean isInitialized() {
        return UnityAds.isInitialized();
    }
}
