package com.kazmi.dev.nextgenadscore.adsNextGen

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowMetrics
import android.widget.FrameLayout
import com.google.android.gms.ads.mediation.admob.AdMobAdapter
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

object BannerAdManager {

    private const val TAG = "BannerAdManager"

    private var bannerAdView: AdView? = null

    enum class BannerAdSize {
        LARGE, MEDIUM, SMALL, ADAPTIVE, LEADERBOARD, FULL_BANNER, CUSTOM
    }

    enum class CollapsibleType {
        NONE, TOP, BOTTOM
    }

    fun showBannerAd(
        adViewContainer: FrameLayout,
        activity: Activity,
        adUnitId: String = "ca-app-pub-3940256099942544/6300978111",
        adSize: BannerAdSize = BannerAdSize.ADAPTIVE,
        customWidth: Int = 0,
        customHeight: Int = 0,
        collapsibleType: CollapsibleType = CollapsibleType.NONE,
        onAdEvent: (AdEvent) -> Unit
    ) {
        if (AdsSettings.isAppPurchased) {
            onAdEvent(AdEvent.SKIPPED_DUE_TO_PURCHASE)
            return
        }

        if (!NetworkObserver.isConnected(activity)) {
            onAdEvent(AdEvent.FAILED_TO_LOAD)
            return
        }

        bannerAdView?.let {
            adViewContainer.removeView(it)
            bannerAdView = null
        }

        loadBannerAd(
            activity,
            adUnitId,
            adSize,
            customWidth,
            customHeight,
            collapsibleType,
            adViewContainer,
            onLoad = { ad ->
                setAdEventCallbacks(ad, onAdEvent)
                onAdEvent(AdEvent.LOADED)
            },
            onFailed = {
                onAdEvent(AdEvent.FAILED_TO_LOAD)
            }
        )
    }

    private fun loadBannerAd(
        activity: Activity,
        adUnitId: String,
        adSize: BannerAdSize,
        customWidth: Int,
        customHeight: Int,
        collapsibleType: CollapsibleType,
        adViewContainer: FrameLayout,
        onLoad: (ad: BannerAd) -> Unit,
        onFailed: () -> Unit
    ) {
        bannerAdView = AdView(activity)
        bannerAdView?.visibility = View.GONE

        val bannerAdSize = when (adSize) {
            BannerAdSize.LARGE -> AdSize.LARGE_BANNER
            BannerAdSize.MEDIUM -> AdSize.MEDIUM_RECTANGLE
            BannerAdSize.SMALL -> AdSize.BANNER
            BannerAdSize.LEADERBOARD -> AdSize.LEADERBOARD
            BannerAdSize.FULL_BANNER -> AdSize.FULL_BANNER
            BannerAdSize.CUSTOM -> AdSize(customWidth, customHeight)
            else -> getAdaptiveBannerAdSize(activity)
        }

        adViewContainer.removeAllViews()
        adViewContainer.addView(bannerAdView)

        val adRequestBuilder = BannerAdRequest.Builder(adUnitId, bannerAdSize)
        
        if (collapsibleType != CollapsibleType.NONE) {
            val extras = Bundle()
            val collapsibleValue = if (collapsibleType == CollapsibleType.TOP) "top" else "bottom"
            extras.putString("collapsible", collapsibleValue)
            adRequestBuilder.putAdSourceExtrasBundle(AdMobAdapter::class.java, extras)
        }

        val adRequest = adRequestBuilder.build()
        bannerAdView!!.loadAd(
            adRequest,
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    bannerAdView?.visibility = View.VISIBLE
                    onLoad(ad)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "onAdFailedToLoad: ${adError.message}")
                    onFailed()
                }
            }
        )
    }

    private fun setAdEventCallbacks(ad: BannerAd, onAdEvent: (AdEvent) -> Unit) {
        ad.adEventCallback = object : BannerAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                onAdEvent(AdEvent.SHOWED)
            }

            override fun onAdDismissedFullScreenContent() {
                onAdEvent(AdEvent.DISMISSED)
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                onAdEvent(AdEvent.FAILED_TO_SHOW)
            }

            override fun onAdImpression() {
                onAdEvent(AdEvent.IMPRESSION)
            }

            override fun onAdClicked() {
                onAdEvent(AdEvent.CLICKED)
            }

            override fun onAdPaid(value: com.google.android.libraries.ads.mobile.sdk.common.AdValue) {
                onAdEvent(AdEvent.PAID)
            }
        }
    }

    private fun getAdaptiveBannerAdSize(activity: Activity): AdSize {
        val displayMetrics = activity.resources.displayMetrics
        val adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = activity.windowManager.currentWindowMetrics
            windowMetrics.bounds.width()
        } else {
            displayMetrics.widthPixels
        }
        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }
}
