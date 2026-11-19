import Foundation
import StoreKit
import ComposeApp

/// StoreKit 2 for the remove-ads non-consumable. Kotlin calls in through
/// NativeBillingProvider. Entitlement writes carry a generation so a launch
/// check that finishes late cannot clear a purchase that already landed.
@objc public final class IosBillingHelper: NSObject, NativeBillingProvider {

    public static let shared = IosBillingHelper()

    private let generations = EntitlementGeneration()
    private var productId = "remove_ads"
    private var product: Product?
    private var updatesTask: Task<Void, Never>?
    private var started = false
    private var purchaseInFlight = false

    private override init() {
        super.init()
    }

    public func start(productId: String) {
        Task { @MainActor in
            await self.startOnMain(productId: productId)
        }
    }

    public func purchaseRemoveAds() {
        Task { @MainActor in
            guard !self.purchaseInFlight else { return }
            self.purchaseInFlight = true
            await self.purchaseOnMain()
            self.purchaseInFlight = false
        }
    }

    public func restorePurchases() {
        Task { @MainActor in
            await self.restoreOnMain()
        }
    }

    @MainActor
    private func startOnMain(productId: String) async {
        self.productId = productId
        listenForUpdates()
        guard !started else { return }
        started = true
        await loadProduct()
        _ = await refreshEntitlements(adjust: nil)
    }

    @MainActor
    private func listenForUpdates() {
        guard updatesTask == nil else { return }
        updatesTask = Task { [weak self] in
            for await update in Transaction.updates {
                await self?.handle(update)
            }
        }
    }

    @MainActor
    private func loadProduct() async {
        do {
            let products = try await Product.products(for: [productId])
            guard let product = products.first(where: { $0.id == productId }) else {
                report(state: "unavailable", message: "Remove Ads is not available right now")
                return
            }
            self.product = product
            IosBillingBridge.shared.priceUpdated(price: product.displayPrice)
            report(state: "ready", message: nil)
        } catch {
            report(state: "unavailable", message: error.localizedDescription)
        }
    }

    @MainActor
    private func purchaseOnMain() async {
        // Drop any in-flight launch snapshot before the sheet returns.
        _ = await generations.next()
        guard let product else {
            _ = await refreshEntitlements(adjust: nil)
            report(state: "unavailable", message: "Remove Ads is not available right now")
            return
        }
        report(state: "purchasing", message: nil)
        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                let transaction: Transaction
                do {
                    transaction = try checkVerified(verification)
                } catch {
                    _ = await refreshEntitlements(adjust: nil)
                    report(state: "error", message: "Purchase could not be verified")
                    return
                }
                await transaction.finish()
                let applied = await refreshEntitlements(adjust: { owns in
                    owns || (transaction.revocationDate == nil && transaction.productID == self.productId)
                })
                switch applied {
                case .applied(true):
                    report(state: "success", message: nil)
                case .applied(false):
                    report(state: "error", message: "Purchase could not be verified")
                case .superseded:
                    // Don't stay on Purchasing if the updates listener won the race
                    // and did not itself report a state.
                    await reportVerifiedPurchase(transaction)
                }
            case .userCancelled:
                // The purchase bumped the generation, which drops an in-flight launch
                // check. Read entitlements again, then leave the button retryable.
                _ = await refreshEntitlements(adjust: nil)
                report(state: "cancelled", message: "Purchase cancelled")
            case .pending:
                // Ask to Buy. A decline sends no Transaction.updates event, so this
                // state must keep the purchase button enabled.
                _ = await refreshEntitlements(adjust: nil)
                report(state: "pending", message: "Waiting for approval")
            @unknown default:
                _ = await refreshEntitlements(adjust: nil)
                report(state: "error", message: "Purchase failed")
            }
        } catch {
            _ = await refreshEntitlements(adjust: nil)
            report(state: "error", message: error.localizedDescription)
        }
    }

    /// The entitlement walk was superseded. Record this verified transaction under
    /// a new generation, and always leave Purchasing.
    @MainActor
    private func reportVerifiedPurchase(_ transaction: Transaction) async {
        let generation = await generations.next()
        guard await generations.current() == generation else {
            report(state: "ready", message: nil)
            return
        }
        let owned = transaction.revocationDate == nil && transaction.productID == productId
        IosBillingBridge.shared.entitlementUpdated(adsRemoved: owned, generation: generation)
        report(state: owned ? "success" : "ready", message: nil)
    }

    @MainActor
    private func restoreOnMain() async {
        _ = await generations.next()
        var syncError: String?
        do {
            try await AppStore.sync()
        } catch {
            // A StoreKit configuration file often has no App Store account.
            // Still read currentEntitlements before surfacing the error.
            syncError = error.localizedDescription
        }
        switch await refreshEntitlements(adjust: nil) {
        case .applied(true):
            report(state: "success", message: nil)
        case .applied(false):
            if let syncError {
                report(state: "error", message: syncError)
            } else {
                report(state: "error", message: "No previous purchase found")
            }
        case .superseded:
            // A newer walk owns the result. Don't claim the purchase is missing.
            report(state: "ready", message: nil)
        }
    }

    @MainActor
    private func handle(_ result: VerificationResult<Transaction>) async {
        let transaction: Transaction
        do {
            transaction = try checkVerified(result)
        } catch {
            return
        }
        guard transaction.productID == productId else {
            await transaction.finish()
            return
        }
        let generation = await generations.next()
        guard await generations.current() == generation else {
            await transaction.finish()
            // The generation check lost a race. Re-read so a revocation is not dropped.
            if case .applied(let owns) = await refreshEntitlements(adjust: nil) {
                report(state: owns ? "success" : "ready", message: nil)
            }
            return
        }
        let owned = transaction.revocationDate == nil
        IosBillingBridge.shared.entitlementUpdated(adsRemoved: owned, generation: generation)
        if owned {
            report(state: "success", message: nil)
        } else if product != nil {
            // A refund must not leave the button disabled on PurchaseSuccess.
            report(state: "ready", message: nil)
        }
        await transaction.finish()
    }

    /// Re-reads currentEntitlements under a new generation. A superseded walk
    /// writes nothing, so an older launch check cannot clear a newer purchase.
    /// Individual unverified transactions are skipped. They do not fail the walk.
    @MainActor
    private func refreshEntitlements(
        adjust: ((Bool) -> Bool)?
    ) async -> EntitlementRefresh {
        let generation = await generations.next()
        let snapshot = await readCurrentEntitlement()
        guard await generations.current() == generation else { return .superseded }
        let owned = adjust?(snapshot) ?? snapshot
        IosBillingBridge.shared.entitlementUpdated(adsRemoved: owned, generation: generation)
        return .applied(owned)
    }

    private func readCurrentEntitlement() async -> Bool {
        var owns = false
        for await result in Transaction.currentEntitlements {
            guard let transaction = try? checkVerified(result) else { continue }
            if transaction.productID == productId && transaction.revocationDate == nil {
                owns = true
            }
        }
        return owns
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .verified(let value):
            return value
        case .unverified:
            throw StoreKitBillingError.unverified
        }
    }

    private func report(state: String, message: String?) {
        IosBillingBridge.shared.stateUpdated(state: state, message: message)
    }
}

private enum EntitlementRefresh {
    case applied(Bool)
    case superseded
}

private enum StoreKitBillingError: Error {
    case unverified
}

/// Serial generation counter. Bumping it invalidates a launch walk that is
/// still reading currentEntitlements.
private actor EntitlementGeneration {
    private var value: Int64 = 0

    func next() -> Int64 {
        value += 1
        return value
    }

    func current() -> Int64 {
        value
    }
}
