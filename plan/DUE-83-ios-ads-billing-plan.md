# DUE-83 plan: iOS AdMob + StoreKit 2 remove-ads

Planning only. No app code in this change. Reviewer: Detrich. Implementation is a later PR.

Linear: [DUE-83](https://linear.app/duenas-tracking/issue/DUE-83/antonymizer-implement-ios-ads-and-remove-ads-purchase-for-app-store)

iOS marketing version is **1.0**, build **1** (`iosApp/Configuration/Config.xcconfig`: `MARKETING_VERSION=1.0`, `CURRENT_PROJECT_VERSION=1`). Leave both numbers alone. Android `version.properties` is `versionName=1.0` / `versionCode=2`. Those are independent. Do not "sync" them in the implementation PR.

## What the issue gets wrong

DUE-83 says both iOS managers are stubs. Billing is a stub. Ads are not.

| Piece | Android | iOS today |
| --- | --- | --- |
| Ad SDK | `play-services-ads` 24.2.0, initialized in `AdManagerAndroid` | CocoaPods `Google-Mobile-Ads-SDK` ~> 11.2, locked at **11.13.0**. `IosAdHelper` loads and presents ads. |
| Formats | Interstitial + rewarded. No banner. | Same two formats in `AdHelper.swift`. No banner. |
| Consent | None (no UMP). Test-device list contains only the emulator. | `NSUserTrackingUsageDescription` is set. `iOSApp.swift` calls `ATTrackingManager` on become-active. No UMP form. Ads start in `iOSApp.init()` **before** ATT returns. |
| Remove-ads IAP | Play Billing, product id `remove_ads`, non-consumable (`INAPP`) | `BillingManagerIos` never talks to StoreKit. `purchaseRemoveAds()` sets `PurchaseState.Unavailable`. |
| Ad-free flag | `SettingsStorage.adsRemoved` (`ads_removed` in DataStore) | Same DataStore key. UI reads it. Nothing on iOS writes `true` after a purchase, because there is no purchase. |

The implementation finishes the iOS ad path (consent, SKAdNetwork, privacy manifest, cooldown parity) and adds StoreKit 2. It does not re-platform ads onto SPM, and it does not add a banner.

## How the code is wired

There is no `expect`/`actual` for ads or billing. `commonMain` has two interfaces. Each platform has a class that implements them.

- `AdManager` and `BillingManager` in `composeApp/src/commonMain/kotlin/com/markduenas/antonymizer/monetization/`.
- `MonetizationState` exposes `adManager.adsRemoved` as the flag Compose collects. It does **not** read `billingManager.adsRemoved`.
- Android constructs both managers in `MainActivity` and calls `initialize()`.
- iOS constructs them in `MainViewController()` (`composeApp/src/iosMain/.../MainViewController.kt`).
- Swift implements Kotlin `NativeAdProvider`. `ContentView.swift` assigns `IosAdHelper.shared` to `NativeAdProviderHolder` before creating the Compose view controller. Full-screen ads call `present(fromRootViewController:)` on that controller.

StoreKit 2 is Swift-only. Follow the same holder pattern. Do not call StoreKit from Kotlin/Native.

Persistence is one DataStore file, not NSUserDefaults. `SettingsStorage.ios.kt` writes `antonymizer_prefs.preferences_pb` under the Documents directory. `AGENTS.md` still says NSUserDefaults. That comment is stale. Do not move this flag to UserDefaults as part of this work.

`App.kt` is the only UI caller:

- Match end calls `showInterstitialIfEligible`, then navigates to Results.
- Hint (`GameScreen`, max 2 per match), continue, and +25% bonus (`ResultsScreen`) call `showRewardedAd`.
- Settings calls `purchaseRemoveAds()` and `restorePurchases()`.

`clearAllData()` does not clear `ads_removed`. Keep it that way. Wiping stats must not wipe a purchase. StoreKit remains the source of truth on the next launch anyway.

## Ad formats (match Android, no banner)

Ship the formats Android already ships.

| Format | When | After remove-ads |
| --- | --- | --- |
| Interstitial | Once, after a match, before Results. 3-minute cooldown. | Hidden. |
| Rewarded | Hint (2 per match), results continue, results bonus. | Still offered. `AdManager` says rewarded stays available after remove-ads. Settings copy already says the purchase "Removes interstitial ads between matches." |

No `AdView` / `GADBannerView` exists on either platform. Do not add one. A banner needs a native view inside Compose, and it fights the "clean, simple interface" goal. Full-screen ads are presented by UIKit from the root view controller. Compose does not host them.

If a banner is ever required later, the view has to be created in Swift (the Google pod is not a Kotlin/Native cinterop) and embedded with Compose Multiplatform `UIKitView` (`androidx.compose.ui.viewinterop.UIKitView` in Compose 1.10). Hide that view when `adsRemoved` is true. That is a non-goal for DUE-83.

### Bug to fix while touching iOS ads

`AdManagerAndroid.showInterstitialIfEligible` checks ads-removed, the 3-minute cooldown, and that an ad is loaded. `AdManagerIos` only checks ads-removed. `AdHelper` stores `lastInterstitialTime` and never reads it. The Swift comment says Kotlin owns the cooldown. Kotlin does not.

Worse: the iOS completion path always calls `recordInterstitialShown()` and `incrementAdsWatched()`, including when Swift invokes the callback because nothing was on screen.

Fix in the implementation:

1. Add a pure function in `commonMain` (name it `InterstitialPolicy.shouldShow`) with the Android conditions: ads removed, ad not loaded, or `now - lastShown < INTERSTITIAL_COOLDOWN_MS` (180_000) all return false.
2. Call it from both `AdManagerAndroid` and `AdManagerIos`. Android behavior stays the same. iOS gains the cooldown.
3. Change `NativeAdProvider.showInterstitial` so the callback reports whether an ad was actually presented. Record the timestamp and the watched count only when that flag is true.
4. Delete the unused Swift cooldown fields.

## Google Mobile Ads on iOS

### Dependency: stay on CocoaPods

The Xcode project is a CocoaPods app, not SPM.

- `iosApp/Podfile` targets `iosApp`, `use_frameworks!`, `pod 'Google-Mobile-Ads-SDK', '~> 11.2'`.
- `Podfile.lock` resolves **11.13.0**, which already pulls **GoogleUserMessagingPlatform 3.0.0**. UMP does not need a new pod.
- `iosApp/iosApp.xcworkspace` includes `Pods/Pods.xcodeproj`.
- `project.pbxproj` links `Pods_iosApp.framework` and runs `[CP] Check Pods Manifest.lock`. Open the **workspace**, not the `xcodeproj`.
- `Pods/` is gitignored. A machine that has not run `pod install` cannot build iOS.
- The `iosApp` group is a `PBXFileSystemSynchronizedRootGroup` (Xcode 16). A new Swift file or `PrivacyInfo.xcprivacy` dropped in `iosApp/iosApp/` is compiled or copied without a pbxproj edit. `Info.plist` is a membership exception, which is correct.

Do not migrate to Swift Package Manager (`https://github.com/googleads/swift-package-manager-google-mobile-ads`). SPM and `use_frameworks!` CocoaPods in the same static Compose framework is a linker risk, and the pod is already integrated. Do not bump the pod to Google Mobile Ads 12 in this work. Stay on the 11.x `GAD*` types `AdHelper.swift` already imports (`GADMobileAds`, `GADInterstitialAd`, `GADRewardedAd`, `GADRequest`, `GADFullScreenContentDelegate`).

Podfile deployment target is iOS 15.0. The Xcode project `IPHONEOS_DEPLOYMENT_TARGET` is **18.2**. The app target is 18.2. StoreKit 2 needs 15+, so 18.2 is fine. Do not lower the Xcode target to match the Podfile.

Kotlin side: `composeApp/build.gradle.kts` builds a **static** framework `ComposeApp` for `iosArm64` and `iosSimulatorArm64`. The Xcode "Compile Kotlin Framework" phase runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`. Swift already `import ComposeApp` and implements a Kotlin interface. New billing types in `iosMain` export the same way. Kotlin objects show up in Swift as `.shared`.

### Info.plist (already partly done)

`iosApp/iosApp/Info.plist` already has:

- `GADApplicationIdentifier` = `ca-app-pub-7540731406850248~1440731565`
- `NSUserTrackingUsageDescription` (wording is too vague; replace it, see Consent)
- `SKAdNetworkItems` with **three** identifiers: `cstr6suwn9.skadnetwork` (Google), `v9wttpbfk9.skadnetwork`, `n38lu8286q.skadnetwork`

Keep the production app id. Google's sample app id (`ca-app-pub-3940256099942544~1458002511`) is unnecessary because debug builds already swap **ad unit** ids, and `Info.plist` is not per-configuration. Test ad units work with the real app id.

Replace the three-item `SKAdNetworkItems` array with the full list published on [Set up the Google Mobile Ads SDK](https://developers.google.com/admob/ios/quick-start) at implementation time. Paste Google's snippet. Do not hand-maintain a subset. There is no mediation (no adapter pods), but Google's list still includes buyers who bid on AdMob inventory. A short list lowers fill. It is not, by itself, an App Review rejection.

`GENERATE_INFOPLIST_FILE = YES` merges build settings into this file. Keys we add to `Info.plist` are kept. Do not also duplicate `GADApplicationIdentifier` as a build setting.

### Initialization order

Today two things start the SDK, both too early:

1. `iOSApp.init()` calls `IosAdHelper.shared.initialize()`, which calls `GADMobileAds.sharedInstance().start` and loads ads.
2. `MainViewController()` calls `adManager.initialize()`, which calls the same Swift `initialize()` again.

ATT runs later, from `didBecomeActive`, and its result is only printed.

New order, one start:

1. Remove the `GADMobileAds.start` call from `iOSApp.init`. Remove the manual `ATTrackingManager` call from `iOSApp` (UMP owns the prompt; see Consent).
2. `ContentView.makeUIViewController` sets `NativeAdProviderHolder` and the new billing holder, creates `MainViewController()`, then sets the root view controller **synchronously** (drop the `DispatchQueue.main.async` delay).
3. `IosAdHelper.initialize()` is idempotent. If the root view controller is not set yet, it marks a pending start and returns. `setRootViewController` runs the pending start.
4. Pending start runs UMP (below). Only if `canRequestAds` is true does it call `GADMobileAds.sharedInstance().start`, then `loadInterstitial()` and `loadRewarded()`.
5. Kotlin `loadInterstitial` / `loadRewarded` must no-op until that start has finished. Swift already no-ops a second load when an ad object exists. Add an explicit "SDK not started" guard so a Kotlin call before consent cannot load.

### Ad unit ids (found in the repo)

Same publisher id `ca-app-pub-7540731406850248` as Android. iOS app id and ad units are different from Android, which is correct (AdMob apps are per platform).

Debug is `#if DEBUG`. The Xcode Debug configuration sets `SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG`. Release, TestFlight, and the App Store do not, so they use the production units.

| | Interstitial | Rewarded |
| --- | --- | --- |
| iOS debug (Google samples, already in `AdHelper.swift`) | `ca-app-pub-3940256099942544/4411468910` | `ca-app-pub-3940256099942544/1712485313` |
| iOS release (already in `AdHelper.swift`) | `ca-app-pub-7540731406850248/7667964658` | `ca-app-pub-7540731406850248/6932731399` |
| Android debug (for comparison, do not copy onto iOS) | `ca-app-pub-3940256099942544/1033173712` | `ca-app-pub-3940256099942544/5224354917` |
| Android release | `ca-app-pub-7540731406850248/9172618012` | `ca-app-pub-7540731406850248/5811221418` |

iOS app id: `ca-app-pub-7540731406850248~1440731565` (`Info.plist`).

Android app id: `ca-app-pub-7540731406850248~4453073567` (`AndroidManifest.xml`).

Google's iOS banner sample `ca-app-pub-3940256099942544/2934735716` is unused. Leave it unused.

These strings are in source. Whether the three iOS ids are live in the AdMob UI is **NEEDS MARK** (question 1).

TestFlight serves the release units. Clicking them on a personal device counts as invalid traffic. After the first production-unit load, the Xcode console prints a test-device hash. `IosAdHelper` should set `GADMobileAds.sharedInstance().requestConfiguration.testDeviceIdentifiers` from that hash in a list that is empty until Mark pastes one. Do not invent a hash. Local Debug does not need it, because the sample units are already test ads.

## Consent: UMP, and ATT through UMP

Do both. Serving only non-personalized ads is not a way around this.

- EEA, UK, and Switzerland still need a GDPR consent decision before ad requests, including non-personalized ones. UMP is the SDK Google ships for that. It is already in the pod lockfile.
- The app already has a tracking usage string and an ATT request. The product direction is personalized ads when the user allows tracking.
- If the user denies ATT, keep requesting ads with the normal `GADRequest`. The SDK omits the IDFA. That is the non-personalized path for people who opt out. It is not the only path.

Implementation:

1. On the pending start in `IosAdHelper` (or a small `ConsentCoordinator.swift` next to it), call `UMPConsentInformation.sharedInstance.requestConsentInfoUpdate` with the root view controller available for the next step. UMP 3.0.0 is the module that will actually compile. Google's newer docs say `ConsentInformation` / `ConsentForm`. Use whichever names this pod exports. Do not upgrade UMP independently of the ads SDK.
2. Then `UMPConsentForm.loadAndPresentIfRequired(from:)`. If no form is required, the callback runs immediately.
3. Read `canRequestAds`. Start Mobile Ads only when it is true. `canRequestAds` stays false until `requestConsentInfoUpdate` has been called at least once, so this update runs on every cold start. It is a network check, not a modal, when consent is already stored.
4. ATT: create the IDFA message in AdMob Privacy & messaging so UMP presents the explainer and then the system prompt. Delete the hand-rolled `requestTrackingAuthorization` in `iOSApp.swift` so users do not get two prompts. Google's rule: if UMP presents ATT, the app must not also call `ATTrackingManager` itself.
5. Fallback, once, only if after the UMP callback `ATTrackingManager.trackingAuthorizationStatus` is still `.notDetermined` and `canRequestAds` is true: call `requestTrackingAuthorization`, and start Mobile Ads in **that** completion, not before. This covers the gap before Mark publishes the IDFA message. Remove the fallback only after the message is confirmed live. Do not call it on every become-active.
6. Privacy options entry point. When `privacyOptionsRequirementStatus == .required`, Settings shows one text button, "Ad privacy choices", in the existing Premium section. It calls `UMPConsentForm.presentPrivacyOptionsForm`. When the status is not required, the button is absent. No new screen.

Expose that flag through `AdManager` so the shared Settings UI can see it:

- `val privacyOptionsRequired: StateFlow<Boolean>`
- `fun presentPrivacyOptions()`

Android returns a flow that stays `false` and a no-op present. iOS updates the flow from Swift after the consent update. Add the two members to `MonetizationState` and pass them from `App.kt` into `SettingsScreen`.

Debug consent: do **not** force `UMPDebugSettings.geography = .EEA` on every debug launch, or local ad testing always stops on a form. Put the geography override behind a clearly named compile flag defaulting to off. Document flipping it for the consent test in the test plan. The UMP test-device hash is the one the SDK prints. **NEEDS MARK** (question 5).

Replace `NSUserTrackingUsageDescription` with a specific line, closer to Google's sample:

> This identifier will be used to deliver personalized ads to you.

The current sentence ("personalized ads and improve your experience") is the kind of vague purpose string App Review rejects.

Publish the GDPR message and the IDFA message in AdMob before TestFlight for real EU coverage. **NEEDS MARK** (question 2). Code can land first. Until the GDPR message exists, UMP may treat consent as not required, which is not acceptable for an App Store build aimed at EU users.

## StoreKit 2

### Product id

Reuse `BillingManager.PRODUCT_REMOVE_ADS`, which is already `"remove_ads"`.

Play Console and App Store Connect product ids are independent, but one shared constant means Swift, Kotlin, the StoreKit file, and Play all say the same string. Apple will accept a non-reverse-DNS id. The id cannot be changed after it is created.

Type: **non-consumable**. Display name "Remove Ads". Description matches the Settings line: removes interstitial ads between matches. Rewarded hints stay available. Price: Android falls back to displaying `$2.99` when Play has not returned a price. Use that as the App Store price Mark creates. The app must show StoreKit's `displayPrice`, not the fallback, once the product has loaded.

**NEEDS MARK** (question 3): confirm there is no existing App Store product under a different id, and that `remove_ads` at $2.99 is the one to create.

### Swift side

New `iosApp/iosApp/StoreKitBilling.swift`. `@MainActor` singleton `IosBillingHelper: NativeBillingProvider`.

Kotlin, new file `composeApp/src/iosMain/.../monetization/NativeBillingProvider.kt`:

```kotlin
interface NativeBillingProvider {
    fun start(productId: String)
    fun purchaseRemoveAds()
    fun restorePurchases()
}

interface IosBillingListener {
    fun onPrice(price: String?)
    fun onEntitlement(adsRemoved: Boolean)
    fun onState(state: String, message: String?)
}

object IosBillingBridge {
    var listener: IosBillingListener? = null
    fun priceUpdated(price: String?) { listener?.onPrice(price) }
    fun entitlementUpdated(adsRemoved: Boolean) { listener?.onEntitlement(adsRemoved) }
    fun stateUpdated(state: String, message: String?) { listener?.onState(state, message) }
}

object NativeBillingProviderHolder {
    var provider: NativeBillingProvider? = null
}
```

State strings the mapper accepts: `ready`, `purchasing`, `pending`, `success`, `cancelled`, `error`, `unavailable`. Map them in `commonMain` (`purchaseStateFromBridge`) so common tests cover the mapping. Unknown strings become `PurchaseState.Unavailable` with the raw message. `cancelled` and `error` both become `PurchaseError`. `pending` stays `Purchasing` (Ask to Buy). Do not grant entitlement on `pending`.

`BillingManagerIos`:

- Drop the seed `stateIn(..., true)` on `adsRemoved`. Use `false`, matching Android and the DataStore default. The UI does not read this flow today (`MonetizationState` reads the ad manager's flow), but the two flows must not disagree. The comment "ads are disabled by default on iOS" is wrong for the UI and goes away.
- `initialize()` sets `IosBillingBridge.listener` to itself and calls `provider.start(BillingManager.PRODUCT_REMOVE_ADS)`. Null provider becomes `PurchaseState.Unavailable("Purchases are not available")`.
- `onPrice` writes `removeAdsPrice` and sets `Ready` only after a real `Product` loaded. Do not leave a tappable `$2.99` when the product failed to load. The Settings button is already disabled unless state is `Ready`. A disabled button may still show the last known price or `$2.99` as a label. That label must not launch a purchase.
- `onEntitlement(true/false)` calls `settingsStorage.setAdsRemoved`. That is the same write `BillingManagerAndroid.processPurchases` already does.
- `onState` assigns `purchaseState` through the mapper.
- `purchaseRemoveAds()` / `restorePurchases()` forward to the provider.
- `endConnection()` can stay empty. The Swift listener lives with the process. There is no iOS `onDestroy` hook today.

`ContentView` sets `NativeBillingProviderHolder.shared.provider = IosBillingHelper.shared` in the same place it sets the ad provider.

Swift behavior:

1. `start` stores the product id (do not hard-code a second copy), starts **one** `Transaction.updates` loop, loads `Product.products(for:)`, and reads `Transaction.currentEntitlements`.
2. `Transaction.updates`: for each update, grant only `VerificationResult.verified`. Ignore `.unverified`. If `productID` matches and `revocationDate == nil`, entitlement true. If it matches and revocation is set, entitlement false. Then `transaction.finish()`. This loop also delivers interrupted purchases and Ask to Buy approvals. Start it once. A second `start` must not start a second loop.
3. Launch entitlement: walk `Transaction.currentEntitlements` to completion, then report **once**. Owned means a verified transaction for `remove_ads` with a nil revocation date. Report `false` when the walk succeeds and nothing matches, so a refund sticks. If the walk throws, report nothing. Leave the DataStore cache alone. Do not call `AppStore.sync()` on launch. Sync can raise an Apple ID prompt. Apple’s guidance is that `currentEntitlements` is already populated on reinstall.
4. `purchaseRemoveAds`: `try await product.purchase()`. `.success` verifies, finishes, reports entitlement true and state `success`. `.userCancelled` reports `cancelled` / "Purchase cancelled" (same wording Android uses). `.pending` reports `pending` and does not grant. Missing product reports `unavailable`.
5. `restorePurchases`: call `AppStore.sync()`, then run the same entitlement walk. If the user owns it, state `success`. If the walk succeeds and they do not, state `error` with "No previous purchase found". StoreKit configuration files often make `AppStore.sync()` throw because there is no App Store account. On that failure, still run the entitlement walk. Surface the sync error only when the walk also finds nothing. Launch continues to skip `sync()`.

Enable the In-App Purchase capability on the `iosApp` target so the App ID can sell. A StoreKit configuration file does not need the capability. TestFlight does. Commit the entitlements file and the `CODE_SIGN_ENTITLEMENTS` build setting if Xcode adds them. The capability often adds no entitlement key. That is normal.

Bundle id to attach the product to: `PRODUCT_BUNDLE_IDENTIFIER` is `com.markduenas.antonymizer.Antonymizer$(TEAM_ID)` and `TEAM_ID` in `Config.xcconfig` is empty, so the id is `com.markduenas.antonymizer.Antonymizer`. Project `DEVELOPMENT_TEAM` is `A8AAE5A4T6`. Android application id is `com.markduenas.antonymizer` (no `.Antonymizer` suffix). **NEEDS MARK** (question 4).

### StoreKit configuration file

Add `iosApp/iosApp/Antonymizer.storekit` (Xcode's JSON, version 3) with one non-consumable:

- productID `remove_ads`
- reference name `Remove Ads`
- display price `2.99`
- locale `en_US`, display name `Remove Ads`
- description: removes interstitial ads between matches
- `familyShareable`: false
- no subscription group

The synchronized root group picks the file up as a resource. Local testing also needs the **Run** scheme to point at it. There is no shared scheme in the repo (`*.xcodeproj/*` is gitignored except `project.pbxproj`, `xcshareddata/`, and `project.xcworkspace/`). Add `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` with `StoreKitConfigurationFileReference` aimed at `Antonymizer.storekit`, and the existing Kotlin build phase left to the project rather than duplicated. Check the scheme into git so a simulator run uses the local product without App Store Connect.

Xcode's StoreKit transaction manager (Debug > StoreKit > Manage Transactions) is how to refund during the local test.

### Restore button

App Review requires a visible restore control for non-consumables. Settings already has one, inside the Premium card, **only while ads are not removed**:

- Title "Restore Purchases", subtitle "Already purchased? Restore here.", outlined "Restore" button.
- It calls `billingManager.restorePurchases()`.
- The purchase button shows `removeAdsPrice` and is enabled only for `PurchaseState.Ready`.

That is the right place. Do not add a toolbar item, a home-screen button, or a new screen.

Gap: once `adsRemoved` is true the whole restore row disappears, replaced by "You've removed ads. Thank you for your support!" A reviewer whose sandbox account already owns the product never sees the word Restore. Add one `TextButton` labeled "Restore Purchases" under that thank-you line, with no second description block. Same `onRestorePurchases` callback. Shared Compose, so Android gets the same quiet button. Android `restorePurchases()` already re-queries Play purchases, so the extra button is safe there.

Do not prompt for restore on launch. `currentEntitlements` covers reinstall.

## Hiding ads immediately on both platforms

One flag, already shared.

`settingsStorage.setAdsRemoved(true)` updates DataStore key `ads_removed`. `AdManagerAndroid` and `AdManagerIos` both expose that flow as `adsRemoved`. `App.kt` collects `monetizationState.adsRemoved` (the ad manager's flow) and passes it to Settings. The Premium card switches to the thank-you state on the next emission. No restart.

`showInterstitialIfEligible` reads `adsRemoved.first()` at match end. A purchase that lands mid-match skips the interstitial that would have played before Results. Rewarded hint and bonus buttons stay, on purpose.

Android already writes the flag from `processPurchases` when the Play purchase is `PURCHASED` and the product id matches. iOS must write it from the StoreKit entitlement callback, not from a local "user tapped buy" boolean. Write `false` when a completed entitlement check says the product is absent (refund, or restore on a fresh install with no purchase). Never write `false` before that check finishes, or a paying user sees an interstitial on cold start while StoreKit is still loading.

Put that rule in a pure function and test it:

```text
resolved(cached, storeCheckCompleted, storeOwnsRemoveAds) =
    if storeCheckCompleted then storeOwnsRemoveAds else cached
```

`BillingManagerIos.onEntitlement` is only invoked when the check completed, and the boolean is `storeOwnsRemoveAds`. The cache is whatever DataStore already holds. Calling `setAdsRemoved` with the store result is the completed branch. Skipping the callback is the not-completed branch.

No Android billing change is required for the hide path beyond the interstitial policy call. Do not clear `ads_removed` from `clearAllData()`.

## Privacy manifest and App Store privacy label

### `PrivacyInfo.xcprivacy` (in the app target)

There is no app privacy manifest today. Google Mobile Ads 11.13 and UMP 3.0 ship their own inside the pods. Those cover the SDK's required-reason APIs. They do not declare the app's tracking flag.

Add `iosApp/iosApp/PrivacyInfo.xcprivacy`. The synchronized group copies it into the bundle.

- `NSPrivacyTracking` = true, because we request ATT and send the IDFA to Google when the user allows it.
- `NSPrivacyTrackingDomains`: copy the domain list from the Google Mobile Ads privacy manifest inside the pod after `pod install` (typical hosts are `googleads.g.doubleclick.net`, `googleadservices.com`, and related Google ad hosts). Do not invent domains.
- `NSPrivacyCollectedDataTypes`: copy the collected-data array from that same pod manifest so the app report matches the SDK we actually link. Expected categories from the current Google disclosure are device ID, advertising data, product interaction, and crash/performance data. Include coarse location only if the pod manifest still lists it. This app does not request location permission.
- `NSPrivacyAccessedAPITypes`: leave empty in the first draft. Our storage is a file in Documents, not `UserDefaults`. Then archive once on a Mac and open the privacy report. Add a required-reason entry only for an API the report attributes to the **app** target. A wrong reason is also a rejection. File-timestamp access from okio/DataStore, if flagged, is commonly reason `C617.1` (files inside the app container). Confirm against the report before writing it in.

Do not add a privacy manifest by hand for the pods. `use_frameworks!` embeds theirs.

### App Store Connect nutrition label (Mark only, not code)

Follow [Google's iOS data disclosure](https://developers.google.com/admob/ios/privacy/data-disclosure) on the day the listing is filled in. Expect at least:

- Data used to track you: Device ID (only if ATT is in the build, which it is).
- Advertising data and product interaction, used for third-party advertising.
- Crash data and performance data if Google's page still says the SDK collects them.
- Do not declare the player name, stats, or leaderboard. They stay on device.
- Do not declare purchase history. We do not send receipts to our own server. StoreKit 2 verification stays on device.

**NEEDS MARK** (question 6): a privacy policy URL. App Store Connect requires one when the app tracks or shows third-party ads. The implementation adds a single "Privacy Policy" text button in the existing About card that opens the URL. If the URL is not known when code is written, leave the button out and block submission. Do not invent a URL.

## Test plan

### Unit tests (this repo, no Mac)

`commonTest` today is a placeholder (`ComposeAppCommonTest` asserts `1 + 2`). Add tests next to a new `commonMain` helper. Run `./gradlew :composeApp:test`.

`InterstitialPolicy.shouldShow(adsRemoved, lastShownAtMillis, nowMillis, adLoaded)`:

- ads removed, ad loaded, cooldown elapsed → false
- ad not loaded → false
- last shown 179_999 ms ago → false
- last shown 180_000 ms ago → true
- last shown 0 (never) → true
- ads not removed, loaded, cooldown elapsed → true

`RemoveAdsEntitlement.resolved(cached, storeCheckCompleted, storeOwns)`:

- check not finished, cache true → true (no cold-start ad flash)
- check not finished, cache false → false
- check finished, store owns → true
- check finished, store does not own, cache true → false (refund)
- check finished, store owns, cache false → true (purchase or restore)

`purchaseStateFromBridge`:

- `ready` → `Ready`
- `purchasing` and `pending` → `Purchasing`
- `success` → `PurchaseSuccess`
- `cancelled` → `PurchaseError("Purchase cancelled")`
- `error` with a message → `PurchaseError` of that message
- unknown → `Unavailable`

Also assert `BillingManager.PRODUCT_REMOVE_ADS` is `remove_ads`.

Do not Robolectric-test Play Billing or try to unit-test `GAD*` / StoreKit. There is no iOS test target. Do not add a Swift test target for this.

Android `showInterstitialIfEligible` should call the same policy function so the tested conditions are the ones in production. That is a behavior-preserving edit on Android.

### Mac / simulator, StoreKit configuration (implementer)

Debug scheme uses `Antonymizer.storekit`. Run the iOS simulator from the **workspace** after `pod install`.

- Console shows Google's test interstitial creative (sample unit, "Test Ad"), not a blank failure and not a production creative.
- Finish a match. An interstitial presents, then Results. Finish another match inside 3 minutes. No second interstitial. Navigation to Results still happens.
- Hint still requires a rewarded test ad, and it still works after remove-ads.
- Settings purchase button shows the StoreKit price (`$2.99` from the configuration file), not a hard-coded string that is tappable while the product is missing.
- Buy `remove_ads`. The Premium card switches to the thank-you state without leaving Settings. Kill the app, relaunch, finish a match: no interstitial.
- Delete the app, reinstall, launch. Entitlement comes back from `currentEntitlements` with no extra tap. Also tap Restore on a fresh install that already owns it (transaction manager) and confirm the card updates.
- In the StoreKit transaction manager, refund the transaction. Relaunch. The interstitial returns and Settings shows the price button again.
- Cancel the purchase sheet. Settings shows "Purchase cancelled". The ad-free flag stays false.
- Restore on a simulator account that has never bought it shows "No previous purchase found" and does not set the flag.
- With the UMP debug-geography flag **off**, a non-EEA debug run reaches a test ad without a consent wall.
- With the flag **on**, the GDPR form appears before any ad request. Dismissing or declining must not crash. Ads load afterward only when `canRequestAds` is true.
- ATT appears once, not twice, and the system dialog text matches the plist string. Deny ATT: a later match still attempts an ad (non-IDFA).
- "Ad privacy choices" is visible only when UMP says privacy options are required.
- "Restore Purchases" is on screen both before purchase (existing row) and after purchase (text button).

### What only Mark can do

These are not implementable from the repo. TestFlight hits real sandbox StoreKit, not `Antonymizer.storekit`.

- Confirm the AdMob iOS app id and the two release ad units, or create them and replace the strings.
- Publish the GDPR message and the IDFA explainer on that iOS app.
- App Store Connect app record for bundle id `com.markduenas.antonymizer.Antonymizer` (or tell us the real bundle id).
- Paid Applications agreement, tax, and banking.
- Non-consumable IAP `remove_ads`, price tier for $2.99, localization, review screenshot, cleared for sale.
- Sandbox Apple ID for TestFlight purchase and restore. Confirm ads disappear after the sandbox purchase and after restore on a second install.
- App Privacy answers, privacy policy URL, and age rating / ads declaration.
- A physical-device test hash if TestFlight should not count his own impressions.

He already owns step 3 and 4 of the Linear issue (app record, IAP, TestFlight, App Review submission). This plan does not include uploading a build.

## Risks

- **Ads before consent.** The current `iOSApp.init` start is the bug most likely to cause an AdMob policy problem or an EU complaint. The new order has to be the one start path. Two starts (SwiftUI `init` plus Kotlin `initialize`) will regress it.
- **CocoaPods not installed.** `Pods/` is gitignored. Building the `xcodeproj` instead of the workspace fails the manifest-lock script. Implementation notes and any README touch should say `pod install` then open `iosApp/iosApp.xcworkspace`.
- **Static Compose framework plus `use_frameworks!`.** Already how the app links. Adding SPM beside it is the risk to avoid. A clean `pod install` on 11.13.0 should keep compiling `import GoogleMobileAds`.
- **SKAdNetwork list goes stale.** Paste Google's list in the implementation PR and name the doc URL in the PR description. Do not trim it.
- **Refunds ignored if we only ever write `true`.** The entitlement walk must be allowed to write `false` after a successful check. It must not write `false` when the check fails.
- **`AppStore.sync()` on launch.** Do not. It prompts. The Restore button is the only caller, and local StoreKit configs often throw. The entitlement walk still has to run.
- **Restore hidden after purchase.** App Review looks for the control. The text button under the thank-you line is the small fix. Do not add a second monetization screen.
- **Ask to Buy / pending.** Granting on `.pending` gives ad-free before the parent approves. Grant only from a verified transaction in `Transaction.updates` or `currentEntitlements`.
- **Unverified transactions.** Jailbreaks and malformed JWS show up as `.unverified`. Ignore them.
- **TestFlight uses production ad units.** Debug sample units do not protect Mark's device once he installs a Release build. Invalid traffic is an AdMob account risk.
- **Vague ATT string and missing privacy policy.** Both are common App Review rejections, independent of whether ads load.
- **Bundle id surprise.** `$(TEAM_ID)` is empty, so the id is not the Android application id. Creating the IAP on the wrong bundle means StoreKit returns no product and the button stays disabled.
- **iOS deployment target mismatch (15.0 vs 18.2).** Harmless for StoreKit 2. Confusing if someone "fixes" the Xcode target downward and then uses an iOS 15 simulator the project has not been run on.
- **`BillingManagerIos` seed `true`.** Unused by the current UI, but a later reader of `billingManager.adsRemoved` would think ads are removed until DataStore emits. Fix the seed in the same PR.
- **No server validation.** Acceptable for one non-consumable with StoreKit 2 on-device verification. Do not add a receipt server in this work.

## Files the implementation PR should touch

New:

- `iosApp/iosApp/StoreKitBilling.swift`
- `iosApp/iosApp/ConsentCoordinator.swift` (or the consent functions inside `AdHelper.swift` if that file stays readable)
- `iosApp/iosApp/PrivacyInfo.xcprivacy`
- `iosApp/iosApp/Antonymizer.storekit`
- `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme`
- `composeApp/src/iosMain/kotlin/com/markduenas/antonymizer/monetization/NativeBillingProvider.kt`
- `composeApp/src/commonMain/kotlin/com/markduenas/antonymizer/monetization/InterstitialPolicy.kt` (policy + entitlement helper + purchase-state mapper)
- `composeApp/src/commonTest/kotlin/com/markduenas/antonymizer/monetization/MonetizationPolicyTest.kt`
- `iosApp/iosApp/Antonymizer.entitlements` only if enabling the capability creates one

Edit:

- `iosApp/iosApp/Info.plist` — full `SKAdNetworkItems`, tighter `NSUserTrackingUsageDescription`. Keep `GADApplicationIdentifier`.
- `iosApp/iosApp/AdHelper.swift` — consent-gated start, `shown` flag, drop the dead cooldown, test-device list hook.
- `iosApp/iosApp/iOSApp.swift` — remove eager Mobile Ads start and the manual ATT request.
- `iosApp/iosApp/ContentView.swift` — set the billing holder; set the root view controller before ads try to present a form.
- `composeApp/src/iosMain/.../monetization/NativeAdProvider.kt` — interstitial callback reports whether an ad was shown; privacy-options surface if it stays on this interface instead of `AdManager` only.
- `composeApp/src/iosMain/.../monetization/AdManagerIos.kt` — use `InterstitialPolicy`, record a show only when presented.
- `composeApp/src/iosMain/.../monetization/BillingManagerIos.kt` — replace the stub.
- `composeApp/src/iosMain/.../MainViewController.kt` — no structural change if `initialize()` stays the hook. Update the "stubs" comment.
- `composeApp/src/androidMain/.../monetization/AdManagerAndroid.kt` — call `InterstitialPolicy` so the tested rules are the live rules. No product-id or ad-unit change.
- `composeApp/src/commonMain/.../monetization/AdManager.kt` — `privacyOptionsRequired` and `presentPrivacyOptions()`.
- `composeApp/src/commonMain/.../monetization/MonetizationState.kt` — forward those.
- `composeApp/src/commonMain/.../ui/screens/settings/SettingsScreen.kt` — restore text button when already purchased; optional ad-privacy text button; privacy-policy text button only with a real URL.
- `composeApp/src/commonMain/.../App.kt` — pass the new callbacks.
- `composeApp/src/androidMain/.../monetization/AdManagerAndroid.kt` already listed. Android `presentPrivacyOptions` is a no-op. `AdManagerAndroid` needs the new interface members. `BillingManagerAndroid` does not.

Do not change:

- Play product id, Android ad unit ids, Android app id.
- `version.properties`, `MARKETING_VERSION`, `CURRENT_PROJECT_VERSION`.
- `clearAllData()`'s handling of `ads_removed` (it already leaves the flag alone).
- Pod major version. No SPM package.

Touch `CLAUDE.md` / `AGENTS.md` only to delete the "iOS billing is a stub" sentences after the code lands. Not in this plan PR.

## NEEDS MARK

1. Are these AdMob iOS ids live and serving? App `ca-app-pub-7540731406850248~1440731565`, interstitial `ca-app-pub-7540731406850248/7667964658`, rewarded `ca-app-pub-7540731406850248/6932731399`. They are already in the repo. If any id is a placeholder, send the real one before Release/TestFlight.
2. Create and publish, on that iOS app, an AdMob GDPR message and an IDFA explainer (Privacy & messaging). The code presents whatever UMP returns. EU consent does not exist until the GDPR message is published.
3. App Store Connect non-consumable product id. Plan uses `remove_ads` at $2.99, same id as Play. Confirm there is no existing product under another id.
4. Bundle id. The Xcode build resolves to `com.markduenas.antonymizer.Antonymizer` because `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` is empty. Team in the project is `A8AAE5A4T6`. Confirm this is the App Store Connect bundle id, or send the one to register.
5. After a debug run, the UMP / Google test-device hash printed in the console, if he wants the EEA form and TestFlight test-mode on a physical device. Do not block the code PR on this.
6. Privacy policy URL for App Store Connect and the About-card link. Submission is blocked without it. No URL will be invented in code.
7. Confirm rewarded ads (hints, continue, bonus) stay after remove-ads. That is what Android and the Settings sentence already do. Say so if remove-ads should also remove rewarded ads. Default is to keep them.
