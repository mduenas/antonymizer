import AppTrackingTransparency
import UIKit
import UserMessagingPlatform
import ComposeApp

/// UMP form first, then ATT if it is still unanswered, then one Mobile Ads start.
/// Nothing here runs until the root view controller is in a window and the app
/// is active.
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
    /// Set only after the form callback returns. The window-not-ready retry does not set it.
    private var consentFlowFinished = false
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
        let action = MonetizationPolicyKt.consentLifecycleCode(
            presentable: isPresentable,
            kotlinReady: kotlinReady,
            consentFlowStarted: consentFlowStarted
        )
        // Already-started and not-ready calls are IGNORE. This path never requests ATT.
        guard action == MonetizationPolicyKt.CONSENT_BEGIN_UPDATE else { return }
        consentFlowStarted = true
        beginConsentUpdate()
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
            // The SDK still waits until the form returns and ATT is resolved.
            Task { @MainActor in
                await self?.handleConsentInfo(canRequest: ConsentInformation.shared.canRequestAds)
            }
        }
    }

    @MainActor
    private func handleConsentInfo(canRequest: Bool) async {
        publishPrivacyOptions()
        // canRequest is the value after the info update, including its error path.
        // Ads do not start from it. The form callback decides.
        _ = canRequest
        guard let viewController = rootViewController, isPresentable else {
            applyPostForm(
                MonetizationPolicyKt.postConsentFormCode(
                    formCouldBePresented: false,
                    canRequestAds: false,
                    attNotDetermined: false
                )
            )
            return
        }
        do {
            try await ConsentForm.loadAndPresentIfRequired(from: viewController)
        } catch {
            // Form load failed. Fall through and read canRequestAds anyway.
            print("Consent form failed: \(error.localizedDescription)")
        }
        publishPrivacyOptions()
        applyPostForm(
            MonetizationPolicyKt.postConsentFormCode(
                formCouldBePresented: true,
                canRequestAds: ConsentInformation.shared.canRequestAds,
                attNotDetermined: ATTrackingManager.trackingAuthorizationStatus == .notDetermined
            )
        )
    }

    @MainActor
    private func applyPostForm(_ action: Int32) {
        if action == MonetizationPolicyKt.CONSENT_RETRY_UPDATE {
            // Window wasn't ready. Do not mark the flow finished.
            consentFlowStarted = false
            return
        }
        consentFlowFinished = true
        if action == MonetizationPolicyKt.CONSENT_REQUEST_TRACKING {
            requestAttFallbackIfNeeded()
        } else if action == MonetizationPolicyKt.CONSENT_START_ADS {
            IosAdHelper.shared.startSdk()
        }
    }

    /// Manual ATT only after UMP has finished and only when ads are allowed.
    /// If UMP already presented the system prompt, the status is no longer
    /// .notDetermined and this returns without a second prompt.
    @MainActor
    private func requestAttFallbackIfNeeded() {
        guard consentFlowFinished else { return }
        guard ConsentInformation.shared.canRequestAds else { return }
        guard !attFallbackRequested else { return }
        guard ATTrackingManager.trackingAuthorizationStatus == .notDetermined else { return }
        guard isPresentable else { return }
        attFallbackRequested = true
        let loadsAlreadyBegan = IosAdHelper.shared.loadsHaveBegun
        ATTrackingManager.requestTrackingAuthorization { _ in
            Task { @MainActor in
                let action = MonetizationPolicyKt.postTrackingCode(
                    canRequestAds: ConsentInformation.shared.canRequestAds,
                    loadsAlreadyBegan: loadsAlreadyBegan
                )
                if action == MonetizationPolicyKt.CONSENT_START_ADS
                    || action == MonetizationPolicyKt.CONSENT_START_AND_RELOAD {
                    IosAdHelper.shared.startSdk()
                }
                if action == MonetizationPolicyKt.CONSENT_START_AND_RELOAD {
                    IosAdHelper.shared.reloadAdsAfterTrackingDecision()
                }
            }
        }
    }

    private func publishPrivacyOptions() {
        let required = ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        IosAdHelper.shared.setPrivacyOptionsRequired(required)
    }
}
