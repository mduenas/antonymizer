package com.markduenas.antonymizer.monetization

/**
 * Interface for native iOS ad operations.
 * Swift code implements this and passes it to Kotlin.
 */
interface NativeAdProvider {
    val isInterstitialReady: Boolean
    val isRewardedReady: Boolean

    fun initialize()
    fun loadInterstitial()
    fun loadRewarded()
    fun showInterstitial(onComplete: () -> Unit)
    fun showRewarded(onRewarded: () -> Unit, onDismissed: () -> Unit)
}

/**
 * Holder for the native ad provider instance.
 * Set from Swift before creating MainViewController.
 */
object NativeAdProviderHolder {
    var provider: NativeAdProvider? = null
}
