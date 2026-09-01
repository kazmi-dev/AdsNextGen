# AdsNextGen

A lightweight, easy-to-use Android library for integrating Google Mobile Ads (NextGen SDK) with built-in support for multiple ad formats, internet connectivity checks, and simplified lifecycle management.

## Features
- **All-in-one Ad Management**: Support for **Banner**, **Interstitial**, **Native**, **Rewarded**, **Rewarded Interstitial**, and **App Open** ads.
- **Unified Callbacks**: Monitor ad events (Loaded, Showed, Clicked, Paid, Rewarded, etc.) using a single enum.
- **Internet Awareness**: Automatically checks for connectivity before attempting to load ads.
- **Purchase Support**: Easily disable all ads globally for pro users.
- **Global Ad IDs**: Set your Ad Unit IDs once and use them everywhere.
- **GDPR & Initialization**: Fast, optimized consent gathering and one-time SDK initialization.

## Installation

### 1. Add the JitPack repository to your settings.gradle.kts
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the dependency to your build.gradle.kts
```kotlin
dependencies {
    implementation("com.github.kazmi-dev:AdsNextGen:1.0.0")
}
```

## Usage

### 1. Global Configuration (Highly Recommended)
Set your production Ad Unit IDs and purchase status globally. This allows you to call ads without passing IDs every time.

```kotlin
// Disable ads for pro users
AdsSettings.isAppPurchased = true 

// Set production IDs (Defaults are AdMob test IDs)
AdsSettings.bannerId = "your_banner_id"
AdsSettings.interstitialId = "your_interstitial_id"
AdsSettings.nativeId = "your_native_id"
AdsSettings.rewardedId = "your_rewarded_id"
AdsSettings.rewardedInterstitialId = "your_rewarded_inter_id"
AdsSettings.appOpenId = "your_app_open_id"
```

### 2. Initialization (Splash Screen)
You can use the helper method to handle both consent and initialization, or call them separately for more control.

#### Option A: Combined Initialization (Recommended)
```kotlin
GoogleConsentManager.initConsentAndAds(
    activity = this,
    admobAppId = getString(R.string.admob_app_id),
    // resumeAdUnitId = "optional_override_id", 
    debugMode = BuildConfig.DEBUG,
    onAdsInitialized = {
        // Safe to navigate
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
)
```

#### Option B: Separate Logic
```kotlin
// 1. Gather Consent
GoogleConsentManager.gatherConsent(this, BuildConfig.DEBUG) { canRequestAds ->
    if (canRequestAds) {
        // 2. Initialize Ads
        GoogleConsentManager.initializeMobileAds(application, "your_app_id") {
            // SDK Ready
        }
    }
}
```

### 3. Banner Ads
```kotlin
BannerAdManager.showBannerAd(
    adViewContainer = binding.bannerContainer,
    activity = this,
    // adUnitId = "optional_override_id",
    adSize = BannerAdManager.BannerAdSize.ADAPTIVE,
    onAdEvent = { event ->
        // Handle events
    }
)
```

### 4. Interstitial Ads
```kotlin
lifecycleScope.launch {
    InterstitialAdManager.loadInterstitialAdWithTimeOut(
        activity = this,
        // adUnitId = "optional_override_id",
        onAdEvent = { event ->
            if (event == AdEvent.DISMISSED || event == AdEvent.FAILED_TO_LOAD) {
                moveNext()
            }
        }
    )
}
```

### 5. Native Ads
```kotlin
val nativeAdManager = NativeAdManager(context)
nativeAdManager.loadNativeAd(
    // adUnitId = "optional_override_id",
    onAdEvent = { event, ad ->
        if (event == AdEvent.LOADED) {
            nativeAdManager.showNativeAd(binding.adContainer, NativeAdManager.NativeSize.MEDIUM)
        }
    }
)
```

### 6. Rewarded Ads
```kotlin
lifecycleScope.launch {
    RewardedAdManager.loadRewardedAdWithTimeOut(
        activity = this,
        onAdEvent = { event ->
            if (event == AdEvent.REWARDED) {
                // Grant reward!
            }
        }
    )
}
```

## License
MIT License
