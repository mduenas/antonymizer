package com.markduenas.antonymizer.monetization

import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform billing manager interface.
 * Handles in-app purchases for ad removal and other premium features.
 */
interface BillingManager {
    /** Current purchase state */
    val purchaseState: StateFlow<PurchaseState>

    /** Whether the "Remove Ads" product has been purchased */
    val adsRemoved: StateFlow<Boolean>

    /** Price string for display (e.g., "$2.99") */
    val removeAdsPrice: StateFlow<String?>

    /** Initialize billing connection */
    fun initialize()

    /** Disconnect billing client */
    fun endConnection()

    /** Launch purchase flow for removing ads */
    fun purchaseRemoveAds()

    /** Restore previous purchases */
    fun restorePurchases()

    companion object {
        // Product IDs must match those configured in Google Play Console
        const val PRODUCT_REMOVE_ADS = "remove_ads"
    }
}

/**
 * States for the purchase flow
 */
sealed class PurchaseState {
    /** Billing not yet initialized */
    data object NotInitialized : PurchaseState()

    /** Ready to make purchases */
    data object Ready : PurchaseState()

    /** Purchase in progress */
    data object Purchasing : PurchaseState()

    /** Purchase completed successfully */
    data object PurchaseSuccess : PurchaseState()

    /** Purchase failed or was cancelled */
    data class PurchaseError(val message: String) : PurchaseState()

    /** Billing service unavailable */
    data class Unavailable(val reason: String) : PurchaseState()
}
