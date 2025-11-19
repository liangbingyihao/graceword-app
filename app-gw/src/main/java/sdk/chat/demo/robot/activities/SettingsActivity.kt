package sdk.chat.demo.robot.activities

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.BuildConfig
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.model.ExportInfo
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.extensions.LanguageUtils
import sdk.chat.demo.robot.handlers.BillingManager
import sdk.chat.demo.robot.handlers.LogUploader
import sdk.chat.demo.robot.utils.ToastHelper
import java.util.List

class SettingsActivity : BaseActivity(), View.OnClickListener {
    private lateinit var tvLang: TextView
    private lateinit var loadingDialog: AlertDialog
    private var exportInfo: ExportInfo? = null
    private lateinit var vVipHint: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        findViewById<View>(R.id.home).setOnClickListener(this)
        findViewById<View>(R.id.export).setOnClickListener(this)
        findViewById<View>(R.id.config_lang).setOnClickListener(this)
        findViewById<View>(R.id.feedback).setOnClickListener(this)
        findViewById<View>(R.id.my_userid).setOnClickListener(this)
        findViewById<View>(R.id.restore_subscription).setOnClickListener(this)
        vVipHint = findViewById<View>(R.id.export_vip)
        tvLang = findViewById<TextView>(R.id.lang_value)
        initView()
        getSettings()
        if(BuildConfig.DEBUG){
            var v = findViewById<View>(R.id.debug)
            v.visibility = View.VISIBLE
            v.setOnClickListener(this)
        }
        LogUploader.reportEvent(
            "mod_settings", listOf<KeyValuePair?>(
                KeyValuePair("settings_action", "0"),
            )
        )
    }

    override fun onResume() {
        super.onResume()
        if (BillingManager.getInstance().hasSubscriptions()) {
            vVipHint.visibility = View.GONE
        } else {
            vVipHint.visibility = View.VISIBLE
        }
    }


    override fun getLayout(): Int {
        return 0
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home -> {
                LogUploader.reportEvent(
                    "mod_settings", listOf<KeyValuePair?>(
                        KeyValuePair("settings_action", "50"),
                    )
                )
                finish()
            }

            R.id.export -> {
                var isVip = "0"
                if(!BillingManager.getInstance().tryToPay(this@SettingsActivity,"export_graceword")){
                    isVip = "1"
                    getExportInfo()
                }
                LogUploader.reportEvent(
                    "mod_settings", listOf<KeyValuePair?>(
                        KeyValuePair("settings_action", "20"),
                        KeyValuePair("isVip", isVip)
                    )
                )
            }

            R.id.config_lang -> {
                startActivity(
                    Intent(
                        this@SettingsActivity,
                        SettingLangsActivity::class.java
                    )
                )
                LogUploader.reportEvent(
                    "mod_settings", listOf<KeyValuePair?>(
                        KeyValuePair("settings_action", "10"),
                    )
                )
            }

            R.id.feedback -> {
                startActivity(
                    Intent(
                        this,
                        ComplaintActivity::class.java
                    )
                )
                LogUploader.reportEvent(
                    "mod_settings", listOf<KeyValuePair?>(
                        KeyValuePair("settings_action", "30"),
                    )
                )
            }

            R.id.debug -> {
                startActivity(
                    Intent(
                        this@SettingsActivity,
                        SpeechToTextActivity::class.java
                    )
                )
            }

            R.id.restore_subscription ->{
                showProgressDialog(R.string.processing)
                dm.add(
                    BillingManager.getInstance().acknowledgePurchase()
                        .subscribe({ success ->
//                    hideLoading()
                            dismissProgressDialog()
                            if (success) {
                                ToastHelper.show(this@SettingsActivity, "Success!")
                                LogUploader.reportEvent(
                                    "mod_purchase_page", listOf<KeyValuePair?>(
                                        KeyValuePair("purchase_entrance", "setting"),
                                        KeyValuePair("purchase_action", "41"),
                                    )
                                )
                            } else {
//                        showAcknowledgeFailed()
                                LogUploader.reportEvent(
                                    "mod_purchase_page", listOf<KeyValuePair?>(
                                        KeyValuePair("purchase_entrance", "setting"),
                                        KeyValuePair("purchase_action", "42"),
                                        KeyValuePair("purchase_error", "backend"),
                                    )
                                )
                            }
                        }, { error ->
//                    hideLoading()
                            dismissProgressDialog()
                            ToastHelper.show(this@SettingsActivity, error.message)
                            LogUploader.reportEvent(
                                "mod_purchase_page", listOf<KeyValuePair?>(
                                    KeyValuePair("purchase_entrance", "setting"),
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

            R.id.my_userid -> {
                var userId = ChatSDK.currentUserID()
                val clipboard =
                    getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("恩语", userId)
                clipboard.setPrimaryClip(clip)
                ToastHelper.show(
                    MainApp.getContext(),
                    MainApp.getContext().getString(R.string.copied)
                )
                LogUploader.reportEvent(
                    "mod_settings", listOf<KeyValuePair?>(
                        KeyValuePair("settings_action", "40"),
                    )
                )
            }
        }
    }

    fun getSettings() {
        try {
            var versionName = packageManager
                .getPackageInfo(packageName, 0)
                .versionName
                ?: "Unknown"
            findViewById<TextView>(R.id.version).text = getString(R.string.my_version,versionName)
        } catch (e: Exception) {
            "Unknown"
        }

        var lang = LanguageUtils.getSavedLanguage(this)
        var rid = 0
        when (lang) {
            "zh-Hans" -> {
                rid = R.string.lan_zh
            }

            "zh-Hant" -> {
                rid = R.string.lan_zh_hant
            }

            "en-US" -> {
                rid = R.string.lan_en
            }
        }
        if (rid > 0) {
            tvLang.setText(rid)
        } else {
            tvLang.text = LanguageUtils.getSystemLanguage()
        }

    }

    private fun initView() {

        loadingDialog = MaterialAlertDialogBuilder(this)
            .setMessage(R.string.setting_export)
            .setPositiveButton(R.string.export_open, null)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_background))
            .create()

        loadingDialog.setOnShowListener {
            // 获取按钮并自定义
            loadingDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.apply {
//                setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
                setTextColor(ContextCompat.getColor(context, R.color.item_text_selected))
                setOnClickListener {
                    if (exportInfo == null) {
                        loadingDialog.setMessage("null!")
                    } else {
                        exportInfo?.let {
                            try {
                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(exportInfo!!.downLoadUrl)
                                    )
                                // 确保只有浏览器能处理此Intent（排除其他可能的应用）
                                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                // 禁止弹出应用选择对话框（直接使用默认浏览器）
                                intent.setPackage(null)
                                startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                ToastHelper.show(this@SettingsActivity, "No browser available")
                            }
                        }
                        loadingDialog.dismiss()
                    }
                }
            }

            loadingDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.apply {
                setTextColor(ContextCompat.getColor(context, R.color.item_text_normal))
            }
        }

    }


    fun getExportInfo() {
        exportInfo = null;
        loadingDialog.setMessage(getString(R.string.setting_export_pending))
        loadingDialog.show()
        dm.add(
            ImageApi.getExploreInfo()
                .subscribeOn(Schedulers.io()) // Specify database operations on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Results return to main thread
                .subscribe(
                    { exportInfo ->

//                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
//                        val clip = ClipData.newPlainText("恩语", exportInfo.downLoadUrl)
//                        clipboard.setPrimaryClip(clip)
//                        ToastHelper.show(
//                            this@SettingsActivity,
//                            R.string.export_hint
//                        )
                        showExportMenus(exportInfo)
                    },
                    { error -> // onError
                        ToastHelper.show(
                            this@SettingsActivity,
                            error.message
                        )
                        loadingDialog.setMessage(getString(R.string.failed_and_retry))
                    })
        )

    }

    fun showExportMenus(exportInfo: ExportInfo) {
        this.exportInfo = exportInfo
        loadingDialog.setMessage(getString(R.string.setting_export_done))
    }
}