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
 * Codes for the iOS consent state machine. Swift compares against these exact
 * values, so the unit tests and the coordinator cannot drift.
 */
const val CONSENT_IGNORE = 0
const val CONSENT_BEGIN_UPDATE = 1
const val CONSENT_REQUEST_TRACKING = 2
const val CONSENT_START_ADS = 3
const val CONSENT_RETRY_UPDATE = 4
const val CONSENT_STOP = 5
const val CONSENT_START_AND_RELOAD = 6

/**
 * A repeated onAppear, update, or become-active call.
 * Once the UMP update has started, every later call is [CONSENT_IGNORE].
 * This path never requests ATT and never starts ads.
 */
fun consentLifecycleCode(
    presentable: Boolean,
    kotlinReady: Boolean,
    consentFlowStarted: Boolean
): Int {
    if (!presentable || !kotlinReady || consentFlowStarted) return CONSENT_IGNORE
    return CONSENT_BEGIN_UPDATE
}

/**
 * After the UMP form callback, including when that callback throws.
 * [formCouldBePresented] false is the window-not-ready retry and does not
 * finish the flow. Tracking is requested only when ads are allowed and ATT
 * is still unanswered. Otherwise the SDK starts only if ATT is already resolved.
 */
fun postConsentFormCode(
    formCouldBePresented: Boolean,
    canRequestAds: Boolean,
    attNotDetermined: Boolean
): Int {
    if (!formCouldBePresented) return CONSENT_RETRY_UPDATE
    if (!canRequestAds) return CONSENT_STOP
    if (attNotDetermined) return CONSENT_REQUEST_TRACKING
    return CONSENT_START_ADS
}

/**
 * After the manual ATT prompt returns. A denial still starts ads when UMP
 * allows them. Reload only if a load began before this decision.
 */
fun postTrackingCode(
    canRequestAds: Boolean,
    loadsAlreadyBegan: Boolean
): Int {
    if (!canRequestAds) return CONSENT_STOP
    if (loadsAlreadyBegan) return CONSENT_START_AND_RELOAD
    return CONSENT_START_ADS
}

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
