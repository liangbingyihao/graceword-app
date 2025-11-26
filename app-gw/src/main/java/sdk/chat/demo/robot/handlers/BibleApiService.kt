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
import sdk.chat.demo.robot.api.model.TaskHistory
import sdk.chat.demo.robot.extensions.LanguageUtils
import java.io.IOException
import java.util.Locale
import java.util.Objects
import kotlin.collections.set


class BibleApiService {
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

//    // 获取书卷列表
//    fun getBooks(callback: (List<BibleBook>?) -> Unit) {
//        val url = BASE_URL + ENDPOINT_BOOKS
//
//        val request = Request.Builder()
//            .url(url)
//            .addHeader("Accept", "application/json")
//            .addHeader("User-Agent", "BibleApp/1.0")
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("BibleApiService", "Failed to fetch books: ${e.message}", e)
//                callback(null)
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                val responseBody = response.body?.string()
//
//                if (response.isSuccessful && responseBody != null) {
//                    try {
//                        val type = object : TypeToken<List<BibleBook>>() {}.type
//                        val books = gson.fromJson<List<BibleBook>>(responseBody, type)
//                        callback(books)
//                    } catch (e: Exception) {
//                        Log.e("BibleApiService", "Failed to parse books response: ${e.message}", e)
//                        callback(null)
//                    }
//                } else {
//                    Log.e("BibleApiService", "Request failed with code: ${response.code}")
//                    callback(null)
//                }
//            }
//        })
//    }
//
//    // 获取圣经译本列表
//    fun getVersions(callback: (List<String>?) -> Unit) {
//        val url = BASE_URL + "versions"
//
//        val request = Request.Builder()
//            .url(url)
//            .addHeader("Accept", "application/json")
//            .addHeader("User-Agent", "BibleApp/1.0")
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("BibleApiService", "Failed to fetch versions: ${e.message}", e)
//                callback(null)
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                val responseBody = response.body?.string()
//
//                if (response.isSuccessful && responseBody != null) {
//                    try {
//                        val type = object : TypeToken<List<String>>() {}.type
//                        val versions = gson.fromJson<List<String>>(responseBody, type)
//                        callback(versions)
//                    } catch (e: Exception) {
//                        Log.e("BibleApiService", "Failed to parse versions response: ${e.message}", e)
//                        callback(null)
//                    }
//                } else {
//                    Log.e("BibleApiService", "Request failed with code: ${response.code}")
//                    callback(null)
//                }
//            }
//        })
//    }
//
//    // 搜索经文
//    fun searchVerse(query: String, callback: (List<BibleChapter>?) -> Unit) {
//        val url = BASE_URL + "search"
//            .replace("{query}", query)
//
//        val request = Request.Builder()
//            .url(url)
//            .addHeader("Accept", "application/json")
//            .addHeader("User-Agent", "BibleApp/1.0")
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("BibleApiService", "Failed to search verses: ${e.message}", e)
//                callback(null)
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                val responseBody = response.body?.string()
//
//                if (response.isSuccessful && responseBody != null) {
//                    try {
//                        val type = object : TypeToken<List<BibleChapter>>() {}.type
//                        val results = gson.fromJson<List<BibleChapter>>(responseBody, type)
//                        callback(results)
//                    } catch (e: Exception) {
//                        Log.e("BibleApiService", "Failed to parse search response: ${e.message}", e)
//                        callback(null)
//                    }
//                } else {
//                    Log.e("BibleApiService", "Search failed with code: ${response.code}")
//                    callback(null)
//                }
//            }
//        })
//    }


    // 伴生对象，提供单例实例
    companion object {
        @Volatile
        private var instance: BibleApiService? = null

        fun getInstance(): BibleApiService {
            return instance ?: synchronized(this) {
                instance ?: BibleApiService().also { instance = it }
            }
        }
    }
}