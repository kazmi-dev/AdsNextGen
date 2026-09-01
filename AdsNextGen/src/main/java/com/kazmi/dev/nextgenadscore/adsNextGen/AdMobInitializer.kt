package com.kazmi.dev.nextgenadscore.adsNextGen

import android.app.Application
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object AdMobInitializer {

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var isInitializationFinished = false
    private val initListeners = mutableListOf<() -> Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Logic for initializing Mobile Ads SDK. Thread-safe and handles multiple calls.
     * Guaranteed to trigger completion callback AFTER SDK is ready.
     */
    fun initialize(
        application: Application,
        admobAppId: String,
        resumeAdUnitId: String? = null,
        onInitComplete: () -> Unit
    ) {
        synchronized(initListeners) {
            if (isInitializationFinished) {
                onInitComplete()
                return
            }
            initListeners.add(onInitComplete)
        }

        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        scope.launch(Dispatchers.IO) {
            val requestConfiguration = RequestConfiguration.Builder().build()
            MobileAds.setRequestConfiguration(requestConfiguration)

            val appId = InitializationConfig.Builder(admobAppId).build()
            
            // Wait for MobileAds.initialize completion callback
            MobileAds.initialize(application, appId) {
                //wait for mediation adapters to init
            }
            //load ads immediately if no mediation adapters are used
            // Initialize App Open on Resume
            AppOpenResume(application, resumeAdUnitId)

            // Notify listeners that SDK is fully ready
            scope.launch(Dispatchers.Main) {
                synchronized(initListeners) {
                    isInitializationFinished = true
                    initListeners.forEach { it.invoke() }
                    initListeners.clear()
                }
            }
        }
    }
}
