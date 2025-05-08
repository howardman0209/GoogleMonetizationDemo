package com.demo.google.monetization

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.demo.google.monetization.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "@MainActivity"
        private const val MAXIMUM_BILLING_SERVICE_CONNECTION_RETRY_COUNT = 3
        private const val GOOGLE_PLAY_MONETIZATION_BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqgRKRX8YBn2seOegQHB+kzmwsg+MKMKXk9rBlG1X0DuZj68VNcoMtjsJXaSLttzIbYtkNUZgUTQiCMWm39T9X2Cuc8wQsPRmhT8cqaHN9erDnL/WM4bWONiBVcWnsVCDXMws1GHdFmBPFGZWxm9gjn+wXO7mPyY/yOna5BiKEfyRHwFasU/TgxjSoloqVeeTG7HWrc545G0FGEAqhBtJt85hN4KCvbAE+OQ0AVKZkHH8MJa3pRrhqrDn07Xyzi/DdWT4zmWSY9xYLaceLhoVXoXusfZ18ZHwm9z5aM+T8zH7FTNJnXG92q8tFajNmwWJCCo6X6Fg9cEQh6x1Nm7eFQIDAQAB"
    }

    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var progressStart = 0L
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private val googleMobileAdsConsentManager by lazy { GoogleMobileAdsConsentManager.getInstance(applicationContext) }

    private val productListAdapter = ProductListAdapter(object : ProductListAdapter.ProductViewListener {
        override fun onProductSelected(productDetails: ProductDetails) {
            Log.d(TAG, "onProductSelected - productDetails: $productDetails")
            sendPurchaseRequest(productDetails)
        }
    })

    private var billingServiceConnectionRetryCount = 0

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Log.d(TAG, "purchasesUpdatedListener - billingResult: $billingResult, purchases: $purchases")
        lifecycleScope.launch {
            showLoadingIndicator(true)
            queryPurchases()
            showLoadingIndicator(false)
        }

        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.find { it.purchaseState == Purchase.PurchaseState.PURCHASED }?.let { purchase ->
                Log.d(TAG, "purchase.products: ${purchase.products}")
                Log.d(TAG, "purchase.originalJson: ${purchase.originalJson}")
                val verified = verifyPurchase(purchase)
                Log.d(TAG, "verified: $verified, purchase: $purchase")
                if (verified) {
                    grantingEntitlement(purchase)
                } else {
                    showToast(getString(R.string.toast_message_purchase_cannot_be_verified))
                }
            } ?: {
                showToast(getString(R.string.toast_message_payment_not_success))
            }
        } else {
            showToast(getString(R.string.toast_message_payment_not_success))
        }
    }

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.rvProducts.adapter = productListAdapter

        binding.fabRestorePurchase.setOnClickListener {
            showLoadingIndicator(true)
            lifecycleScope.launch {
                val purchases = queryPurchases()

                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && verifyPurchase(purchase)) {
                        grantingEntitlement(purchase)
                    }
                }
                showLoadingIndicator(false) {
                    showToast(getString(R.string.toast_message_purchase_restored))
                }
            }
        }

        binding.btnFeatureAfterAds.setOnClickListener {
            loadAndShowRewardedAd { rewardItem ->
                Log.d(TAG, "rewardItem: $rewardItem")
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                Log.d(TAG, "rewardType: $rewardType, rewardAmount: $rewardAmount")
                updateCoinsDisplay(Preference.incrementAndGetNumberOfCoin(applicationContext, 1))
                showToast(getString(R.string.toast_message_coin_granted))
                Log.d(TAG, "User earned the reward.")
            }
        }

        binding.btnUseCoin.setOnClickListener {
            if (Preference.getNumberOfCoin(applicationContext) < 1) {
                showToast(getString(R.string.toast_message_not_enough_coin))
                return@setOnClickListener
            }

            updateCoinsDisplay(Preference.decrementAndGetNumberOfCoin(applicationContext, 1))
        }

        updateCoinsDisplay(Preference.getNumberOfCoin(applicationContext))

        establishBillingServiceConnection()

        // Ads
        googleMobileAdsConsentManager.gatherConsent(this) { error ->
            if (error != null) {
                // Consent not obtained in current session.
                Log.d(TAG, "${error.errorCode}: ${error.message}")
            }

            if (googleMobileAdsConsentManager.canRequestAds) {
                initializeMobileAdsSdk()
            }
        }

        if (googleMobileAdsConsentManager.canRequestAds) {
            initializeMobileAdsSdk()
        }
    }

    private fun updateNoAdsBadge(noAds: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.tvNoAds.visibility = if (noAds) View.VISIBLE else View.GONE
        }
    }

    private fun updateCoinsDisplay(numberOfCoin: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.tvCoins.text = String.format(getString(R.string.label_coin_display), numberOfCoin)
        }
    }

    private fun establishBillingServiceConnection() {
        Log.d(TAG, "establishBillingServiceConnection, retry: $billingServiceConnectionRetryCount")
        if (billingServiceConnectionRetryCount == 0) showLoadingIndicator(true)

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished: $billingResult")

                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        handleSuccessBillingServiceConnection()
                    }

                    BillingClient.BillingResponseCode.NETWORK_ERROR,
                    BillingClient.BillingResponseCode.ERROR,
                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                        if (billingServiceConnectionRetryCount++ < MAXIMUM_BILLING_SERVICE_CONNECTION_RETRY_COUNT) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                delay(3000)
                                establishBillingServiceConnection()
                            }
                        } else {
                            handleFailBillingServiceConnection()
                        }
                    }

                    else -> handleFailBillingServiceConnection()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "onBillingServiceDisconnected")
            }
        })
    }

    private fun handleSuccessBillingServiceConnection() {
        Log.d(TAG, "handleSuccessBillingServiceConnection")
        showLoadingIndicator(false)

        lifecycleScope.launch(Dispatchers.Main) {
            queryProductDetails()
            val purchases = queryPurchases()
            val purchasedNoAds = purchases.any {
                it.purchaseState == Purchase.PurchaseState.PURCHASED
                        && it.products.any { productId -> productId == InAppProduct.REMOVE_ADS.productId }
            }
            Preference.saveNoAds(applicationContext, purchasedNoAds)

            updateNoAdsBadge(purchasedNoAds)
        }
    }

    private fun handleFailBillingServiceConnection() {
        Log.d(TAG, "handleFailBillingServiceConnection")
        showLoadingIndicator(false)

        lifecycleScope.launch(Dispatchers.Main) {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setCancelable(false)
                .setTitle("Billing service issue")
                .setMessage("Please try again later")
                .setPositiveButton("Confirm") { _, _ ->
                    onBackPressed()
                }
                .show()
        }
    }

    private suspend fun queryProductDetails() {
        val productList = InAppProduct.entries.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val productDetails = withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params)
        }

        Log.d(TAG, "productDetails: $productDetails")
        productDetails.productDetailsList?.let {
            productListAdapter.setProducts(it)
        }
    }

    private suspend fun queryPurchases(): List<Purchase> {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val purchasesResult = withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(params)
        }

        Log.d(TAG, "purchasesResult: $purchasesResult")
        productListAdapter.setPurchases(purchasesResult.purchasesList)
        return purchasesResult.purchasesList
    }

    private fun sendPurchaseRequest(productDetails: ProductDetails) {
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(this, billingFlowParams)
    }

    private fun verifyPurchase(purchase: Purchase): Boolean {
        return Security.verifyPurchase(
            publicKeyBase64 = GOOGLE_PLAY_MONETIZATION_BASE64_PUBLIC_KEY,
            signedData = purchase.originalJson,
            signature = purchase.signature
        )
    }

    private fun grantingEntitlement(purchase: Purchase) {
        fun unlockNoAds() {
            Log.d(TAG, "unlockNoAds")
            val purchasedNoAds = purchase.products.any { it == InAppProduct.REMOVE_ADS.productId }
            if (purchasedNoAds) {
                Preference.saveNoAds(applicationContext, true)
                updateNoAdsBadge(true)
                removeBannerAd()
                Log.d(TAG, "No ads unlocked")
            }
        }

        fun grantCoins() {
            Log.d(TAG, "grantCoins")
            val purchasedCoins = purchase.products.any { it == InAppProduct.COIN_X2.productId }
            if (purchasedCoins) {
                updateCoinsDisplay(Preference.incrementAndGetNumberOfCoin(applicationContext, 2))
                Log.d(TAG, "coins added")
            }
        }

        val isConsumable = purchase.products.firstOrNull()?.let {
            val inAppProduct = InAppProduct.byProductId(it)
            inAppProduct.isConsumable
        } == true

        // check purchased product consumable
        if (isConsumable) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    grantCoins()
                }
            }
        } else {
            // check purchase is already acknowledged
            if (purchase.isAcknowledged) {
                unlockNoAds()
                return
            }

            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    unlockNoAds()
                }
            }
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Set your test devices.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(listOf(TEST_DEVICE_HASHED_ID)).build()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity) {}
            withContext(Dispatchers.Main) {
                loadBannerAd()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadBannerAd() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (!Preference.getNoAds(applicationContext)) {
                Log.d(TAG, "load ad")
                val adRequest = AdRequest.Builder().build()
                binding.bannerAdView.visibility = View.VISIBLE
                binding.bannerAdView.loadAd(adRequest)
            }
        }
    }

    private fun removeBannerAd() {
        lifecycleScope.launch(Dispatchers.Main) {
            Log.d(TAG, "remove banner ad")
            binding.bannerAdView.visibility = View.GONE
        }
    }

    private fun loadAndShowRewardedAd(onRewardGrant: (rewardItem: RewardItem) -> Unit) {
        showLoadingIndicator(true)
        loadRewardedAd { loadedAd ->
            showLoadingIndicator(false)
            if (loadedAd == null) {
                showToast(getString(R.string.toast_message_fail_to_load_ads))
                return@loadRewardedAd
            }

            showRewardedVideo(loadedAd, onRewardGrant)
        }
    }

    private fun loadRewardedAd(onCompleted: ((ad: RewardedAd?) -> Unit)? = null) {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            SAMPLE_REWARDED_VIDEO_AD_UNIT_AD,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    onCompleted?.invoke(null)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad was loaded.")
                    onCompleted?.invoke(ad)
                }
            },
        )
    }

    private fun showRewardedVideo(loadedRewardedAd: RewardedAd, onRewardGrant: (rewardItem: RewardItem) -> Unit) {
        lifecycleScope.launch(Dispatchers.Main) {
            val loadedAd = loadedRewardedAd
            loadedAd.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d(TAG, "Ad failed to show.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                        // Called when ad is dismissed.
                    }
                }

            loadedAd.show(this@MainActivity) { rewardItem ->
                // Handle the reward.
                onRewardGrant.invoke(rewardItem)
            }
        }
    }

    private fun showToast(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoadingIndicator(show: Boolean, onDismiss: (() -> Unit)? = null) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (show) {
                binding.loadingIndicator.visibility = View.VISIBLE
                progressStart = System.currentTimeMillis()
            } else {
                val processTime = System.currentTimeMillis() - progressStart
                Log.d("ProgressDialog", "processTime: $processTime")
                if (processTime < 500) {
                    delay(500L - processTime)
                    binding.loadingIndicator.visibility = View.GONE
                } else {
                    binding.loadingIndicator.visibility = View.GONE
                }
                onDismiss?.invoke()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingClient.endConnection()
    }
}