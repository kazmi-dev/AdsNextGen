package com.kazmi.dev.nextgenadscore.adsNextGen

object AdsSettings {
    /**
     * Set this to true if the user has purchased the app to disable all ads.
     */
    var isAppPurchased: Boolean = false

    /**
     * Default Ad Unit IDs. These are used if no ID is provided in the ad manager calls.
     * Initialized with official AdMob test IDs.
     */
    var bannerId: String = "ca-app-pub-3940256099942544/6300978111"
    var interstitialId: String = "ca-app-pub-3940256099942544/1033173712"
    var nativeId: String = "ca-app-pub-3940256099942544/2247696110"
    var rewardedId: String = "ca-app-pub-3940256099942544/5224354917"
    var rewardedInterstitialId: String = "ca-app-pub-3940256099942544/5354046379"
    var appOpenId: String = "ca-app-pub-3940256099942544/9257395915"
}
