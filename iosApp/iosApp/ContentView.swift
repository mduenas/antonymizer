import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        NativeAdProviderHolder.shared.provider = IosAdHelper.shared
        NativeBillingProviderHolder.shared.provider = IosBillingHelper.shared

        let viewController = MainViewControllerKt.MainViewController()
        IosAdHelper.shared.setRootViewController(viewController)
        IosConsentCoordinator.shared.setRootViewController(viewController)
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        IosAdHelper.shared.setRootViewController(uiViewController)
        IosConsentCoordinator.shared.setRootViewController(uiViewController)
        // No-ops until the view's window is attached and the app is active.
        IosConsentCoordinator.shared.startIfReady()
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .onAppear {
                IosConsentCoordinator.shared.startIfReady()
            }
    }
}
