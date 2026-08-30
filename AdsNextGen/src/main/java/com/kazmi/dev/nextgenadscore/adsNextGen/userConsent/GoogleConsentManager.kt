package com.kazmi.dev.nextgenadscore.adsNextGen.userConsent

import android.app.Activity
import android.app.Application
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.kazmi.dev.nextgenadscore.adsNextGen.AppOpenResume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object GoogleConsentManager {

    private const val TAG = "GoogleConsentManager"

    private lateinit var consentInfo: ConsentInformation
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Returns true if the privacy options form is required.
     */
    val isPrivacyOptionsRequired: Boolean
        get() = if (::consentInfo.isInitialized) {
            consentInfo.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } else false

    /**
     * Resets consent state (useful for testing).
     */
    fun resetConsent() {
        if (::consentInfo.isInitialized) {
            consentInfo.reset()
        }
    }

    /**
     * Initializes consent gathering and SDK initialization.
     * Use this in your Splash Screen.
     * 
     * @param activity The current activity.
     * @param admobAppId Your AdMob App ID.
     * @param resumeAdUnitId Ad Unit ID for App Open ads on resume. If null, uses AdsSettings.appOpenId.
     * @param debugMode Set to true to enable debug geography (EEA).
     * @param testDeviceHashedId Your device's hashed ID for UMP debug mode.
     * @param onInitializationComplete Callback triggered when the app is ready to navigate.
     */
    fun initConsentInfo(
        activity: Activity,
        admobAppId: String,
        resumeAdUnitId: String? = null,
        debugMode: Boolean = false,
        testDeviceHashedId: String? = null,
        onInitializationComplete: () -> Unit
    ) {
        consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        val paramsBuilder = ConsentRequestParameters.Builder()
        if (debugMode) {
            val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            
            testDeviceHashedId?.let {
                debugSettingsBuilder.addTestDeviceHashedId(it)
            }
            
            paramsBuilder.setConsentDebugSettings(debugSettingsBuilder.build())
        }

        val consentRequestParameters = paramsBuilder.build()

        consentInfo.requestConsentInfoUpdate(
            activity,
            consentRequestParameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    // Trigger initialization if we can request ads (even if there was a form error)
                    if (consentInfo.canRequestAds()) {
                        initializeAds(activity.application, admobAppId, resumeAdUnitId, onInitializationComplete)
                    } else {
                        onInitializationComplete()
                    }
                }
            },
            { error ->
                if (consentInfo.canRequestAds()) {
                    initializeAds(activity.application, admobAppId, resumeAdUnitId, onInitializationComplete)
                } else {
                    onInitializationComplete()
                }
            }
        )
    }

    private fun initializeAds(
        application: Application,
        admobAppId: String,
        resumeAdUnitId: String?,
        onComplete: () -> Unit,
    ) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            onComplete()
            return
        }

        scope.launch(Dispatchers.IO) {
            val requestConfiguration = RequestConfiguration.Builder().build()
            MobileAds.setRequestConfiguration(requestConfiguration)

            val appId = InitializationConfig.Builder(admobAppId).build()
            MobileAds.initialize(application, appId) {
                // Initialize App Open on Resume
                AppOpenResume(application, resumeAdUnitId)
            }
            
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    /**
     * Shows the privacy options form (should be called from settings menu).
     */
    fun showPrivacyOptionForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.d(TAG, "showPrivacyOptionForm Error: ${formError.message}")
            }
        }
    }
}
