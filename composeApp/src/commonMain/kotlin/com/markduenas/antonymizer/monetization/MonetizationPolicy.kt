package com.markduenas.antonymizer.monetization

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared rules for when an interstitial may show. Both platforms call this so
 * the cooldown, the ad-free flag, and a clock moved backward stay in lockstep.
 */
object InterstitialPolicy {
    fun shouldShow(
        adsRemoved: Boolean,
        lastShownAtMillis: Long,
        nowMillis: Long,
        adLoaded: Boolean,
        cooldownMillis: Long = AdManager.INTERSTITIAL_COOLDOWN_MS
    ): Boolean {
        if (adsRemoved || !adLoaded) return false
        // A clock set backward would otherwise look like the cooldown is still running.
        if (nowMillis < lastShownAtMillis) return true
        return nowMillis - lastShownAtMillis >= cooldownMillis
    }
}

/**
 * UMP's canRequestAds is the only gate for requesting an ad.
 * An ATT denial does not suppress the request. The SDK omits the IDFA itself.
 */
fun shouldRequestAds(canRequestAds: Boolean): Boolean = canRequestAds

/**
 * Applies StoreKit or Play entitlement writes in generation order.
 * A launch check that finishes after a purchase must not clear the new grant.
 */
class EntitlementApplier(
    private val write: suspend (Boolean) -> Unit
) {
    private val mutex = Mutex()
    private var lastAppliedGeneration = Long.MIN_VALUE

    suspend fun apply(generation: Long, adsRemoved: Boolean) {
        mutex.withLock {
            if (generation < lastAppliedGeneration) return@withLock
            lastAppliedGeneration = generation
            write(adsRemoved)
        }
    }
}

/**
 * Maps the string states the iOS StoreKit bridge sends into [PurchaseState].
 * Pending and cancelled stay retryable. See [canStartRemoveAdsPurchase].
 */
fun purchaseStateFromBridge(state: String, message: String?): PurchaseState = when (state) {
    "ready" -> PurchaseState.Ready
    "purchasing" -> PurchaseState.Purchasing
    "pending" -> PurchaseState.AwaitingApproval(message ?: "Waiting for approval")
    "success" -> PurchaseState.PurchaseSuccess
    "cancelled" -> PurchaseState.PurchaseError(message ?: "Purchase cancelled")
    "error" -> PurchaseState.PurchaseError(message ?: "Purchase failed")
    "unavailable" -> PurchaseState.Unavailable(message ?: "Purchases are not available")
    else -> PurchaseState.Unavailable(message ?: "Purchases are not available")
}

/**
 * The Remove Ads button stays enabled unless a purchase sheet is up, billing
 * never became ready, or the store is unavailable. Ask to Buy and a cancel
 * both leave the button usable.
 */
fun canStartRemoveAdsPurchase(state: PurchaseState): Boolean = when (state) {
    PurchaseState.Ready,
    is PurchaseState.AwaitingApproval,
    is PurchaseState.PurchaseError -> true
    else -> false
}
