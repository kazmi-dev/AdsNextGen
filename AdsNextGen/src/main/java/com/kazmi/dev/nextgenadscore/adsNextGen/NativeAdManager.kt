package com.kazmi.dev.nextgenadscore.adsNextGen

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.kazmi.dev.nextgenadscore.R

class NativeAdManager(private val context: Context) {

    companion object {
        private const val TAG = "NativeAdManager"
    }

    private var nativeAd: NativeAd? = null
    private var isAdLoading: Boolean = false

    enum class NativeSize {
        SMALL, MEDIUM, FULL
    }

    fun loadNativeAd(
        adUnitId: String? = null,
        onAdEvent: (AdEvent, NativeAd?) -> Unit
    ) {
        val finalAdUnitId = adUnitId ?: AdsSettings.nativeId

        if (AdsSettings.isAppPurchased) {
            onAdEvent(AdEvent.SKIPPED_DUE_TO_PURCHASE, null)
            return
        }

        if (!NetworkObserver.isConnected(context)) {
            onAdEvent(AdEvent.FAILED_TO_LOAD, null)
            return
        }

        isAdLoading = true
        val adRequest = NativeAdRequest.Builder(finalAdUnitId, listOf(NativeAd.NativeAdType.NATIVE)).build()

        NativeAdLoader.load(
            adRequest,
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    Log.d(TAG, "onNativeAdLoaded: SUCCESS")
                    this@NativeAdManager.nativeAd = nativeAd
                    isAdLoading = false
                    
                    nativeAd.adEventCallback = object : NativeAdEventCallback {
                        override fun onAdShowedFullScreenContent() {
                            onAdEvent(AdEvent.SHOWED, nativeAd)
                        }

                        override fun onAdDismissedFullScreenContent() {
                            onAdEvent(AdEvent.DISMISSED, nativeAd)
                        }

                        override fun onAdImpression() {
                            onAdEvent(AdEvent.IMPRESSION, nativeAd)
                        }

                        override fun onAdClicked() {
                            onAdEvent(AdEvent.CLICKED, nativeAd)
                        }

                        override fun onAdPaid(value: AdValue) {
                            onAdEvent(AdEvent.PAID, nativeAd)
                        }
                    }
                    
                    onAdEvent(AdEvent.LOADED, nativeAd)
                }

                override fun onAdFailedToLoad(adError: com.google.android.libraries.ads.mobile.sdk.common.LoadAdError) {
                    Log.d(TAG, "onAdFailedToLoad: ${adError.message}")
                    isAdLoading = false
                    onAdEvent(AdEvent.FAILED_TO_LOAD, null)
                }
            }
        )
    }

    fun showNativeAd(
        container: FrameLayout,
        size: NativeSize = NativeSize.MEDIUM,
        layoutResId: Int? = null
    ) {
        val ad = nativeAd ?: return
        
        val layoutId = layoutResId ?: when (size) {
            NativeSize.SMALL -> R.layout.native_ad_small
            NativeSize.MEDIUM -> R.layout.native_ad_medium
            NativeSize.FULL -> R.layout.native_ad_full
        }

        val adView = LayoutInflater.from(context).inflate(layoutId, null) as NativeAdView
        populateNativeAdView(ad, adView)
        
        container.removeAllViews()
        container.addView(adView)
    }

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.findViewById<TextView>(R.id.ad_headline)?.let {
            it.text = nativeAd.headline
            adView.headlineView = it
        }

        adView.findViewById<TextView>(R.id.ad_body)?.let {
            it.text = nativeAd.body
            adView.bodyView = it
        }

        adView.findViewById<Button>(R.id.ad_call_to_action)?.let {
            it.text = nativeAd.callToAction
            adView.callToActionView = it
        }

        adView.findViewById<ImageView>(R.id.ad_app_icon)?.let {
            it.setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView = it
        }

        val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
        if (mediaView != null) {
            mediaView.mediaContent = nativeAd.mediaContent
        }
        
        adView.registerNativeAd(nativeAd, mediaView)
    }

    fun getLoadedAd(): NativeAd? = nativeAd

    fun destroy() {
        nativeAd = null
    }
}
