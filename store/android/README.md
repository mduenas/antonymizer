# Android Play listing assets

There was no `store/` directory and no fastlane metadata in the repo. This folder is the Play Console source for the first Android release. It does not change the app, the version (`1.0` / versionCode `2`), or signing.

| Path | What it is |
| --- | --- |
| `en-US/title.txt` | App name, 11 characters (limit 30) |
| `en-US/short_description.txt` | Short description, 74 characters (limit 80) |
| `en-US/full_description.txt` | Full description, 1686 characters (limit 4000) |
| `en-US/listing-fields.md` | Category, tags, and contact fields |
| `phone/` | Six phone screenshots, 1080×1920, 24-bit PNG |
| `graphics/icon-512.png` | 512×512 store icon |
| `graphics/feature-graphic-1024x500.png` | 1024×500 feature graphic, 24-bit PNG |
| `play-console-answers.md` | Data safety, content rating, ads, audience, and the other Console forms |
| `remove-ads-product.md` | One-time product `remove_ads` |

Open items are marked **NEEDS MARK** in those files. The index is at the top of `play-console-answers.md`.

## Screenshots

Rendered from the real Compose screens (`HomeScreen`, `GameScreen`, `ResultsScreen`, `LeaderboardScreen`) with Roborazzi 1.42 and Robolectric 4.14.1.

- Graphics mode: Robolectric `NATIVE`
- SDK under test: 34
- Qualifier: `w540dp-h960dp-xhdpi`, which is 1080×1920 (9:16)
- The test writes each capture, then flattens it onto `#FFFBFF` (the light theme background) so the file is 24-bit RGB with no alpha

The capture class is `screenshots/src/test/kotlin/com/markduenas/antonymizer/screenshots/PlayStoreScreenshotTest.kt`. It does not run during a normal test. `:screenshots:testDebugUnitTest` skips it unless you pass `-PplayStoreScreenshots=true`.

Regenerate the six PNGs in `phone/` with:

```bash
./gradlew :screenshots:recordRoborazziDebug -PplayStoreScreenshots=true --tests com.markduenas.antonymizer.screenshots.PlayStoreScreenshotTest
```

That needs an Android SDK (`local.properties` `sdk.dir` or `ANDROID_HOME`). It overwrites `store/android/phone/*.png`.

Sample player data is the same demo data the other screenshot tests use (`ScreenshotData`): an experienced Gold-rank player, the word HAPPY, a finished Hard match, and five sample leaderboard rows. Those names are fixture data so the leaderboard screen is not empty. The leaderboard in the app is stored on the device. It is not an online ranking.

Settings is not included. That screen shows Sound Effects and Vibration switches, and there is no playback or haptic code behind them.

| File | Screen |
| --- | --- |
| `phone/01-home.png` | Home, Gold rank, Challenger Bot selected |
| `phone/02-question.png` | Question 7 of 10, hint available |
| `phone/03-question-result.png` | Correct-answer result |
| `phone/04-results.png` | Match results, victory |
| `phone/05-leaderboard.png` | Leaderboard with sample rows |
| `phone/06-match-ready.png` | Match ready, versus Expert Bot |

Checked-in Roborazzi baselines under `screenshots/screenshots/` are 1078×2399, which is taller than Play's 2:1 limit. These six files are the Play-sized captures.

## Icon and feature graphic

`graphics/icon-512.png` is the existing launcher artwork. The xxxhdpi launcher PNG is 192×192 of the same image. The 512 was scaled down from the 1024 master (`antonyms_icon.png`, same file as the iOS app icon) so it is not an upscale of the 192. Corners outside the gold ring are transparent.

`graphics/feature-graphic-1024x500.png` places that icon on a purple field taken from the app theme (`#1A1038` to `#462E8C`), with the name and the line "Race a bot to the antonym." It is RGB, with no alpha. The opponent is the scripted bot in `BotBehavior`, not a learned model, so the graphic does not say "AI".
