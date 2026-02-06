import Foundation
import GoogleMobileAds
import UIKit
import ComposeApp

/// Singleton class to manage AdMob ads on iOS
/// Implements NativeAdProvider protocol from Kotlin
@objc public class IosAdHelper: NSObject, NativeAdProvider {

    // MARK: - Singleton
    @objc public static let shared = IosAdHelper()

    // MARK: - Production Ad Unit IDs
    private let interstitialAdUnitID = "ca-app-pub-7540731406850248/7667964658"
    private let rewardedAdUnitID = "ca-app-pub-7540731406850248/6932731399"

    // MARK: - Ad State
    private var interstitialAd: GADInterstitialAd?
    private var rewardedAd: GADRewardedAd?

    // NativeAdProvider protocol properties
    public var isInterstitialReady: Bool {
        return interstitialAd != nil
    }

    public var isRewardedReady: Bool {
        return rewardedAd != nil
    }

    // Store the root view controller for presenting ads
    private weak var rootViewController: UIViewController?

    public func setRootViewController(_ vc: UIViewController) {
        rootViewController = vc
    }

    // MARK: - Cooldown tracking
    private var lastInterstitialTime: TimeInterval = 0
    private let interstitialCooldown: TimeInterval = 180 // 3 minutes

    // MARK: - Callbacks
    private var interstitialCompletion: (() -> Void)?
    private var rewardedCallback: (() -> Void)?
    private var rewardedDismissedCallback: (() -> Void)?

    // MARK: - Initialization
    private override init() {
        super.init()
    }

    /// Initialize the Google Mobile Ads SDK (NativeAdProvider protocol)
    public func initialize() {
        GADMobileAds.sharedInstance().start { status in
            print("AdMob SDK initialized: \(status.adapterStatusesByClassName)")
            self.loadInterstitial()
            self.loadRewarded()
        }
    }

    // MARK: - Interstitial Ads (NativeAdProvider protocol)

    public func loadInterstitial() {
        guard interstitialAd == nil else { return }

        let request = GADRequest()
        GADInterstitialAd.load(withAdUnitID: interstitialAdUnitID, request: request) { [weak self] ad, error in
            if let error = error {
                print("Failed to load interstitial ad: \(error.localizedDescription)")
                return
            }
            print("Interstitial ad loaded")
            self?.interstitialAd = ad
            self?.interstitialAd?.fullScreenContentDelegate = self
        }
    }

    /// Show interstitial (NativeAdProvider protocol)
    /// Cooldown is managed by Kotlin side
    public func showInterstitial(onComplete: @escaping () -> Void) {
        guard let viewController = rootViewController ?? UIApplication.shared.windows.first?.rootViewController else {
            print("No view controller available")
            onComplete()
            return
        }

        guard let ad = interstitialAd else {
            print("Interstitial not ready")
            loadInterstitial()
            onComplete()
            return
        }

        interstitialCompletion = onComplete
        lastInterstitialTime = Date().timeIntervalSince1970
        ad.present(fromRootViewController: viewController)
    }

    // MARK: - Rewarded Ads (NativeAdProvider protocol)

    public func loadRewarded() {
        guard rewardedAd == nil else { return }

        let request = GADRequest()
        GADRewardedAd.load(withAdUnitID: rewardedAdUnitID, request: request) { [weak self] ad, error in
            if let error = error {
                print("Failed to load rewarded ad: \(error.localizedDescription)")
                return
            }
            print("Rewarded ad loaded")
            self?.rewardedAd = ad
            self?.rewardedAd?.fullScreenContentDelegate = self
        }
    }

    /// Show rewarded ad (NativeAdProvider protocol)
    public func showRewarded(onRewarded: @escaping () -> Void, onDismissed: @escaping () -> Void) {
        guard let viewController = rootViewController ?? UIApplication.shared.windows.first?.rootViewController else {
            print("No view controller available")
            onDismissed()
            return
        }

        guard let ad = rewardedAd else {
            print("Rewarded ad not ready")
            loadRewarded()
            onDismissed()
            return
        }

        rewardedCallback = onRewarded
        rewardedDismissedCallback = onDismissed

        ad.present(fromRootViewController: viewController) { [weak self] in
            print("User earned reward")
            self?.rewardedCallback?()
            self?.rewardedCallback = nil
            self?.rewardedDismissedCallback = nil
        }
    }
}

// MARK: - GADFullScreenContentDelegate

extension IosAdHelper: GADFullScreenContentDelegate {

    public func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        print("Ad dismissed")

        if ad is GADInterstitialAd {
            interstitialAd = nil
            loadInterstitial()
            interstitialCompletion?()
            interstitialCompletion = nil
        } else if ad is GADRewardedAd {
            rewardedAd = nil
            loadRewarded()
            // If rewardedCallback is still set, user dismissed without earning reward
            if rewardedCallback != nil {
                rewardedDismissedCallback?()
                rewardedCallback = nil
                rewardedDismissedCallback = nil
            }
        }
    }

    public func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        print("Ad failed to present: \(error.localizedDescription)")

        if ad is GADInterstitialAd {
            interstitialAd = nil
            loadInterstitial()
            interstitialCompletion?()
            interstitialCompletion = nil
        } else if ad is GADRewardedAd {
            rewardedAd = nil
            loadRewarded()
            rewardedDismissedCallback?()
            rewardedCallback = nil
            rewardedDismissedCallback = nil
        }
    }
}
