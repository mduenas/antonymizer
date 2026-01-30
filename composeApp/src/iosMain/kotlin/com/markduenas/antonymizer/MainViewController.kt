package com.markduenas.antonymizer

import androidx.compose.ui.window.ComposeUIViewController
import com.markduenas.antonymizer.data.source.SettingsStorage
import com.markduenas.antonymizer.data.source.createDataStore

fun MainViewController() = ComposeUIViewController {
    val dataStore = createDataStore()
    val settingsStorage = SettingsStorage(dataStore)
    App(settingsStorage)
}
