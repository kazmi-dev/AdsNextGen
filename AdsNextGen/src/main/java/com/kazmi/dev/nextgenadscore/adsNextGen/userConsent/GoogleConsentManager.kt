package com.kazmi.dev.nextgenadscore.adsNextGen.userConsent

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.kazmi.dev.nextgenadscore.adsNextGen.AdMobInitializer

object GoogleConsentManager {

    private const val TAG = "GoogleConsentManager"
    private lateinit var consentInfo: ConsentInformation

    /**
     * Returns true if ads can be requested based on the current consent status.
     */
    fun canRequestAds(activity: Activity): Boolean {
        if (!::consentInfo.isInitialized) {
            consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        }
        return consentInfo.canRequestAds()
    }

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
    fun resetConsent(activity: Activity) {
        if (!::consentInfo.isInitialized) {
            consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        }
        consentInfo.reset()
    }

    /**
     * Optimized flow: Checks if ads can be requested immediately after info update,
     * potentially initializing ads in parallel with the consent form.
     */
    fun initConsentAndAds(
        activity: Activity,
        admobAppId: String,
        resumeAdUnitId: String? = null,
        debugMode: Boolean = false,
        testDeviceHashedId: String? = null,
        onAdsInitialized: () -> Unit = {}
    ) {
        consentInfo = UserMessagingPlatform.getConsentInformation(activity)

        val paramsBuilder = ConsentRequestParameters.Builder()
        if (debugMode) {
            val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            testDeviceHashedId?.let { debugSettingsBuilder.addTestDeviceHashedId(it) }
            paramsBuilder.setConsentDebugSettings(debugSettingsBuilder.build())
        }

        consentInfo.requestConsentInfoUpdate(
            activity,
            paramsBuilder.build(),
            {
                // Parallel Check: If we already have consent/no consent needed, init ads now.
                if (consentInfo.canRequestAds()) {
                    AdMobInitializer.initialize(activity.application, admobAppId, resumeAdUnitId, onAdsInitialized)
                }

                // Show form if required.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.e(TAG, "Consent form error: ${formError.message}")
                    }
                    
                    // Final Check: If form changed status to 'can request ads', init ads.
                    if (consentInfo.canRequestAds()) {
                        AdMobInitializer.initialize(activity.application, admobAppId, resumeAdUnitId, onAdsInitialized)
                    } else {
                        // User might have denied consent. Trigger callback to let app proceed.
                        onAdsInitialized()
                    }
                }
            },
            { error ->
                Log.e(TAG, "Consent info update error: ${error.message}")
                if (consentInfo.canRequestAds()) {
                    AdMobInitializer.initialize(activity.application, admobAppId, resumeAdUnitId, onAdsInitialized)
                } else {
                    onAdsInitialized()
                }
            }
        )
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
