package com.kazmi.dev.nextgenadscore.adsNextGen

import android.app.Activity
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

object AppOpenAdManager {

    private const val TAG = "AppOpenAdManager"
    private var appOpenAd: AppOpenAd? = null
    private var isAdLoading: Boolean = false

    suspend fun loadAppOpenAdWithTimeOut(
        activity: Activity,
        adUnitId: String? = null,
        duration: Long = 8000,
        onAdEvent: (AdEvent) -> Unit
    ) {
        val finalAdUnitId = adUnitId ?: AdsSettings.appOpenId

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
                AppOpenAd.load(
                    adRequest,
                    object : AdLoadCallback<AppOpenAd> {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            isAdLoading = false
                            Log.d(TAG, "onAdFailedToLoad: ${adError.message}")
                            onAdEvent(AdEvent.FAILED_TO_LOAD)
                            if (cont.isActive) cont.resume(false)
                        }

                        override fun onAdLoaded(ad: AppOpenAd) {
                            appOpenAd = ad
                            isAdLoading = false
                            onAdEvent(AdEvent.LOADED)
                            if (cont.isActive) cont.resume(true)
                        }
                    }
                )
            }
        }

        if (result == true) {
            showAppOpenAd(activity, onAdEvent)
        } else {
            isAdLoading = false
            if (result == null) onAdEvent(AdEvent.FAILED_TO_LOAD)
        }
    }

    private fun showAppOpenAd(
        activity: Activity,
        onAdEvent: (AdEvent) -> Unit
    ) {
        val ad = appOpenAd
        if (ad == null) {
            onAdEvent(AdEvent.FAILED_TO_SHOW)
            return
        }

        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                onAdEvent(AdEvent.SHOWED)
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                onAdEvent(AdEvent.DISMISSED)
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                appOpenAd = null
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
}
