package com.markduenas.antonymizer.monetization

import com.markduenas.antonymizer.data.source.SettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * StoreKit 2 implementation. Swift owns transactions. This class writes the
 * shared ads-removed flag and the purchase state Compose already collects.
 */
class BillingManagerIos(
    private val settingsStorage: SettingsStorage,
    private val scope: CoroutineScope
) : BillingManager, IosBillingListener {

    private val entitlementApplier = EntitlementApplier { removed ->
        settingsStorage.setAdsRemoved(removed)
    }

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.NotInitialized)
    override val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    override val adsRemoved: StateFlow<Boolean> = settingsStorage.adsRemoved
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    private val _removeAdsPrice = MutableStateFlow<String?>(null)
    override val removeAdsPrice: StateFlow<String?> = _removeAdsPrice.asStateFlow()

    private var initialized = false

    override fun initialize() {
        if (initialized) return
        initialized = true
        IosBillingBridge.listener = this
        val provider = NativeBillingProviderHolder.provider
        if (provider == null) {
            _purchaseState.value = PurchaseState.Unavailable("Purchases are not available")
            return
        }
        provider.start(BillingManager.PRODUCT_REMOVE_ADS)
    }

    override fun endConnection() {
        // StoreKit's transaction listener lives with the process.
    }

    override fun purchaseRemoveAds() {
        val provider = NativeBillingProviderHolder.provider
        if (provider == null) {
            _purchaseState.value = PurchaseState.Unavailable("Purchases are not available")
            return
        }
        provider.purchaseRemoveAds()
    }

    override fun restorePurchases() {
        val provider = NativeBillingProviderHolder.provider
        if (provider == null) {
            _purchaseState.value = PurchaseState.Unavailable("Purchases are not available")
            return
        }
        provider.restorePurchases()
    }

    override fun onPrice(price: String?) {
        _removeAdsPrice.value = price
    }

    override fun onEntitlement(adsRemoved: Boolean, generation: Long) {
        scope.launch {
            entitlementApplier.apply(generation, adsRemoved)
        }
    }

    override fun onState(state: String, message: String?) {
        _purchaseState.value = purchaseStateFromBridge(state, message)
    }
}
