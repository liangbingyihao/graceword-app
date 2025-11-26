package sdk.chat.demo.robot.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import sdk.chat.demo.MainApp
import sdk.chat.demo.bible.DynamicBibleDao
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.BibleBooksActivity
import sdk.chat.demo.robot.adpter.ChapterPagerAdapter
import sdk.chat.demo.robot.api.model.BibleChapter
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.handlers.BibleApiService
import sdk.chat.demo.robot.handlers.LogUploader

class BiblePagerFragment : Fragment(), View.OnClickListener {

    private lateinit var viewPager: ViewPager2
    private lateinit var chapterTitle: TextView
    private lateinit var chapterProgress: TextView
    private var currentPosition = 0

    private lateinit var adapter: ChapterPagerAdapter
    lateinit var bibleApiService: BibleApiService

    private val chapters = mutableListOf<BibleChapter>()
    private var currentBookId = 1
    private var currentChapterNumber = 1
    private var totalChapters = 0
    private var reference = ""
    private var fullscreen = false
    private var pageType = "half"

    // 用于防止频繁滑动的变量
    private var isLoading = false
    private var hasInited = false
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
        arguments?.let {
            currentBookId = it.getInt(ARG_BOOK_ID, 1)
            currentChapterNumber = it.getInt(ARG_CHAPTER_NUMBER, 1)
            reference = it.getString(ARG_REFERENCE, "")
            fullscreen = it.getBoolean(ARG_FULLSCREEN)
        } ?: run {
            currentBookId = 1
            currentChapterNumber = 1
            reference = ""
            fullscreen = false
        }
        if (fullscreen) {
            pageType = "full"
            return inflater.inflate(R.layout.fragment_bible_pager_fullscreen, container, false)
        } else {
            return inflater.inflate(R.layout.fragment_bible_pager, container, false)
        }
//        Log.e("bible_data","$currentBookId,$currentChapterNumber")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化视图
        viewPager = view.findViewById(R.id.chapter_view_pager)
        chapterTitle = view.findViewById(R.id.chapter_title)
        chapterProgress = view.findViewById(R.id.chapter_progress)

        // 初始化API服务
        bibleApiService = BibleApiService.getInstance()

        // 加载章节数据
        loadChapter(currentBookId, currentChapterNumber, reference)

        // 设置ViewPager页面变化监听
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position

                // 更新当前章节信息
                currentChapterNumber = adapter.getChapterNumber(position)
                // 触发当前页面的数据加载
                triggerDataLoadForPosition(position)

                // 预加载相邻页面（可选）
                preloadAdjacentPages(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)


                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    // 滑动停止后，确保当前页面数据加载
                    triggerDataLoadForPosition(currentPosition)
                    if (hasInited) {
                        LogUploader.reportEvent(
                            "mod_bible", listOf<KeyValuePair?>(
                                KeyValuePair("bible_page_type", pageType),
                                KeyValuePair("bible_action", "40")
                            )
                        )
                    } else {
                        hasInited = true
                    }
                }
            }
        })

        view.findViewById<View>(R.id.exit).setOnClickListener(this)
        if (!fullscreen) {
            view.findViewById<View?>(R.id.top_room)?.setOnClickListener(this)
            view.findViewById<View>(R.id.more)?.setOnClickListener(this)
        }
//        // 设置按钮点击事件
//        prevChapterBtn.setOnClickListener {
//            navigateToPreviousChapter()
//        }
//
//        nextChapterBtn.setOnClickListener {
//            navigateToNextChapter()
//        }
        LogUploader.reportEvent(
            "mod_bible", listOf<KeyValuePair?>(
                KeyValuePair("bible_page_type", pageType),
                KeyValuePair("bible_action", "10")
            )
        )
    }

    private fun triggerDataLoadForPosition(position: Int) {
        val fragment = adapter.getFragment(position)
        var chapter = chapters.getOrNull(position)
        fragment?.let {
            // 触发 Fragment 的懒加载机制
            it.resetLoadState(chapter)
            updateChapterUI(chapter)
        }
    }

    private fun preloadAdjacentPages(currentPosition: Int) {
        // 预加载前一页
        if (currentPosition > 0) {
            adapter.getFragment(currentPosition - 1)?.resetLoadState()
        }

        // 预加载后一页
        if (currentPosition < adapter.itemCount - 1) {
            adapter.getFragment(currentPosition + 1)?.resetLoadState()
        }
    }

    // 显示加载中
    private fun showLoading() {
        requireView().post {
            chapterProgress.text = "..."
        }
    }

    private fun showError() {
        requireView().post {
            chapterProgress.setText(R.string.error_loading_chapter)
        }
    }

    // 隐藏加载中
    private fun hideLoading() {
        // 在实际应用中可以隐藏ProgressDialog或ProgressBar
    }

    private fun updateChapterUI(chapter: BibleChapter?) {
        chapterProgress.text = ""
        // 更新标题和进度
        chapterTitle.text = "${chapter?.bookName} ${chapter?.chapterNumber}"
    }

    private fun initChapterAdapter() {

        // 创建适配器
        adapter = ChapterPagerAdapter(requireActivity(), chapters)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1
        // 设置当前页面
        val initialPosition = chapters.indexOfFirst {
            it.bookId == currentBookId && it.chapterNumber == currentChapterNumber
        }

        if (initialPosition != -1) {
            viewPager.currentItem = initialPosition
        }
    }

    // 加载经文章节
    private fun loadChapter(bookId: Int, chapterNumber: Int, reference: String) {
        // 设置加载状态为true
        isLoading = true
        // 显示加载中
        showLoading()

        bibleApiService.getChapterFromDB(
            dynamicBibleDao,
            bookId,
            chapterNumber,
            reference
        ) { chapter ->
            if (chapter != null) {
                // 更新当前章节信息
                currentBookId = chapter.bookId
                currentChapterNumber = chapter.chapterNumber
                for (i in 1..chapter.chapterCount) {
                    if (i == chapter.chapterNumber) {
                        chapters.add(chapter)
                    } else {
                        chapters.add(
                            chapter.copy(
                                chapterNumber = i,
                                verses = emptyList()
                            )
                        )
                    }
                }

                // 更新UI
                requireView().post {
                    updateChapterUI(chapter)
                    initChapterAdapter()
                }
            } else {
                // 显示错误
                showError()
            }

            // 隐藏加载中
            hideLoading()
            isLoading = false
        }
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.exit, R.id.top_room -> {
                LogUploader.reportEvent(
                    "mod_bible", listOf<KeyValuePair?>(
                        KeyValuePair("bible_page_type", pageType),
                        KeyValuePair("bible_action", "20")
                    )
                )
                activity?.finish()
            }
            R.id.more ->{
                BibleBooksActivity.start(requireContext(),currentBookId,currentChapterNumber)
                activity?.finish()
            }
        }
    }

    companion object {
        // 片段参数键
        private const val ARG_BOOK_ID = "book_id"
        private const val ARG_CHAPTER_NUMBER = "chapter_number"
        private const val ARG_REFERENCE = "reference"
        private const val ARG_FULLSCREEN = "fullscreen"
        private val versePattern = """\d+:(\d+)""".toRegex()

        // 创建新实例，可传入初始章节参数
        fun newInstance(
            bookId: Int = 1,
            chapterNumber: Int = 1,
            reference: String = "",
            fullscreen: Boolean = false,
        ): BiblePagerFragment {
            val fragment = BiblePagerFragment()
            val args = Bundle()
            args.putInt(ARG_BOOK_ID, bookId)
            args.putInt(ARG_CHAPTER_NUMBER, chapterNumber)
            args.putString(ARG_REFERENCE, reference)
            args.putBoolean(ARG_FULLSCREEN, fullscreen)
            fragment.arguments = args
            return fragment
        }
    }
}