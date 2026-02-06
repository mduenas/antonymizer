import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Set up the native ad provider before creating the main view controller
        NativeAdProviderHolder.shared.provider = IosAdHelper.shared

        let viewController = MainViewControllerKt.MainViewController()

        // Set the root view controller for ad presentation
        DispatchQueue.main.async {
            IosAdHelper.shared.setRootViewController(viewController)
        }

        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Update root view controller reference if needed
        IosAdHelper.shared.setRootViewController(uiViewController)
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}



