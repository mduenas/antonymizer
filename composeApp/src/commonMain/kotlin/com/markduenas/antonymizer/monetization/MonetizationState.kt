package com.markduenas.antonymizer.monetization

import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the monetization state accessible throughout the app.
 * Wraps AdManager and BillingManager for common access patterns.
 */
data class MonetizationState(
    val adManager: AdManager,
    val billingManager: BillingManager
) {
    val adsRemoved: StateFlow<Boolean> get() = adManager.adsRemoved
    val interstitialReady: StateFlow<Boolean> get() = adManager.interstitialReady
    val rewardedReady: StateFlow<Boolean> get() = adManager.rewardedReady
    val removeAdsPrice: StateFlow<String?> get() = billingManager.removeAdsPrice
    val purchaseState: StateFlow<PurchaseState> get() = billingManager.purchaseState
}
