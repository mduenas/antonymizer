package com.markduenas.antonymizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.markduenas.antonymizer.data.source.SettingsStorage
import com.markduenas.antonymizer.data.source.initDataStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val dataStore = initDataStore(applicationContext)
        val settingsStorage = SettingsStorage(dataStore)

        setContent {
            App(settingsStorage)
        }
    }
}
