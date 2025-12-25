package sdk.chat.demo.robot.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.SearchActivity
import sdk.chat.demo.robot.adpter.BibleBookAdapter
import sdk.chat.demo.robot.api.model.BibleBook
import sdk.chat.demo.robot.api.model.BibleData
import sdk.chat.demo.robot.api.model.KeyValuePair
import sdk.chat.demo.robot.extensions.LanguageUtils
import sdk.chat.demo.robot.handlers.LogUploader

class BibleBooksFragment : Fragment(), View.OnClickListener {
    private lateinit var tabOldTestament: TextView
    private lateinit var tabNewTestament: TextView
    private lateinit var indicatorOld: View
    private lateinit var indicatorNew: View
    private lateinit var recyclerViewBooks: RecyclerView

    private lateinit var adapter: BibleBookAdapter

    // 根据第一张图片中的书籍列表（完全按照图片顺序）
    private lateinit var oldTestamentBooks: List<BibleBook>

    private lateinit var newTestamentBooks: List<BibleBook>

    private var currentTab: Int = 0

    private var scrollPosition = 0
    private var selectedBookPosition = -1
    private var selectedChapterId = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bible_books, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData()
        initViews(view)
        setupTabs()
        setupRecyclerView()

        // 恢复保存的状态
        savedInstanceState?.let {
            scrollPosition = it.getInt(KEY_SCROLL_POSITION, 0)
            selectedBookPosition = it.getInt(KEY_SELECTED_BOOK_POS, -1)
            currentTab = it.getInt(KEY_SELECTED_TAB, 0)
        }

        if (selectedBookPosition < 0) {
            arguments?.let {
                var bookId = it.getInt(KEY_SELECTED_BOOK_ID, -1)
                selectedChapterId = it.getInt(KEY_SELECTED_CHAPTER_ID, -1)
                if (BibleData.isNewTestament(bookId)) {
                    currentTab = 1
                    selectedBookPosition = newTestamentBooks.indexOfFirst { it -> it.id == bookId }
                } else {
                    selectedBookPosition = oldTestamentBooks.indexOfFirst { it -> it.id == bookId }
                }
                scrollPosition = selectedBookPosition
            } ?: run {
                selectedBookPosition = -1
                selectedChapterId = -1
            }
            Log.e("bible_data", "savedInstanceState==null $scrollPosition,$selectedBookPosition")
        }

        // 默认显示旧约
        restoreUIState()
    }

    private fun initData() {
//        Log.e("bible_data", "BibleBooksFragment.initData")
        // 设置适配器
        val lang = LanguageUtils.getAppLanguage(MainApp.getContext(), false).lowercase()

        if (lang.contains("en")) {
            oldTestamentBooks = BibleData.englishOldTestament
            newTestamentBooks = BibleData.englishNewTestament
        } else if (lang.contains("hant")) {
            oldTestamentBooks = BibleData.traditionalChineseOldTestament
            newTestamentBooks = BibleData.traditionalChineseNewTestament
        } else {
            oldTestamentBooks = BibleData.simplifiedChineseOldTestament
            newTestamentBooks = BibleData.simplifiedChineseNewTestament
        }
    }

    private fun initViews(view: View) {
        tabOldTestament = view.findViewById(R.id.tab_old_testament)
        tabNewTestament = view.findViewById(R.id.tab_new_testament)
        indicatorOld = view.findViewById(R.id.indicator_old)
        indicatorNew = view.findViewById(R.id.indicator_new)
        recyclerViewBooks = view.findViewById(R.id.recyclerView_books)

        view.findViewById<View>(R.id.exit).setOnClickListener(this)
        view.findViewById<View>(R.id.search).setOnClickListener(this)
    }

    private fun setupTabs() {
        // 设置选项卡点击监听
        tabOldTestament.setOnClickListener {
            switchToTab(0)
        }

        tabNewTestament.setOnClickListener {
            switchToTab(1)
        }
    }

    private fun switchToTab(tab: Int) {
//        Log.e("bible_data", "BibleBooksFragment.switchToTab")
        adapter.clearSelection()
        val colorSelected = ContextCompat.getColor(requireContext(), R.color.item_text_selected)
        val colorGray = ContextCompat.getColor(requireContext(), R.color.text_gray3)

        when (tab) {
            0 -> {
                currentTab = 0
                // 更新选项卡样式
                tabOldTestament.setTextColor(colorSelected)
                tabNewTestament.setTextColor(colorGray)
                indicatorOld.setBackgroundColor(colorSelected)
                indicatorOld.visibility = View.VISIBLE
                indicatorNew.visibility = View.INVISIBLE

                // 更新书籍列表
                adapter.updateBooks(oldTestamentBooks)
            }

            else -> {
                currentTab = 1
                // 更新选项卡样式
                tabOldTestament.setTextColor(colorGray)
                tabNewTestament.setTextColor(colorSelected)
                indicatorNew.setBackgroundColor(colorSelected)
                indicatorOld.visibility = View.INVISIBLE
                indicatorNew.visibility = View.VISIBLE

                // 更新书籍列表
                adapter.updateBooks(newTestamentBooks)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = BibleBookAdapter(emptyList()) { bookName ->
            onBookSelected(bookName)
        }

        recyclerViewBooks.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewBooks.adapter = adapter

        // 添加分割线
        recyclerViewBooks.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
    }

    private fun onBookSelected(bookName: BibleBook) {
        // 跳转到章节选择界面
        selectedBookPosition = adapter.getSelectedPosition()
        saveCurrentState()
        val chapterFragment = ChapterFragment.newInstance(bookName)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, chapterFragment)
            .addToBackStack("chapter")
            .commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // 保存当前状态
        saveCurrentState()

        outState.putInt(KEY_SCROLL_POSITION, scrollPosition)
        outState.putInt(KEY_SELECTED_BOOK_POS, selectedBookPosition)
        outState.putInt(KEY_SELECTED_TAB, currentTab)
    }

    private fun saveCurrentState() {
        // 保存滚动位置
        val layoutManager = recyclerViewBooks.layoutManager as? LinearLayoutManager
        scrollPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0
    }

    companion object {
        private const val KEY_SCROLL_POSITION = "scroll_position"
        private const val KEY_SELECTED_BOOK_POS = "book_pos"
        private const val KEY_SELECTED_BOOK_ID = "book_id"
        private const val KEY_SELECTED_CHAPTER_ID = "chapter_id"
        private const val KEY_SELECTED_TAB = "selected_tab"

        fun newInstance(
            bookId: Int = -1,
            chapterNumber: Int = -1
        ): BibleBooksFragment {
            val fragment = BibleBooksFragment()
            val args = Bundle()
            args.putInt(KEY_SELECTED_BOOK_ID, bookId)
            args.putInt(KEY_SELECTED_CHAPTER_ID, chapterNumber)
            fragment.arguments = args
            return fragment
        }
    }

    private fun restoreUIState() {
        // 恢复滚动位置（延迟执行，等待布局完成）
//        Log.e("bible_data", "restoreUIState $scrollPosition,$selectedBookPosition")
        switchToTab(currentTab)
        if (scrollPosition > 0) {
//            recyclerViewBooks.postDelayed({
//            }, 5000)
            Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                override fun run() {
//                    Log.e("bible_data", "restoreUIState scrollPosition to $scrollPosition")
                    recyclerViewBooks.scrollToPosition(scrollPosition)
                }
            }, 100)
        }

        adapter.setSelectedPosition(selectedBookPosition)
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.exit -> {
                activity?.finish()
            }
            R.id.search ->{
                SearchActivity.start(activity,true)
            }
        }
    }
}