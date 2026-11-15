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
const val CONSENT_RETRY_TRACKING = 7
const val CONSENT_PRESENT_FORM = 8

/**
 * A repeated onAppear, update, or become-active call.
 * While the UMP form is still in flight the result is [CONSENT_IGNORE], so ATT
 * cannot start early. After the form finishes, a later activation may retry
 * ATT only when ads are allowed and the prompt has not been shown yet.
 */
fun consentLifecycleCode(
    presentable: Boolean,
    kotlinReady: Boolean,
    consentFlowStarted: Boolean,
    consentFlowFinished: Boolean = false,
    canRequestAds: Boolean = false,
    attNotDetermined: Boolean = false,
    attAlreadyRequested: Boolean = false
): Int {
    if (!presentable || !kotlinReady) return CONSENT_IGNORE
    if (!consentFlowStarted) return CONSENT_BEGIN_UPDATE
    if (!consentFlowFinished) return CONSENT_IGNORE
    if (canRequestAds && attNotDetermined && !attAlreadyRequested) return CONSENT_RETRY_TRACKING
    return CONSENT_IGNORE
}

/**
 * After requestConsentInfoUpdate returns, including when it passes an error.
 * A failed update with no stored consent is retried on the next activation.
 * A stored decision still continues to the form.
 */
fun postConsentInfoCode(updateFailed: Boolean, canRequestAds: Boolean): Int {
    if (updateFailed && !canRequestAds) return CONSENT_RETRY_UPDATE
    return CONSENT_PRESENT_FORM
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
 * After the privacy-options form. A new grant starts ads, still waiting for
 * ATT when it is unanswered. A denial does not start the SDK.
 */
fun afterPrivacyOptionsCode(
    canRequestAds: Boolean,
    attNotDetermined: Boolean,
    sdkAlreadyStarted: Boolean
): Int {
    if (!canRequestAds) return CONSENT_STOP
    if (attNotDetermined) return CONSENT_REQUEST_TRACKING
    if (sdkAlreadyStarted) return CONSENT_START_AND_RELOAD
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
