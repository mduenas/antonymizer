package com.markduenas.antonymizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.markduenas.antonymizer.data.source.SettingsStorage
import com.markduenas.antonymizer.data.source.initDataStore
import com.markduenas.antonymizer.monetization.AdManagerAndroid
import com.markduenas.antonymizer.monetization.BillingManagerAndroid
import com.markduenas.antonymizer.monetization.MonetizationState

class MainActivity : ComponentActivity() {

    private lateinit var adManager: AdManagerAndroid
    private lateinit var billingManager: BillingManagerAndroid

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val dataStore = initDataStore(applicationContext)
        val settingsStorage = SettingsStorage(dataStore)

        // Initialize monetization
        adManager = AdManagerAndroid(applicationContext, settingsStorage, lifecycleScope)
        billingManager = BillingManagerAndroid(applicationContext, settingsStorage, lifecycleScope)

        adManager.setActivity(this)
        billingManager.setActivity(this)

        adManager.initialize()
        billingManager.initialize()

        val monetizationState = MonetizationState(adManager, billingManager)

        setContent {
            App(settingsStorage, monetizationState)
        }
    }

    override fun onResume() {
        super.onResume()
        adManager.setActivity(this)
        billingManager.setActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}
