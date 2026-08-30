package com.kazmi.dev.nextgenadscore.adsNextGen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

object RewardedInterstitialAdManager {

    private const val TAG = "RewardedInterstitial"

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isAdLoading: Boolean = false
    var isRewardedInterstitialShowing: Boolean = false

    suspend fun loadRewardedInterstitialAdWithTimeOut(
        activity: Activity,
        adUnitId: String = "ca-app-pub-3940256099942544/5354046379",
        duration: Long = 8000,
        onAdEvent: (AdEvent) -> Unit
    ) {
        if (AdsSettings.isAppPurchased) {
            onAdEvent(AdEvent.SKIPPED_DUE_TO_PURCHASE)
            return
        }

        if (isAdLoading) return

        if (!NetworkObserver.isConnected(activity)) {
            onAdEvent(AdEvent.FAILED_TO_LOAD)
            return
        }

        isAdLoading = true

        val result = withTimeoutOrNull(duration.milliseconds) {
            suspendCancellableCoroutine { cont ->
                val adRequest = AdRequest.Builder(adUnitId).build()

                RewardedInterstitialAd.load(
                    adRequest,
                    object : AdLoadCallback<RewardedInterstitialAd> {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            isAdLoading = false
                            Log.d(TAG, "onAdFailedToLoad: ${adError.message}")
                            onAdEvent(AdEvent.FAILED_TO_LOAD)
                            if (cont.isActive) cont.resume(false)
                        }

                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                            rewardedInterstitialAd = ad
                            isAdLoading = false
                            onAdEvent(AdEvent.LOADED)
                            if (cont.isActive) cont.resume(true)
                        }
                    }
                )
            }
        }

        if (result == true) {
            showRewardedInterstitialAd(activity, onAdEvent)
        } else {
            isAdLoading = false
            if (result == null) onAdEvent(AdEvent.FAILED_TO_LOAD)
        }
    }

    private fun showRewardedInterstitialAd(
        activity: Activity,
        onAdEvent: (AdEvent) -> Unit
    ) {
        val ad = rewardedInterstitialAd
        if (ad == null) {
            onAdEvent(AdEvent.FAILED_TO_SHOW)
            return
        }

        ad.adEventCallback = object : RewardedInterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                isRewardedInterstitialShowing = true
                onAdEvent(AdEvent.SHOWED)
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                isRewardedInterstitialShowing = false
                onAdEvent(AdEvent.DISMISSED)
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                rewardedInterstitialAd = null
                isRewardedInterstitialShowing = false
                onAdEvent(AdEvent.FAILED_TO_SHOW)
            }

            override fun onAdClicked() {
                onAdEvent(AdEvent.CLICKED)
            }

            override fun onAdImpression() {
                onAdEvent(AdEvent.IMPRESSION)
            }

            override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                onAdEvent(AdEvent.PAID)
            }
        }

        ad.show(activity) {
            onAdEvent(AdEvent.REWARDED)
        }
    }
}
