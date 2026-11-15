package com.markduenas.antonymizer.monetization

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonetizationPolicyTest {

    private val cooldown = AdManager.INTERSTITIAL_COOLDOWN_MS

    @Test
    fun interstitialHiddenWhenAdsRemoved() {
        assertFalse(
            InterstitialPolicy.shouldShow(
                adsRemoved = true,
                lastShownAtMillis = 0L,
                nowMillis = cooldown,
                adLoaded = true
            )
        )
    }

    @Test
    fun interstitialHiddenWhenAdNotLoaded() {
        assertFalse(
            InterstitialPolicy.shouldShow(
                adsRemoved = false,
                lastShownAtMillis = 0L,
                nowMillis = cooldown,
                adLoaded = false
            )
        )
    }

    @Test
    fun interstitialHiddenInsideCooldown() {
        assertFalse(
            InterstitialPolicy.shouldShow(
                adsRemoved = false,
                lastShownAtMillis = 1_000L,
                nowMillis = 1_000L + cooldown - 1,
                adLoaded = true
            )
        )
    }

    @Test
    fun interstitialShowsWhenCooldownElapses() {
        assertTrue(
            InterstitialPolicy.shouldShow(
                adsRemoved = false,
                lastShownAtMillis = 1_000L,
                nowMillis = 1_000L + cooldown,
                adLoaded = true
            )
        )
    }

    @Test
    fun interstitialShowsWhenNeverShown() {
        // DataStore's default last-shown value is 0. A real clock is long past the cooldown.
        assertTrue(
            InterstitialPolicy.shouldShow(
                adsRemoved = false,
                lastShownAtMillis = 0L,
                nowMillis = 1_700_000_000_000L,
                adLoaded = true
            )
        )
    }

    @Test
    fun clockMovedBackwardCountsAsCooldownElapsed() {
        assertTrue(
            InterstitialPolicy.shouldShow(
                adsRemoved = false,
                lastShownAtMillis = 10_000L,
                nowMillis = 9_000L,
                adLoaded = true
            )
        )
    }

    @Test
    fun lifecycleWhileConsentIsInFlightDoesNotRequestTracking() {
        assertEquals(
            CONSENT_BEGIN_UPDATE,
            consentLifecycleCode(
                presentable = true,
                kotlinReady = true,
                consentFlowStarted = false
            )
        )
        assertEquals(
            CONSENT_IGNORE,
            consentLifecycleCode(
                presentable = true,
                kotlinReady = true,
                consentFlowStarted = true
            )
        )
        assertEquals(
            CONSENT_IGNORE,
            consentLifecycleCode(
                presentable = false,
                kotlinReady = true,
                consentFlowStarted = false
            )
        )
    }

    @Test
    fun declinedConsentDoesNotRequestTrackingOrStartAds() {
        assertEquals(
            CONSENT_STOP,
            postConsentFormCode(
                formCouldBePresented = true,
                canRequestAds = false,
                attNotDetermined = true
            )
        )
    }

    @Test
    fun unansweredTrackingWaitsUntilAfterTheForm() {
        assertEquals(
            CONSENT_REQUEST_TRACKING,
            postConsentFormCode(
                formCouldBePresented = true,
                canRequestAds = true,
                attNotDetermined = true
            )
        )
        assertEquals(
            CONSENT_RETRY_UPDATE,
            postConsentFormCode(
                formCouldBePresented = false,
                canRequestAds = true,
                attNotDetermined = true
            )
        )
    }

    @Test
    fun deniedTrackingStillStartsAdsAfterConsent() {
        assertEquals(
            CONSENT_START_ADS,
            postConsentFormCode(
                formCouldBePresented = true,
                canRequestAds = true,
                attNotDetermined = false
            )
        )
        assertEquals(
            CONSENT_START_ADS,
            postTrackingCode(canRequestAds = true, loadsAlreadyBegan = false)
        )
        assertEquals(
            CONSENT_START_AND_RELOAD,
            postTrackingCode(canRequestAds = true, loadsAlreadyBegan = true)
        )
        assertEquals(
            CONSENT_STOP,
            postTrackingCode(canRequestAds = false, loadsAlreadyBegan = true)
        )
    }

    @Test
    fun productIdMatchesPlayAndAppStore() {
        assertEquals("remove_ads", BillingManager.PRODUCT_REMOVE_ADS)
    }

    @Test
    fun bridgeStatesMapToPurchaseState() {
        assertEquals(PurchaseState.Ready, purchaseStateFromBridge("ready", null))
        assertEquals(PurchaseState.Purchasing, purchaseStateFromBridge("purchasing", null))
        assertEquals(
            PurchaseState.AwaitingApproval("Waiting for approval"),
            purchaseStateFromBridge("pending", null)
        )
        assertEquals(PurchaseState.PurchaseSuccess, purchaseStateFromBridge("success", null))
        assertEquals(
            PurchaseState.PurchaseError("Purchase cancelled"),
            purchaseStateFromBridge("cancelled", null)
        )
        assertEquals(
            PurchaseState.PurchaseError("Store down"),
            purchaseStateFromBridge("error", "Store down")
        )
        assertTrue(purchaseStateFromBridge("nope", "x") is PurchaseState.Unavailable)
    }

    @Test
    fun pendingAndCancelledDoNotStickThePurchaseButton() {
        val pending = purchaseStateFromBridge("pending", "Waiting for approval")
        val cancelled = purchaseStateFromBridge("cancelled", "Purchase cancelled")
        val purchasing = purchaseStateFromBridge("purchasing", null)
        assertTrue(canStartRemoveAdsPurchase(pending))
        assertTrue(canStartRemoveAdsPurchase(cancelled))
        assertTrue(canStartRemoveAdsPurchase(PurchaseState.Ready))
        assertFalse(canStartRemoveAdsPurchase(purchasing))
        assertFalse(canStartRemoveAdsPurchase(PurchaseState.NotInitialized))
        assertFalse(canStartRemoveAdsPurchase(PurchaseState.Unavailable("offline")))
    }

    @Test
    fun newerEntitlementOverwritesAndStaleLaunchCheckDoesNot() = runBlocking {
        val writes = mutableListOf<Boolean>()
        val applier = EntitlementApplier { writes += it }
        applier.apply(1, false)
        applier.apply(2, true)
        applier.apply(1, false)
        applier.apply(3, false)
        assertEquals(listOf(false, true, false), writes)
    }

    @Test
    fun entitlementWritesAreSerializedAcrossTheSuspend() = runBlocking {
        val writes = mutableListOf<Boolean>()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val applier = EntitlementApplier { value ->
            if (!value) {
                entered.complete(Unit)
                release.await()
            }
            writes += value
        }
        val first = launch { applier.apply(1, false) }
        entered.await()
        val second = launch { applier.apply(2, true) }
        delay(50)
        assertEquals(emptyList(), writes)
        release.complete(Unit)
        first.join()
        second.join()
        assertEquals(listOf(false, true), writes)
    }
}
