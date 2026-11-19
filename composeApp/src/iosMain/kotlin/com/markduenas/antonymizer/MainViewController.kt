package com.markduenas.antonymizer

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.markduenas.antonymizer.data.source.SettingsStorage
import com.markduenas.antonymizer.data.source.createDataStore
import com.markduenas.antonymizer.monetization.AdManagerIos
import com.markduenas.antonymizer.monetization.BillingManagerIos
import com.markduenas.antonymizer.monetization.MonetizationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private class IosAppGraph(
    val settingsStorage: SettingsStorage,
    val monetizationState: MonetizationState
)

fun MainViewController() = ComposeUIViewController {
    // The content lambda recomposes. One graph keeps a single StoreKit listener.
    val graph = remember {
        val dataStore = createDataStore()
        val settingsStorage = SettingsStorage(dataStore)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val adManager = AdManagerIos(settingsStorage, scope)
        val billingManager = BillingManagerIos(settingsStorage, scope)
        adManager.initialize()
        billingManager.initialize()
        IosAppGraph(settingsStorage, MonetizationState(adManager, billingManager))
    }
    App(graph.settingsStorage, graph.monetizationState)
}
