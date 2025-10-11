package sdk.chat.demo.robot.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class LoadMoreSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    // 回调接口
    interface OnLoadMoreListener {
        fun onLoadMore()
        fun onLoadLatestActive()
    }

    private var loadMoreListener: OnLoadMoreListener? = null
    private var isLoadingMore = false
    private var canLoadMore = true
    private var lastLoadTime = 0L
    private val loadMoreThrottleMs = 1000L // 防抖时间1秒
    private val scrollThreshold = 50 // 底部阈值(px)

    // 设置加载更多监听
    fun setOnLoadMoreListener(listener: OnLoadMoreListener?) {
        this.loadMoreListener = listener
    }

    // 更新加载状态
    fun setLoadingMore(loading: Boolean) {
        isLoadingMore = loading
    }

    fun setCanLoadMore(anymore: Boolean) {
        canLoadMore = anymore
    }

    // 绑定RecyclerView
    fun setupWithRecyclerView(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var lastDy = 0
            private var currentState = RecyclerView.SCROLL_STATE_IDLE

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                currentState = newState
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        Log.d("Scroll", "滑动停止")
                        // 滑动停止时的业务逻辑
                        loadMoreListener?.onLoadLatestActive()
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        Log.d("Scroll", "开始拖动")
                        // 用户开始拖动时的逻辑
                    }
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        Log.d("Scroll", "惯性滑动中")
                        // 惯性滑动时的逻辑
                    }
                }
            }


            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                lastDy = dy
                if (dy > 0) { // 仅在上滑时检测
                    checkLoadMore(recyclerView)
                }
            }

        })
    }

    // 绑定NestedScrollView
    fun setupWithNestedScrollView(scrollView: NestedScrollView) {
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) { // 仅在上滑时检测
                checkLoadMore(scrollView)
            }
        }
    }

    private fun checkLoadMore(view: Any) {
        if (shouldIgnoreLoadMore()) return

        if (isAtBottom(view)) {
            triggerLoadMore()
        } else {
//            Logger.warn("onLoadIsAtBottom:false")
        }
    }

    private fun shouldIgnoreLoadMore(): Boolean {
        return !canLoadMore || isLoadingMore ||
                System.currentTimeMillis() - lastLoadTime < loadMoreThrottleMs
    }

    private fun isAtBottom(view: Any): Boolean {
        return when (view) {
            is RecyclerView -> isRecyclerViewAtBottom(view)
            is NestedScrollView -> isScrollViewAtBottom(view)
            else -> false
        }
    }

    private fun isRecyclerViewAtBottom(recyclerView: RecyclerView): Boolean {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return false
        return layoutManager.run {
            if (reverseLayout) {
                return findFirstVisibleItemPosition() == 0 && !recyclerView.canScrollVertically(1)
            }
            val visibleItemCount = childCount
            val totalItemCount = itemCount
            val firstVisibleItem = findFirstVisibleItemPosition()
            return (firstVisibleItem + visibleItemCount) >= totalItemCount - 1 && // 最后一项
                    !recyclerView.canScrollVertically(1) // 不能再上滑
        }
    }

    private fun isScrollViewAtBottom(scrollView: NestedScrollView): Boolean {
        val child = scrollView.getChildAt(0) ?: return false
        return scrollView.scrollY >= (child.height - scrollView.height - scrollThreshold)
    }

    private fun triggerLoadMore() {
        isLoadingMore = true
        lastLoadTime = System.currentTimeMillis()
        loadMoreListener?.onLoadMore()
    }
}