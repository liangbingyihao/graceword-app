package sdk.chat.demo.robot.fragments

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
class GridItemBorderDecoration(
    private val spanCount: Int,
    private val borderWidth: Int,
    private val borderColor: Int
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = borderWidth.toFloat()
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        drawBorders(canvas, parent)
    }

    private fun drawBorders(canvas: Canvas, parent: RecyclerView) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            drawCellBorder(canvas, child)
        }
    }

    private fun drawCellBorder(canvas: Canvas, child: View) {
        val left = child.left.toFloat()
        val top = child.top.toFloat()
        val right = child.right.toFloat()
        val bottom = child.bottom.toFloat()

        // 绘制单元格边框
        canvas.drawRect(left, top, right, bottom, paint)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        // 为边框留出空间
        outRect.set(borderWidth, borderWidth, borderWidth, borderWidth)
    }
}

class CombinedGridDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val borderWidth: Int,
    private val borderColor: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        color = borderColor
        style = Paint.Style.STROKE
        strokeWidth = borderWidth.toFloat()
        isAntiAlias = true
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

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

        // 为边框添加额外空间
        outRect.left += borderWidth
        outRect.right += borderWidth
        outRect.top += borderWidth
        outRect.bottom += borderWidth
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            val left = child.left - params.leftMargin.toFloat()
            val top = child.top - params.topMargin.toFloat()
            val right = child.right + params.rightMargin.toFloat()
            val bottom = child.bottom + params.bottomMargin.toFloat()

            val halfBorder = borderWidth / 2f
            canvas.drawRect(
                left + halfBorder,
                top + halfBorder,
                right - halfBorder,
                bottom - halfBorder,
                paint
            )
        }
    }
}

class ChapterFragment : Fragment() {

    private lateinit var ivBack: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var recyclerViewChapters: RecyclerView

    private lateinit var adapter: ChaptersAdapter
    private var bookId: Int = 0
    private var bookName: String = ""
    private var chapterCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bookId = it.getInt(BOOK_ID_KEY, 0)
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
        val chapters = (1..chapterCount).map { it }

        adapter = ChaptersAdapter(chapters) { chapterNumber ->
            onChapterSelected(chapterNumber)
        }

        // 使用GridLayoutManager实现6列的网格布局
        recyclerViewChapters.layoutManager = GridLayoutManager(requireContext(), 6)
        recyclerViewChapters.adapter = adapter

        // 添加网格间距
//        recyclerViewChapters.addItemDecoration(GridSpacingItemDecoration(6, 16, true))
//        recyclerViewChapters.addItemDecoration(GridItemBorderDecoration(6, 1, Color.LTGRAY))
        val colorGray = ContextCompat.getColor(requireContext(), R.color.gray_divider)
        recyclerViewChapters.addItemDecoration(CombinedGridDecoration(6, 0, 1, colorGray, true))
    }

    private fun onChapterSelected(chapterNumber: Int) {
        // 处理章节选择事件
//        Toast.makeText(requireContext(), "选择了$bookName 第${chapterNumber}章", Toast.LENGTH_SHORT).show()
        BibleActivity.start(
            requireContext(),
            bookId = bookId,
            chapterNumber = chapterNumber,
            fullscreen = true
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        // 可选：清除选中状态
//        adapter.clearSelection()
    }

    companion object {
        private const val BOOK_ID_KEY = "book_id"
        private const val BOOK_NAME_KEY = "book_name"
        private const val CHAPTER_COUNT_KEY = "cnt_chapter"

        fun newInstance(book: BibleBook): ChapterFragment {
            return ChapterFragment().apply {
                arguments = Bundle().apply {
                    putInt(BOOK_ID_KEY, book.id)
                    putString(BOOK_NAME_KEY, book.name)
                    putInt(CHAPTER_COUNT_KEY, book.chapterCount)
                }
            }
        }
    }
}