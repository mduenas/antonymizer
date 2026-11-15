# Android Play listing assets

There was no `store/` directory and no fastlane metadata in the repo. This folder is the Play Console source for the first Android release. It does not change the app, the version (`1.0` / versionCode `2`), or signing.

| Path | What it is |
| --- | --- |
| `en-US/title.txt` | App name, 11 characters (limit 30) |
| `en-US/short_description.txt` | Short description, 78 characters (limit 80) |
| `en-US/full_description.txt` | Full description, 1681 characters (limit 4000) |
| `en-US/listing-fields.md` | Category, tags, and contact fields |
| `phone/` | Six phone screenshots, 1080×1920, 24-bit PNG |
| `graphics/icon-512.png` | 512×512 store icon |
| `graphics/feature-graphic-1024x500.png` | 1024×500 feature graphic, 24-bit PNG |
| `play-console-answers.md` | Data safety, content rating, ads, audience, and the other Console forms |
| `remove-ads-product.md` | One-time product `remove_ads` |

Open items are marked **NEEDS MARK** in those files. The index is at the top of `play-console-answers.md`.

## Screenshots

Rendered from the real Compose screens (`HomeScreen`, `GameScreen`, `ResultsScreen`, `LeaderboardScreen`, `SettingsScreen`) with Roborazzi 1.42 and Robolectric 4.14.1.

- Graphics mode: Robolectric `NATIVE`
- SDK under test: 34
- Qualifier: `w540dp-h960dp-xhdpi`, which is 1080×1920 (9:16)
- Captured with `captureRoboImage` on the compose root, then flattened onto `#FFFBFF` (the light theme background) so the files are 24-bit RGB with no alpha channel

The capture test lived only in the local `:screenshots` run. It is not committed. Sample player data is the same demo data the screenshot tests already use (`ScreenshotData`): an experienced Gold-rank player, the word HAPPY, a finished Hard match, and five sample leaderboard rows. Those names are fixture data so the leaderboard screen is not empty. The leaderboard in the app is stored on the device. It is not an online ranking.

| File | Screen |
| --- | --- |
| `phone/01-home.png` | Home, Gold rank, Challenger Bot selected |
| `phone/02-question.png` | Question 7 of 10, hint available |
| `phone/03-question-result.png` | Correct-answer result |
| `phone/04-results.png` | Match results, victory |
| `phone/05-leaderboard.png` | Leaderboard with sample rows |
| `phone/06-settings.png` | Settings, including Remove Ads at the $2.99 fallback |

Checked-in Roborazzi baselines under `screenshots/screenshots/` are 1078×2399, which is taller than Play's 2:1 limit, and the settings baseline predates the Remove Ads card. These six files are new captures of the current UI.

## Icon and feature graphic

`graphics/icon-512.png` is the existing launcher artwork. The xxxhdpi launcher PNG is 192×192 of the same image. The 512 was scaled down from the 1024 master (`antonyms_icon.png`, same file as the iOS app icon) so it is not an upscale of the 192. Corners outside the gold ring are transparent.

`graphics/feature-graphic-1024x500.png` places that icon on a purple field taken from the app theme (`#1A1038` to `#462E8C`), with the name and the short-description line. It is RGB, with no alpha.
