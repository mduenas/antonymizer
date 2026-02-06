package com.markduenas.antonymizer

import androidx.compose.ui.window.ComposeUIViewController
import com.markduenas.antonymizer.data.source.SettingsStorage
import com.markduenas.antonymizer.data.source.createDataStore
import com.markduenas.antonymizer.monetization.AdManagerIos
import com.markduenas.antonymizer.monetization.BillingManagerIos
import com.markduenas.antonymizer.monetization.MonetizationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun MainViewController() = ComposeUIViewController {
    val dataStore = createDataStore()
    val settingsStorage = SettingsStorage(dataStore)

    // Create a coroutine scope for iOS
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Initialize iOS monetization stubs
    val adManager = AdManagerIos(settingsStorage, scope)
    val billingManager = BillingManagerIos(settingsStorage, scope)

    adManager.initialize()
    billingManager.initialize()

    val monetizationState = MonetizationState(adManager, billingManager)

    App(settingsStorage, monetizationState)
}
