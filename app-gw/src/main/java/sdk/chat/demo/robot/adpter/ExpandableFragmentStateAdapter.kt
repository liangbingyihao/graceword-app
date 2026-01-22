package sdk.chat.demo.robot.adpter

import sdk.chat.demo.robot.api.model.BibleChapter
import sdk.chat.demo.robot.fragments.BibleChapterFragment
import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import sdk.chat.demo.robot.fragments.PlaceholderFragment
import java.lang.ref.WeakReference
import kotlin.math.abs

class ExpandableFragmentStateAdapter(
    fragmentActivity: FragmentActivity,
    private val chapters: List<BibleChapter>,
    private val initialCapacity: Int = 3,
) : FragmentStateAdapter(fragmentActivity) {

    private var minPosition = 0
    private var maxPosition = initialCapacity - 1
    private val fragments = mutableMapOf<Int, WeakReference<Fragment>>()
    private var totalItemCount = chapters.size

    //    fun setTotalItemCount(count: Int) {
//        totalItemCount = count
//        notifyDataSetChanged()
//    }
    private var _isMultiSelectMode: Boolean = false

    fun setMultiSelectMode(enabled: Boolean) {
        if (_isMultiSelectMode != enabled) {
            _isMultiSelectMode = enabled
            for (i in 0 until chapters.size) {
                var fragment = getFragment(i)
                fragment?.let {
                    // 触发 Fragment 的懒加载机制
                    Log.e("setMultiSelectMode","${it.isAdded},${it.isResumed}")
                    (it as BibleChapterFragment).setMultiSelectMode(enabled)
                }
//                fragment?.setMultiSelectMode(enabled)
            }
        }
    }

    override fun getItemCount(): Int = totalItemCount

    override fun createFragment(position: Int): Fragment {
        // 确保位置在有效范围内
        if ((position < minPosition || position > maxPosition)) {
            Log.d("bible_data", "createPlaceholderFragment: $position,$minPosition 到 $maxPosition")
            return createPlaceholderFragment(position)
        }

        ensurePositionInRange(position)
        // 如果这个位置的Fragment还未创建
        if (!fragments.containsKey(position) || fragments[position]?.get() == null) {
            fragments[position] = WeakReference(createNewFragment(position))
        }

        return fragments[position]?.get() ?: createPlaceholderFragment(position)
    }

    private fun createPlaceholderFragment(position: Int): Fragment {
        return PlaceholderFragment.newInstance(position)
    }

    fun getFragment(position: Int): Fragment? {
        return fragments[position]?.get()
    }

    private fun ensurePositionInRange(position: Int) {
        // 扩展范围，确保目标位置及其前后位置都在范围内
        val newMin = if (position - 1 < minPosition) position - 1 else minPosition
        val newMax = if (position + 1 > maxPosition) position + 1 else maxPosition

        if (newMin < minPosition || newMax > maxPosition) {
            minPosition = if (newMin < 0) 0 else newMin
            maxPosition = if (newMax >= totalItemCount) totalItemCount - 1 else newMax

            // 创建新范围内的Fragment
            createFragmentsInRange()
        }
    }

    private fun createFragmentsInRange() {
        Log.d("bible_data", "createFragmentsInRange: $minPosition 到 $maxPosition")
        for (i in minPosition..maxPosition) {
            if (!fragments.containsKey(i) && i < totalItemCount) {
                fragments[i] = WeakReference(createNewFragment(i))
            }
        }

        // 清理超出范围的Fragment
//        cleanUpOutOfRangeFragments()
    }

    private fun cleanUpOutOfRangeFragments() {
        val toRemove = mutableListOf<Int>()
        for ((position, _) in fragments) {
            if (position < minPosition || position > maxPosition) {
                toRemove.add(position)
            }
        }

        toRemove.forEach { position ->
            fragments.remove(position)
        }
    }

    private fun createNewFragment(position: Int): Fragment {
        val chapter = chapters[position]
        return BibleChapterFragment.newInstance(
            bookId = chapter.bookId,
            chapterNumber = chapter.chapterNumber,
            isMultiSelectMode = false
        )
    }

    fun getCurrentRange(): Pair<Int, Int> = Pair(minPosition, maxPosition)

    fun expandRange(minPosition: Int? = null, maxPosition: Int? = null) {
        minPosition?.let {
            this.minPosition = if (it < 0) 0 else it
        }
        maxPosition?.let {
            this.maxPosition = if (it >= totalItemCount) totalItemCount - 1 else it
        }
        createFragmentsInRange()
    }

    // 获取指定位置的章节号
    fun getChapterNumber(position: Int): Int {
        return chapters[position].chapterNumber
    }

    fun cleanup() {
        fragments.clear()
    }
}