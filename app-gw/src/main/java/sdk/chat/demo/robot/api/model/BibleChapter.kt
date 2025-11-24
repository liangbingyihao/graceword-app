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
    val id: Int,
    val name: String,
    val chapterCount: Int
)

// BibleData.kt
object BibleData {
    // 简体中文 - 旧约 (1-39)
    val simplifiedChineseOldTestament = listOf(
        // 摩西五经
        BibleBook(1, "创世纪", 50),
        BibleBook(2, "出埃及记", 40),
        BibleBook(3, "利未记", 27),
        BibleBook(4, "民数记", 36),
        BibleBook(5, "申命记", 34),

        // 历史书
        BibleBook(6, "约书亚记", 24),
        BibleBook(7, "士师记", 21),
        BibleBook(8, "路得记", 4),
        BibleBook(9, "撒母耳记上", 31),
        BibleBook(10, "撒母耳记下", 24),
        BibleBook(11, "列王纪上", 22),
        BibleBook(12, "列王纪下", 25),
        BibleBook(13, "历代志上", 29),
        BibleBook(14, "历代志下", 36),
        BibleBook(15, "以斯拉记", 10),
        BibleBook(16, "尼希米记", 13),
        BibleBook(17, "以斯帖记", 10),

        // 诗歌智慧书
        BibleBook(18, "约伯记", 42),
        BibleBook(19, "诗篇", 150),
        BibleBook(20, "箴言", 31),
        BibleBook(21, "传道书", 12),
        BibleBook(22, "雅歌", 8),

        // 大先知书
        BibleBook(23, "以赛亚书", 66),
        BibleBook(24, "耶利米书", 52),
        BibleBook(25, "耶利米哀歌", 5),
        BibleBook(26, "以西结书", 48),
        BibleBook(27, "但以理书", 12),

        // 小先知书
        BibleBook(28, "何西阿书", 14),
        BibleBook(29, "约珥书", 3),
        BibleBook(30, "阿摩司书", 9),
        BibleBook(31, "俄巴底亚书", 1),
        BibleBook(32, "约拿书", 4),
        BibleBook(33, "弥迦书", 7),
        BibleBook(34, "那鸿书", 3),
        BibleBook(35, "哈巴谷书", 3),
        BibleBook(36, "西番雅书", 3),
        BibleBook(37, "哈该书", 2),
        BibleBook(38, "撒迦利亚书", 14),
        BibleBook(39, "玛拉基书", 4)
    )

    // 简体中文 - 新约 (40-66)
    val simplifiedChineseNewTestament = listOf(
        // 福音书
        BibleBook(40, "马太福音", 28),
        BibleBook(41, "马可福音", 16),
        BibleBook(42, "路加福音", 24),
        BibleBook(43, "约翰福音", 21),

        // 历史书
        BibleBook(44, "使徒行传", 28),

        // 保罗书信
        BibleBook(45, "罗马书", 16),
        BibleBook(46, "哥林多前书", 16),
        BibleBook(47, "哥林多后书", 13),
        BibleBook(48, "加拉太书", 6),
        BibleBook(49, "以弗所书", 6),
        BibleBook(50, "腓立比书", 4),
        BibleBook(51, "歌罗西书", 4),
        BibleBook(52, "帖撒罗尼迦前书", 5),
        BibleBook(53, "帖撒罗尼迦后书", 3),
        BibleBook(54, "提摩太前书", 6),
        BibleBook(55, "提摩太后书", 4),
        BibleBook(56, "提多书", 3),
        BibleBook(57, "腓利门书", 1),

        // 普通书信
        BibleBook(58, "希伯来书", 13),
        BibleBook(59, "雅各书", 5),
        BibleBook(60, "彼得前书", 5),
        BibleBook(61, "彼得后书", 3),
        BibleBook(62, "约翰一书", 5),
        BibleBook(63, "约翰二书", 1),
        BibleBook(64, "约翰三书", 1),
        BibleBook(65, "犹大书", 1),

        // 预言书
        BibleBook(66, "启示录", 22)
    )

    // 英文 - 旧约
    val englishOldTestament = listOf(
        // Pentateuch
        BibleBook(1, "Genesis", 50),
        BibleBook(2, "Exodus", 40),
        BibleBook(3, "Leviticus", 27),
        BibleBook(4, "Numbers", 36),
        BibleBook(5, "Deuteronomy", 34),

        // Historical Books
        BibleBook(6, "Joshua", 24),
        BibleBook(7, "Judges", 21),
        BibleBook(8, "Ruth", 4),
        BibleBook(9, "1 Samuel", 31),
        BibleBook(10, "2 Samuel", 24),
        BibleBook(11, "1 Kings", 22),
        BibleBook(12, "2 Kings", 25),
        BibleBook(13, "1 Chronicles", 29),
        BibleBook(14, "2 Chronicles", 36),
        BibleBook(15, "Ezra", 10),
        BibleBook(16, "Nehemiah", 13),
        BibleBook(17, "Esther", 10),

        // Poetical Books
        BibleBook(18, "Job", 42),
        BibleBook(19, "Psalms", 150),
        BibleBook(20, "Proverbs", 31),
        BibleBook(21, "Ecclesiastes", 12),
        BibleBook(22, "Song of Solomon", 8),

        // Major Prophets
        BibleBook(23, "Isaiah", 66),
        BibleBook(24, "Jeremiah", 52),
        BibleBook(25, "Lamentations", 5),
        BibleBook(26, "Ezekiel", 48),
        BibleBook(27, "Daniel", 12),

        // Minor Prophets
        BibleBook(28, "Hosea", 14),
        BibleBook(29, "Joel", 3),
        BibleBook(30, "Amos", 9),
        BibleBook(31, "Obadiah", 1),
        BibleBook(32, "Jonah", 4),
        BibleBook(33, "Micah", 7),
        BibleBook(34, "Nahum", 3),
        BibleBook(35, "Habakkuk", 3),
        BibleBook(36, "Zephaniah", 3),
        BibleBook(37, "Haggai", 2),
        BibleBook(38, "Zechariah", 14),
        BibleBook(39, "Malachi", 4)
    )

    // 英文 - 新约
    val englishNewTestament = listOf(
        // Gospels
        BibleBook(40, "Matthew", 28),
        BibleBook(41, "Mark", 16),
        BibleBook(42, "Luke", 24),
        BibleBook(43, "John", 21),

        // History
        BibleBook(44, "Acts", 28),

        // Pauline Epistles
        BibleBook(45, "Romans", 16),
        BibleBook(46, "1 Corinthians", 16),
        BibleBook(47, "2 Corinthians", 13),
        BibleBook(48, "Galatians", 6),
        BibleBook(49, "Ephesians", 6),
        BibleBook(50, "Philippians", 4),
        BibleBook(51, "Colossians", 4),
        BibleBook(52, "1 Thessalonians", 5),
        BibleBook(53, "2 Thessalonians", 3),
        BibleBook(54, "1 Timothy", 6),
        BibleBook(55, "2 Timothy", 4),
        BibleBook(56, "Titus", 3),
        BibleBook(57, "Philemon", 1),

        // General Epistles
        BibleBook(58, "Hebrews", 13),
        BibleBook(59, "James", 5),
        BibleBook(60, "1 Peter", 5),
        BibleBook(61, "2 Peter", 3),
        BibleBook(62, "1 John", 5),
        BibleBook(63, "2 John", 1),
        BibleBook(64, "3 John", 1),
        BibleBook(65, "Jude", 1),

        // Prophecy
        BibleBook(66, "Revelation", 22)
    )

    // 繁体中文 - 旧约
    val traditionalChineseOldTestament = listOf(
        // 摩西五經
        BibleBook(1, "創世紀", 50),
        BibleBook(2, "出埃及記", 40),
        BibleBook(3, "利未記", 27),
        BibleBook(4, "民數記", 36),
        BibleBook(5, "申命記", 34),

        // 歷史書
        BibleBook(6, "約書亞記", 24),
        BibleBook(7, "士師記", 21),
        BibleBook(8, "路得記", 4),
        BibleBook(9, "撒母耳記上", 31),
        BibleBook(10, "撒母耳記下", 24),
        BibleBook(11, "列王紀上", 22),
        BibleBook(12, "列王紀下", 25),
        BibleBook(13, "歷代志上", 29),
        BibleBook(14, "歷代志下", 36),
        BibleBook(15, "以斯拉記", 10),
        BibleBook(16, "尼希米記", 13),
        BibleBook(17, "以斯帖記", 10),

        // 詩歌智慧書
        BibleBook(18, "約伯記", 42),
        BibleBook(19, "詩篇", 150),
        BibleBook(20, "箴言", 31),
        BibleBook(21, "傳道書", 12),
        BibleBook(22, "雅歌", 8),

        // 大先知書
        BibleBook(23, "以賽亞書", 66),
        BibleBook(24, "耶利米書", 52),
        BibleBook(25, "耶利米哀歌", 5),
        BibleBook(26, "以西結書", 48),
        BibleBook(27, "但以理書", 12),

        // 小先知書
        BibleBook(28, "何西阿書", 14),
        BibleBook(29, "約珥書", 3),
        BibleBook(30, "阿摩司書", 9),
        BibleBook(31, "俄巴底亞書", 1),
        BibleBook(32, "約拿書", 4),
        BibleBook(33, "彌迦書", 7),
        BibleBook(34, "那鴻書", 3),
        BibleBook(35, "哈巴谷書", 3),
        BibleBook(36, "西番雅書", 3),
        BibleBook(37, "哈該書", 2),
        BibleBook(38, "撒迦利亞書", 14),
        BibleBook(39, "瑪拉基書", 4)
    )

    // 繁体中文 - 新约
    val traditionalChineseNewTestament = listOf(
        // 福音書
        BibleBook(40, "馬太福音", 28),
        BibleBook(41, "馬可福音", 16),
        BibleBook(42, "路加福音", 24),
        BibleBook(43, "約翰福音", 21),

        // 歷史書
        BibleBook(44, "使徒行傳", 28),

        // 保羅書信
        BibleBook(45, "羅馬書", 16),
        BibleBook(46, "哥林多前書", 16),
        BibleBook(47, "哥林多後書", 13),
        BibleBook(48, "加拉太書", 6),
        BibleBook(49, "以弗所書", 6),
        BibleBook(50, "腓立比書", 4),
        BibleBook(51, "歌羅西書", 4),
        BibleBook(52, "帖撒羅尼迦前書", 5),
        BibleBook(53, "帖撒羅尼迦後書", 3),
        BibleBook(54, "提摩太前書", 6),
        BibleBook(55, "提摩太後書", 4),
        BibleBook(56, "提多書", 3),
        BibleBook(57, "腓利門書", 1),

        // 普通書信
        BibleBook(58, "希伯來書", 13),
        BibleBook(59, "雅各書", 5),
        BibleBook(60, "彼得前書", 5),
        BibleBook(61, "彼得後書", 3),
        BibleBook(62, "約翰一書", 5),
        BibleBook(63, "約翰二書", 1),
        BibleBook(64, "約翰三書", 1),
        BibleBook(65, "猶大書", 1),

        // 預言書
        BibleBook(66, "啟示錄", 22)
    )

    // 获取指定语言的书籍列表
    fun getBooks(language: String, testament: String): List<BibleBook> {
        return when (language) {
            "simplified" -> when (testament) {
                "old" -> simplifiedChineseOldTestament
                "new" -> simplifiedChineseNewTestament
                else -> emptyList()
            }

            "traditional" -> when (testament) {
                "old" -> traditionalChineseOldTestament
                "new" -> traditionalChineseNewTestament
                else -> emptyList()
            }

            "english" -> when (testament) {
                "old" -> englishOldTestament
                "new" -> englishNewTestament
                else -> emptyList()
            }

            else -> emptyList()
        }
    }

    // 根据书籍名称获取章节数
    fun getChapterCount(bookName: String): Int {
        val allBooks = simplifiedChineseOldTestament + simplifiedChineseNewTestament
        return allBooks.find { it.name == bookName }?.chapterCount ?: 0
    }
}