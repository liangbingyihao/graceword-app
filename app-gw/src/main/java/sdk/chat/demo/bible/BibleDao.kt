package sdk.chat.demo.bible

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import sdk.chat.demo.robot.api.model.BibleBook
import sdk.chat.demo.robot.api.model.BibleChapter
import sdk.chat.demo.robot.api.model.BibleSearchResult
import sdk.chat.demo.robot.api.model.Verse

class BibleDao(private val database: SQLiteDatabase) {

    // 获取所有书籍
    fun getAllBooks(language: String = "simplified"): List<BibleBook> {
        val books = mutableListOf<BibleBook>()
        val nameColumn = when (language) {
            "traditional" -> "name_traditional"
            "english" -> "name_english"
            else -> "name_simplified"
        }

        val query = "SELECT id, $nameColumn, chapter_count FROM books ORDER BY id"
        val cursor: Cursor? = database.rawQuery(query, null)

        cursor?.use {
            while (it.moveToNext()) {
                val book = BibleBook(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow(nameColumn)),
                    chapterCount = it.getInt(it.getColumnIndexOrThrow("chapter_count"))
                )
                books.add(book)
            }
        }

        return books
    }

    // 获取指定约的书籍
    fun getBooksByTestament(testament: String, language: String = "simplified"): List<BibleBook> {
        val books = mutableListOf<BibleBook>()
        val nameColumn = when (language) {
            "traditional" -> "name_traditional"
            "english" -> "name_english"
            else -> "name_simplified"
        }

        val query =
            "SELECT id, $nameColumn, chapter_count FROM books WHERE testament = ? ORDER BY id"
        val cursor: Cursor? = database.rawQuery(query, arrayOf(testament))

        cursor?.use {
            while (it.moveToNext()) {
                val book = BibleBook(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow(nameColumn)),
                    chapterCount = it.getInt(it.getColumnIndexOrThrow("chapter_count"))
                )
                books.add(book)
            }
        }

        return books
    }

    // 根据ID获取书籍
    fun getBookById(bookId: Int, language: String = "simplified"): BibleBook? {
        val nameColumn = when (language) {
            "traditional" -> "name_traditional"
            "english" -> "name_english"
            else -> "name_simplified"
        }

        val query = "SELECT id, $nameColumn, chapter_count FROM books WHERE id = ?"
        val cursor: Cursor? = database.rawQuery(query, arrayOf(bookId.toString()))

        cursor?.use {
            if (it.moveToFirst()) {
                return BibleBook(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow(nameColumn)),
                    chapterCount = it.getInt(it.getColumnIndexOrThrow("chapter_count"))
                )
            }
        }

        return null
    }

    // 获取章节经文
    fun getVerses(bookId: Int, chapter: Int, language: String = "simplified"): BibleChapter {
        val verses = mutableListOf<Verse>()

        val query =
            "SELECT verse, text FROM Bible WHERE book_id = ? AND chapter = ? ORDER BY verse"
        val cursor: Cursor? =
            database.rawQuery(query, arrayOf(bookId.toString(), chapter.toString()))

        cursor?.use {
            while (it.moveToNext()) {
                val verse = Verse(
                    verseNumber = it.getInt(it.getColumnIndexOrThrow("verse")),
                    text = it.getString(it.getColumnIndexOrThrow("text")),
                    referenced = false
                )
                verses.add(verse)
            }
        }

        return BibleChapter(
            bookName = "",
            bookId = bookId,
            chapterNumber = chapter,
            chapterCount = verses.size,
            verses = verses
        )
    }

    // 搜索经文
    fun searchVerses(query: String): List<BibleSearchResult> {
        val results = mutableListOf<BibleSearchResult>()

        val searchQuery =
            "SELECT v.book_id, v.chapter, v.verse, v.text " +
                    "FROM Bible v " +
                    "WHERE v.text LIKE ? ORDER BY v.book_id, v.chapter, v.verse"

        val cursor: Cursor? = database.rawQuery(searchQuery, arrayOf("%$query%"))

        cursor?.use {
            while (it.moveToNext()) {
                val result = BibleSearchResult(
                    bookId = it.getInt(it.getColumnIndexOrThrow("book_id")),
                    bookName = "",
                    chapter = it.getInt(it.getColumnIndexOrThrow("chapter")),
                    verse = it.getInt(it.getColumnIndexOrThrow("verse")),
                    content = it.getString(it.getColumnIndexOrThrow("text")),
                )
                results.add(result)
            }
        }

        return results
    }
}

data class BibleVerse(
    val bookId: Int,
    val chapter: Int,
    val verse: Int,
    val content: String
)
