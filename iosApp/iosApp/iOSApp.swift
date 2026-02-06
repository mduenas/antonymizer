import SwiftUI
import AppTrackingTransparency
import GoogleMobileAds

@main
struct iOSApp: App {

    init() {
        // Initialize AdMob early
        IosAdHelper.shared.initialize()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
                    // Request ATT permission when app becomes active (required for personalized ads)
                    requestTrackingPermission()
                }
        }
    }

    private func requestTrackingPermission() {
        // Only request if not determined yet
        if ATTrackingManager.trackingAuthorizationStatus == .notDetermined {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                ATTrackingManager.requestTrackingAuthorization { status in
                    print("ATT status: \(status.rawValue)")
                }
            }
        }
    }
}