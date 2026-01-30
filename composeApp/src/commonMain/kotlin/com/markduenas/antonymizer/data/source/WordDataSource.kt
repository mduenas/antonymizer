package com.markduenas.antonymizer.data.source

import antonymizer.composeapp.generated.resources.Res
import com.markduenas.antonymizer.data.model.AntonymPair
import com.markduenas.antonymizer.data.model.AntonymPairsData
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

class WordDataSource {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadAntonymPairs(): List<AntonymPair> {
        return try {
            val jsonString = Res.readBytes("files/antonym_pairs.json").decodeToString()
            val data = json.decodeFromString<AntonymPairsData>(jsonString)
            data.pairs
        } catch (e: Exception) {
            emptyList()
        }
    }
}
