package sdk.chat.demo.robot.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vojtkovszky.billinghelper.BillingEvent
import com.vojtkovszky.billinghelper.BillingHelper
import com.vojtkovszky.billinghelper.BillingListener
import com.vojtkovszky.billinghelper.SubscriptionPurchaseParams
import org.tinylog.Logger
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.BuildConfig
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.WebViewActivity
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.api.model.Plan
import sdk.chat.demo.robot.api.model.Product
import sdk.chat.demo.robot.extensions.toMeaningfulStr
import sdk.chat.demo.robot.extensions.toPriceInfo
import sdk.chat.demo.robot.handlers.BillingManager
import sdk.chat.demo.robot.handlers.LogUploader
import sdk.chat.demo.robot.utils.ToastHelper

class BillingFragment : BaseFragment(), BillingListener, View.OnClickListener {
    private lateinit var product1: View
    private lateinit var product2: View
    private lateinit var headerView: ImageView
    private lateinit var btStart: MaterialButton
    private var billing: BillingHelper? = null
    private var selectedProduct: String = ""
    private var pendingPurchase: Boolean = false
    private var product: Product? = null
    private var from: String = ""

    companion object {
        // 片段参数键
        private const val ARG_FROM = "from"

        // 创建新实例，可传入初始章节参数
        fun newInstance(
            from: String = ""
        ): BillingFragment {
            val fragment = BillingFragment()
            val args = Bundle()
            args.putString(ARG_FROM, from)
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (!pendingPurchase && BillingManager.getInstance().hasSubscriptions()) {
            showCustomDialog(R.layout.dialog_billing_already, true)
        }
    }

    override fun getLayout(): Int {
        return R.layout.fragment_billing
    }

    override fun initViews() {
        btStart = rootView.findViewById<MaterialButton>(R.id.button_start_trial)
        btStart.setOnClickListener(this)
        rootView.findViewById<View>(R.id.exit).setOnClickListener(this)
        rootView.findViewById<View>(R.id.text_restore).setOnClickListener(this)
        rootView.findViewById<View>(R.id.text_terms).setOnClickListener(this)
        rootView.findViewById<View>(R.id.text_privacy).setOnClickListener(this)
        headerView = rootView.findViewById<ImageView>(R.id.photoView)
    }

    private fun initPlanView(root: View, product: Plan?, isFirst: Boolean) {
        if (product != null) {
            var productDetails = billing?.getProductDetails(product.productId)
            val offerDetails = productDetails?.subscriptionOfferDetails?.getOrNull(0)
            if (offerDetails == null) {
                root.visibility = View.GONE
                return
            } else {
                root.visibility = View.VISIBLE
            }


            var title = root.findViewById<TextView>(R.id.text_title)
            var price = root.findViewById<TextView>(R.id.text_price)
//            title.text = product.offerTitle
//            price.text = product.offerSubtitle

//            val priceInfo = offerDetails.pricingPhases.toPriceInfo(product)
            val priceInfo = product.toMeaningfulStr(offerDetails.pricingPhases)
            if (priceInfo.isEmpty()) {
                return
            }
            title.text = priceInfo[0]
            if (priceInfo.size > 1) {
                price.text = priceInfo[1]
            } else {
                price.visibility = View.GONE
            }
            var badge = root.findViewById<TextView>(R.id.badge_save)
            var hasOffer = offerDetails.pricingPhases.pricingPhaseList.size > 1
            if(hasOffer){
                product.startButton = product.offerStartButton
            }
            if (isFirst) {
                if (hasOffer && !product.offerPromotion.isEmpty()) {
                    badge.text = product.offerPromotion
                    title.setTextColor(ContextCompat.getColor(root.context, R.color.bg_bill_menu))
                } else if (!hasOffer && !product.promotion.isEmpty()) {
                    badge.text = product.promotion
                } else {
                    badge.visibility = View.GONE
                }
            }else{
                title.setTextColor(ContextCompat.getColor(root.context, R.color.item_text_normal))
                badge.visibility = View.GONE
            }
//            if (!product.promotion.isEmpty()) {
//                title.setTextColor(ContextCompat.getColor(root.context, R.color.bg_bill_menu))
//                badge.text = product.promotion
//            } else {
//                title.setTextColor(ContextCompat.getColor(root.context, R.color.item_text_normal))
//                badge.visibility = View.GONE
//            }
            root.setOnClickListener(this)


        } else {
            root.visibility = View.GONE
        }
    }

    private fun initProductView() {
        product?.let {
            Glide.with(headerView)
                .load(it.headerImage)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.mipmap.ic_products) // 占位图
                .error(R.mipmap.ic_products) // 错误图
                .into(headerView)
        }

        if (product != null) {
            product1 = rootView.findViewById<View>(R.id.product1_container)
            product2 = rootView.findViewById<View>(R.id.product2_container)
            var containers = listOf<View>(
                product1, product2
            )

            for (i in 0 until 2) {
                val plan = product!!.plans.getOrNull(i)
                initPlanView(containers[i], plan, i == 0)
            }

            product1.performClick()
        }

    }

    override fun clearData() {
    }

    override fun reloadData() {
    }

    override fun onDestroy() {
        super.onDestroy()
        // make sure to clean it up when you're done
        billing?.removeBillingListener(this)
//        billing?.endClientConnection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dm.dispose()
    }

    override fun onBillingEvent(
        event: BillingEvent,
        message: String?,
        responseCode: Int?,
        subResponseCode: Int?
    ) {
        Log.e("BillingManager", activity?.packageName + "," + event.toString() + message)
        when (event) {
            BillingEvent.PURCHASE_COMPLETE -> {
                acknowledge()
            }

            BillingEvent.BILLING_CONNECTION_FAILED,
            BillingEvent.QUERY_PRODUCT_DETAILS_FAILED,
            BillingEvent.PURCHASE_FAILED,
            BillingEvent.PURCHASE_CANCELLED
                -> {
                val currentActivity = activity ?: return
                ToastHelper.show(currentActivity, message)
                dismissProgressDialog()
                var action = ""
                if (event == BillingEvent.PURCHASE_FAILED) {
                    action = "32"
                } else if (event == BillingEvent.PURCHASE_CANCELLED) {
                    action = "34"
                }
                if (action != "") {
                    LogUploader.reportEvent(
                        "mod_purchase_page", listOf<KeyValuePair?>(
                            KeyValuePair("purchase_entrance", from),
                            KeyValuePair("purchase_action", action),
                            KeyValuePair("product_id", selectedProduct),
                            KeyValuePair(
                                "purchase_error",
                                "responseCode:$responseCode,message:$message"
                            ),
                        )
                    )
                }
            }
//            BillingEvent.BILLING_DISCONNECTED -> TODO()
//            BillingEvent.QUERY_PRODUCT_DETAILS_COMPLETE -> TODO()
//            BillingEvent.QUERY_OWNED_PURCHASES_COMPLETE -> TODO()
//            BillingEvent.QUERY_OWNED_PURCHASES_FAILED -> TODO()
//            BillingEvent.PURCHASE_ACKNOWLEDGE_SUCCESS -> TODO()
//            BillingEvent.PURCHASE_ACKNOWLEDGE_FAILED -> TODO()
//            BillingEvent.CONSUME_PURCHASE_SUCCESS -> TODO()
//            BillingEvent.CONSUME_PURCHASE_FAILED -> TODO()
            else -> {
                dismissProgressDialog()
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = super.onCreateView(inflater, container, savedInstanceState)
        initViews()
        arguments?.let {
            from = it.getString(ARG_FROM, "")
        } ?: run {
            from = ""
        }
        LogUploader.reportEvent(
            "mod_purchase_page", listOf<KeyValuePair?>(
                KeyValuePair("purchase_entrance", from),
                KeyValuePair("purchase_action", "10")
            )
        )
        getProducts()
        return rootView
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        product1 = view.findViewById<View>(R.id.product1)
//        product2 = view.findViewById<View>(R.id.product2)
//
//        product1.setOnClickListener(this)
//        product2.setOnClickListener(this)
//        view.findViewById<View>(R.id.button_start_trial).setOnClickListener(this)
//        view.findViewById<View>(R.id.exit).setOnClickListener(this)
//
//    }

    private fun setSelectPlan(index: Int): Boolean {
        val plan = product?.plans?.getOrNull(index)
        if (plan != null) {
            selectedProduct = plan.productId
            btStart.text = plan.startButton

            LogUploader.reportEvent(
                "mod_purchase_page", listOf<KeyValuePair?>(
                    KeyValuePair("purchase_entrance", from),
                    KeyValuePair("purchase_action", "20"),
                    KeyValuePair("product_id", selectedProduct)
                )
            )
            return true
        }
        return false
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.product1_container -> {
                if (setSelectPlan(0)) {
                    product1.isSelected = true
                    product2.isSelected = false
                }
            }

            R.id.product2_container -> {
                if (setSelectPlan(1)) {
                    product2.isSelected = true
                    product1.isSelected = false
                }
            }

            R.id.button_start_trial -> {
                if (!selectedProduct.isEmpty()) {
                    LogUploader.reportEvent(
                        "mod_purchase_page", listOf<KeyValuePair?>(
                            KeyValuePair("purchase_entrance", from),
                            KeyValuePair("purchase_action", "30"),
                            KeyValuePair("product_id", selectedProduct)
                        )
                    )
                    activity?.let {
                        pendingPurchase = true

                        val dstPurchase = billing?.getSubscriptions()
                        var subscriptionParams: SubscriptionPurchaseParams? = null
                        var obfuscatedAccountId = ChatSDK.currentUserID().replace("user_", "")
                        if (dstPurchase != null) {
                            subscriptionParams = SubscriptionPurchaseParams(
                                updateOldToken = dstPurchase.purchaseToken,
                                updateReplacementMode = BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION
                            )
                            obfuscatedAccountId = dstPurchase.accountIdentifiers?.obfuscatedAccountId
                                ?: obfuscatedAccountId
                            if (BuildConfig.DEBUG) {
                                Logger.error { "launchPurchaseFlow,dstPurchase:${dstPurchase.originalJson}" }
                            } else {
                                Logger.error { "launchPurchaseFlow, with dstPurchase:${dstPurchase.orderId}" }
                            }
                        } else {
                            Logger.error { "launchPurchaseFlow,dstPurchase is null" }
                        }
//                        subscriptionParams = null
                        showProgressDialog(R.string.processing)
                        billing?.launchPurchaseFlow(
                            it,
                            selectedProduct,
                            obfuscatedAccountId = ChatSDK.currentUserID().replace("user_", ""),
                            isOfferPersonalized = true,
                            subscriptionParams = subscriptionParams
                        )
                    }
                }else{
                    ToastHelper.show(activity,"No products...")
                }
            }

            R.id.text_restore -> {
//                billing?.getProductNamesForType(BillingClient.ProductType.SUBS)?.map { it ->
//                    var ps = billing?.getPurchasesWithProductName(it)
//                    if (ps != null) {
//                        Log.e(
//                            "BillingManager",
//                            activity?.packageName + ",acknowledgePurchases " + ps.size
//                        )
//                        billing?.acknowledgePurchases(ps)
//                    }
//                }

                LogUploader.reportEvent(
                    "mod_purchase_page", listOf<KeyValuePair?>(
                        KeyValuePair("purchase_entrance", from),
                        KeyValuePair("purchase_action", "40"),
                    )
                )
                showProgressDialog(R.string.processing)
                dm.add(
                    BillingManager.getInstance().acknowledgePurchase(restore = true)
                        .subscribe({ success ->
//                    hideLoading()
                            dismissProgressDialog()
                            if (success) {
                                ToastHelper.show(activity, "Success!")
                                activity?.finish()
                                LogUploader.reportEvent(
                                    "mod_purchase_page", listOf<KeyValuePair?>(
                                        KeyValuePair("purchase_entrance", from),
                                        KeyValuePair("purchase_action", "41"),
                                    )
                                )
                            } else {
//                        showAcknowledgeFailed()
//                                showAcknowledgeErrorDialog()
                                LogUploader.reportEvent(
                                    "mod_purchase_page", listOf<KeyValuePair?>(
                                        KeyValuePair("purchase_entrance", from),
                                        KeyValuePair("purchase_action", "42"),
                                        KeyValuePair("purchase_error", "backend"),
                                    )
                                )
                            }
                        }, { error ->
//                    hideLoading()
                            dismissProgressDialog()
//                            showAcknowledgeErrorDialog()
                            ToastHelper.show(activity, error.message)
                            LogUploader.reportEvent(
                                "mod_purchase_page", listOf<KeyValuePair?>(
                                    KeyValuePair("purchase_entrance", from),
                                    KeyValuePair("purchase_action", "42"),
                                    KeyValuePair(
                                        "purchase_error",
                                        error.message ?: "Unknown error"
                                    ),
                                )
                            )
//                    showNetworkError(error)
                        })
                )
            }

            R.id.text_terms -> {
                if (product != null) {
                    var title = (p0 as TextView).text.toString()
                    WebViewActivity.launchWithUrl(p0.context, product?.termOfService, title)
                }
            }

            R.id.text_privacy -> {
                if (product != null) {
                    var title = (p0 as TextView).text.toString()
                    WebViewActivity.launchWithUrl(p0.context, product?.privacyAgreement, title)
                }
            }

            R.id.exit -> {
                LogUploader.reportEvent(
                    "mod_purchase_page", listOf<KeyValuePair?>(
                        KeyValuePair("purchase_entrance", from),
                        KeyValuePair("purchase_action", "50"),
                    )
                )
                activity?.finish()
            }
        }

    }


    fun acknowledge() {
        var purchaseList = billing?.getPurchasesWithProductName(selectedProduct)
        var purchase = purchaseList?.maxByOrNull { it.purchaseTime }
//        if (BuildConfig.DEBUG && "test" == from && purchase != null) {
//            purchase = Purchase(purchase.originalJson, "test")
//        }
        if (purchase != null) {
            dm.add(
                BillingManager.getInstance().acknowledgePurchase(purchase)
                    .subscribe({ success ->
//                    hideLoading()
                        dismissProgressDialog()
                        if (success) {
                            showCustomDialog()
                            LogUploader.reportEvent(
                                "mod_purchase_page", listOf<KeyValuePair?>(
                                    KeyValuePair("purchase_entrance", from),
                                    KeyValuePair("purchase_action", "31"),
                                    KeyValuePair("product_id", selectedProduct)
                                )
                            )
                        } else {
                            showAcknowledgeErrorDialog()
                            LogUploader.reportEvent(
                                "mod_purchase_page", listOf<KeyValuePair?>(
                                    KeyValuePair("purchase_entrance", from),
                                    KeyValuePair("purchase_action", "33"),
                                    KeyValuePair("product_id", selectedProduct)
                                )
                            )
                        }
                    }, { error ->
//                    hideLoading()
                        dismissProgressDialog()
                        showAcknowledgeErrorDialog()
                        ToastHelper.show(activity, error.message)
                        LogUploader.reportEvent(
                            "mod_purchase_page", listOf<KeyValuePair?>(
                                KeyValuePair("purchase_entrance", from),
                                KeyValuePair("purchase_action", "33"),
                                KeyValuePair("product_id", selectedProduct)
                            )
                        )
//                    showNetworkError(error)
                    })
            )
        }
    }


    fun getProducts() {
        product = BillingManager.getInstance().productGW
//        dm.add(
//            BillingManager.getInstance().getGWProducts()
//                .subscribe({ productRes ->
//                    product = productRes
//                    initProductView()
//                }, { error ->
////                    hideLoading()
//                    ToastHelper.show(activity, error.message)
////                    showNetworkError(error)
//                })
//        )


        dm.add(
            BillingManager.getInstance().getBillingHelper()
                .subscribe({ billingHelper ->
                    billing = billingHelper
                    billing?.addBillingListener(this)
                    initProductView()
                }, { error ->
                    // 初始化失败
                })
        )
    }

    fun showAcknowledgeErrorDialog() {
        var v = showCustomDialog(R.layout.dialog_billing_already)
        if (v != null) {
            (v.findViewById<TextView?>(R.id.main_content)).setText(R.string.vip_already2)
        }
    }

    fun showCustomDialog(
        resource: Int = R.layout.dialog_billing_success,
        autoFinish: Boolean = true,
    ): View? {

        val currentActivity = activity ?: return null

        val dialogView =
            LayoutInflater.from(currentActivity).inflate(resource, null)

        val dialog = MaterialAlertDialogBuilder(currentActivity)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        // 设置按钮点击事件
        dialogView.findViewById<View>(R.id.got).setOnClickListener {
            dialog.dismiss()
        }
        if (autoFinish&&!"test".equals(from)) {
            dialog.setOnDismissListener { activity?.finish() }
        }

        dialog.show()

        return dialogView

    }
}