package com.markduenas.antonymizer.monetization

/**
 * Interface for native iOS ad operations.
 * Swift code implements this and passes it to Kotlin.
 */
interface NativeAdProvider {
    val isInterstitialReady: Boolean
    val isRewardedReady: Boolean
    val privacyOptionsRequired: Boolean

    fun initialize()
    fun loadInterstitial()
    fun loadRewarded()

    /**
     * [onPresented] runs when the interstitial is about to appear, which is when
     * the cooldown is stamped. [onComplete] runs when it closes or cannot be shown.
     */
    fun showInterstitial(onPresented: () -> Unit, onComplete: () -> Unit)
    fun showRewarded(onRewarded: () -> Unit, onDismissed: () -> Unit)
    fun presentPrivacyOptions()
}

/**
 * Holder for the native ad provider instance.
 * Set from Swift before creating MainViewController.
 */
object NativeAdProviderHolder {
    var provider: NativeAdProvider? = null
}

/**
 * Swift reports load and dismiss here so the Compose ready flags stay current.
 * Boolean function parameters export as Swift Bool. A Boolean lambda does not.
 */
object IosAdsBridge {
    var onReadyChanged: ((Boolean, Boolean) -> Unit)? = null

    fun readyChanged(interstitialReady: Boolean, rewardedReady: Boolean) {
        onReadyChanged?.invoke(interstitialReady, rewardedReady)
    }
}

/** Swift reports UMP privacy-options requirement changes. */
object IosConsentBridge {
    var onPrivacyOptionsRequired: ((Boolean) -> Unit)? = null

    fun privacyOptionsRequiredChanged(required: Boolean) {
        onPrivacyOptionsRequired?.invoke(required)
    }
}
