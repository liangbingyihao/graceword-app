package sdk.chat.demo.robot.fragments

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.activities.BibleActivity
import sdk.chat.demo.robot.adpter.ChaptersAdapter
import sdk.chat.demo.robot.api.model.BibleBook

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) outRect.top = spacing
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) outRect.top = spacing
        }
    }
}

class ChapterFragment : Fragment() {

    private lateinit var ivBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var recyclerViewChapters: RecyclerView

    private lateinit var adapter: ChaptersAdapter
    private var bookName: String = ""
    private var chapterCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookName = it.getString(BOOK_NAME_KEY, "")
            chapterCount = it.getInt(CHAPTER_COUNT_KEY, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bible_chapters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
    }

    private fun initViews(view: View) {
        ivBack = view.findViewById(R.id.iv_back)
        tvTitle = view.findViewById(R.id.tv_title)
        recyclerViewChapters = view.findViewById(R.id.recyclerView_chapters)

        // 设置标题为书籍名称
        tvTitle.text = bookName

        // 返回按钮点击事件
        ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        val chapters = (1..chapterCount+1).map { it.toString() }

        adapter = ChaptersAdapter(chapters) { chapterNumber ->
            onChapterSelected(chapterNumber)
        }

        // 使用GridLayoutManager实现6列的网格布局
        recyclerViewChapters.layoutManager = GridLayoutManager(requireContext(), 6)
        recyclerViewChapters.adapter = adapter

        // 添加网格间距
        recyclerViewChapters.addItemDecoration(GridSpacingItemDecoration(6, 16, true))
    }

    private fun onChapterSelected(chapterNumber: String) {
        // 处理章节选择事件
//        Toast.makeText(requireContext(), "选择了$bookName 第${chapterNumber}章", Toast.LENGTH_SHORT).show()
        BibleActivity.start(requireContext(), reference = "$bookName $chapterNumber",fullscreen = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        // 可选：清除选中状态
//        adapter.clearSelection()
    }

    companion object {
        private const val BOOK_NAME_KEY = "book_name"
        private const val CHAPTER_COUNT_KEY = "cnt_chapter"

        fun newInstance(book: BibleBook): ChapterFragment {
            return ChapterFragment().apply {
                arguments = Bundle().apply {
                    putString(BOOK_NAME_KEY, book.name)
                    putInt(CHAPTER_COUNT_KEY, book.chapterCount)
                }
            }
        }
    }
}