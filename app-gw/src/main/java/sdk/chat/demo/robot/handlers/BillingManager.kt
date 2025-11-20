package sdk.chat.demo.robot.handlers

import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vojtkovszky.billinghelper.BillingEvent
import com.vojtkovszky.billinghelper.BillingHelper
import com.vojtkovszky.billinghelper.BillingListener
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.tinylog.Logger
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.BuildConfig
import sdk.chat.demo.robot.activities.BillingActivity
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.ImageApi.getServerConfigs
import sdk.chat.demo.robot.api.model.ActionConfig
import sdk.chat.demo.robot.api.model.GWConfigs
import sdk.chat.demo.robot.api.model.Product
import java.io.IOException
import java.util.Objects

class BillingManager private constructor() {
    companion object {
        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager().also { instance = it }
            }
        }

        val actionLimits = listOf(
            ActionConfig(ActionConfig.DAILY_MSG, 5),
            ActionConfig(ActionConfig.DAILY_PIC, 1)
        ).associate { it.actionName to it.dailyLimit }
        private var URL_ACKNOWLEDGE = ImageApi.URL2_MAIN + "purchase/play"
        private var URL_PRODUCT = ImageApi.URL2_MAIN + "purchase/plans"
    }

    // 内存缓存
    private var billingHelper: BillingHelper? = null
    private var productSubscriptions: List<String>? = null
    private var isInitialized = false
    private var initializationInProgress = false
    private var hasSubscriptions = false
    private var expireAtMs = 0L
    private var gson: Gson = Gson()

    private var _productGW: Product? = null
    val productGW: Product?
        get() = _productGW
    // 状态管理
    private val billingStateSubject = BehaviorSubject.create<BillingState>()

    init {
        billingStateSubject.onNext(BillingState.NotInitialized)
    }



    // 初始化 BillingHelper（依赖网络配置）
    fun initializeBilling(): Single<BillingHelper> {
        return Single.defer {
            synchronized(this) {
                if (isInitialized) {
                    // 已经初始化，直接返回缓存的 BillingHelper
                    billingHelper?.let { Single.just(it) }
                        ?: Single.error(IllegalStateException("BillingHelper not available"))
                } else if (initializationInProgress) {
                    // 初始化正在进行中，等待结果
                    billingStateSubject
                        .filter { it is BillingState.Initialized || it is BillingState.Error }
                        .firstOrError()
                        .flatMap { state ->
                            when (state) {
                                is BillingState.Initialized -> Single.just(state.billingHelper)
                                is BillingState.Error -> Single.error(state.error)
                                else -> Single.error(IllegalStateException("Unexpected state"))
                            }
                        }
                } else {
                    // 开始初始化流程
                    initializationInProgress = true
                    billingStateSubject.onNext(BillingState.Loading)

                    // 1. 先获取服务器配置
                    getGWProducts()
                        .flatMap { products ->

                            // 3. 创建 BillingHelper
                            createBillingHelper(products.plans.map { it -> it.productId })
                        }
                        .doOnSuccess { billingHelper ->
                            synchronized(this) {
                                this.billingHelper = billingHelper
                                this.isInitialized = true
                                this.initializationInProgress = false
                                billingStateSubject.onNext(BillingState.Initialized(billingHelper))
                            }
                            if (!hasSubscriptions()) {
                                acknowledgePurchase(restore = true)
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(
                                        { /* 成功，不处理 */ },
                                        { error -> /* 失败，可记录日志但不影响主流程 */ }
                                    )
                            }
                        }
                        .doOnError { error ->
                            synchronized(this) {
                                this.initializationInProgress = false
                                billingStateSubject.onNext(BillingState.Error(error))
                            }
                        }
                        .subscribeOn(Schedulers.io())
                }
            }
        }
    }

    // 获取 BillingHelper（如果未初始化则自动初始化）
    fun getBillingHelper(): Single<BillingHelper> {
        return initializeBilling()
    }

    fun checkSubscriptions() {
        hasSubscriptions = (billingHelper?.hasSubscriptions() == true)
    }

    fun hasSubscriptions(): Boolean {
        return expireAtMs > System.currentTimeMillis()
    }

    fun setExpiredAt(expireAt: Long) {
        expireAtMs = maxOf(expireAtMs, System.currentTimeMillis() + expireAt)
        ChatSDK.events().source().accept(NetworkEvent(EventType.BillChange));
    }

    fun tryToPay(context: Context, from: String): Boolean {
        if (!hasSubscriptions()) {
            BillingActivity.start(context, from)
            return true
        }
        return false;
    }

    // 重新初始化（强制刷新配置）
    fun reinitializeBilling(): Single<BillingHelper> {
        return Single.defer {
            synchronized(this) {
                // 清空缓存
                billingHelper?.let {
                    // 关闭旧的 BillingHelper 连接
                    it.billingClient.endConnection()
                }
                billingHelper = null
                _productGW = null
                isInitialized = false

                // 重新初始化
                initializeBilling()
            }
        }
    }

    // 监听 Billing 状态
    fun observeBillingState(): Observable<BillingState> {
        return billingStateSubject
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .distinctUntilChanged()
    }


    // 创建 BillingHelper 实例
    private val defaultProductSubscriptions =
        listOf("com.graceword.sub.yearly", "com.graceword.sub.quarterly")

    private fun createBillingHelper(
        productSubscriptions: List<String>?
    ): Single<BillingHelper> {
        return Single.create { emitter ->
            try {
                val billingHelper = BillingHelper(
                    context = MainApp.getContext(),
                    productInAppPurchases = emptyList(),
                    productSubscriptions = if (productSubscriptions.isNullOrEmpty()) {
                        defaultProductSubscriptions
                    } else {
                        productSubscriptions
                    },
                    startConnectionImmediately = true,
                    autoAcknowledgePurchases = false,
                    enableLogging = BuildConfig.DEBUG,
                    billingListener = object : BillingListener {
                        override fun onBillingEvent(
                            event: BillingEvent,
                            message: String?,
                            responseCode: Int?,
                            subResponseCode: Int?
                        ) {
                            handleBillingEvent(event, message, responseCode, subResponseCode)
                        }
                    }
                )
                emitter.onSuccess(billingHelper)
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    private fun handleBillingEvent(
        event: BillingEvent,
        message: String?,
        responseCode: Int?,
        subResponseCode: Int?
    ) {
        Log.e("BillingManager", event.name + "," + message)
        when (event) {
            BillingEvent.PURCHASE_COMPLETE,
            BillingEvent.QUERY_OWNED_PURCHASES_COMPLETE -> {
                if (hasSubscriptions != (billingHelper?.hasSubscriptions() == true)) {
                    hasSubscriptions = !hasSubscriptions
                    ChatSDK.events().source().accept(NetworkEvent(EventType.BillChange));
                }
            }

            BillingEvent.PURCHASE_ACKNOWLEDGE_SUCCESS -> {
                billingHelper?.initQueryOwnedPurchases()
            }

            BillingEvent.QUERY_PRODUCT_DETAILS_COMPLETE -> {
//                Logger.error {
//                    billingHelper?.getProductDetails("com.graceword.sub.yearly").toString()
//                }
//                Logger.error {
//                    billingHelper?.getProductDetails("com.graceword.sub.quarterly").toString()
//                }
            }

            else -> {

            }
//            BillingEvent.BILLING_CONNECTED -> TODO()
//            BillingEvent.BILLING_CONNECTION_FAILED -> TODO()
//            BillingEvent.BILLING_DISCONNECTED -> TODO()
//            BillingEvent.QUERY_PRODUCT_DETAILS_COMPLETE -> TODO()
//            BillingEvent.QUERY_PRODUCT_DETAILS_FAILED -> TODO()
//            BillingEvent.QUERY_OWNED_PURCHASES_COMPLETE -> TODO()
//            BillingEvent.QUERY_OWNED_PURCHASES_FAILED -> TODO()
//            BillingEvent.PURCHASE_COMPLETE -> TODO()
//            BillingEvent.PURCHASE_FAILED -> TODO()
//            BillingEvent.PURCHASE_CANCELLED -> TODO()
//            BillingEvent.PURCHASE_ACKNOWLEDGE_SUCCESS -> TODO()
//            BillingEvent.PURCHASE_ACKNOWLEDGE_FAILED -> TODO()
//            BillingEvent.CONSUME_PURCHASE_SUCCESS -> TODO()
//            BillingEvent.CONSUME_PURCHASE_FAILED -> TODO()
        }
    }

    // 检查是否已初始化
    fun isInitialized(): Boolean = isInitialized

    // 清空缓存
    fun clearCache() {
        synchronized(this) {
            billingHelper?.billingClient?.endConnection()
            billingHelper = null
            _productGW = null
            isInitialized = false
            billingStateSubject.onNext(BillingState.NotInitialized)
        }
    }

    fun acknowledgePurchase(purchase: Purchase? = null, restore: Boolean = false): Single<Boolean> {
        return Single.create { emitter ->
            try {
                val dstPurchase = purchase
                    ?: billingHelper?.getProductNamesForType(BillingClient.ProductType.SUBS)
                        ?.firstNotNullOfOrNull { product ->
                            billingHelper!!.getPurchasesWithProductName(product)
                                .maxByOrNull { it.purchaseTime }
                        }

                if (dstPurchase == null || dstPurchase.originalJson.isEmpty() || dstPurchase.signature.isEmpty()) {
                    emitter.onError(IllegalArgumentException("Invalid purchase data"))
                    return@create
                }

                val params = mutableMapOf<String, String>().apply {
                    put("tz", GWApiManager.timeZoneId)
                    put("json_purchase_info", dstPurchase.originalJson)
                    put("signature", dstPurchase.signature)
                    if (restore) {
                        put("restore", "true")
                    }
                }

                val request = GWApiManager.buildPostRequest(params, URL_ACKNOWLEDGE)

                GWApiManager.shared().client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            response.use { // 确保 response 被正确关闭
                                if (!response.isSuccessful) {
                                    emitter.onError(IOException("HTTP ${response.code}: ${response.message}"))
                                    return
                                }

                                val responseBody = response.body?.string() ?: ""
                                if (responseBody.isEmpty()) {
                                    emitter.onError(IOException("Empty response body"))
                                    return
                                }

                                // 解析响应
                                val jsonObject = try {
                                    gson.fromJson(responseBody, JsonObject::class.java)
                                } catch (e: Exception) {
                                    emitter.onError(IOException("Invalid JSON response: ${e.message}"))
                                    return
                                }

                                val code = jsonObject.getAsJsonPrimitive("code")?.asString
                                if (code == "OK") {
                                    emitter.onSuccess(true)
                                    setExpiredAt(1800000)
                                } else {
                                    val errorMessage =
                                        jsonObject.getAsJsonPrimitive("message")?.asString
                                            ?: "Acknowledge failed with code: $code"
//                                    emitter.onSuccess(false)
                                    emitter.onError(IOException(errorMessage))
                                }
                            }
                        } catch (e: Exception) {
                            if (!emitter.isDisposed) {
                                emitter.onError(e)
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun getGWProducts(): Single<Product?> {
        return Single.create<Product?> { emitter ->
            try {

                val request = Request.Builder()
                    .url(requireNotNull(URL_PRODUCT.toHttpUrlOrNull()))
                    .build()

                GWApiManager.shared().client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            response.use { // 确保 response 被正确关闭
                                var ret = GWApiManager.shared()
                                    .handleResponse(response, Product::class.java)
                                if(ret!=null){
                                    _productGW = ret
                                }
                                if(_productGW!=null){
                                    emitter.onSuccess(_productGW!!)
                                }else{
                                    emitter.onError(Exception("no products"))
                                }
                            }
                        } catch (e: Exception) {
                            if (!emitter.isDisposed) {
                                emitter.onError(e)
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    // Billing 状态密封类
    sealed class BillingState {
        object NotInitialized : BillingState()
        object Loading : BillingState()
        data class Initialized(val billingHelper: BillingHelper) : BillingState()
        data class Error(val error: Throwable) : BillingState()
    }
}