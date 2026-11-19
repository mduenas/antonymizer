package com.markduenas.antonymizer.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.markduenas.antonymizer.data.model.BotDifficulty
import com.markduenas.antonymizer.domain.game.GameState
import com.markduenas.antonymizer.domain.game.PlayerAnswer
import com.markduenas.antonymizer.ui.screens.game.GameScreen
import com.markduenas.antonymizer.ui.screens.home.HomeScreen
import com.markduenas.antonymizer.ui.screens.leaderboard.LeaderboardScreen
import com.markduenas.antonymizer.ui.screens.results.ResultsScreen
import com.markduenas.antonymizer.ui.theme.AntonymArenaTheme
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Play phone screenshots, 1080x1920, written to store/android/phone.
 *
 * Off by default so :screenshots:testDebugUnitTest skips this class.
 * Regenerate with:
 *
 * ./gradlew :screenshots:recordRoborazziDebug -PplayStoreScreenshots=true --tests com.markduenas.antonymizer.screenshots.PlayStoreScreenshotTest
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w540dp-h960dp-xhdpi")
class PlayStoreScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun gate() {
        assumeTrue(
            "Set -PplayStoreScreenshots=true to render Play listing screenshots.",
            System.getProperty("play.store.screenshots") == "true"
        )
    }

    @Test
    fun home() {
        composeRule.setContent {
            Shot {
                HomeScreen(
                    playerStats = ScreenshotData.experiencedPlayerStats,
                    playerName = "WordMaster",
                    onPlayClick = {},
                    onLeaderboardClick = {},
                    onSettingsClick = {}
                )
            }
        }
        capture("01-home.png")
    }

    @Test
    fun question() {
        composeRule.setContent {
            Shot {
                GameScreen(
                    gameState = ScreenshotData.questionActiveState,
                    rewardedAdReady = true,
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        capture("02-question.png")
    }

    @Test
    fun questionResult() {
        composeRule.setContent {
            Shot {
                GameScreen(
                    gameState = GameState.QuestionResult(
                        roundNumber = 2,
                        questionNumber = 7,
                        question = ScreenshotData.questionActiveState.question,
                        playerAnswer = PlayerAnswer(
                            answer = "sad",
                            isCorrect = true,
                            responseTimeMs = 1245L,
                            pointsEarned = 342
                        ),
                        botAnswer = PlayerAnswer(
                            answer = "joyful",
                            isCorrect = false,
                            responseTimeMs = 2100L,
                            pointsEarned = 0
                        ),
                        playerTotalScore = 762,
                        botTotalScore = 380
                    ),
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        capture("03-question-result.png")
    }

    @Test
    fun victory() {
        composeRule.setContent {
            Shot {
                ResultsScreen(
                    result = ScreenshotData.victoryResult,
                    difficulty = BotDifficulty.HARD,
                    rewardedAdReady = false,
                    onPlayAgain = {},
                    onGoHome = {}
                )
            }
        }
        capture("04-results.png")
    }

    @Test
    fun leaderboard() {
        composeRule.setContent {
            Shot {
                LeaderboardScreen(
                    entries = ScreenshotData.leaderboardEntries,
                    onBackClick = {}
                )
            }
        }
        capture("05-leaderboard.png")
    }

    @Test
    fun matchReady() {
        composeRule.setContent {
            Shot {
                GameScreen(
                    gameState = GameState.MatchReady(
                        difficulty = BotDifficulty.HARD,
                        playerName = "WordMaster"
                    ),
                    onAnswerSelected = {},
                    onStartMatch = {},
                    onExitGame = {},
                    onMatchComplete = {}
                )
            }
        }
        capture("06-match-ready.png")
    }

    private fun capture(name: String) {
        val raw = File(phoneDir(), "$name.raw.png")
        raw.parentFile?.mkdirs()
        composeRule.onRoot().captureRoboImage(raw.absolutePath)
        check(raw.isFile) { "Roborazzi did not write ${raw.absolutePath}" }
        val dest = File(phoneDir(), name)
        flattenOntoBackground(raw, dest)
        raw.delete()
        val written = decodePng(dest.readBytes())
        assertEquals(name, 1080, written.width)
        assertEquals(name, 1920, written.height)
    }

    private fun phoneDir(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        val root = if (File(cwd, "store/android/phone").isDirectory) {
            cwd
        } else {
            checkNotNull(cwd.parentFile) { "No parent for $cwd" }
        }
        return File(root, "store/android/phone")
    }

    /**
     * Play rejects screenshot alpha. Paint onto the light theme background (#FFFBFF)
     * and write a color-type-2 PNG. java.awt is not on the Android unit-test classpath.
     */
    private fun decodePng(bytes: ByteArray): Bitmap {
        return checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) {
            "Could not decode PNG (${bytes.size} bytes)"
        }
    }

    private fun flattenOntoBackground(source: File, dest: File) {
        val decoded = decodePng(source.readBytes())
        val painted = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(painted)
        canvas.drawColor(android.graphics.Color.parseColor("#FFFBFF"))
        canvas.drawBitmap(decoded, 0f, 0f, null)
        writeRgbPng(painted, dest)
        decoded.recycle()
        painted.recycle()
    }

    private fun writeRgbPng(bitmap: Bitmap, dest: File) {
        val width = bitmap.width
        val height = bitmap.height
        val raw = ByteArrayOutputStream((width * 3 + 1) * height)
        val row = IntArray(width)
        for (y in 0 until height) {
            raw.write(0)
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            for (pixel in row) {
                raw.write((pixel ushr 16) and 0xFF)
                raw.write((pixel ushr 8) and 0xFF)
                raw.write(pixel and 0xFF)
            }
        }
        val deflater = Deflater()
        deflater.setInput(raw.toByteArray())
        deflater.finish()
        val compressed = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            if (count > 0) compressed.write(buffer, 0, count)
        }
        deflater.end()

        val png = ByteArrayOutputStream()
        png.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        fun chunk(type: String, data: ByteArray) {
            val typeBytes = type.toByteArray(Charsets.US_ASCII)
            png.write(ByteBuffer.allocate(4).putInt(data.size).array())
            png.write(typeBytes)
            png.write(data)
            val crc = CRC32()
            crc.update(typeBytes)
            crc.update(data)
            png.write(ByteBuffer.allocate(4).putInt(crc.value.toInt()).array())
        }
        val ihdr = ByteBuffer.allocate(13)
        ihdr.putInt(width)
        ihdr.putInt(height)
        ihdr.put(8)
        ihdr.put(2)
        ihdr.put(0)
        ihdr.put(0)
        ihdr.put(0)
        chunk("IHDR", ihdr.array())
        chunk("IDAT", compressed.toByteArray())
        chunk("IEND", ByteArray(0))
        dest.writeBytes(png.toByteArray())
    }
}

@Composable
private fun Shot(content: @Composable () -> Unit) {
    AntonymArenaTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}
