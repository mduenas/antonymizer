# Play Console answers — Antonymizer

Android package `com.markduenas.antonymizer`. Version **1.0** (`versionName`) / **versionCode 2**, from `version.properties`. This file does not change those values.

These are draft answers for the first Play Console setup. They were taken from the app manifest, the Gradle version catalog, and the SDK manifests those dependencies merge in. Anything that is not settled is marked **NEEDS MARK**.

Store listing text: `en-US/`. Remove Ads product: `remove-ads-product.md`.

## NEEDS MARK index

1. Store listing email.
2. Store listing phone (optional).
3. Website URL. Suggestion only: `https://www.markduenas.com/projects/antonymizer/`
4. Privacy policy URL. Suggestion only: `https://www.markduenas.com/projects/antonymizer/privacy-policy/`. No policy URL exists in the repo or the app. Play requires one because the app has ads.
5. Whether a later change should add an in-app link to that policy. This task does not change app code. The Settings screen has no link today.
6. Category still named **Word**, and the five suggested tags, against the live Console lists (`en-US/listing-fields.md`).
7. Target age groups, and whether the listing "appeals to children".
8. IARC contact email.
9. IARC language and controlled-substance answers. The word list includes ordinary dictionary words such as hell, stupid, and drunk. No strong profanity was found.
10. The live content-rating question about third-party ad content, if the form still asks it.
11. Ads SDK data types. Google's published disclosure (checked 2026-11-13) describes Mobile Ads SDK **25.5.0**. This build uses **play-services-ads 24.2.0**.
12. Whether to also declare Crash logs. The current Google page lists diagnostics (launch time, hang rate, energy), not crash logs.
13. Purchase history. Declare it (recommended below) or rely on Play's FAQ exemption for data handled only by Google Play Billing.
14. Device or account identifiers "if applicable" on a device that has a Google account, even though the app has no login.
15. The single yes/no for "users can request data deletion". In-app Clear All Data is only partial, and ad/purchase data is deleted through Android or the Google account, not through a developer account.
16. `android:allowBackup="true"`. Auto Backup may copy the local DataStore file to the user's Google Drive. Developers cannot read that backup. Usual answer is that this is not "collection". Confirm before submit.
17. Remove Ads price. Suggested USD 2.99, which is the string the app shows when Play returns no price.
18. Remove Ads country availability. Leave the EEA, the UK, and Switzerland out until DUE-84 ships. See Countries below.
19. Whether the Play product description should add that rewarded ads can still play. The in-app card does not say that.
20. Internal-testing note for review: the game itself needs no login. Buying Remove Ads needs a licensed tester on a Play-installed build.
21. Store name. The Play listing here is **Antonymizer** (launcher label, versionName 1.0). The home screen title is **ANTONYM ARENA**, and Settings → About says **Antonym Arena**, version **1.0.0**. Which name should the store and the app use?

## App access

**All functionality is available without special access.**

There is no login, account, password, or server. A reviewer can open the app and play. The display name defaults to "Player" and can be edited in Settings (stored only on the device, 20 characters max).

NEEDS MARK (item 20): the Remove Ads purchase can be completed only by a licensed tester on a build installed from Play. The game is not locked behind that.

## Ads

**Contains ads: Yes.**

Formats implemented in `AdManagerAndroid`:

- Interstitial, shown from `App.kt` when a match ends, and only if Remove Ads has not been purchased, three minutes have passed since the last interstitial, and an ad is loaded.
- Rewarded, used for hints (at most two per match; a granted hint removes two wrong answers). The results screen can also offer a rewarded ad when one is loaded.

No banner, native, or app-open ad is implemented. Debug builds use Google's sample test ad units. Release builds use the production units in `AdManagerAndroid`.

The app manifest sets the AdMob app id meta-data `com.google.android.gms.ads.APPLICATION_ID`. It does not show a consent form. `user-messaging-platform` 3.1.0 is pulled in by the ads SDK, and the app never calls it. See Countries for what that means for the EEA, the UK, and Switzerland (DUE-84).

## Advertising ID

**Yes, the app uses the advertising ID.**

The app source manifest does not declare `com.google.android.gms.permission.AD_ID` and does not remove it. These libraries merge it in:

- `com.google.android.gms:play-services-ads-api:24.2.0` (dependency of `play-services-ads:24.2.0`)
- `com.google.android.gms:play-services-ads-identifier:18.0.0`
- `com.google.android.gms:play-services-measurement-sdk-api:20.1.2` (transitive from ads-api)

Purpose to select: **Advertising or marketing**. App code does not read the advertising ID itself. The Mobile Ads SDK does, for ads. Google's disclosure also uses those identifiers for analytics and fraud prevention; those purposes belong on the Data safety form, below.

The same ads-api manifest also merges `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, and `ACCESS_ADSERVICES_TOPICS`.

## News

**No.** This is not a news app. There is no news feed.

## Government

**No.** This is not a government app.

## Financial features

**No.**

The only payment is the one-time Remove Ads product through Google Play Billing. That is an in-app product, not a financial feature. The app does not offer banking, loans, credit, crypto, trading, or cash rewards. Card data stays in Google Play. The app never reads it.

## Health

**No.** No health, medical, or fitness features. No health permissions.

## Target audience and content

NEEDS MARK (item 7). Suggested answers, based on the app as it is built:

- Target age groups: **13–15, 16–17, and 18 and over.**
- Do **not** select under 13 unless you intend to meet the Families policy.
- The app is **not designed primarily for children**. There is no age gate, no `setTagForChildDirectedTreatment`, and no child-directed ad configuration.
- It **could still appeal to children**: it is a short vocabulary game, the icon is bright, and nothing stops a younger player from installing it.

If any "under 13" box is checked, the current AdMob setup is not ready to ship. Ads would need a child-directed treatment, and some ad formats and identifiers are restricted.

Suggested answer for "unintentionally appeal to children" on the store listing: the listing text is written for a general word-game audience and does not use child-directed language. The icon is the existing launcher art, which is colorful. Confirm the appeal question in the form rather than guessing it.

## Content rating (IARC)

Category: **Game**.

NEEDS MARK (item 8): the email address IARC uses for the rating certificate.

Suggested answers from the content that is actually in the app:

| Topic | Suggested answer | Why |
| --- | --- | --- |
| Violence | None | No violent scenes, weapons, or blood. UI is a word quiz. |
| Fear / horror | None | |
| Sexuality / nudity | None | |
| Language / profanity | None in the UI | See the word-list note below. NEEDS MARK (item 9). |
| Controlled substances | None depicted | The vocabulary list includes the word "drunk". NEEDS MARK (item 9). |
| Gambling | None | No wagering, loot boxes, or random paid rewards. Score is points only. |
| User interaction | Users cannot communicate | No chat, no public profiles, no sharing a name with other players. The leaderboard is on-device. |
| Location sharing with other users | No | No location permission. |
| Digital purchases | **Yes** | One-time product `remove_ads`. |
| Unrestricted web or user content | No browser, no user publishing | Ads can open an advertiser page. NEEDS MARK (item 10) if the form asks whether ads are outside your control. Answer yes to that specific question. |

Word list: `antonym_pairs.json` has **929** pairs (80 easy, 531 medium, 318 hard), not the "3000+" figure in older project notes. A scan of all 1,408 unique word strings found no slurs and no strong profanity. It did find ordinary dictionary items: hell, hate, dead, drunk, war, slavery, stupid (and unrelated words that merely contain those letters, such as assiduous). They are quiz answers, not insults shown to the player. Suggested IARC language answer is still "no profanity", with item 9 left for you to confirm.

## Data safety

### Does the app collect or share any of the required user data types?

**Yes.** Collection and sharing come from the Google SDKs below. The app's own gameplay data stays on the device.

### Is all of that data encrypted in transit?

**Yes.**

- Google's Mobile Ads disclosure says the SDK's user data is encrypted in transit with TLS. That page was last updated 2026-11-13 and describes SDK 25.5.0. This project depends on 24.2.0. NEEDS MARK (item 11) to re-read the page if the SDK is upgraded, and to accept the draft for 24.2.0.
- Play Billing talks only to Google Play, over Play's billing connection. The app has no HTTP client of its own (no Firebase, no analytics SDK added by this project, no backend URL in the source).

No packet capture was done. The "Yes" is from Google's published SDK statement plus the absence of any other network stack in this repo.

### What the app itself stores

`SettingsStorage` writes a DataStore file on the device (`antonymizer_prefs.preferences_pb` under the app files dir). Keys: player name, sound flag, music flag, vibration flag, difficulty, player stats JSON, leaderboard JSON, games played, `ads_removed`, hint tokens, last interstitial time, total ads watched.

None of that is sent to a developer server. Play's definition of collection is data transmitted **off** the device. Local-only data is not declared.

Two caveats:

- `android:allowBackup="true"` in the app manifest. Android Auto Backup can copy that file to the user's Google Drive. You cannot read that backup. Standard Data safety guidance says not to declare end-to-end encrypted backup you cannot access. NEEDS MARK (item 16).
- Settings → Clear All Data does **not** wipe every key. See Deletion below.

Sound and vibration switches are stored. No audio or haptic playback code was found, so the store listing does not advertise sound or vibration.

There is a `music_enabled` preference and no music control in Settings. Not advertised.

### SDK versions in this build

From `gradle/libs.versions.toml`:

- `com.google.android.gms:play-services-ads:24.2.0`
- `com.android.billingclient:billing-ktx:8.0.0` (pulls `billing:8.0.0`)

The app manifest (`composeApp/src/androidMain/AndroidManifest.xml`) declares **no** `<uses-permission>` elements. Permissions below are merged from the libraries. They were read from those AARs' manifests, not from a guess.

### Google Mobile Ads SDK — collects and shares

Source: [Google Play data disclosure for the Google Mobile Ads SDK (Legacy)](https://developers.google.com/admob/android/privacy/play-data-disclosure), which says it applies to SDK 25.5.0. NEEDS MARK (item 11) because this app is on 24.2.0. Until that is re-checked, declare what Google lists as collected **and shared** automatically, for **advertising, analytics, and fraud prevention, security, and compliance**:

| Play data type | What Google says the SDK collects | Collected | Shared | Ephemeral | Optional for the user? |
| --- | --- | --- | --- | --- | --- |
| Location → Approximate location | IP address, which may be used to estimate general location | Yes | Yes | No | No. The app does not ask for location and has no location permission. The SDK still collects IP. There is no in-app opt-out. |
| App activity → App interactions | App launch, taps, and video views (ad interaction) | Yes | Yes | No | No |
| App info and performance → Diagnostics | Launch time, hang rate, energy usage | Yes | Yes | No | No |
| Device or other IDs | Android advertising ID, app set ID, and, if applicable, identifiers tied to accounts signed in on the device | Yes | Yes | No | No. The user can reset the ad id in Android settings, but the app does not offer a switch. NEEDS MARK (item 14) on the "signed-in account identifiers" phrase: this app has no account, and the SDK may still see a Google account on the device. |

Do **not** declare precise location, name, email, phone, photos, contacts, messages, or health. The app does not request those, and the ads disclosure does not list them.

Do **not** declare Crash logs unless you decide item 12 the other way. The current Google page does not list crash logs.

Shared with: Google, as part of AdMob. The app has no code that forwards this data to any other party. Do not name extra ad partners; none are configured in the repo.

Purposes, both for collection and for sharing, matching Google's page: Advertising or marketing; Analytics; Fraud prevention, security, and compliance.

Required vs optional: collection is **required** for these four types. The SDK starts from `AdManagerAndroid.initialize()`, which the app runs without a consent dialog. Buying Remove Ads skips interstitials only. It does not stop SDK initialization, and rewarded ads can still load.

### Play Billing Library — purchase history

`BillingManagerAndroid` uses Play Billing 8.0.0:

- queries one-time product `remove_ads`
- receives `Purchase` objects (product id, purchase token, purchase state, acknowledged flag)
- sends the purchase token back to Google Play in `acknowledgePurchase`
- stores only the local boolean `ads_removed`

It does not call `setObfuscatedAccountId` or `setObfuscatedProfileId`. It does not read the advertising ID or Android ID. It does not send the token to a developer server. The billing AAR's manifest adds only `com.android.vending.BILLING` (plus a package query for the Play billing service).

**User payment info** (card number and similar): **do not declare.** The app never receives it. Google Play collects it as the payment processor.

**Purchase history:** recommended declaration:

| | |
| --- | --- |
| Data type | Financial info → Purchase history |
| Collected | Yes. The library returns the purchase to the app, and acknowledgement sends the purchase token to Google Play. |
| Shared | Yes, with Google Play only. Not with any other third party. |
| Purposes | App functionality (turn on the Remove Ads entitlement). Fraud prevention, security, and compliance (acknowledgement and Play's purchase check). |
| Ephemeral | No. `ads_removed` is kept on device, and Play keeps the purchase on the Google account. |
| Optional | No. The user can choose not to buy, but a completed purchase is processed this way. There is no separate "don't record this purchase" control. |

NEEDS MARK (item 13): Play's Data safety help says you do not have to declare data that Google Play Billing processes when you do not collect it yourself. This app does receive the purchase token on the device and sends it back to acknowledge. The conservative answer is to declare purchase history as above. If you rely on the exemption instead, say so explicitly in the form and do not also declare it.

The brief for this task expected Play Billing to collect device or advertising IDs as well as purchase history. In this code, advertising IDs come from the ads SDK, documented above. The billing classes do not read them. Do not add a second Device-ID row for Billing unless a current Play Billing disclosure says the library transmits one on its own.

### Permissions merged in, for the form's "check the manifest" pass

The app source manifest declares no `<uses-permission>`. It sets `android:allowBackup="true"` and the AdMob app id meta-data.

The debug merged manifest (`:composeApp:processDebugMainManifest`, versionCode 2, versionName 1.0) contains exactly these `uses-permission` entries:

- `com.android.vending.BILLING` — Play Billing
- `android.permission.INTERNET` — ads SDK
- `android.permission.ACCESS_NETWORK_STATE` — ads SDK
- `com.google.android.gms.permission.AD_ID` — ads SDK
- `android.permission.ACCESS_ADSERVICES_AD_ID` — ads SDK
- `android.permission.ACCESS_ADSERVICES_ATTRIBUTION` — ads SDK
- `android.permission.ACCESS_ADSERVICES_TOPICS` — ads SDK
- `android.permission.WAKE_LOCK` — `play-services-measurement-sdk-api`, pulled in by the ads SDK
- `android.permission.FOREGROUND_SERVICE` — AndroidX WorkManager (`work-runtime`), a dependency of `play-services-ads-api`. The app does not start a foreground service of its own.
- `com.markduenas.antonymizer.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — signature permission AndroidX adds for its own broadcast receivers. Not a user-data permission.

`RECEIVE_BOOT_COMPLETED` is not in the merged manifest. The ads-api manifest marks it `tools:node="remove"`.

Library manifests behind that merge:

- `play-services-ads-api:24.2.0`: INTERNET, ACCESS_NETWORK_STATE, AD_ID, the three ACCESS_ADSERVICES_* permissions, and the boot-completed removal
- `play-services-measurement-sdk-api:20.1.2`: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, AD_ID
- `play-services-ads-identifier:18.0.0`: AD_ID
- `user-messaging-platform:3.1.0`: no permissions
- `billing` / `billing-ktx` 8.0.0: `com.android.vending.BILLING`

No location, camera, microphone, contacts, storage, or photo permission is in the merged manifest. FOREGROUND_SERVICE and WAKE_LOCK are not data types on the Data safety form.

### Deletion

NEEDS MARK (item 15) for the yes/no Play asks. Facts to use:

- There is no user account, so there is no account-deletion URL and no server copy of a profile.
- Settings → Clear All Data (`AppViewModel.clearAllData`) resets stats, clears the local leaderboard, sets the name back to "Player", and turns the sound and vibration flags on. It does **not** clear `ads_removed`, hint tokens, last interstitial time, total ads watched, difficulty, or the unused music flag.
- Advertising ID: Google's disclosure says the user can reset or delete it in Android settings. The app cannot delete Google's copy.
- Purchase history: it lives on the Google account. The developer has no server record to erase. Uninstalling the app does not refund or delete the Play purchase. Reinstalling and Restore Purchases reads it again.

A defensible form answer is: **Yes** for data you count as collected by the developer only where an in-app or platform control exists, and name the mechanism honestly (in-app clear for local gameplay data; Android ad-id settings; Google account for purchases). If the form's "Yes" requires that every declared type be deletable by a request to you, answer **No** and keep the notes above, because you have no deletion endpoint for AdMob or Play purchase records.

## Store name

NEEDS MARK (item 21). This folder does not change the listing name, the app, or the version.

| Surface | What it says today |
| --- | --- |
| Play app name in `en-US/title.txt` | Antonymizer |
| Launcher label (`strings.xml` `app_name`) | Antonymizer |
| `version.properties` | versionName 1.0, versionCode 2 |
| Home screen title | ANTONYM ARENA |
| Settings → About | Antonym Arena, hardcoded "Version 1.0.0" |

Mark: which name should be on the Play store, and should the in-app title and the About version string be aligned with it later? Until that is decided, paste **Antonymizer** as the store name. It matches the name on the device.

## Countries

NEEDS MARK (item 18) for every country that is not in the exclusion below.

Android has no UMP consent flow yet (**DUE-84**). The ads SDK is initialized with no consent form. `user-messaging-platform` is only a transitive dependency of `play-services-ads-api`, and the app never calls it. Ad data collection is therefore not optional for the user. See Data safety.

Leave these out of the production rollout, and off the `remove_ads` country list, until DUE-84 ships:

- European Economic Area
- United Kingdom
- Switzerland

## Privacy policy

**No privacy policy URL is referenced anywhere in the app or the repo.**

Checked: source (`privacy`, `http`, `markduenas.com`), `AndroidManifest.xml`, the Settings About card, and `README.md`. Settings shows the name "Antonym Arena", a hardcoded "Version 1.0.0", and a one-sentence blurb. No link.

Play requires a privacy policy URL for apps that contain ads, and for the Data safety section.

NEEDS MARK (items 4 and 5):

- Suggested URL, only if you host the page there: `https://www.markduenas.com/projects/antonymizer/privacy-policy/`
- Suggested website, same condition: `https://www.markduenas.com/projects/antonymizer/`
- Do not enter either URL in Play until the page is actually live.
- The binary does not link to a policy. Play's ads policy is easier to satisfy when the app does. That would be an app change, and it is out of scope here.

When the page is written, it needs to cover at least:

- Google Mobile Ads (AdMob): advertising ID, app-set id, IP address used for approximate location, ad interactions, diagnostics; shared with Google for advertising, analytics, and fraud prevention; TLS; ad id can be reset in Android settings.
- Google Play Billing: purchase of `remove_ads`; purchase token handled with Google Play; no card data in the app; purchase history kept by Google.
- Data stored only on the device: display name, stats, local leaderboard, settings, and the local copy of the Remove Ads flag.
- No account, no developer server, no sale of data by the developer.
- Children: the app is not directed at children under 13 (confirm this matches the target-audience answer).
- How to reach you: the same email used on the store listing (NEEDS MARK, item 1).
- Clear All Data, including what it does not erase.

## Other declarations that are no

| Form | Answer |
| --- | --- |
| News app | No |
| Government app | No |
| Financial features | No |
| Health | No |
| Login / restricted access | No |
