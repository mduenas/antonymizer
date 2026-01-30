package com.markduenas.antonymizer.data.source

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

private lateinit var dataStore: DataStore<Preferences>

fun initDataStore(context: Context): DataStore<Preferences> {
    if (::dataStore.isInitialized) {
        return dataStore
    }
    dataStore = createDataStoreWithPath {
        context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath
    }
    return dataStore
}

actual fun createDataStore(): DataStore<Preferences> {
    if (!::dataStore.isInitialized) {
        throw IllegalStateException("DataStore not initialized. Call initDataStore(context) first.")
    }
    return dataStore
}
