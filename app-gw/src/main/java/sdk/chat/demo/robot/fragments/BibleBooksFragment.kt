package sdk.chat.demo.robot.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.GuideActivity.GuideViewAdapter
import sdk.chat.demo.robot.adpter.BibleBookAdapter
import sdk.chat.demo.robot.api.model.BibleBook
import sdk.chat.demo.robot.api.model.BibleData
import java.util.Locale

class BibleBooksFragment : Fragment(), View.OnClickListener {
    private lateinit var tabOldTestament: TextView
    private lateinit var tabNewTestament: TextView
    private lateinit var indicatorOld: View
    private lateinit var indicatorNew: View
    private lateinit var recyclerViewBooks: RecyclerView

    private lateinit var adapter: BibleBookAdapter

    // 根据第一张图片中的书籍列表（完全按照图片顺序）
    private lateinit var oldTestamentBooks: List<BibleBook>

    private lateinit var  newTestamentBooks: List<BibleBook>

    private var currentTab: BibleTab = BibleTab.OLD_TESTAMENT

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

        // 默认显示旧约
        switchToTab(BibleTab.OLD_TESTAMENT)
    }

    private fun initData(){
        // 设置适配器
        val lang = Locale.getDefault().toLanguageTag().lowercase(Locale.getDefault())

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

        view.findViewById<View>(R.id.iv_back).setOnClickListener {
            activity?.finish()
        }
    }

    private fun setupTabs() {
        // 设置选项卡点击监听
        tabOldTestament.setOnClickListener {
            switchToTab(BibleTab.OLD_TESTAMENT)
        }

        tabNewTestament.setOnClickListener {
            switchToTab(BibleTab.NEW_TESTAMENT)
        }
    }

    private fun switchToTab(tab: BibleTab) {
        currentTab = tab
        adapter.clearSelection()

        when (tab) {
            BibleTab.OLD_TESTAMENT -> {
                // 更新选项卡样式
                tabOldTestament.setTextColor(Color.RED)
                tabNewTestament.setTextColor(Color.parseColor("#666666"))
                indicatorOld.setBackgroundColor(Color.RED)
                indicatorNew.setBackgroundColor(Color.parseColor("#E0E0E0"))

                // 更新书籍列表
                adapter.updateBooks(oldTestamentBooks)
            }
            BibleTab.NEW_TESTAMENT -> {
                // 更新选项卡样式
                tabOldTestament.setTextColor(Color.parseColor("#666666"))
                tabNewTestament.setTextColor(Color.RED)
                indicatorOld.setBackgroundColor(Color.parseColor("#E0E0E0"))
                indicatorNew.setBackgroundColor(Color.RED)

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
        val chapterFragment = ChapterFragment.newInstance(bookName)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, chapterFragment)
            .addToBackStack("chapter")
            .commit()
    }

    companion object {
        fun newInstance(): BibleBooksFragment {
            return BibleBooksFragment()
        }
    }

    enum class BibleTab {
        OLD_TESTAMENT, NEW_TESTAMENT
    }


    override fun onClick(p0: View?) {
    }
}