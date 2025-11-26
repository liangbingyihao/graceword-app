package sdk.chat.demo.robot.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.MainApp
import sdk.chat.demo.bible.DynamicBibleDao
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.VerseAdapter
import sdk.chat.demo.robot.api.model.BibleChapter
import sdk.chat.demo.robot.handlers.BibleApiService
import java.lang.ref.WeakReference


//implements android.view.View.OnClickListener

interface BibleDataProvider {
    fun isFullScreen(): Boolean
}

class BibleChapterFragment : Fragment(), View.OnClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chapterTitle: TextView
    private lateinit var chapterProgress: TextView
//    private lateinit var prevChapterBtn: Button
//    private lateinit var nextChapterBtn: Button

    private lateinit var adapter: VerseAdapter
    lateinit var bibleApiService: BibleApiService

    private var currentBookId = 1
    private var currentChapterCount = 1
    private var currentChapterNumber = 1

    // 用于记录被滑动的位置，用于在API请求失败时恢复视图
    private var swipedPosition: Int? = null

    // 用于防止频繁滑动的变量
    private var isLoading = false
    private val MIN_SWIPE_INTERVAL = 800 // 最小滑动间隔时间（毫秒）
    private var lastSwipeTime = 0L
    private var bibleChapter: WeakReference<BibleChapter>? = null
    private lateinit var dynamicBibleDao: DynamicBibleDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dynamicBibleDao = DynamicBibleDao(MainApp.getInstance().bibleDBManager)
    }

    override fun onDestroy() {
        super.onDestroy()
        dynamicBibleDao.close()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val resId = if ((activity as? BibleDataProvider)?.isFullScreen() == true) {
            R.layout.fragment_bible_chapter_fullscreen
        } else {
            R.layout.fragment_bible_chapter
        }
        return inflater.inflate(resId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化视图
        recyclerView = view.findViewById(R.id.verse_recycler_view)
        chapterTitle = view.findViewById(R.id.chapter_title)
        chapterProgress = view.findViewById(R.id.chapter_progress)
//        prevChapterBtn = view.findViewById(R.id.prev_chapter_btn)
//        nextChapterBtn = view.findViewById(R.id.next_chapter_btn)

        // 初始化RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
//        recyclerView.addItemDecoration(
//            DividerItemDecoration(
//                context,
//                DividerItemDecoration.VERTICAL
//            )
//        )

        // 初始化API服务
        bibleApiService = BibleApiService.getInstance()

        // 从arguments获取初始章节参数
        var reference = ""
        arguments?.let {
            currentBookId = it.getInt(ARG_BOOK_ID, 1)
            currentChapterNumber = it.getInt(ARG_CHAPTER_NUMBER, 1)
            reference = it.getString(ARG_REFERENCE, "")
        } ?: run {
            currentBookId = 1
            currentChapterNumber = 1
            reference = ""
        }

        // 加载初始章节
//        loadChapter(currentBookId, currentChapterNumber, reference)
//        chapterTitle.text = "${currentBookId} ${currentChapterNumber}"

//        view.findViewById<View>(R.id.exit).setOnClickListener(this)
//        view.findViewById<View>(R.id.top_room).setOnClickListener(this)


        // 设置左右滑动切换章节
//        setupSwipeToChangeChapter()
//        Log.e("bible_data", "onViewCreated,${currentBookId} $currentChapterNumber");
    }

    fun resetLoadState(chapter: BibleChapter? = null) {
        if (bibleChapter?.get() != null) {
//            Log.e("bible_data", "resetLoadState 1,${currentBookId} $currentChapterNumber");
            return
        } else if (chapter != null && !chapter.verses.isEmpty()) {
//            Log.e("bible_data", "resetLoadState 2,${currentBookId} $currentChapterNumber");
            bibleChapter = WeakReference(chapter)
            requireView().post {
                updateChapterUI(chapter)
            }
            return
        }
        if (!isLoading) {
//            Log.e("bible_data", "resetLoadState 3,${currentBookId} $currentChapterNumber");
            loadChapter(currentBookId, currentChapterNumber, "")
        }
    }

    // 加载经文章节
    private fun loadChapter(bookId: Int, chapterNumber: Int, reference: String) {
        // 设置加载状态为true
        isLoading = true
        // 显示加载中
        showLoading()

        bibleApiService.getChapterFromDB(
            dynamicBibleDao, bookId, chapterNumber, reference
        ) { chapter ->
            if (chapter != null) {
                resetLoadState(chapter)
            } else {
                // 显示错误
                showError()
            }

            // 隐藏加载中
            hideLoading()
            isLoading = false
        }
    }

    // 更新章节UI
    private fun updateChapterUI(chapter: BibleChapter, reference: String = "") {
        currentBookId = chapter.bookId
        currentChapterCount = chapter.chapterCount
        currentChapterNumber = chapter.chapterNumber
        chapterProgress.text = ""
        // 更新标题和进度
        chapterTitle.text = "${chapter.bookName} ${chapter.chapterNumber}"

        // 更新RecyclerView
        adapter = VerseAdapter(chapter.verses) { position, isSelected ->
            // 处理经文选中状态变化
//            handleVerseSelectionChanged(position, isSelected)
        }
        recyclerView.adapter = adapter

        // 滚动到顶部
        val initialPosition = chapter.verses.indexOfFirst {
            it.referenced
        }
        if (initialPosition >= 0) {
            recyclerView.scrollToPosition(initialPosition)
        } else {
            recyclerView.scrollToPosition(0)
        }

//        // 更新按钮状态
//        prevChapterBtn.isEnabled = currentChapterNumber > 1
//        nextChapterBtn.isEnabled = currentChapterNumber < chapter.chapterCount
        // 重置被滑动的位置
        swipedPosition = null
    }

    // 显示加载中
    private fun showLoading() {
        requireView().post {
            chapterProgress.text = "..."
        }
    }

    // 隐藏加载中
    private fun hideLoading() {
        // 在实际应用中可以隐藏ProgressDialog或ProgressBar
    }

    // 显示错误
    private fun showError() {
        requireView().post {
            chapterProgress.setText(R.string.error_loading_chapter)
            // 如果有被滑动的位置，恢复该位置的视图
            swipedPosition?.let { position ->
                recyclerView.postDelayed({
                    recyclerView.adapter?.notifyItemChanged(position)
                    // 重置被滑动的位置
                    swipedPosition = null
                }, 100)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.exit, R.id.top_room -> {
                activity?.finish()
            }
        }
    }

    fun extractFirstVerseNumber(verseReference: String?): Int? {
        if (verseReference != null && !verseReference.isEmpty()) {
            val matchResult = versePattern.find(verseReference)
            return matchResult?.groupValues?.get(1)?.toIntOrNull()
        } else {
            return null
        }
    }

    companion object {
        // 片段参数键
        private const val ARG_BOOK_ID = "book_id"
        private const val ARG_CHAPTER_NUMBER = "chapter_number"
        private const val ARG_REFERENCE = "reference"
        private val versePattern = """\d+:(\d+)""".toRegex()

        // 创建新实例，可传入初始章节参数
        fun newInstance(
            bookId: Int = 1,
            chapterNumber: Int = 1,
            reference: String = ""
        ): BibleChapterFragment {
            val fragment = BibleChapterFragment()
            val args = Bundle()
            args.putInt(ARG_BOOK_ID, bookId)
            args.putInt(ARG_CHAPTER_NUMBER, chapterNumber)
            args.putString(ARG_REFERENCE, reference)
            fragment.arguments = args
            return fragment
        }
    }
}