package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.fragments.BibleBooksFragment
import sdk.chat.demo.robot.fragments.BiblePagerFragment

class BibleBooksActivity : BaseActivity() {
    companion object {
        private const val ARG_BOOK = "book_id"
        private const val ARG_CHAPTER = "chapter_id"

        // 提供静态启动方法（推荐）
        fun start(
            context: Context,
            bookId: Int = 0,
            chapterNumber: Int = 0
        ) {
            val intent = Intent(context, BibleBooksActivity::class.java).apply {
                putExtra(ARG_BOOK, bookId)
                putExtra(ARG_CHAPTER, chapterNumber)
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

        var bookId = intent.getIntExtra(ARG_BOOK, -1)
        var chapterNumber = intent.getIntExtra(ARG_CHAPTER, -1)

//        Log.e("bible_data","bible index:$bookId,$chapterNumber")

        // 加载BibleChapterFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    BibleBooksFragment.newInstance(bookId, chapterNumber)
                )
                .commit()
        }
    }
}