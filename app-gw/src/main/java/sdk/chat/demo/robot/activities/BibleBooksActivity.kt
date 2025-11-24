package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.fragments.BibleBooksFragment
import sdk.chat.demo.robot.fragments.BiblePagerFragment

class BibleBooksActivity : BaseActivity() {
    companion object {
        private const val ARG_BOOK = "book_id"
        private const val ARG_REFERENCE = "reference"
        private const val ARG_FULLSCREEN = "fullscreen"

        // 提供静态启动方法（推荐）
        fun start(
            context: Context,
        ) {
            val intent = Intent(context, BibleBooksActivity::class.java)
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

        // 加载BibleChapterFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    BibleBooksFragment.newInstance()
                )
                .commit()
        }
    }
}