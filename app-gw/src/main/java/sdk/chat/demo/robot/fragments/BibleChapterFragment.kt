package sdk.chat.demo.robot.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.adpter.VerseAdapter
import sdk.chat.demo.robot.api.model.BibleChapter
import sdk.chat.demo.robot.handlers.BibleApiService

//implements android.view.View.OnClickListener

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bible_chapter, container, false)
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
        loadChapter(currentBookId, currentChapterNumber, reference)

        view.findViewById<View>(R.id.exit).setOnClickListener(this)
        view.findViewById<View>(R.id.top_room).setOnClickListener(this)

//        // 设置按钮点击事件
//        prevChapterBtn.setOnClickListener {
//            if (currentChapterNumber > 1) {
//                loadChapter(currentBookId, currentChapterNumber - 1, "")
//            }
//        }
//
//        nextChapterBtn.setOnClickListener {
//            if (currentChapterNumber < currentChapterCount) {
//                loadChapter(currentBookId, currentChapterNumber + 1, "")
//            }
//        }

//        view.findViewById<View>(R.id.chapter_info_bar).apply {
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//                getBackground().setAlpha(0);
//            } else {
//                setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
//            }
//        }

        // 设置左右滑动切换章节
        setupSwipeToChangeChapter()
    }

    // 加载经文章节
    private fun loadChapter(bookId: Int, chapterNumber: Int, reference: String) {
        // 设置加载状态为true
        isLoading = true
        // 显示加载中
        showLoading()

        bibleApiService.getChapter(bookId, chapterNumber, reference) { chapter ->
            if (chapter != null) {
                // 更新当前章节信息
                currentBookId = chapter.bookId
                currentChapterCount = chapter.chapterCount
                currentChapterNumber = chapter.chapterNumber

                // 更新UI
                requireView().post {
                    updateChapterUI(chapter, reference)
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

    // 更新章节UI
    private fun updateChapterUI(chapter: BibleChapter, reference: String) {
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
        var pos = extractFirstVerseNumber(reference)
        if (pos != null) {
            adapter.setSelected(pos - 1, true)
            recyclerView.scrollToPosition(pos - 1)
        } else {
            recyclerView.scrollToPosition(0)
        }

//        // 更新按钮状态
//        prevChapterBtn.isEnabled = currentChapterNumber > 1
//        nextChapterBtn.isEnabled = currentChapterNumber < chapter.chapterCount
        // 重置被滑动的位置
        swipedPosition = null
    }

    // 设置左右滑动切换章节
    private fun setupSwipeToChangeChapter() {
        val swipeHelper = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val currentTime = System.currentTimeMillis()
                if (isLoading || currentTime - lastSwipeTime < MIN_SWIPE_INTERVAL) {
                    // 取消滑动效果
                    Log.e("onSwiped", "cancel")
                    recyclerView.adapter?.notifyItemChanged(viewHolder.bindingAdapterPosition)
                    return
                }
                var shouldNotifyItemChange = false
                swipedPosition = viewHolder.bindingAdapterPosition
                lastSwipeTime = currentTime

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // 向左滑动，加载下一章
                        if (currentChapterNumber < currentChapterCount) {
                            // 记录被滑动的位置
//                            swipedPosition = viewHolder.bindingAdapterPosition
//                            lastSwipeTime = currentTime
                            loadChapter(currentBookId, currentChapterNumber + 1, "")
                        } else if (currentBookId < 66) {
                            loadChapter(currentBookId + 1, 1, "")
                        } else {
                            // 已经是最后一章，显示提示并取消滑动效果
                            shouldNotifyItemChange = true
                            Toast.makeText(context, R.string.last_chapter, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    ItemTouchHelper.RIGHT -> {
                        // 向右滑动，加载上一章
                        if (currentChapterNumber > 1) {
                            // 记录被滑动的位置
                            loadChapter(currentBookId, currentChapterNumber - 1, "")
                        } else if (currentBookId > 1) {
                            loadChapter(currentBookId + 1, 1, "")
                        } else {
                            // 已经是第一章，显示提示并取消滑动效果
                            shouldNotifyItemChange = true
                            Toast.makeText(context, R.string.first_chapter, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                // 关键修复：立即在下一帧恢复 item 状态
                if (shouldNotifyItemChange) {
                    lastSwipeTime = 0
                    swipedPosition = null
                    recyclerView.post {
                        recyclerView.adapter?.notifyItemChanged(viewHolder.bindingAdapterPosition)
                    }
                }
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                // 确保视图恢复正常状态
//                recyclerView.adapter?.notifyItemChanged(viewHolder.bindingAdapterPosition)
            }
        }

        ItemTouchHelper(swipeHelper).attachToRecyclerView(recyclerView)
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