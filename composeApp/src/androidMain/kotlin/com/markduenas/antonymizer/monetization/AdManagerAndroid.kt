package com.markduenas.antonymizer.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.markduenas.antonymizer.data.source.SettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Android implementation of AdManager using Google AdMob.
 *
 * Test Ad Unit IDs (for development):
 * - Interstitial: ca-app-pub-3940256099942544/1033173712
 * - Rewarded: ca-app-pub-3940256099942544/5224354917
 */
class AdManagerAndroid(
    context: Context,
    private val settingsStorage: SettingsStorage,
    private val scope: CoroutineScope
) : AdManager {

    companion object {
        private const val TAG = "AdManagerAndroid"

        // Production ad unit IDs
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7540731406850248/9172618012"
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-7540731406850248/5811221418"
    }

    private val contextRef = WeakReference(context)
    private var activityRef: WeakReference<Activity>? = null

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private val _interstitialReady = MutableStateFlow(false)
    override val interstitialReady: StateFlow<Boolean> = _interstitialReady.asStateFlow()

    private val _rewardedReady = MutableStateFlow(false)
    override val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    override val adsRemoved: StateFlow<Boolean> = settingsStorage.adsRemoved
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    private var isInitialized = false

    override fun initialize() {
        if (isInitialized) return

        val context = contextRef.get() ?: return

        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
            isInitialized = true

            // Pre-load ads after initialization
            loadInterstitial()
            loadRewarded()
        }
    }

    fun setActivity(activity: Activity?) {
        activityRef = activity?.let { WeakReference(it) }
    }

    override fun loadInterstitial() {
        val context = contextRef.get() ?: return
        if (interstitialAd != null) return // Already loaded

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded")
                    interstitialAd = ad
                    _interstitialReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                    interstitialAd = null
                    _interstitialReady.value = false
                }
            }
        )
    }

    override fun loadRewarded() {
        val context = contextRef.get() ?: return
        if (rewardedAd != null) return // Already loaded

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded")
                    rewardedAd = ad
                    _rewardedReady.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                    _rewardedReady.value = false
                }
            }
        )
    }

    override fun showInterstitialIfEligible(onComplete: () -> Unit) {
        scope.launch(Dispatchers.Main) {
            // Check if ads are removed
            if (adsRemoved.first()) {
                Log.d(TAG, "Ads removed, skipping interstitial")
                onComplete()
                return@launch
            }

            // Check cooldown
            val lastShown = settingsStorage.lastInterstitialTime.first()
            val now = System.currentTimeMillis()
            if (now - lastShown < AdManager.INTERSTITIAL_COOLDOWN_MS) {
                Log.d(TAG, "Interstitial cooldown active, skipping")
                onComplete()
                return@launch
            }

            // Check if ad is ready
            val ad = interstitialAd
            val activity = activityRef?.get()

            if (ad == null || activity == null) {
                Log.d(TAG, "Interstitial not ready or no activity")
                loadInterstitial() // Try to load for next time
                onComplete()
                return@launch
            }

            // Set up callback
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial dismissed")
                    interstitialAd = null
                    _interstitialReady.value = false
                    loadInterstitial() // Preload next ad
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Interstitial failed to show: ${error.message}")
                    interstitialAd = null
                    _interstitialReady.value = false
                    loadInterstitial()
                    onComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial shown")
                    scope.launch {
                        recordInterstitialShown()
                        settingsStorage.incrementAdsWatched()
                    }
                }
            }

            // Show the ad
            ad.show(activity)
        }
    }

    override fun showRewardedAd(onRewarded: () -> Unit, onDismissed: () -> Unit) {
        scope.launch(Dispatchers.Main) {
            val ad = rewardedAd
            val activity = activityRef?.get()

            if (ad == null || activity == null) {
                Log.d(TAG, "Rewarded ad not ready or no activity")
                loadRewarded() // Try to load for next time
                onDismissed()
                return@launch
            }

            var wasRewarded = false

            // Set up callback
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed, wasRewarded: $wasRewarded")
                    rewardedAd = null
                    _rewardedReady.value = false
                    loadRewarded() // Preload next ad

                    if (wasRewarded) {
                        scope.launch {
                            settingsStorage.incrementAdsWatched()
                        }
                        onRewarded()
                    } else {
                        onDismissed()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Rewarded ad failed to show: ${error.message}")
                    rewardedAd = null
                    _rewardedReady.value = false
                    loadRewarded()
                    onDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad shown")
                }
            }

            // Show the ad with reward callback
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                wasRewarded = true
            }
        }
    }

    override suspend fun setAdsRemoved(removed: Boolean) {
        settingsStorage.setAdsRemoved(removed)
    }

    override suspend fun recordInterstitialShown() {
        settingsStorage.setLastInterstitialTime(System.currentTimeMillis())
    }
}
