package com.markduenas.antonymizer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AntonymPair(
    val word: String,
    val antonym: String,
    val difficulty: WordDifficulty = WordDifficulty.MEDIUM
)

@Serializable
enum class WordDifficulty {
    EASY,
    MEDIUM,
    HARD
}

@Serializable
data class AntonymPairsData(
    val pairs: List<AntonymPair>
)
