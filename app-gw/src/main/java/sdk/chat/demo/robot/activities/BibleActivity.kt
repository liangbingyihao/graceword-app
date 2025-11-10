package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.fragments.BibleChapterFragment

class BibleActivity : BaseActivity() {
    companion object {
        private const val ARG_REFERENCE = "reference"

        // 提供静态启动方法（推荐）
        fun start(context: Context, reference:String="") {
            val intent = Intent(context, BibleActivity::class.java).apply {
                putExtra(ARG_REFERENCE, reference)
            }
            context.startActivity(intent)
        }
    }

    override fun getLayout(): Int {
        return 0;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawableResource(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bible)

//        // 设置Toolbar
//        val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        setSupportActionBar(toolbar)
        val reference = intent.getStringExtra(ARG_REFERENCE).toString()

        // 加载BibleChapterFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, BibleChapterFragment.newInstance(reference = reference))
                .commit()
        }
    }
}