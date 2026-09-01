package com.kazmi.dev.nextgenadscore.adsNextGen

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.kazmi.dev.nextgenadscore.adsNextGen.userConsent.GoogleConsentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class AppOpenResume(
    application: Application,
    adUnitId: String? = null,
    private val onAdEvent: ((AdEvent) -> Unit)? = null
) : Application.ActivityLifecycleCallbacks, LifecycleEventObserver {

    private val finalAdUnitId = adUnitId ?: AdsSettings.appOpenId
    private var appOpenAd: AppOpenAd? = null
    private var isAdLoading: Boolean = false
    private var isAdShowing: Boolean = false
    private var currentActivity: Activity? = null
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        application.registerActivityLifecycleCallbacks(this)
    }

    private suspend fun loadAppOpenAdWithTimeOut(duration: Long = 8000) {
        val activity = currentActivity ?: return
        if (AdsSettings.isAppPurchased) {
            onAdEvent?.invoke(AdEvent.SKIPPED_DUE_TO_PURCHASE)
            return
        }

        if (!GoogleConsentManager.canRequestAds(activity)) {
            return
        }

        if (isAdLoading || isAdShowing) return
        if (!NetworkObserver.isConnected(activity)) return

        isAdLoading = true

        val result = withTimeoutOrNull(duration.milliseconds) {
            suspendCancellableCoroutine { cont ->
                val adRequest = AdRequest.Builder(finalAdUnitId).build()
                AppOpenAd.load(
                    adRequest,
                    object : AdLoadCallback<AppOpenAd> {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            isAdLoading = false
                            onAdEvent?.invoke(AdEvent.FAILED_TO_LOAD)
                            if (cont.isActive) cont.resume(false)
                        }

                        override fun onAdLoaded(ad: AppOpenAd) {
                            appOpenAd = ad
                            isAdLoading = false
                            onAdEvent?.invoke(AdEvent.LOADED)
                            if (cont.isActive) cont.resume(true)
                        }
                    }
                )
            }
        }

        if (result == true) {
            showAppOpenAd()
        } else {
            isAdLoading = false
        }
    }

    private fun showAppOpenAd() {
        val ad = appOpenAd ?: return
        val activity = currentActivity ?: return

        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                isAdShowing = true
                onAdEvent?.invoke(AdEvent.SHOWED)
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isAdShowing = false
                onAdEvent?.invoke(AdEvent.DISMISSED)
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                appOpenAd = null
                isAdShowing = false
                onAdEvent?.invoke(AdEvent.FAILED_TO_SHOW)
            }

            override fun onAdClicked() {
                onAdEvent?.invoke(AdEvent.CLICKED)
            }

            override fun onAdImpression() {
                onAdEvent?.invoke(AdEvent.IMPRESSION)
            }

            override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                onAdEvent?.invoke(AdEvent.PAID)
            }
        }
        ad.show(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }
    override fun onActivityStopped(activity: Activity) {}

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_START && !InterstitialAdManager.isInterstitialShowing && !isAdShowing) {
            scope.launch {
                loadAppOpenAdWithTimeOut()
            }
        }
    }
}
