package sdk.chat.demo.robot.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.handlers.AuthService
import sdk.chat.demo.robot.handlers.BillingManager
import sdk.guru.common.RX
import siyamed.shapeimageview.PorterShapeImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.drawable.toDrawable
import sdk.chat.demo.robot.utils.ToastHelper

class AccountActivity : BaseActivity(), View.OnClickListener {
    private lateinit var tvGetVip: TextView
    private lateinit var tvUserName: TextView
//    private lateinit var loadingDialog: AlertDialog
//    private lateinit var vVipHint: View
//    private var exportInfo: ExportInfo? = null
//    private var contactEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        findViewById<View>(R.id.home).setOnClickListener(this)

    }

    private fun initView(){
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
            tvUserName = findViewById<TextView>(R.id.user_name)
            tvUserName.text = lastUser.displayName
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
                    renewal.text = getString(R.string.vip_expired_time, timeStr)
                } catch (e: Exception) {
                    renewal.visibility = View.INVISIBLE
                }
            } else {
                getVip.visibility = View.VISIBLE
                vipStatus.visibility = View.GONE
                renewal.visibility = View.INVISIBLE
                findViewById<View>(R.id.vip_status_container).setOnClickListener(this)
            }
            findViewById<View>(R.id.log_out).setOnClickListener(this)
            tvUserName.setOnClickListener(this)
        }
    }

    override fun onResume() {
        super.onResume()
        initView()
    }


    override fun getLayout(): Int {
        return 0
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.home -> {
                finish()
            }

            R.id.vip_status_container -> {
                BillingActivity.start(this@AccountActivity, "account")
            }

            R.id.user_name -> {
                showCustomInputDialog()
            }

            R.id.log_out -> {

                dm.add(
                    AuthService.logout()
                        .observeOn(RX.main())
                        .andThen(AuthService.authenticate())
                        .observeOn(RX.main())
                        .doFinally {
                            startActivity(Intent(this, MainDrawerActivity::class.java))
                            finish()
                        }
                        .subscribe(
                            {
                                Log.e("AuthService","logout success")
                                ToastHelper.show(
                                    this@AccountActivity,
                                    "Logout success"
                                )
                            },
                            { error -> // onError
                                Log.e("AuthService","logout..${error.message}")
                                ToastHelper.show(
                                    this@AccountActivity,
                                    error.message
                                )
                            })
                )
            }

        }
    }

    fun showCustomInputDialog() {
        // 创建自定义布局
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_username, null)

        val editText = dialogView.findViewById<EditText>(R.id.main_content)
        editText.setText(tvUserName.text)
        editText.setSelection(tvUserName.text.length)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.submit)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        // 设置确认按钮点击事件
        btnConfirm.setOnClickListener {
            val inputText = editText.text.toString().trim()

            if (inputText.isNotEmpty()) {
                // 处理输入内容
//                handleInput(inputText)
                setDisplayName(inputText)
                dialog.dismiss()
            } else {
                // 输入为空时的提示
                editText.error = "no input"
                ToastHelper.show(this, "input your display name...")
            }
        }
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog.show()

        // 自动弹出键盘
        editText.postDelayed({
            editText.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 300)

    }

    private fun setDisplayName(displayName: String) {
        if (displayName.isEmpty()) {
            return
        }
        dm.add(
            AuthService.setDisplayName(displayName)
                .observeOn(RX.main())
                .subscribe(
                    {
                        ToastHelper.show(
                            this@AccountActivity,
                            "success"
                        )
                        tvUserName.text = displayName
                    },
                    { error -> // onError
                        ToastHelper.show(
                            this@AccountActivity,
                            error.message
                        )
                    })
        )
    }
}