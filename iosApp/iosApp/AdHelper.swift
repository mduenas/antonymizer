import Foundation
import GoogleMobileAds
import UIKit
import ComposeApp

/// AdMob interstitial and rewarded ads. The SDK starts only after
/// IosConsentCoordinator reports that UMP allows ad requests.
/// Implements NativeAdProvider from Kotlin.
@objc public class IosAdHelper: NSObject, NativeAdProvider {

    @objc public static let shared = IosAdHelper()

    // Debug uses Google's iOS sample units. Release, TestFlight, and the App Store
    // use the production units. Do not copy the Android sample unit ids here.
    #if DEBUG
    private let interstitialAdUnitID = "ca-app-pub-3940256099942544/4411468910"
    private let rewardedAdUnitID = "ca-app-pub-3940256099942544/1712485313"
    #else
    private let interstitialAdUnitID = "ca-app-pub-7540731406850248/7667964658"
    private let rewardedAdUnitID = "ca-app-pub-7540731406850248/6932731399"
    #endif

    /// Paste the hash the SDK prints on a physical device. Empty means no override.
    /// Sample ad units in debug are already test ads, so this list is for Release.
    private let admobTestDeviceIds: [String] = []

    private var interstitialAd: GADInterstitialAd?
    private var rewardedAd: GADRewardedAd?

    public var isInterstitialReady: Bool { interstitialAd != nil }
    public var isRewardedReady: Bool { rewardedAd != nil }

    public private(set) var privacyOptionsRequired: Bool = false

    private weak var rootViewController: UIViewController?
    private var adsReady = false
    private var sdkStartRequested = false

    private var interstitialPresented: (() -> Void)?
    private var interstitialCompletion: (() -> Void)?
    private var rewardedCallback: (() -> Void)?
    private var rewardedDismissedCallback: (() -> Void)?

    private override init() {
        super.init()
    }

    public func setRootViewController(_ viewController: UIViewController) {
        rootViewController = viewController
    }

    /// Kotlin asks for ads. Consent, not this call, starts the SDK.
    public func initialize() {
        IosConsentCoordinator.shared.noteKotlinReady()
    }

    /// True once Mobile Ads start has been requested, including before the
    /// start callback loads the first ads.
    var loadsHaveBegun: Bool { sdkStartRequested }

    /// Called only after UMP allows ads and ATT is no longer .notDetermined.
    /// A denial still loads with GADRequest. The SDK omits the IDFA.
    func startSdk() {
        guard !sdkStartRequested else { return }
        sdkStartRequested = true
        if !admobTestDeviceIds.isEmpty {
            GADMobileAds.sharedInstance().requestConfiguration.testDeviceIdentifiers = admobTestDeviceIds
        }
        GADMobileAds.sharedInstance().start { [weak self] status in
            print("AdMob SDK initialized: \(status.adapterStatusesByClassName)")
            self?.adsReady = true
            self?.loadInterstitial()
            self?.loadRewarded()
        }
    }

    /// After an ATT decision, drop any ad that was requested without the new status.
    func reloadAdsAfterTrackingDecision() {
        guard adsReady else { return }
        interstitialAd = nil
        rewardedAd = nil
        publishReady()
        loadInterstitial()
        loadRewarded()
    }

    func setPrivacyOptionsRequired(_ required: Bool) {
        privacyOptionsRequired = required
        IosConsentBridge.shared.privacyOptionsRequiredChanged(required: required)
    }

    public func loadInterstitial() {
        guard adsReady, interstitialAd == nil else { return }
        let request = GADRequest()
        GADInterstitialAd.load(withAdUnitID: interstitialAdUnitID, request: request) { [weak self] ad, error in
            if let error = error {
                print("Failed to load interstitial ad: \(error.localizedDescription)")
                self?.publishReady()
                return
            }
            self?.interstitialAd = ad
            self?.interstitialAd?.fullScreenContentDelegate = self
            self?.publishReady()
        }
    }

    public func loadRewarded() {
        guard adsReady, rewardedAd == nil else { return }
        let request = GADRequest()
        GADRewardedAd.load(withAdUnitID: rewardedAdUnitID, request: request) { [weak self] ad, error in
            if let error = error {
                print("Failed to load rewarded ad: \(error.localizedDescription)")
                self?.publishReady()
                return
            }
            self?.rewardedAd = ad
            self?.rewardedAd?.fullScreenContentDelegate = self
            self?.publishReady()
        }
    }

    /// Cooldown is stamped from adWillPresentFullScreenContent, not from dismiss.
    public func showInterstitial(onPresented: @escaping () -> Void, onComplete: @escaping () -> Void) {
        guard let viewController = viewControllerForPresentation() else {
            onComplete()
            return
        }
        guard let ad = interstitialAd else {
            loadInterstitial()
            onComplete()
            return
        }
        interstitialPresented = onPresented
        interstitialCompletion = onComplete
        ad.present(fromRootViewController: viewController)
    }

    public func showRewarded(onRewarded: @escaping () -> Void, onDismissed: @escaping () -> Void) {
        guard let viewController = viewControllerForPresentation() else {
            onDismissed()
            return
        }
        guard let ad = rewardedAd else {
            loadRewarded()
            onDismissed()
            return
        }
        rewardedCallback = onRewarded
        rewardedDismissedCallback = onDismissed
        ad.present(fromRootViewController: viewController) { [weak self] in
            self?.rewardedCallback?()
            self?.rewardedCallback = nil
            self?.rewardedDismissedCallback = nil
        }
    }

    public func presentPrivacyOptions() {
        IosConsentCoordinator.shared.presentPrivacyOptions()
    }

    private func viewControllerForPresentation() -> UIViewController? {
        if let rootViewController, rootViewController.viewIfLoaded?.window != nil {
            return rootViewController
        }
        return nil
    }

    private func publishReady() {
        IosAdsBridge.shared.readyChanged(
            interstitialReady: interstitialAd != nil,
            rewardedReady: rewardedAd != nil
        )
    }

    private func finishInterstitial() {
        interstitialAd = nil
        interstitialPresented = nil
        let completion = interstitialCompletion
        interstitialCompletion = nil
        loadInterstitial()
        publishReady()
        completion?()
    }

    private func finishRewarded(callDismissed: Bool) {
        rewardedAd = nil
        rewardedCallback = nil
        let dismissedCallback = rewardedDismissedCallback
        rewardedDismissedCallback = nil
        loadRewarded()
        publishReady()
        if callDismissed {
            dismissedCallback?()
        }
    }
}

extension IosAdHelper: GADFullScreenContentDelegate {

    public func adWillPresentFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        // Stamp the cooldown when the interstitial is shown, matching Android's
        // onAdShowedFullScreenContent rather than dismiss time.
        if ad is GADInterstitialAd {
            interstitialPresented?()
            interstitialPresented = nil
        }
    }

    public func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        if ad is GADInterstitialAd {
            finishInterstitial()
        } else if ad is GADRewardedAd {
            // The reward handler clears rewardedCallback before dismiss.
            finishRewarded(callDismissed: rewardedCallback != nil)
        }
    }

    public func ad(
        _ ad: GADFullScreenPresentingAd,
        didFailToPresentFullScreenContentWithError error: Error
    ) {
        print("Ad failed to present: \(error.localizedDescription)")
        if ad is GADInterstitialAd {
            interstitialPresented = nil
            finishInterstitial()
        } else if ad is GADRewardedAd {
            finishRewarded(callDismissed: true)
        }
    }
}
