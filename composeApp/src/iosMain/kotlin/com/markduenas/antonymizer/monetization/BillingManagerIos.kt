package com.markduenas.antonymizer.monetization

import com.markduenas.antonymizer.data.source.SettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * iOS implementation of BillingManager.
 * Currently a stub - StoreKit integration can be added later.
 * For now, ads are disabled by default on iOS.
 */
class BillingManagerIos(
    private val settingsStorage: SettingsStorage,
    private val scope: CoroutineScope
) : BillingManager {

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Unavailable("iOS billing not implemented"))
    override val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    // Default to ads removed on iOS since we don't have ad implementation
    override val adsRemoved: StateFlow<Boolean> = settingsStorage.adsRemoved
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), true)

    private val _removeAdsPrice = MutableStateFlow<String?>("$2.99")
    override val removeAdsPrice: StateFlow<String?> = _removeAdsPrice.asStateFlow()

    override fun initialize() {
        // TODO: Implement StoreKit integration
    }

    override fun endConnection() {
        // No-op for iOS stub
    }

    override fun purchaseRemoveAds() {
        // TODO: Implement StoreKit purchase
        _purchaseState.value = PurchaseState.Unavailable("iOS purchases not yet available")
    }

    override fun restorePurchases() {
        // TODO: Implement StoreKit restore
    }
}
