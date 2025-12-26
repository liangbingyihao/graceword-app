package sdk.chat.demo.robot.handlers

import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.robot.api.model.BibleSearchResult
import java.util.TreeSet

object BibleSelectionManager {
    // 使用 TreeSet 自动保持排序
    private val selectedVerses = TreeSet<BibleSearchResult>()
    // 用于快速查找的索引
    private val verseIndex = mutableMapOf<String, BibleSearchResult>()

    fun getSelectedVersesWithReference(): String {
        if (selectedVerses.isEmpty()) return ""

        val result = StringBuilder()
        var previousVerse: BibleSearchResult? = null

        // 1. 构建经文内容部分
        selectedVerses.forEachIndexed { index, verse ->
            if (index > 0) {
                // 检查是否相邻
                if (previousVerse != null && isConsecutive(previousVerse!!, verse)) {
                    result.append(" ")  // 相邻经文用空格
                } else {
                    result.append("... ")  // 不相邻经文用...
                }
            }
            result.append(verse.content.trim())
            previousVerse = verse
        }

        // 2. 构建合并后的出处部分
        val reference = getMergedReference()

        // 3. 组合成最终格式：内容 (合并出处)
        if (reference.isNotEmpty()) {
            result.append(" ($reference)")
        }

        return result.toString()
    }

    /**
     * 判断两节经文是否相邻
     */
    private fun isConsecutive(prev: BibleSearchResult, current: BibleSearchResult): Boolean {
        // 1. 如果是同一书卷、同一章节
        if (prev.bookId == current.bookId && prev.chapter == current.chapter) {
            // 检查节数是否连续
            return current.verse == prev.verse + 1
        }
        return false
    }

    /**
     * 获取合并后的经文出处
     */
    private fun getMergedReference(): String {
        if (selectedVerses.isEmpty()) return ""

        val result = StringBuilder()

        // 按书卷分组
        val versesByBook = selectedVerses.groupBy { it.bookId }
        var isFirstBook = true

        for ((bookId, verses) in versesByBook) {
            val bookName = verses.first().bookName

            // 按章节分组
            val versesByChapter = verses.groupBy { it.chapter }
            val chapterTexts = mutableListOf<String>()

            for ((chapter, chapterVerses) in versesByChapter) {
                // 提取节数并排序
                val verseNumbers = chapterVerses.map { it.verse }.sorted()

                // 合并连续节数
                val mergedRanges = mergeConsecutiveNumbers(verseNumbers)

                // 构建章节文本
                val chapterText = if (mergedRanges.size == 1) {
                    val range = mergedRanges.first()
                    if (range.first == range.last) {
                        "$chapter:${range.first}"
                    } else {
                        "$chapter:${range.first}-${range.last}"
                    }
                } else {
                    val rangeTexts = mergedRanges.map { range ->
                        if (range.first == range.last) {
                            range.first.toString()
                        } else {
                            "${range.first}-${range.last}"
                        }
                    }
                    "$chapter:" + rangeTexts.joinToString(",")
                }

                chapterTexts.add(chapterText)
            }

            // 添加书卷名称和章节节数
            if (!isFirstBook) {
                result.append("；")
            }
            result.append("$bookName ${chapterTexts.joinToString("，")}")
            isFirstBook = false
        }

        return result.toString()
    }

    private fun mergeConsecutiveNumbers(numbers: List<Int>): List<IntRange> {
        if (numbers.isEmpty()) return emptyList()

        val ranges = mutableListOf<IntRange>()
        var start = numbers.first()
        var end = start

        for (i in 1 until numbers.size) {
            if (numbers[i] == end + 1) {
                // 连续，扩展范围
                end = numbers[i]
            } else {
                // 不连续，保存当前范围
                ranges.add(start..end)
                start = numbers[i]
                end = start
            }
        }

        // 添加最后一个范围
        ranges.add(start..end)

        return ranges
    }

    /**
     * 添加选中的经文，保持顺序
     */
    fun addVerseSelected(verse: BibleSearchResult) {
        // 检查是否已存在
        val key = verse.compositeKey
        if (!verseIndex.containsKey(key)) {
            // 添加到 TreeSet（自动排序）
            selectedVerses.add(verse)
            // 添加到索引
            verseIndex[key] = verse

            ChatSDK.events().source()
                .accept(NetworkEvent(EventType.ShowVersePic))
        }
    }

    /**
     * 移除选中的经文
     */
    fun removeVerseSelected(bookId: Int, chapter: Int, verse: Int) {
        val key = "$bookId:$chapter:$verse"
        val verseObj = verseIndex.remove(key)
        verseObj?.let { selectedVerses.remove(it) }

        ChatSDK.events().source()
            .accept(NetworkEvent(EventType.ShowVersePic))
    }

    /**
     * 检查经文是否被选中
     */
    fun isVerseSelected(bookId: Int, chapter: Int, verse: String): Boolean {
        return try {
            val verseNum = verse.toInt()
            val key = "$bookId:$chapter:$verseNum"
            verseIndex.containsKey(key)
        } catch (e: NumberFormatException) {
            false
        }
    }

    /**
     * 检查经文是否被选中（使用 Int 参数）
     */
    fun isVerseSelected(bookId: Int, chapter: Int, verse: Int): Boolean {
        val key = "$bookId:$chapter:$verse"
        return verseIndex.containsKey(key)
    }

    /**
     * 获取选中的所有经文
     */
    fun getSelectedVerses(): List<BibleSearchResult> {
        return selectedVerses.toList()
    }

    /**
     * 获取指定书的选中经文
     */
    fun getSelectedVersesByBook(bookId: Int): List<BibleSearchResult> {
        return selectedVerses.filter { it.bookId == bookId }
    }

    /**
     * 获取指定章节的选中经文
     */
    fun getSelectedVersesByChapter(bookId: Int, chapter: Int): List<BibleSearchResult> {
        return selectedVerses.filter { it.bookId == bookId && it.chapter == chapter }
    }

    /**
     * 清空所有选中
     */
    fun clearAll() {
        selectedVerses.clear()
        verseIndex.clear()
    }

    /**
     * 获取选中经文数量
     */
    fun getSelectedCount(): Int = selectedVerses.size

    /**
     * 批量添加经文
     */
    fun addVersesSelected(verses: List<BibleSearchResult>) {
        verses.forEach { addVerseSelected(it) }
    }

    /**
     * 切换选中状态
     */
    fun toggleVerseSelected(verse: BibleSearchResult) {
        val key = verse.compositeKey
        if (verseIndex.containsKey(key)) {
            removeVerseSelected(verse.bookId, verse.chapter, verse.verse)
        } else {
            addVerseSelected(verse)
        }
    }

}