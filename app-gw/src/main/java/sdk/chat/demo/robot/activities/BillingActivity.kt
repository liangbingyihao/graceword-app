package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.gyf.immersionbar.ImmersionBar
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.fragments.BillingFragment
import sdk.chat.demo.robot.handlers.BillingManager
import sdk.chat.demo.robot.utils.ToastHelper

class BillingActivity : BaseActivity() {
    companion object {
        private const val EXTRA_FROM = "chat_from"

        // 提供静态启动方法（推荐）
        fun start(context: Context, from: String? = null, isAuto: Boolean = false): Boolean {
            var isVip = BillingManager.getInstance().hasSubscriptions()
            if (!isVip && !BillingManager.getInstance().isInitialized()) {
                if (!isAuto) {
                    ToastHelper.show(context, R.string.check_google_play_network)
                }
                return false
            } else if (isVip && isAuto) {
                return true
            }
            val intent = Intent(context, BillingActivity::class.java).apply {
                putExtra(EXTRA_FROM, from)
            }
            context.startActivity(intent)
            return true
        }
    }

    override fun getLayout(): Int {
        return 0;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing)
        ImmersionBar.with(this).init()
        Log.e("BillingManager", "BillingActivity.onCreate")
        var from = intent.getStringExtra(EXTRA_FROM) ?: ""

//        // 设置Toolbar
//        val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        setSupportActionBar(toolbar)
//        val reference = intent.getStringExtra(ARG_REFERENCE).toString()

        // 加载BibleChapterFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BillingFragment.newInstance(from))
                .commit()
        }
    }
}