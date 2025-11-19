package sdk.chat.demo.robot.adpter


import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import sdk.chat.demo.robot.api.model.BibleChapter
import sdk.chat.demo.robot.fragments.BibleChapterFragment
import java.lang.ref.WeakReference

class ChapterPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val chapters: List<BibleChapter>
) : FragmentStateAdapter(fragmentActivity) {

    private val fragmentMap = mutableMapOf<Int, WeakReference<BibleChapterFragment>>()

    override fun getItemCount(): Int {
        return chapters.size
    }

    override fun createFragment(position: Int): Fragment {
        val chapter = chapters[position]
        val fragment = BibleChapterFragment.newInstance(
            bookId = chapter.bookId,
            chapterNumber = chapter.chapterNumber
        )
        fragmentMap[position] = WeakReference(fragment)

        return fragment
    }

    fun getFragment(position: Int): BibleChapterFragment? {
        return fragmentMap[position]?.get()
    }

    // 获取指定位置的章节ID
    fun getChapterId(position: Int): Int {
        return chapters[position].bookId
    }

    // 获取指定位置的章节号
    fun getChapterNumber(position: Int): Int {
        return chapters[position].chapterNumber
    }

    // 获取指定章节的位置
    fun getPosition(bookId: Int, chapterNumber: Int): Int {
        return chapters.indexOfFirst {
            it.bookId == bookId && it.chapterNumber == chapterNumber
        }
    }
}
