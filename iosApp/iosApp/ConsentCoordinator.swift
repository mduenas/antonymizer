import AppTrackingTransparency
import UIKit
import UserMessagingPlatform

/// UMP consent, then the Google Mobile Ads SDK.
/// The form and the ATT fallback run only once the root view controller is in a
/// window and the application is active. They are not started from setup.
final class IosConsentCoordinator {

    static let shared = IosConsentCoordinator()

    #if DEBUG
    /// Leave false so local ad testing is not stuck on a consent form.
    /// Set true to force an EEA geography and exercise the GDPR message.
    private let forceEeaConsentFormForDebug = false
    #endif

    private weak var rootViewController: UIViewController?
    private var kotlinReady = false
    private var consentFlowStarted = false
    private var attFallbackRequested = false

    private init() {}

    func setRootViewController(_ viewController: UIViewController) {
        rootViewController = viewController
    }

    func noteKotlinReady() {
        Task { @MainActor in
            self.kotlinReady = true
            self.startIfReadyOnMain()
        }
    }

    /// Safe to call from onAppear, didBecomeActive, and updateUIViewController.
    /// The body runs on the main actor and returns until there is a window and
    /// the app is active, so setup itself never presents a form.
    func startIfReady() {
        Task { @MainActor in
            self.startIfReadyOnMain()
        }
    }

    func presentPrivacyOptions() {
        Task { @MainActor in
            guard let viewController = self.rootViewController, self.isPresentable else { return }
            do {
                try await ConsentForm.presentPrivacyOptionsForm(from: viewController)
            } catch {
                print("Privacy options form failed: \(error.localizedDescription)")
            }
            self.publishPrivacyOptions()
        }
    }

    @MainActor
    private func startIfReadyOnMain() {
        guard kotlinReady, isPresentable else { return }
        if !consentFlowStarted {
            consentFlowStarted = true
            beginConsentUpdate()
        } else {
            requestAttFallbackIfNeeded()
        }
    }

    @MainActor
    private var isPresentable: Bool {
        guard UIApplication.shared.applicationState == .active else { return false }
        return rootViewController?.viewIfLoaded?.window != nil
    }

    private func beginConsentUpdate() {
        let parameters = RequestParameters()
        #if DEBUG
        if forceEeaConsentFormForDebug {
            let debugSettings = DebugSettings()
            debugSettings.geography = .EEA
            parameters.debugSettings = debugSettings
        }
        #endif
        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { [weak self] _ in
            // Read canRequestAds even when this callback has an error. A stored
            // decision from a previous session can still allow ads while offline.
            Task { @MainActor in
                await self?.handleConsentInfo(canRequest: ConsentInformation.shared.canRequestAds)
            }
        }
    }

    @MainActor
    private func handleConsentInfo(canRequest: Bool) async {
        publishPrivacyOptions()
        // Do not wait for the form when a previous decision already allows ads.
        if canRequest {
            IosAdHelper.shared.startSdk()
        }
        if let viewController = rootViewController, isPresentable {
            do {
                try await ConsentForm.loadAndPresentIfRequired(from: viewController)
            } catch {
                // Form load failed. Fall through and read canRequestAds anyway.
                print("Consent form failed: \(error.localizedDescription)")
            }
        } else {
            // The window was not ready. Let the next active callback try again.
            consentFlowStarted = false
        }
        publishPrivacyOptions()
        if ConsentInformation.shared.canRequestAds {
            IosAdHelper.shared.startSdk()
        }
        requestAttFallbackIfNeeded()
    }

    @MainActor
    private func requestAttFallbackIfNeeded() {
        guard !attFallbackRequested else { return }
        guard ATTrackingManager.trackingAuthorizationStatus == .notDetermined else { return }
        guard isPresentable else { return }
        attFallbackRequested = true
        ATTrackingManager.requestTrackingAuthorization { _ in
            Task { @MainActor in
                // Denial still requests ads. The SDK drops the IDFA on its own.
                guard ConsentInformation.shared.canRequestAds else { return }
                IosAdHelper.shared.startSdk()
                IosAdHelper.shared.reloadAdsAfterTrackingDecision()
            }
        }
    }

    private func publishPrivacyOptions() {
        let required = ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        IosAdHelper.shared.setPrivacyOptionsRequired(required)
    }
}
