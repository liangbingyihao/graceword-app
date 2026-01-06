package sdk.chat.demo.robot.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.extensions.LanguageUtils
import sdk.chat.demo.robot.handlers.AuthService
import sdk.chat.demo.robot.handlers.BillingManager
import sdk.chat.demo.robot.handlers.LogUploader
import sdk.chat.demo.robot.utils.ToastHelper
import siyamed.shapeimageview.PorterShapeImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountActivity : BaseActivity(), View.OnClickListener {
    private lateinit var tvGetVip: TextView
    private lateinit var tvLang: TextView
//    private lateinit var loadingDialog: AlertDialog
//    private lateinit var vVipHint: View
//    private var exportInfo: ExportInfo? = null
//    private var contactEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        findViewById<View>(R.id.home).setOnClickListener(this)

        var lastUser = AuthService.getLastLoginUser()
        if (lastUser != null && !lastUser.isGuest) {
            var imAvatar = findViewById<PorterShapeImageView>(R.id.avatar)
            Glide.with(this@AccountActivity)
                .load(lastUser.avatarUrl)
                .skipMemoryCache(false)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.color.bg_bill_menu)
                .error(R.mipmap.ic_launcher)
                .into(imAvatar)
            findViewById<TextView>(R.id.user_name).text = lastUser.displayName
            var vipStatus = findViewById<TextView>(R.id.vip_status)
            var getVip = findViewById<TextView>(R.id.get_vip)
            var renewal = findViewById<TextView>(R.id.renewal_time)
            if (lastUser.membershipActive) {
                getVip.visibility = View.GONE
                vipStatus.visibility = View.VISIBLE
                renewal.visibility = View.VISIBLE
                try {
                    var timeStr = SimpleDateFormat(
                        "yyyy/MM/dd",
                        Locale.getDefault()
                    ).format(Date(lastUser.membershipExpiredAt * 1000))
                    renewal.text = getString(R.string.next_renewal_time, timeStr)
                } catch (e: Exception) {
                    renewal.visibility = View.INVISIBLE
                }
            } else {
                getVip.visibility = View.VISIBLE
                vipStatus.visibility = View.GONE
                renewal.visibility = View.INVISIBLE
            }
        }
        findViewById<View>(R.id.log_out).setOnClickListener(this)
//        findViewById<View>(R.id.config_lang).setOnClickListener(this)
//        findViewById<View>(R.id.feedback).setOnClickListener(this)
//        findViewById<View>(R.id.my_userid).setOnClickListener(this)
//        findViewById<View>(R.id.contact_email).setOnClickListener(this)
//        findViewById<View>(R.id.restore_subscription).setOnClickListener(this)
//        vVipHint = findViewById<View>(R.id.export_vip)
//        tvLang = findViewById<TextView>(R.id.lang_value)
//        initView()
//        getSettings()
//        if (BuildConfig.DEBUG) {
//            var v = findViewById<View>(R.id.debug)
//            v.visibility = View.VISIBLE
//            v.setOnClickListener(this)
//        }
//        if (ImageApi.getGwConfigs() != null) {
//            contactEmail = ImageApi.getGwConfigs().contactEmail
//            findViewById<TextView>(R.id.email).setText(contactEmail)
//        }
//        LogUploader.reportEvent(
//            "mod_settings", listOf<KeyValuePair?>(
//                KeyValuePair("settings_action", "0"),
//            )
//        )
    }

    override fun onResume() {
        super.onResume()
        if (BillingManager.getInstance().hasSubscriptions()) {
        }
    }


    override fun getLayout(): Int {
        return 0
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home -> {
                finish()
            }


            R.id.log_out -> {
                startActivity(
                    Intent(
                        this@AccountActivity,
                        SettingLangsActivity::class.java
                    )
                )
                LogUploader.reportEvent(
                    "mod_settings", listOf<KeyValuePair?>(
                        KeyValuePair("settings_action", "10"),
                    )
                )
            }

        }
    }


}