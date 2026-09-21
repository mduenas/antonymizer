package com.markduenas.antonymizer.monetization

import com.markduenas.antonymizer.data.source.SettingsStorage
import com.markduenas.antonymizer.util.currentTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * iOS implementation of AdManager using native AdMob SDK via NativeAdProvider.
 *
 * Ad unit IDs are selected in AdHelper.swift:
 * - Debug: Google sample interstitial/rewarded test units
 * - Release: production ca-app-pub-7540731406850248/... units
 */
class AdManagerIos(
    private val settingsStorage: SettingsStorage,
    private val scope: CoroutineScope
) : AdManager {

    private val nativeProvider: NativeAdProvider?
        get() = NativeAdProviderHolder.provider

    private val _interstitialReady = MutableStateFlow(false)
    override val interstitialReady: StateFlow<Boolean> = _interstitialReady.asStateFlow()

    private val _rewardedReady = MutableStateFlow(false)
    override val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    override val adsRemoved: StateFlow<Boolean> = settingsStorage.adsRemoved
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    override fun initialize() {
        nativeProvider?.initialize()
        updateAdReadyState()
    }

    override fun loadInterstitial() {
        nativeProvider?.loadInterstitial()
        updateAdReadyState()
    }

    override fun loadRewarded() {
        nativeProvider?.loadRewarded()
        updateAdReadyState()
    }

    private fun updateAdReadyState() {
        _interstitialReady.value = nativeProvider?.isInterstitialReady ?: false
        _rewardedReady.value = nativeProvider?.isRewardedReady ?: false
    }

    override fun showInterstitialIfEligible(onComplete: () -> Unit) {
        scope.launch(Dispatchers.Main) {
            // Check if ads are removed
            if (adsRemoved.first()) {
                onComplete()
                return@launch
            }

            val provider = nativeProvider
            if (provider == null) {
                onComplete()
                return@launch
            }

            provider.showInterstitial {
                updateAdReadyState()
                scope.launch {
                    recordInterstitialShown()
                    settingsStorage.incrementAdsWatched()
                }
                onComplete()
            }
        }
    }

    override fun showRewardedAd(onRewarded: () -> Unit, onDismissed: () -> Unit) {
        scope.launch(Dispatchers.Main) {
            val provider = nativeProvider
            if (provider == null) {
                onDismissed()
                return@launch
            }

            provider.showRewarded(
                onRewarded = {
                    updateAdReadyState()
                    scope.launch {
                        settingsStorage.incrementAdsWatched()
                    }
                    onRewarded()
                },
                onDismissed = {
                    updateAdReadyState()
                    onDismissed()
                }
            )
        }
    }

    override suspend fun setAdsRemoved(removed: Boolean) {
        settingsStorage.setAdsRemoved(removed)
    }

    override suspend fun recordInterstitialShown() {
        settingsStorage.setLastInterstitialTime(currentTimeMillis())
    }
}
