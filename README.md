# AdsNextGen

A lightweight, easy-to-use Android library for integrating Google Mobile Ads (NextGen SDK) with built-in support for multiple ad formats, internet connectivity checks, and simplified lifecycle management.

## Features
- **All-in-one Ad Management**: Support for Banner, Interstitial, Rewarded, Rewarded Interstitial, and App Open ads.
- **Unified Callbacks**: Monitor ad events (Loaded, Showed, Clicked, Paid, Rewarded, etc.) using a single enum.
- **Internet Awareness**: Automatically checks for connectivity before attempting to load ads.
- **Purchase Support**: Easily disable all ads globally for pro users.
- **Test Mode**: Built-in test ad unit IDs for easy development.
- **NextGen SDK**: Built on top of the latest Google Android Libraries for Ads.

## Installation

### 1. Add the JitPack repository to your settings.gradle.kts
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
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

### 1. Initialization (Splash Screen)
In your Splash screen, call `initConsentInfo`. This handles GDPR consent and initializes the SDK. Once `onInitializationComplete` is called, you can navigate to the next screen.

```kotlin
GoogleConsentManager.initConsentInfo(
    activity = this,
    admobAppId = getString(R.string.admob_app_id),
    resumeAdUnitId = "your_app_open_id",
    debugMode = BuildConfig.DEBUG, // Set true to test GDPR forms
    onInitializationComplete = {
        // Move to the next activity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
)
```

### 2. Global Configuration
Set the purchase status to disable ads globally:
```kotlin
AdsSettings.isAppPurchased = true // Default is false
```

### 3. Banner Ads
```kotlin
BannerAdManager.showBannerAd(
    adViewContainer = binding.bannerContainer,
    activity = this,
    adUnitId = "your_ad_unit_id", // Optional: defaults to test ID
    adSize = BannerAdManager.BannerAdSize.ADAPTIVE,
    onAdEvent = { event ->
        when(event) {
            AdEvent.LOADED -> // Ad loaded
            AdEvent.FAILED_TO_LOAD -> // Handle failure
            // ...
        }
    }
)
```

### 4. Interstitial Ads
```kotlin
lifecycleScope.launch {
    InterstitialAdManager.loadInterstitialAdWithTimeOut(
        activity = this,
        adUnitId = "your_ad_unit_id", // Optional: defaults to test ID
        onAdEvent = { event ->
            when(event) {
                AdEvent.DISMISSED -> // Proceed to next screen
                AdEvent.FAILED_TO_LOAD -> // Handle failure
                // ...
            }
        }
    )
}
```

### 5. Native Ads
```kotlin
val nativeAdManager = NativeAdManager(context)
nativeAdManager.loadNativeAd(
    adUnitId = "your_ad_unit_id",
    onAdEvent = { event, ad ->
        if (event == AdEvent.LOADED) {
            // Show using default medium layout
            nativeAdManager.showNativeAd(binding.adContainer, NativeAdManager.NativeSize.MEDIUM)
            
            // OR show using your own custom layout
            // nativeAdManager.showNativeAd(binding.adContainer, layoutResId = R.layout.my_custom_native_layout)
        }
    }
)
```

## License
MIT License
