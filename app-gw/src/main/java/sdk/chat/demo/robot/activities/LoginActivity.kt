package sdk.chat.demo.robot.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.gyf.immersionbar.ImmersionBar
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.EditCardActivity
import sdk.chat.demo.robot.api.model.ApiTokenRequest
import sdk.chat.demo.robot.handlers.AuthService
import sdk.chat.demo.robot.handlers.GoogleIdentityManager
import sdk.chat.demo.robot.utils.ToastHelper


class LoginActivity : BaseActivity(), View.OnClickListener {

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

            R.id.login -> {
                lifecycleScope.launch {
                    val result = GoogleIdentityManager.getGoogleIdCredential(this@LoginActivity)
                    if (result.isFailure) {
                        val errorMessage = result.exceptionOrNull()?.message ?: "登录失败，请重试"
                        ToastHelper.show(this@LoginActivity, errorMessage)
                    } else {
                        var ret = result.getOrNull()
                        if (ret != null) {
                            showProgressDialog("登录中")
                            try {
                                AuthService.authenticate(
                                    ApiTokenRequest(
                                        googleId = ret.id,
                                        googleToken = ret.idToken
                                    )
                                ).blockingAwait()
                                startActivity(
                                    Intent(
                                        this@LoginActivity,
                                        MainDrawerActivity::class.java
                                    )
                                )
                                finish()
                            } catch (e: Exception) {
                                ToastHelper.show(this@LoginActivity, e.toString())
                            }
                            dismissProgressDialog()
                        }
                    }
                }

//                lifecycleScope.launch {
//                    Glide.with(this@LoginActivity)
//                        .downloadOnly()
//                        .load("adapter.getUrlAt(position - 2)?.backgroundUrl") // 预加载下一页
//                        .preload()
//                }
            }
        }
    }

}