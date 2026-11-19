# Remove Ads — one-time product

Create this in Play Console before the first build that should show a live price. The Android billing code looks up this id and no other.

## Values to enter

| Play Console field | Value |
| --- | --- |
| Product ID | `remove_ads` |
| Product type | One-time product. Not a subscription. Not consumable. |
| Name / title | Remove Ads |
| Description | One-time purchase. Removes interstitial ads between matches. |
| Price | NEEDS MARK. Suggested default **USD 2.99**. |
| Countries | NEEDS MARK. Leave the EEA, the UK, and Switzerland out until DUE-84 (UMP consent) ships. See Countries in `play-console-answers.md`. |

The description above is the sentence already shown on the Settings card (`SettingsScreen.kt`). It is 64 characters. Play's one-time product description limit is 200.

## Where the id comes from

`BillingManager.PRODUCT_REMOVE_ADS` in `composeApp/src/commonMain/kotlin/com/markduenas/antonymizer/monetization/BillingManager.kt` is the string `remove_ads`. The comment above it says the id must match Play Console.

`BillingManagerAndroid.queryProductDetails()` queries that id with `BillingClient.ProductType.INAPP` and reads `oneTimePurchaseOfferDetails.formattedPrice`.

On a successful purchase the app:

- sets the local `ads_removed` preference to true
- calls `acknowledgePurchase` with the purchase token
- does **not** call `consumeAsync`

That is a non-consumable one-time product. If the Play product is created as a consumable, or as a subscription, this code will not treat it as a permanent Remove Ads entitlement.

The purchase button on Settings is enabled only when billing reports `PurchaseState.Ready`.

## Price

If Play returns no formatted price, or the product-details query fails, the app displays the literal fallback `$2.99` (`BillingManagerAndroid`). After the product exists and the query succeeds, the app shows whatever formatted price Play returns. The fallback does not lock the price you choose.

NEEDS MARK: confirm USD 2.99, or pick another price. Also set which countries can buy it.

## What the purchase does, and does not do

It stops interstitial ads. `showInterstitialIfEligible` returns immediately when `ads_removed` is true. Interstitials otherwise run when a match ends, and not again until three minutes have passed (`AdManager.INTERSTITIAL_COOLDOWN_MS`).

It does not turn off rewarded ads. `showRewardedAd` does not check `ads_removed`. Hints (up to two per match) still ask for a rewarded ad.

NEEDS MARK: the in-app card does not mention rewarded ads. If the Play product description should say that rewarded ads can still play, use this instead (118 characters):

`One-time purchase. Removes interstitial ads between matches. Rewarded ads for hints can still play.`

## Status

The product has to be Active before a purchase can complete.

Who can buy, from the [billing test guide](https://developer.android.com/google/play/billing/test):

- A normal Play user who installs from Play (a test track or production) can complete a real purchase and is charged.
- A license tester can sideload a build, including a debug-signed one, without uploading that build. The package name must be `com.markduenas.antonymizer`, and the Google account on the device must be on this app's license-test list. Those purchases use Play's test payment methods and are not charged.
- An account that is not a license tester cannot complete billing on a build that was not signed and uploaded to Play. That case can still show the `$2.99` fallback and then fail to open the purchase sheet.
