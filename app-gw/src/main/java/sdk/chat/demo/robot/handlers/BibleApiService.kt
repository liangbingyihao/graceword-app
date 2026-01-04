package sdk.chat.demo.robot.handlers

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import sdk.chat.demo.MainApp
import sdk.chat.demo.bible.DynamicBibleDao
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.model.BibleChapter
import sdk.chat.demo.robot.api.model.BibleData
import sdk.chat.demo.robot.api.model.BibleData.ScriptureReference
import sdk.chat.demo.robot.api.model.BibleSearchResult
import sdk.chat.demo.robot.extensions.LanguageUtils
import java.io.IOException
import java.util.Objects


object BibleApiService {
    val URL_BIBLE_DATA: String = ImageApi.URL2 + "bible/"

    // Gson解析器
    private val gson = Gson()

    // API端点
    private val ENDPOINT_CHAPTER = "chapter"
    private val ENDPOINT_BOOKS = "books"

    // 获取指定章节
    fun getChapter(
        bookId: Int,
        chapterNumber: Int,
        reference: String,
        callback: (BibleChapter?) -> Unit
    ) {
        val urlBuilder =
            Objects.requireNonNull<HttpUrl?>((URL_BIBLE_DATA + ENDPOINT_CHAPTER).toHttpUrlOrNull())
                .newBuilder()
        var lang = LanguageUtils.getAppLanguage(MainApp.getContext())
        if (lang.contains("en")) {
            lang = "en"
        }
        if (!reference.isEmpty()) {
            urlBuilder.addQueryParameter("reference", reference)
        } else if (bookId > 0 && chapterNumber > 0) {
            urlBuilder.addQueryParameter("book_number", bookId.toString())
            urlBuilder.addQueryParameter("chapter", chapterNumber.toString())

        }

        urlBuilder.addQueryParameter("lang", lang)

        val request = Request.Builder()
            .url(urlBuilder.build())
            .build()

        GWApiManager.shared().client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("BibleApiService", "Failed to fetch chapter: ${e.message}", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    try {

                        val data = gson.fromJson<JsonObject?>(
                            responseBody,
                            JsonObject::class.java
                        ).getAsJsonObject("data")

                        val chapter = gson.fromJson(data, BibleChapter::class.java)
                        callback(chapter)
                    } catch (e: Exception) {
                        Log.e(
                            "BibleApiService",
                            "Failed to parse chapter response: ${e.message}",
                            e
                        )
                        callback(null)
                    }
                } else {
                    Log.e("BibleApiService", "Request failed with code: ${response.code}")
                    callback(null)
                }
            }
        })
    }

    fun getChapterFromDB(
        bibleDao: DynamicBibleDao,
        bookId: Int,
        chapterNumber: Int,
        reference: String,
        callback: (BibleChapter?) -> Unit
    ) {
        var bookId2 = bookId
        var chapterNumber2 = chapterNumber
        var scriptureReference: ScriptureReference? = null
        if (!reference.isEmpty() && bookId <= 0) {
            try {
                scriptureReference = BibleData.parseScriptureReference(reference)
                bookId2 = scriptureReference.bookId
                chapterNumber2 = scriptureReference.chapterStart
            } catch (e: Exception) {
                Log.e("bible_data", e.toString())
            }
            if (!reference.isEmpty() && bookId2 <= 0) {
                return getChapter(bookId, chapterNumber, reference, callback)
            }
        }
//        Log.e("bible_data", "getChapterFromDB:$bookId2,$chapterNumber2,$reference")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val verses: BibleChapter = bibleDao.getVerses(bookId2, chapterNumber2)
                var chapter = BibleData.getBookById(bookId2)
                verses.bookName = chapter.name
                verses.chapterCount = chapter.chapterCount
                if (scriptureReference != null && scriptureReference.isValid) {
                    var verseStart = scriptureReference.verseStart ?: 0
                    var verseEnd = scriptureReference.verseEnd ?: verseStart
                    if (scriptureReference.chapterStart > scriptureReference.chapterEnd) {
                        verseStart = verseEnd
                    }
                    if (verseStart > 0) {
                        verses.verses.forEach { verse ->
                            if (verse.verseNumber >= verseStart && verse.verseNumber <= verseEnd) {
                                verse.referenced = true
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    callback(verses)
                }
            } catch (e: Exception) {
                Log.e("bible_data", e.toString())
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    fun searchBibleFromDB(
        query: String,
        callback: (List<BibleSearchResult>?) -> Unit
    ) {
        var bibleDao = DynamicBibleDao(MainApp.getInstance().bibleDBManager)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val verses: List<BibleSearchResult> = bibleDao.searchVerses(query)
                verses.map { it ->
                    it.bookName = BibleData.getBookById(it.bookId).name
                    it.content = it.content.replace(
                        query,
                        "<span style='color:#CF4B40'>$query</span>",
                        ignoreCase = true
                    )
                    it.reference = "${it.bookName} ${it.chapter}:${it.verse}"
                }
                withContext(Dispatchers.Main) {
                    callback(verses)
                }
            } catch (e: Exception) {
                Log.e("bible_data", e.toString())
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }
}