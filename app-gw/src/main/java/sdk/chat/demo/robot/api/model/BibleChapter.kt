package sdk.chat.demo.robot.api.model

import com.google.gson.annotations.SerializedName

// 经文章节模型
data class BibleChapter(
    @SerializedName("book_name")
    val bookName: String,
    @SerializedName("book_number")
    val bookId: Int,
    @SerializedName("chapter")
    val chapterNumber: Int,
    @SerializedName("chapter_count")
    val chapterCount: Int,
    val verses: List<Verse>
)

// 经文章节模型
data class Verse(
    @SerializedName("verse")
    val verseNumber: Int,
    val text: String,
    val referenced: Boolean
)

// 圣经书卷模型
data class BibleBook(
    val id: String,
    val name: String,
    val chapterCount: Int
)