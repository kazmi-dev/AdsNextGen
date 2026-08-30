package com.kazmi.dev.nextgenadscore.adsNextGen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

object RewardedAdManager {

    private const val TAG = "RewardedAdManager"

    private var rewardedAd: RewardedAd? = null
    private var isAdLoading: Boolean = false
    var isRewardedShowing: Boolean = false

    suspend fun loadRewardedAdWithTimeOut(
        activity: Activity,
        adUnitId: String? = null,
        duration: Long = 8000,
        onAdEvent: (AdEvent) -> Unit
    ) {
        val finalAdUnitId = adUnitId ?: AdsSettings.rewardedId

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
                val adRequest = AdRequest.Builder(finalAdUnitId).build()

                RewardedAd.load(
                    adRequest,
                    object : AdLoadCallback<RewardedAd> {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            isAdLoading = false
                            Log.d(TAG, "onAdFailedToLoad: ${adError.message}")
                            onAdEvent(AdEvent.FAILED_TO_LOAD)
                            if (cont.isActive) cont.resume(false)
                        }

                        override fun onAdLoaded(ad: RewardedAd) {
                            rewardedAd = ad
                            isAdLoading = false
                            onAdEvent(AdEvent.LOADED)
                            if (cont.isActive) cont.resume(true)
                        }
                    }
                )
            }
        }

        if (result == true) {
            showRewardedAd(activity, onAdEvent)
        } else {
            isAdLoading = false
            if (result == null) onAdEvent(AdEvent.FAILED_TO_LOAD)
        }
    }

    private fun showRewardedAd(
        activity: Activity,
        onAdEvent: (AdEvent) -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onAdEvent(AdEvent.FAILED_TO_SHOW)
            return
        }

        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                isRewardedShowing = true
                onAdEvent(AdEvent.SHOWED)
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                isRewardedShowing = false
                onAdEvent(AdEvent.DISMISSED)
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                rewardedAd = null
                isRewardedShowing = false
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
