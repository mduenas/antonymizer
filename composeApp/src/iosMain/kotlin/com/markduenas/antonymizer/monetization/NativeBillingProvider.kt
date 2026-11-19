package com.markduenas.antonymizer.monetization

/**
 * StoreKit 2 lives in Swift. Kotlin calls this; Swift reports price, entitlement,
 * and purchase state back through [IosBillingBridge].
 */
interface NativeBillingProvider {
    fun start(productId: String)
    fun purchaseRemoveAds()
    fun restorePurchases()
}

interface IosBillingListener {
    fun onPrice(price: String?)
    fun onEntitlement(adsRemoved: Boolean, generation: Long)
    fun onState(state: String, message: String?)
}

object IosBillingBridge {
    var listener: IosBillingListener? = null

    fun priceUpdated(price: String?) {
        listener?.onPrice(price)
    }

    fun entitlementUpdated(adsRemoved: Boolean, generation: Long) {
        listener?.onEntitlement(adsRemoved, generation)
    }

    fun stateUpdated(state: String, message: String?) {
        listener?.onState(state, message)
    }
}

object NativeBillingProviderHolder {
    var provider: NativeBillingProvider? = null
}
