package com.markduenas.antonymizer.monetization

import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform ad manager interface.
 * Provides abstraction for interstitial and rewarded ads.
 */
interface AdManager {
    /** Whether ads have been removed via IAP */
    val adsRemoved: StateFlow<Boolean>

    /** Whether an interstitial ad is ready to show */
    val interstitialReady: StateFlow<Boolean>

    /** Whether a rewarded ad is ready to show */
    val rewardedReady: StateFlow<Boolean>

    /** Initialize the ad SDK */
    fun initialize()

    /** Load an interstitial ad */
    fun loadInterstitial()

    /** Load a rewarded ad */
    fun loadRewarded()

    /**
     * Show an interstitial ad if:
     * 1. User hasn't purchased ad removal
     * 2. 3-minute cooldown has passed since last interstitial
     * 3. An ad is loaded and ready
     *
     * @param onComplete Called after ad is dismissed or if ad couldn't be shown
     */
    fun showInterstitialIfEligible(onComplete: () -> Unit)

    /**
     * Show a rewarded ad for hints/continues.
     * Rewarded ads are always available even if user purchased ad removal.
     *
     * @param onRewarded Called if user successfully watched the ad
     * @param onDismissed Called if user dismissed early or ad failed
     */
    fun showRewardedAd(onRewarded: () -> Unit, onDismissed: () -> Unit)

    /** Mark ads as removed (after IAP) */
    suspend fun setAdsRemoved(removed: Boolean)

    /** Update last interstitial timestamp */
    suspend fun recordInterstitialShown()

    companion object {
        const val INTERSTITIAL_COOLDOWN_MS = 3 * 60 * 1000L // 3 minutes
    }
}

/**
 * Rewarded ad types for different game features
 */
enum class RewardedAdType {
    HINT,           // Eliminate 2 wrong answers
    CONTINUE,       // Play one more round after losing
    BONUS_POINTS    // +25% bonus points after victory
}
