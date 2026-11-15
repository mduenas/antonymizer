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
 * iOS ads go through [NativeAdProvider], implemented by IosAdHelper.swift.
 * The Google Mobile Ads SDK starts only after UMP says ads can be requested.
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

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    override val adsRemoved: StateFlow<Boolean> = settingsStorage.adsRemoved
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    private var initialized = false

    override fun initialize() {
        if (initialized) return
        initialized = true
        IosAdsBridge.onReadyChanged = { interstitialReady, rewardedReady ->
            _interstitialReady.value = interstitialReady
            _rewardedReady.value = rewardedReady
        }
        IosConsentBridge.onPrivacyOptionsRequired = { required ->
            _privacyOptionsRequired.value = required
        }
        _privacyOptionsRequired.value = nativeProvider?.privacyOptionsRequired == true
        nativeProvider?.initialize()
        updateAdReadyState()
    }

    override fun loadInterstitial() {
        nativeProvider?.loadInterstitial()
    }

    override fun loadRewarded() {
        nativeProvider?.loadRewarded()
    }

    private fun updateAdReadyState() {
        val provider = nativeProvider ?: return
        _interstitialReady.value = provider.isInterstitialReady
        _rewardedReady.value = provider.isRewardedReady
    }

    override fun showInterstitialIfEligible(onComplete: () -> Unit) {
        scope.launch(Dispatchers.Main) {
            val provider = nativeProvider
            if (provider == null) {
                onComplete()
                return@launch
            }
            val show = InterstitialPolicy.shouldShow(
                adsRemoved = adsRemoved.first(),
                lastShownAtMillis = settingsStorage.lastInterstitialTime.first(),
                nowMillis = currentTimeMillis(),
                adLoaded = provider.isInterstitialReady
            )
            if (!show) {
                if (!provider.isInterstitialReady) provider.loadInterstitial()
                onComplete()
                return@launch
            }
            provider.showInterstitial(
                onPresented = {
                    scope.launch {
                        recordInterstitialShown()
                        settingsStorage.incrementAdsWatched()
                    }
                },
                onComplete = {
                    updateAdReadyState()
                    onComplete()
                }
            )
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

    override fun presentPrivacyOptions() {
        nativeProvider?.presentPrivacyOptions()
    }
}
