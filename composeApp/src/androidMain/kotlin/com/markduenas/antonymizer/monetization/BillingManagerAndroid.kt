package com.markduenas.antonymizer.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.markduenas.antonymizer.data.source.SettingsStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Android implementation of BillingManager using Google Play Billing Library.
 */
class BillingManagerAndroid(
    context: Context,
    private val settingsStorage: SettingsStorage,
    private val scope: CoroutineScope
) : BillingManager {

    companion object {
        private const val TAG = "BillingManager"
    }

    private val contextRef = WeakReference(context)
    private var activityRef: WeakReference<Activity>? = null

    private var billingClient: BillingClient? = null
    private var removeAdsProductDetails: ProductDetails? = null

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.NotInitialized)
    override val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    override val adsRemoved: StateFlow<Boolean> = settingsStorage.adsRemoved
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    private val _removeAdsPrice = MutableStateFlow<String?>(null)
    override val removeAdsPrice: StateFlow<String?> = _removeAdsPrice.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        handlePurchasesUpdated(billingResult, purchases)
    }

    fun setActivity(activity: Activity?) {
        activityRef = activity?.let { WeakReference(it) }
    }

    override fun initialize() {
        val context = contextRef.get() ?: return

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    _purchaseState.value = PurchaseState.Ready
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _purchaseState.value = PurchaseState.Unavailable(
                        billingResult.debugMessage ?: "Billing setup failed"
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                _purchaseState.value = PurchaseState.Unavailable("Billing service disconnected")
                // Try to reconnect
                startConnection()
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingManager.PRODUCT_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                removeAdsProductDetails = productDetailsList.firstOrNull()
                removeAdsProductDetails?.let { product ->
                    val price = product.oneTimePurchaseOfferDetails?.formattedPrice
                    _removeAdsPrice.value = price ?: "$2.99"
                    Log.d(TAG, "Product details loaded: ${product.productId}, price: $price")
                }
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
                // Use default price as fallback
                _removeAdsPrice.value = "$2.99"
            }
        }
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            } else {
                Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    processPurchases(purchases)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
                _purchaseState.value = PurchaseState.PurchaseError("Purchase cancelled")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                _purchaseState.value = PurchaseState.PurchaseError(
                    billingResult.debugMessage ?: "Purchase failed"
                )
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.products.contains(BillingManager.PRODUCT_REMOVE_ADS)) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    // Grant entitlement
                    scope.launch {
                        settingsStorage.setAdsRemoved(true)
                    }

                    // Acknowledge purchase if not already acknowledged
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }

                    _purchaseState.value = PurchaseState.PurchaseSuccess
                    Log.d(TAG, "Remove Ads purchase granted")
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    override fun purchaseRemoveAds() {
        val productDetails = removeAdsProductDetails
        val activity = activityRef?.get()
        val client = billingClient

        if (productDetails == null || activity == null || client == null) {
            Log.e(TAG, "Cannot launch purchase: productDetails=$productDetails, activity=$activity")
            _purchaseState.value = PurchaseState.PurchaseError("Purchase not available")
            return
        }

        _purchaseState.value = PurchaseState.Purchasing

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = client.launchBillingFlow(activity, billingFlowParams)

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${result.debugMessage}")
            _purchaseState.value = PurchaseState.PurchaseError(
                result.debugMessage ?: "Failed to launch purchase"
            )
        }
    }

    override fun restorePurchases() {
        queryExistingPurchases()
    }

    override fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
    }
}
