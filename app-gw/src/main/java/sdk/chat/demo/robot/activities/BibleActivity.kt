package sdk.chat.demo.robot.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.fragments.BibleBooksFragment
import sdk.chat.demo.robot.fragments.BibleDataProvider
import sdk.chat.demo.robot.fragments.BiblePagerFragment

class BibleActivity : BaseActivity(),BibleDataProvider {
    companion object {
        private const val ARG_BOOK = "book_id"
        private const val ARG_REFERENCE = "reference"
        private const val ARG_FULLSCREEN = "fullscreen"
        private const val ARG_CHAPTER_NUMBER = "chapter_number"

        // 提供静态启动方法（推荐）
        fun start(
            context: Context,
            reference: String = "",
            fullscreen: Boolean = false,
            bookId: Int = 0,
            chapterNumber: Int = 0
        ) {
            val intent = Intent(context, BibleActivity::class.java).apply {
                putExtra(ARG_BOOK, bookId)
                putExtra(ARG_REFERENCE, reference)
                putExtra(ARG_FULLSCREEN, fullscreen)
                putExtra(ARG_CHAPTER_NUMBER, chapterNumber)
            }
            context.startActivity(intent)
        }
    }

    override fun getLayout(): Int {
        return 0;
    }

    private var fullscreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawableResource(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bible)

//        // 设置Toolbar
//        val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        setSupportActionBar(toolbar)
        var reference = intent.getStringExtra(ARG_REFERENCE).toString()
        if (!reference.isEmpty()) {
            reference = reference.removeSurrounding("(", ")")
        }

        fullscreen = intent.getBooleanExtra(ARG_FULLSCREEN, false)

        var bookId = intent.getIntExtra(ARG_BOOK, 0)
        var chapterNumber = intent.getIntExtra(ARG_CHAPTER_NUMBER, 0)

        // 加载BibleChapterFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    BiblePagerFragment.newInstance(
                        bookId,
                        chapterNumber=chapterNumber,
                        reference = reference,
                        fullscreen = fullscreen
                    )
                )
                .commit()
        }
    }

    override fun isFullScreen(): Boolean {
        return fullscreen
    }
}