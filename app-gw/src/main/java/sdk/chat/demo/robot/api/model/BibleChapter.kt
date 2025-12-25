package sdk.chat.demo.robot.api.model

import com.google.gson.annotations.SerializedName
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.extensions.LanguageUtils
import java.util.Locale

// 经文章节模型
data class BibleChapter(
    @SerializedName("book_name")
    var bookName: String,
    @SerializedName("book_number")
    val bookId: Int,
    @SerializedName("chapter")
    val chapterNumber: Int,
    @SerializedName("chapter_count")
    var chapterCount: Int,
    val verses: List<Verse>
)

// 经文章节模型
data class Verse(
    @SerializedName("verse")
    val verseNumber: Int,
    val text: String,
    var referenced: Boolean
)

// 圣经书卷模型
data class BibleBook(
    val id: Int,
    val name: String,
    val chapterCount: Int
)

data class BibleSearchResult(
    val bookId: Int,
    var bookName: String,
    val chapter: Int,
    val verse: Int,
    var content: String,
    var reference: String=""
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

    fun isNewTestament(bookId: Int): Boolean{
        return bookId > 39
    }

    fun getBookById(bookId: Int): BibleBook {
        val lang = LanguageUtils.getAppLanguage(MainApp.getContext(), false).lowercase()
        var m: List<BibleBook> = emptyList()
        var isNewTestament = isNewTestament(bookId)

        if (lang.contains("en")) {
            if (isNewTestament) {
                m = englishNewTestament
            } else {
                m = englishOldTestament
            }
        } else if (lang.contains("hant")) {
            if (isNewTestament) {
                m = traditionalChineseNewTestament
            } else {
                m = traditionalChineseOldTestament
            }
        } else {
            if (isNewTestament) {
                m = simplifiedChineseNewTestament
            } else {
                m = simplifiedChineseOldTestament
            }
        }

        return m.first { it -> it.id == bookId }

    }

    // 书卷简写映射
    data class BookAbbreviation(
        val bookId: Int,
        val abbreviations: List<String>  // 所有可能的简写（中英繁）
    )

    // 经文出处解析结果
    data class ScriptureReference(
        val bookId: Int,              // 书卷ID
        val bookName: String,          // 标准书卷名
        val chapterStart: Int,        // 起始章节
        val chapterEnd: Int,          // 结束章节（如果是范围）
        val verseStart: Int? = null,  // 起始节（可选）
        val verseEnd: Int? = null,    // 结束节（可选）
        val isValid: Boolean = true,   // 是否有效
        val errorMessage: String? = null // 错误信息
    )

    // 统一的书卷简写映射（包含所有语言版本）
    // 统一的书卷简写映射（包含简体、繁体、英文）
    val bookAbbreviations = listOf(
        // 旧约
        BookAbbreviation(1, listOf(
            "创", "创世",
            "創", "創世", "創世記", "創世记",
            "Gen", "Genesis", "ge", "gn"
        )),
        BookAbbreviation(2, listOf(
            "出", "出埃", "出埃及", "出埃及记",
            "出埃及記", "出埃及記",
            "Ex", "Exodus", "exo", "exod"
        )),
        BookAbbreviation(3, listOf(
            "利", "利未", "利未记",
            "利未記",
            "Lev", "Leviticus", "le", "lv"
        )),
        BookAbbreviation(4, listOf(
            "民", "民数", "民数记",
            "民數", "民數記",
            "Num", "Numbers", "nu", "nm", "nb"
        )),
        BookAbbreviation(5, listOf(
            "申", "申命", "申命记",
            "申命記",
            "Deut", "Deuteronomy", "de", "dt"
        )),
        BookAbbreviation(6, listOf(
            "书", "约书亚", "书亚", "约书亚记",
            "書", "約書亞", "書亞", "約書亞記", "約书亚", "約书亚记",
            "Josh", "Joshua", "jos", "josh", "jsh"
        )),
        BookAbbreviation(7, listOf(
            "士", "士师", "士师记",
            "士師", "士師記",
            "Judg", "Judges", "jdg", "judg", "jg"
        )),
        BookAbbreviation(8, listOf(
            "得", "路得", "路得记",
            "得", "路得", "路得記",
            "Ruth", "ru", "rth"
        )),
        BookAbbreviation(9, listOf(
            "撒上", "撒母耳上", "撒上记",
            "撒上", "撒母耳上", "撒上記", "撒母耳記上",
            "1Sam", "1 Samuel", "1sa", "1sam", "1sm", "1s", "1samuel"
        )),
        BookAbbreviation(10, listOf(
            "撒下", "撒母耳下", "撒下记",
            "撒下", "撒母耳下", "撒下記", "撒母耳記下",
            "2Sam", "2 Samuel", "2sa", "2sam", "2sm", "2s", "2samuel"
        )),
        BookAbbreviation(11, listOf(
            "王上", "列王上", "列王记上",
            "王上", "列王上", "列王記上",
            "1Kgs", "1 Kings", "1ki", "1kgs", "1k", "1kings"
        )),
        BookAbbreviation(12, listOf(
            "王下", "列王下", "列王记下",
            "王下", "列王下", "列王記下",
            "2Kgs", "2 Kings", "2ki", "2kgs", "2k", "2kings"
        )),
        BookAbbreviation(13, listOf(
            "代上", "历代上", "历代志上",
            "代上", "歷代上", "歷代志上", "歷代記上",
            "1Chr", "1 Chronicles", "1ch", "1chr", "1chron", "1chronicles"
        )),
        BookAbbreviation(14, listOf(
            "代下", "历代下", "历代志下",
            "代下", "歷代下", "歷代志下", "歷代記下",
            "2Chr", "2 Chronicles", "2ch", "2chr", "2chron", "2chronicles"
        )),
        BookAbbreviation(15, listOf(
            "拉", "以斯拉", "以斯拉记",
            "拉", "以斯拉", "以斯拉記",
            "Ezra", "ezr", "ez"
        )),
        BookAbbreviation(16, listOf(
            "尼", "尼希米", "尼希米记",
            "尼", "尼希米", "尼希米記",
            "Neh", "Nehemiah", "neh", "ne"
        )),
        BookAbbreviation(17, listOf(
            "斯", "以斯帖", "以斯帖记",
            "斯", "以斯帖", "以斯帖記", "以斯帖记",
            "Esth", "Esther", "est", "esth", "es"
        )),
        BookAbbreviation(18, listOf(
            "伯", "约伯", "约伯记",
            "伯", "約伯", "約伯記", "約伯记",
            "Job", "job", "jb"
        )),
        BookAbbreviation(19, listOf(
            "诗", "诗篇", "诗",
            "詩", "詩篇",
            "Psa", "Ps", "Psalm", "Psalms", "ps", "psa", "pslm"
        )),
        BookAbbreviation(20, listOf(
            "箴", "箴言",
            "箴", "箴言",
            "Prov", "Proverbs", "pr", "prv", "pro", "prov"
        )),
        BookAbbreviation(21, listOf(
            "传", "传道", "传道书",
            "傳", "傳道", "傳道書", "传道书",
            "Eccl", "Ecclesiastes", "ec", "ecc", "eccl"
        )),
        BookAbbreviation(22, listOf(
            "歌", "雅歌",
            "歌", "雅歌",
            "Song", "Song of Solomon", "so", "sg", "sos", "song"
        )),
        BookAbbreviation(23, listOf(
            "赛", "以赛亚", "以赛亚书",
            "賽", "以賽亞", "以賽亞書", "以赛亚书",
            "Isa", "Isaiah", "is", "isa"
        )),
        BookAbbreviation(24, listOf(
            "耶", "耶利米", "耶利米书",
            "耶", "耶利米", "耶利米書", "耶利米书",
            "Jer", "Jeremiah", "je", "jer", "jerm", "jeremiah"
        )),
        BookAbbreviation(25, listOf(
            "哀", "耶利米哀", "耶利米哀歌",
            "哀", "耶利米哀", "耶利米哀歌",
            "Lam", "Lamentations", "la", "lam"
        )),
        BookAbbreviation(26, listOf(
            "结", "以西结", "以西结书",
            "結", "以西結", "以西結書", "以西结书",
            "Ezek", "Ezekiel", "eze", "ezek", "ek", "ezk"
        )),
        BookAbbreviation(27, listOf(
            "但", "但以理", "但以理书",
            "但", "但以理", "但以理書", "但以理书",
            "Dan", "Daniel", "da", "dan", "dn"
        )),
        BookAbbreviation(28, listOf(
            "何", "何西阿", "何西阿书",
            "何", "何西阿", "何西阿書", "何西阿书",
            "Hos", "Hosea", "ho", "hos"
        )),
        BookAbbreviation(29, listOf(
            "珥", "约珥", "约珥书",
            "珥", "約珥", "約珥書", "约珥书",
            "Joel", "joe", "jl", "joel"
        )),
        BookAbbreviation(30, listOf(
            "摩", "阿摩司", "阿摩司书",
            "摩", "阿摩司", "阿摩司書", "阿摩司书",
            "Amos", "am", "amos"
        )),
        BookAbbreviation(31, listOf(
            "俄", "俄巴底亚", "俄巴底亚书",
            "俄", "俄巴底亞", "俄巴底亞書", "俄巴底亚书",
            "Obad", "Obadiah", "ob", "obad", "oba"
        )),
        BookAbbreviation(32, listOf(
            "拿", "约拿", "约拿书",
            "拿", "約拿", "約拿書", "约拿书",
            "Jonah", "jon", "jnh", "jonah"
        )),
        BookAbbreviation(33, listOf(
            "弥", "弥迦", "弥迦书",
            "彌", "彌迦", "彌迦書", "弥迦书",
            "Mic", "Micah", "mi", "mic"
        )),
        BookAbbreviation(34, listOf(
            "鸿", "那鸿", "那鸿书",
            "鴻", "那鴻", "那鴻書", "那鸿书",
            "Nah", "Nahum", "na", "nah"
        )),
        BookAbbreviation(35, listOf(
            "哈", "哈巴谷", "哈巴谷书",
            "哈", "哈巴谷", "哈巴谷書", "哈巴谷书",
            "Hab", "Habakkuk", "hab", "hb", "habak"
        )),
        BookAbbreviation(36, listOf(
            "番", "西番雅", "西番雅书",
            "番", "西番雅", "西番雅書", "西番雅书",
            "Zeph", "Zephaniah", "zep", "zeph", "zp", "zph"
        )),
        BookAbbreviation(37, listOf(
            "该", "哈该", "哈该书",
            "該", "哈該", "哈該書", "哈该书",
            "Hag", "Haggai", "hag", "hg", "haggai"
        )),
        BookAbbreviation(38, listOf(
            "亚", "撒迦利亚", "撒迦利亚书",
            "亞", "撒迦利亞", "撒迦利亞書", "撒迦利亚书",
            "Zech", "Zechariah", "zec", "zech", "zc", "zch"
        )),
        BookAbbreviation(39, listOf(
            "玛", "玛拉基", "玛拉基书",
            "瑪", "瑪拉基", "瑪拉基書", "玛拉基书",
            "Mal", "Malachi", "mal", "ml", "malachi"
        )),

        // 新约
        BookAbbreviation(40, listOf(
            "太", "马太", "马太福音", "太福音",
            "太", "馬太", "馬太福音", "太福音", "马太福音",
            "Matt", "Matthew", "mt", "matt", "mat"
        )),
        BookAbbreviation(41, listOf(
            "可", "马可", "马可福音", "可福音",
            "可", "馬可", "馬可福音", "可福音", "马可福音",
            "Mark", "mk", "mar", "mark", "mrk"
        )),
        BookAbbreviation(42, listOf(
            "路", "路加", "路加福音", "路福音",
            "路", "路加", "路加福音", "路福音",
            "Luke", "lk", "luk", "luke", "lu"
        )),
        BookAbbreviation(43, listOf(
            "约", "约翰", "约翰福音", "约福音",
            "約", "約翰", "約翰福音", "約福音", "约翰福音",
            "John", "jn", "joh", "john", "jo"
        )),
        BookAbbreviation(44, listOf(
            "徒", "使徒行", "使徒行传", "徒行传",
            "徒", "使徒行", "使徒行傳", "徒行傳", "使徒行传",
            "Acts", "ac", "act", "acts"
        )),
        BookAbbreviation(45, listOf(
            "罗", "罗马", "罗马书",
            "羅", "羅馬", "羅馬書", "罗马书",
            "Rom", "Romans", "ro", "rom", "rm", "romans"
        )),
        BookAbbreviation(46, listOf(
            "林前", "哥前", "哥林多前书", "林前书",
            "林前", "哥前", "哥林多前書", "林前書", "哥林多前书",
            "1Cor", "1 Corinthians", "1co", "1cor", "1corinthians", "1corinth", "1c", "1corin"
        )),
        BookAbbreviation(47, listOf(
            "林后", "哥后", "哥林多后书", "林后书",
            "林后", "哥后", "哥林多後書", "林後書", "哥林多后书",
            "2Cor", "2 Corinthians", "2co", "2cor", "2corinthians", "2corinth", "2c", "2corin"
        )),
        BookAbbreviation(48, listOf(
            "加", "加拉太", "加拉太书",
            "加", "加拉太", "加拉太書", "加拉太书",
            "Gal", "Galatians", "ga", "gal", "gl", "galat"
        )),
        BookAbbreviation(49, listOf(
            "弗", "以弗所", "以弗所书",
            "弗", "以弗所", "以弗所書", "以弗所书",
            "Eph", "Ephesians", "eph", "ep", "ephes", "ephesians"
        )),
        BookAbbreviation(50, listOf(
            "腓", "腓立比", "腓立比书",
            "腓", "腓立比", "腓立比書", "腓立比书",
            "Phil", "Philippians", "php", "phil", "philipp", "philippians", "phl", "ph"
        )),
        BookAbbreviation(51, listOf(
            "西", "歌罗西", "歌罗西书",
            "西", "歌羅西", "歌羅西書", "歌罗西书",
            "Col", "Colossians", "col", "co", "coloss", "colossians", "cls", "cl"
        )),
        BookAbbreviation(52, listOf(
            "帖前", "帖撒前", "帖撒罗尼迦前书", "帖前书",
            "帖前", "帖撒前", "帖撒羅尼迦前書", "帖前書", "帖撒罗尼迦前书",
            "1Thess", "1 Thessalonians", "1th", "1thess", "1thessalonians", "1thes", "1ts", "1thss"
        )),
        BookAbbreviation(53, listOf(
            "帖后", "帖撒后", "帖撒罗尼迦后书", "帖后书",
            "帖後", "帖撒後", "帖撒羅尼迦後書", "帖後書", "帖撒罗尼迦后书",
            "2Thess", "2 Thessalonians", "2th", "2thess", "2thessalonians", "2thes", "2ts", "2thss"
        )),
        BookAbbreviation(54, listOf(
            "提前", "提摩前", "提摩太前书", "提前书",
            "提前", "提摩前", "提摩太前書", "提前書", "提摩太前书",
            "1Tim", "1 Timothy", "1ti", "1tim", "1timothy", "1tm", "1t", "1timo"
        )),
        BookAbbreviation(55, listOf(
            "提后", "提摩后", "提摩太后书", "提后书",
            "提後", "提摩後", "提摩太後書", "提後書", "提摩太后书",
            "2Tim", "2 Timothy", "2ti", "2tim", "2timothy", "2tm", "2t", "2timo"
        )),
        BookAbbreviation(56, listOf(
            "多", "提多", "提多书",
            "多", "提多", "提多書", "提多书",
            "Titus", "tit", "ti", "titus", "tts", "tt"
        )),
        BookAbbreviation(57, listOf(
            "门", "腓利门", "腓利门书",
            "門", "腓利門", "腓利門書", "腓利门书",
            "Phlm", "Philemon", "phm", "philem", "phlm", "philemon", "pm", "plm"
        )),
        BookAbbreviation(58, listOf(
            "来", "希伯来", "希伯来书",
            "來", "希伯來", "希伯來書", "希伯来书",
            "Heb", "Hebrews", "heb", "he", "hebr", "hebrews", "hbr", "hb"
        )),
        BookAbbreviation(59, listOf(
            "雅", "雅各", "雅各书",
            "雅", "雅各", "雅各書", "雅各书",
            "Jas", "James", "jas", "ja", "james", "jm", "jms"
        )),
        BookAbbreviation(60, listOf(
            "彼前", "彼前书", "彼得前书",
            "彼前", "彼前書", "彼得前書", "彼得前书",
            "1Pet", "1 Peter", "1pe", "1pet", "1peter", "1pt", "1p", "1ptr"
        )),
        BookAbbreviation(61, listOf(
            "彼后", "彼后书", "彼得后书",
            "彼後", "彼後書", "彼得後書", "彼得后书",
            "2Pet", "2 Peter", "2pe", "2pet", "2peter", "2pt", "2p", "2ptr"
        )),
        BookAbbreviation(62, listOf(
            "约一", "约一书", "约翰一书","约壹",
            "約一", "約一書", "約翰一書", "约翰一书",
            "1John", "1 John", "1jn", "1joh", "1john", "1j", "1jo", "1jhn"
        )),
        BookAbbreviation(63, listOf(
            "约二", "约二书", "约翰二书",
            "約二", "約二書", "約翰二書", "约翰二书",
            "2John", "2 John", "2jn", "2joh", "2john", "2j", "2jo", "2jhn"
        )),
        BookAbbreviation(64, listOf(
            "约三", "约三书", "约翰三书",
            "約三", "約三書", "約翰三書", "约翰三书",
            "3John", "3 John", "3jn", "3joh", "3john", "3j", "3jo", "3jhn"
        )),
        BookAbbreviation(65, listOf(
            "犹", "犹大", "犹大书",
            "猶", "猶大", "猶大書", "犹大书",
            "Jude", "jud", "jude", "jd", "jde"
        )),
        BookAbbreviation(66, listOf(
            "启", "启示", "启示录",
            "啟", "啟示", "啟示錄", "启示录", "啟示录",
            "Rev", "Revelation", "re", "rev", "revel", "revelation", "rv", "rvl"
        ))
    )

    // 创建简写到书卷ID的快速查找映射
    private val abbreviationToBookId: Map<String, Int> by lazy {
        val map = mutableMapOf<String, Int>()

        // 首先添加完整书名
        var allBooks = listOf(
            simplifiedChineseOldTestament,
            simplifiedChineseNewTestament,
            englishNewTestament,
            englishOldTestament,
            traditionalChineseNewTestament,
            traditionalChineseOldTestament
        )
        allBooks.forEach { books ->
            books.forEach { book -> map[book.name.lowercase()] = book.id }
        }

        // 然后添加所有简写
        bookAbbreviations.forEach { abbreviation ->
            abbreviation.abbreviations.forEach { abbr ->
                map[abbr.lowercase()] = abbreviation.bookId
            }
        }

        map
    }

    // 主函数：解析经文出处字符串
    fun parseScriptureReference(reference: String): ScriptureReference {
        return try {
            val normalizedRef = normalizeReference(reference)
            parseNormalizedReference(normalizedRef)
        } catch (e: Exception) {
            ScriptureReference(
                bookId = -1,
                bookName = "",
                chapterStart = 1,
                chapterEnd = 1,
                isValid = false,
                errorMessage = "解析失败: ${e.message}"
            )
        }
    }

    // 标准化引用字符串
    private fun normalizeReference(reference: String): String {
        return reference
            .trim()
            .replace("\\s+".toRegex(), " ") // 多个空格替换为一个
            .replace("：", ":")             // 中文冒号转英文
            .replace("，", ",")             // 中文逗号转英文
            .replace("；", ";")             // 中文分号转英文
            .replace("－", "-")             // 中文破折号转英文
            .replace("~", "-")              // 波浪线转破折号
            .replace("～", "-")             // 中文波浪线转破折号
            .replace("至", "-")             // "至"转破折号
            .replace("到", "-")             // "到"转破折号
    }

    // 解析标准化后的引用
    private fun parseNormalizedReference(reference: String): ScriptureReference {
        // 分离书卷名和章节信息
        val (bookPart, chapterVersePart) = extractBookAndChapterParts(reference)

        if (bookPart.isEmpty()) {
            return createErrorResult("未找到书卷名")
        }

        // 查找书卷
        val bookId = findBookIdByNameOrAbbreviation(bookPart)
            ?: return createErrorResult("未找到书卷: $bookPart")

        val book = getBookById(bookId) ?: return createErrorResult("书卷ID无效: $bookId")

        // 解析章节和节信息
        val (chapterStart, chapterEnd, verseStart, verseEnd) = parseChapterVerse(
            chapterVersePart,
            book
        )

        return ScriptureReference(
            bookId = bookId,
            bookName = book.name,
            chapterStart = chapterStart,
            chapterEnd = chapterEnd,
            verseStart = verseStart,
            verseEnd = verseEnd
        )
    }

    // 通过名称或简写查找书卷ID
    private fun findBookIdByNameOrAbbreviation(searchTerm: String): Int? {
        val normalizedSearch = searchTerm.trim().lowercase()

        // 1. 精确匹配
        abbreviationToBookId[normalizedSearch]?.let { return it }

        // 2. 包含匹配（宽松匹配）
        val matchedEntry = abbreviationToBookId.entries.find {
            it.key.contains(normalizedSearch) || normalizedSearch.contains(it.key)
        }

        return matchedEntry?.value
    }

    // 分离书卷名和章节部分
    private fun extractBookAndChapterParts(reference: String): Pair<String, String> {
        val patterns = listOf(
            "(.*?)(\\d+.*)".toRegex(),  // 书卷名 + 数字开头
            "(.*?)[:：](.*)".toRegex()  // 书卷名 + 冒号 + 章节
        )

        for (pattern in patterns) {
            val match = pattern.find(reference)
            if (match != null) {
                val bookPart = match.groupValues[1].trim()
                val chapterPart = match.groupValues[2].trim()
                if (bookPart.isNotEmpty() && chapterPart.isNotEmpty()) {
                    return bookPart to chapterPart
                }
            }
        }

        // 如果没有匹配，尝试整个字符串作为书卷名
        return reference to ""
    }

    // 解析章节和节信息
    private fun parseChapterVerse(chapterVersePart: String, book: BibleBook): ChapterVerseInfo {
        if (chapterVersePart.isEmpty()) {
            return ChapterVerseInfo(1, 1, null, null)
        }

        return when {
            // 格式: "3:16" 或 "3:16-18"
            chapterVersePart.contains(":") -> parseWithColonFormat(chapterVersePart, book)
            // 格式: "3-5" 或 "3"
            chapterVersePart.contains("-") -> parseChapterRange(chapterVersePart, book)
            // 格式: "3" (只有章节)
            else -> {
                val regex = """^(\d+)""".toRegex()
                val chapter = regex.find(chapterVersePart)?.value?.toInt()
                if(chapter!=null){
                    validateChapter(chapter, book)
                    ChapterVerseInfo(chapter, chapter, null, null)
                }else{
                    ChapterVerseInfo(1, 1, null, null)
                }
            }

//            else -> throw IllegalArgumentException("无效的章节格式: $chapterVersePart")
        }
    }

    // 解析带冒号的格式 (如 "3:16" 或 "3:16-18")
    private fun parseWithColonFormat(part: String, book: BibleBook): ChapterVerseInfo {
        val parts = part.split(":")
        if (parts.size != 2) {
            throw IllegalArgumentException("无效的章节格式: $part")
        }

        val chapterStr = parts[0]
        val versePart = parts[1]

        val chapter = chapterStr.toIntOrNull()
            ?: throw IllegalArgumentException("无效的章节号: $chapterStr")

        validateChapter(chapter, book)

        return when {
            // 格式: "3:16-18"
            versePart.contains("-") -> {
                val verseParts = versePart.split("-")
                if (verseParts.size != 2) {
                    throw IllegalArgumentException("无效的节范围: $versePart")
                }
                val verseStart = verseParts[0].toIntOrNull()
                val verseEnd = verseParts[1].toIntOrNull()
                if (verseStart == null || verseEnd == null) {
                    throw IllegalArgumentException("无效的节号: $versePart")
                }
                ChapterVerseInfo(chapter, chapter, verseStart, verseEnd)
            }
            // 格式: "3:16"
            else -> {
                val verse = versePart.toIntOrNull()
                    ?: throw IllegalArgumentException("无效的节号: $versePart")
                ChapterVerseInfo(chapter, chapter, verse, verse)
            }
        }
    }

    // 解析章节范围 (如 "3-5")
    private fun parseChapterRange(part: String, book: BibleBook): ChapterVerseInfo {
        val parts = part.split("-")
        if (parts.size != 2) {
            throw IllegalArgumentException("无效的章节范围: $part")
        }

        val start = parts[0].toIntOrNull()
        val end = parts[1].toIntOrNull()

        if (start == null || end == null) {
            throw IllegalArgumentException("无效的章节号: $part")
        }

        validateChapter(start, book)
        validateChapter(end, book)

        if (start > end) {
            throw IllegalArgumentException("起始章节不能大于结束章节: $part")
        }

        return ChapterVerseInfo(start, end, null, null)
    }

    // 验证章节是否有效
    private fun validateChapter(chapter: Int, book: BibleBook) {
        if (chapter < 1 || chapter > book.chapterCount) {
            throw IllegalArgumentException("${book.name} 没有第 $chapter 章 (共 ${book.chapterCount} 章)")
        }
    }

//    // 工具函数：通过ID获取书卷
//    fun getBookById(bookId: Int): BibleBook? {
//        return allBooks.find { it.id == bookId }
//    }

    // 工具函数：获取书卷的所有可能简写
    fun getBookAbbreviations(bookId: Int): List<String> {
        return bookAbbreviations.find { it.bookId == bookId }?.abbreviations ?: emptyList()
    }

    // 内部数据类
    private data class ChapterVerseInfo(
        val chapterStart: Int,
        val chapterEnd: Int,
        val verseStart: Int?,
        val verseEnd: Int?
    )

    private fun createErrorResult(message: String): ScriptureReference {
        return ScriptureReference(
            bookId = -1,
            bookName = "",
            chapterStart = 1,
            chapterEnd = 1,
            isValid = false,
            errorMessage = message
        )
    }

}