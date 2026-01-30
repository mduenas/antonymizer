package com.markduenas.antonymizer.data.repository

import com.markduenas.antonymizer.data.model.AntonymPair
import com.markduenas.antonymizer.data.model.WordDifficulty
import com.markduenas.antonymizer.data.source.WordDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface WordRepository {
    suspend fun getAllPairs(): List<AntonymPair>
    suspend fun getPairsByDifficulty(difficulty: WordDifficulty): List<AntonymPair>
    suspend fun getRandomPairs(count: Int, difficulty: WordDifficulty? = null): List<AntonymPair>
    suspend fun getDistractors(correctAntonym: String, count: Int): List<String>
}

class WordRepositoryImpl(
    private val dataSource: WordDataSource = WordDataSource()
) : WordRepository {

    private var cachedPairs: List<AntonymPair>? = null
    private val mutex = Mutex()

    override suspend fun getAllPairs(): List<AntonymPair> {
        return mutex.withLock {
            cachedPairs ?: dataSource.loadAntonymPairs().also { cachedPairs = it }
        }
    }

    override suspend fun getPairsByDifficulty(difficulty: WordDifficulty): List<AntonymPair> {
        return getAllPairs().filter { it.difficulty == difficulty }
    }

    override suspend fun getRandomPairs(count: Int, difficulty: WordDifficulty?): List<AntonymPair> {
        val pairs = if (difficulty != null) {
            getPairsByDifficulty(difficulty)
        } else {
            getAllPairs()
        }
        return pairs.shuffled().take(count)
    }

    override suspend fun getDistractors(correctAntonym: String, count: Int): List<String> {
        val allAntonyms = getAllPairs()
            .map { it.antonym }
            .filter { it != correctAntonym }
            .distinct()

        return allAntonyms.shuffled().take(count)
    }
}
