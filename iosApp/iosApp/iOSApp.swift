import SwiftUI

@main
struct iOSApp: App {

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
                    // Consent and ATT wait until this fires and the root view is in a window.
                    IosConsentCoordinator.shared.startIfReady()
                }
        }
    }
}
