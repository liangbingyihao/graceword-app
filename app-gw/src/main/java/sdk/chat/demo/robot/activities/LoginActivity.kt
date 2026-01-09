package sdk.chat.demo.robot.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gyf.immersionbar.ImmersionBar
import kotlinx.coroutines.launch
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.model.ApiTokenRequest
import sdk.chat.demo.robot.extensions.showMaterialConfirmationDialog
import sdk.chat.demo.robot.handlers.AuthService
import sdk.chat.demo.robot.handlers.AuthService.authenticate
import sdk.chat.demo.robot.handlers.GoogleIdentityManager
import sdk.chat.demo.robot.utils.ToastHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import sdk.guru.common.RX


class LoginActivity : BaseActivity(), View.OnClickListener {
    private var dialogImportData: View? = null
    private var tokenReq: ApiTokenRequest? = null
    private var cbImport: CheckBox? = null
    private var cbNotImport: CheckBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ImmersionBar.with(this)
                .titleBar(findViewById<View>(R.id.title_bar))
                .init()
        } else {
            ImmersionBar.with(this).init()
        }
        findViewById<View>(R.id.back).setOnClickListener(this)
        findViewById<View>(R.id.login).setOnClickListener(this)
        findViewById<View>(R.id.btn_confirm).setOnClickListener(this)
        setupAgreementText()
        dialogImportData = findViewById<View>(R.id.dialog_data_import)
        dialogImportData?.let { dialog ->
            dialog.visibility = View.GONE
            dialog.findViewById<View>(R.id.btn_confirm)?.setOnClickListener(this)
            cbImport = dialog.findViewById<CheckBox>(R.id.rb_import)
            cbNotImport = dialog.findViewById<CheckBox>(R.id.rb_not_import)
            cbImport?.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    // 当选中一个时，取消选中其他所有 CheckBox
                    cbNotImport?.isChecked = false
                }
            }
            cbNotImport?.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    // 当选中一个时，取消选中其他所有 CheckBox
                    cbImport?.isChecked = false
                }
            }
        } ?: run {
            // 处理对话框未找到的情况
        }
    }

    override fun getLayout(): Int {
        return 0
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.back -> {
                finish()
            }

            R.id.btn_confirm -> {
                dialogImportData?.visibility = View.GONE
                if (cbImport?.isChecked == true) {
                    tokenReq?.binding = true
                }
                sendApiTokenRequest()
            }

            R.id.login -> {
                lifecycleScope.launch {
                    tokenReq = null
                    val result = GoogleIdentityManager.getGoogleIdCredential(this@LoginActivity)
                    if (result.isFailure) {
                        val errorMessage = result.exceptionOrNull()?.message ?: "登录失败，请重试"
                        ToastHelper.show(this@LoginActivity, errorMessage)
                    } else {
                        var ret = result.getOrNull()
                        if (ret != null) {
                            tokenReq = ApiTokenRequest(
                                googleId = ret.id,
                                googleToken = ret.idToken
                            )
                            handleGoogleIdCredential()
//                            showProgressDialog("登录中")
//                            try {
//                                AuthService.authenticate(
//                                    ApiTokenRequest(
//                                        googleId = ret.id,
//                                        googleToken = ret.idToken
//                                    )
//                                ).blockingAwait()
//                                startActivity(
//                                    Intent(
//                                        this@LoginActivity,
//                                        MainDrawerActivity::class.java
//                                    )
//                                )
//                                finish()
//                            } catch (e: Exception) {
//                                ToastHelper.show(this@LoginActivity, e.message)
//                            }
//                            dismissProgressDialog()
                        }
                    }
                }

            }
        }
    }

    private fun handleGoogleIdCredential() {
        tokenReq?.let {
            if (dialogImportData != null && !AuthService.exitsGoogleId(it.googleId)) {
                Log.e("AuthService", "!AuthService exitsGoogleId ${it.googleId}")
                dialogImportData!!.visibility = View.VISIBLE
            } else {
                Log.e("AuthService", "AuthService exitsGoogleId ${it.googleId}")
                sendApiTokenRequest()
            }
        }
    }

    private fun sendApiTokenRequest() {
        showProgressDialog("登录中")
        dm.add(
            authenticate(
                tokenReq
            )
                .observeOn(RX.main())
                .doFinally { dismissProgressDialog() }
                .subscribe(
                    {
                        if (AuthService.isOauthAlreadyLinked()) {
                            showBindingError()
                        } else {
                            startActivity(Intent(this, MainDrawerActivity::class.java))
                            finish()
                        }
                    },
                    { error -> // onError
                        Toast.makeText(
                            this@LoginActivity,
                            error.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    })
        )
    }

    private fun showBindingError() {

        val dialog = MaterialAlertDialogBuilder(this@LoginActivity)
            .setMessage(R.string.account_already_exists)
            .setPositiveButton(R.string.get_it) { dialog, _ ->
                dialog.dismiss()
            }
            .setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_background))
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            // 只设置 Positive 按钮的样式
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setTextColor(ContextCompat.getColor(context, R.color.item_text_selected))
            }
        }
        dialog.setOnDismissListener {
            startActivity(Intent(this, MainDrawerActivity::class.java))
            finish()
        }

        dialog.show()
    }

    private fun setupAgreementText() {
        val textView = findViewById<TextView>(R.id.tv_agreement)

        // 获取多语言文本
        val fullText = getString(R.string.login_agreement)
        val privacyText = getString(R.string.privacy_policy)
        val termsText = getString(R.string.terms_of_service)

        // 创建 SpannableString
        val spannable = SpannableString(fullText)

        // 设置隐私政策可点击
        val privacyStart = fullText.indexOf(privacyText)
        if (privacyStart != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onPrivacyPolicyClicked()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    // 自定义样式
                    ds.color = ContextCompat.getColor(this@LoginActivity, R.color.user_agreement)
                    ds.isUnderlineText = false  // 取消下划线
                }
            }
            spannable.setSpan(
                clickableSpan,
                privacyStart,
                privacyStart + privacyText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 设置使用条款可点击
        val termsStart = fullText.indexOf(termsText)
        if (termsStart != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    onTermsOfUseClicked()
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(this@LoginActivity, R.color.user_agreement)
                    ds.isUnderlineText = false
                }
            }
            spannable.setSpan(
                clickableSpan,
                termsStart,
                termsStart + termsText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // 设置到 TextView
        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()  // 启用点击
        textView.highlightColor = Color.TRANSPARENT  // 移除点击后的背景色
    }

    private fun onPrivacyPolicyClicked() {
        // 打开隐私政策
        var configs = ImageApi.getGwConfigs()

        if (configs != null && configs.privacyAgreement?.isEmpty() != true) {
            WebViewActivity.launchWithUrl(
                this@LoginActivity,
                configs.privacyAgreement,
                getString(R.string.privacy_policy)
            )
        }
    }

    private fun onTermsOfUseClicked() {
        // 打开使用条款
        var configs = ImageApi.getGwConfigs()

        if (configs != null && configs.termOfService?.isEmpty() != true) {
            WebViewActivity.launchWithUrl(
                this@LoginActivity,
                configs.termOfService,
                getString(R.string.terms_of_service)
            )
        }
    }

    private fun openWebPage(url: String, title: String) {
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra("url", url)
            putExtra("title", title)
        }
        startActivity(intent)
    }
}