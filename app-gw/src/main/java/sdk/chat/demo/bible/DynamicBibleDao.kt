package sdk.chat.demo.bible

import android.util.Log
import sdk.chat.demo.robot.api.model.BibleBook
import sdk.chat.demo.robot.api.model.BibleChapter

// DynamicBibleDao.kt
class DynamicBibleDao(private val databaseManager: DynamicBibleDatabaseManager) {

    // 缓存当前的BibleDao实例
    private var currentBibleDao: BibleDao? = null
    private var currentDatabaseFile: String? = null

    // 获取或创建BibleDao实例
    private fun getBibleDao(): BibleDao? {
        val currentDatabase = databaseManager.getDatabase() ?: return null
        val currentFile = databaseManager.getCurrentDatabaseFile()

        // 如果数据库文件发生变化或实例不存在，创建新实例
        if (currentFile != currentDatabaseFile || currentBibleDao == null) {
            Log.e("loadbible","getBibleDao: $currentFile")
            currentBibleDao = BibleDao(currentDatabase)
            currentDatabaseFile = currentFile
        }

        return currentBibleDao
    }


    // 获取经文（使用当前语言）
    fun getVerses(bookId: Int, chapter: Int): BibleChapter {
        return getBibleDao()?.getVerses(bookId, chapter) ?: BibleChapter(
            bookName = "",
            bookId = bookId,
            chapterNumber = 100,
            chapterCount = 0,
            verses = emptyList()
        )
    }

    // 搜索经文（使用当前语言）
    fun searchVerses(query: String): List<BibleSearchResult> {
        return getBibleDao()?.searchVerses(query) ?: emptyList()
    }

    // 获取书籍列表（使用当前语言）
    fun getBooks(testament: String): List<BibleBook> {
        return getBibleDao()?.getBooksByTestament(testament) ?: emptyList()
    }

    // 获取所有书籍
    fun getAllBooks(): List<BibleBook> {
        return getBibleDao()?.getAllBooks() ?: emptyList()
    }

    // 根据ID获取书籍
    fun getBookById(bookId: Int): BibleBook? {
        return getBibleDao()?.getBookById(bookId)
    }

//    // 根据名称获取书籍
//    fun getBookByName(bookName: String): BibleBook? {
//        return getBibleDao()?.getBookByName(bookName)
//    }
//
//    // 获取指定约的书籍数量
//    fun getBookCount(testament: String): Int {
//        return getBibleDao()?.getBookCount(testament) ?: 0
//    }

    // 获取章节数量
    fun getChapterCount(bookId: Int): Int {
        return getBookById(bookId)?.chapterCount ?: 0
    }

//    // 切换语言并获取数据
//    fun getVersesInLanguage(languageCode: String, bookId: Int, chapter: Int): List<BibleVerse> {
//        if (databaseManager.switchToLanguage(languageCode)) {
//            // 强制刷新DAO实例
//            currentBibleDao = null
//            return getVerses(bookId, chapter)
//        }
//        return emptyList()
//    }
//
//    // 显式切换语言
//    fun switchLanguage(languageCode: String): Boolean {
//        val result = databaseManager.switchToLanguage(languageCode)
//        if (result) {
//            // 语言切换成功，重置DAO实例
//            currentBibleDao = null
//        }
//        return result
//    }
//
//    // 获取当前语言
//    fun getCurrentLanguage(): String {
//        return databaseManager.getCurrentLanguage()
//    }

    // 获取支持的书籍列表
    fun getSupportedLanguages(): List<LanguageOption> {
        return LanguageMapping.getSupportedLanguages()
    }

    // 清理资源
    fun close() {
        currentBibleDao = null
        currentDatabaseFile = null
    }
}