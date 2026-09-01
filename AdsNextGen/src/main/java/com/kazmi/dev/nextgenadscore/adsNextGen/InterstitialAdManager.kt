package com.kazmi.dev.nextgenadscore.adsNextGen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.kazmi.dev.nextgenadscore.adsNextGen.userConsent.GoogleConsentManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

object InterstitialAdManager {

    private const val TAG = "InterstitialAdManager"

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading: Boolean = false
    var isInterstitialShowing: Boolean = false

    suspend fun loadInterstitialAdWithTimeOut(
        activity: Activity,
        adUnitId: String? = null,
        duration: Long = 8000,
        onAdEvent: (AdEvent) -> Unit
    ) {
        val finalAdUnitId = adUnitId ?: AdsSettings.interstitialId

        if (AdsSettings.isAppPurchased) {
            onAdEvent(AdEvent.SKIPPED_DUE_TO_PURCHASE)
            return
        }

        if (!GoogleConsentManager.canRequestAds(activity)) {
            onAdEvent(AdEvent.FAILED_TO_LOAD)
            return
        }

        if (!NetworkObserver.isConnected(activity)) {
            onAdEvent(AdEvent.FAILED_TO_LOAD)
            return
        }

        if (isAdLoading) return

        isAdLoading = true

        val result = withTimeoutOrNull(duration.milliseconds) {
            suspendCancellableCoroutine { cont ->
                val adRequest = AdRequest.Builder(finalAdUnitId).build()

                InterstitialAd.load(
                    adRequest,
                    object : AdLoadCallback<InterstitialAd> {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            isAdLoading = false
                            Log.d(TAG, "onAdFailedToLoad: ${adError.message}")
                            onAdEvent(AdEvent.FAILED_TO_LOAD)
                            if (cont.isActive) cont.resume(false)
                        }

                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            isAdLoading = false
                            onAdEvent(AdEvent.LOADED)
                            if (cont.isActive) cont.resume(true)
                        }
                    }
                )
            }
        }

        if (result == true) {
            showInterstitialAd(activity, onAdEvent)
        } else {
            isAdLoading = false
            if (result == null) onAdEvent(AdEvent.FAILED_TO_LOAD)
        }
    }

    private fun showInterstitialAd(
        activity: Activity,
        onAdEvent: (AdEvent) -> Unit
    ) {
        val ad = interstitialAd
        if (ad == null) {
            onAdEvent(AdEvent.FAILED_TO_SHOW)
            return
        }

        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                isInterstitialShowing = true
                onAdEvent(AdEvent.SHOWED)
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                isInterstitialShowing = false
                onDismiss()
                onAdEvent(AdEvent.DISMISSED)
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                interstitialAd = null
                isInterstitialShowing = false
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

        ad.show(activity)
    }

    private fun onDismiss() {}
}
