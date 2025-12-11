package sdk.chat.demo.robot.utils
import android.text.InputFilter
import android.text.Spanned
import android.util.Log

/**
 * 中英文总字数限制过滤器
 * 限制：中文字数 + 英文单词数 ≤ maxTotalCount
 */
class AdvancedChineseEnglishFilter(
    private val maxTotalCount: Int,  // 中文字数 + 英文单词数的最大总数
    private val onCountChange: ((totalCount: Int) -> Unit)? = null
) : InputFilter {

    companion object {
        // 简体中文基本范围
        private val BASIC_CHINESE_RANGE = 0x4E00..0x9FFF

        // 常见繁体字（简体对应的繁体）
        private val SIMPLIFIED_TO_TRADITIONAL = mapOf(
            '国' to '國', '爱' to '愛', '华' to '華', '学' to '學',
            '电' to '電', '车' to '車', '书' to '書', '会' to '會',
            '机' to '機', '门' to '門', '东' to '東', '开' to '開',
            '关' to '關', '风' to '風', '云' to '雲', '龙' to '龍',
            '凤' to '鳳', '鸟' to '鳥', '鱼' to '魚', '马' to '馬',
            '为' to '為', '发' to '髮', '后' to '後', '里' to '裡',
            '面' to '麵', '干' to '幹', '几' to '幾', '只' to '隻',
            '才' to '纔', '出' to '齣', '叶' to '葉', '台' to '臺',
            '朴' to '樸', '松' to '鬆', '范' to '範', '谷' to '穀',
            '系' to '係', '表' to '錶', '里' to '裏', '面' to '麪',
            '郁' to '鬱', '姜' to '薑', '咸' to '鹹', '丑' to '醜',
            '了' to '瞭'
        )

        // 繁体字集合
        private val TRADITIONAL_CHARS = SIMPLIFIED_TO_TRADITIONAL.values.toSet()
    }

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {

        if (source.isNullOrEmpty()) {
            notifyCountChange(dest?.toString() ?: "")
            return null
        }

        val destText = dest?.toString() ?: ""
        val newText = StringBuilder(destText).apply {
            replace(dstart, dend, source.toString())
        }.toString()

        val stats = analyzeText(newText)

        Log.e("ChineseEnglishFilter",stats.toString())
        if (stats.totalCount > maxTotalCount) {
            val validInput = calculateValidInput(source.toString(), destText, dstart, dend)
            notifyCountChange(destText)
            return validInput
        }

        notifyCountChange(newText)
        return null
    }

    private fun calculateValidInput(
        source: String,
        dest: String,
        dstart: Int,
        dend: Int
    ): String {
        var result = ""
        var currentDest = dest

        for (char in source) {
            val testText = StringBuilder(currentDest).apply {
                replace(dstart, dend, result + char)
            }.toString()

            val stats = analyzeText(testText)
            if (stats.totalCount <= maxTotalCount) {
                result += char
            } else {
                break
            }
        }

        return result
    }

    /**
     * 分析文本
     */
    fun analyzeText(text: String): TextStatistics {
        var traditionalCount = 0
        var simplifiedCount = 0

        for (char in text) {
            val codePoint = char.code

            if (codePoint in BASIC_CHINESE_RANGE) {
                if (char in TRADITIONAL_CHARS) {
                    traditionalCount++
                } else {
                    // 检查是否是简体字
                    if (char in SIMPLIFIED_TO_TRADITIONAL.keys) {
                        simplifiedCount++
                    } else {
                        // 不在映射表中，假设是简体
                        simplifiedCount++
                    }
                }
            }
        }

        val englishCount = countEnglishWords(text)
        val totalCount = traditionalCount + simplifiedCount + englishCount

        return TextStatistics(
            traditionalCount = traditionalCount,
            simplifiedCount = simplifiedCount,
            englishCount = englishCount,
            totalCount = totalCount
        )
    }

    /**
     * 判断是否为繁体字
     */
    fun isTraditionalChar(char: Char): Boolean {
        return char in TRADITIONAL_CHARS
    }

    /**
     * 判断是否为简体字
     */
    fun isSimplifiedChar(char: Char): Boolean {
        return char.code in BASIC_CHINESE_RANGE &&
                char !in TRADITIONAL_CHARS &&
                char in SIMPLIFIED_TO_TRADITIONAL.keys
    }

    /**
     * 转换为繁体
     */
    fun toTraditional(text: String): String {
        return text.map { char ->
            SIMPLIFIED_TO_TRADITIONAL[char] ?: char
        }.joinToString("")
    }

    /**
     * 转换为简体
     */
    fun toSimplified(text: String): String {
        val reverseMap = SIMPLIFIED_TO_TRADITIONAL.entries.associate { (k, v) -> v to k }
        return text.map { char ->
            reverseMap[char] ?: char
        }.joinToString("")
    }

    private fun countEnglishWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim()
            .split("\\s+".toRegex())
            .count { it.isNotBlank() && it.any(Char::isLetter) }
    }

    private fun notifyCountChange(text: String) {
        val stats = analyzeText(text)
        onCountChange?.invoke(
            stats.totalCount
        )
    }

    data class TextStatistics(
        val traditionalCount: Int,
        val simplifiedCount: Int,
        val englishCount: Int,
        val totalCount: Int
    ) {
        val chineseCount: Int get() = traditionalCount + simplifiedCount
        val remaining: Int get() = 0
        val maxTotalCount: Int get() = 0

        override fun toString(): String {
            return "繁体: $traditionalCount, 简体: $simplifiedCount, " +
                    "英文: $englishCount, 总数: $totalCount"
        }
    }
}