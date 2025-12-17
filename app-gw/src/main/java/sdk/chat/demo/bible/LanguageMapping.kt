package sdk.chat.demo.bible

import android.content.Context
import android.os.Build
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.api.model.BibleData
import sdk.chat.demo.robot.extensions.LanguageUtils
import java.util.Locale

// LanguageMapping.kt
object LanguageMapping {

    // 语言代码到数据库文件名的映射
    private val languageToDatabaseMap = mapOf(
        // 简体中文
        "zh" to "bible_zh.db",
        "zh-CN" to "bible_zh.db",
        "zh-SG" to "bible_zh.db",

        // 繁体中文
        "zh-TW" to "bible_zh_rTW.db",
        "zh-HK" to "bible_zh_rHK.db",
        "zh-MO" to "bible_zh_rHK.db",

        // 英文
        "en" to "bible_en.db",
        "en-US" to "bible_en.db",
        "en-GB" to "bible_en.db",
        "en-CA" to "bible_en.db",
        "en-AU" to "bible_en.db",

        // 其他语言
        "es" to "bible_es.db",
        "fr" to "bible_fr.db",
        "de" to "bible_de.db",
        "ja" to "bible_ja.db",
        "ko" to "bible_ko.db"
    )

    // 获取当前系统语言对应的数据库文件名
    fun getDatabaseFileNameForCurrentLanguage(context: Context): String {
        val lang = LanguageUtils.getAppLanguage(MainApp.getContext(), false).lowercase()
        if (lang.contains("en")) {
            return "KJV.db"
        } else if (lang.contains("hant") or lang.contains("tw") or lang.contains("hk")) {
            return "ChiUn_HK.db"
        } else {
            return "ChiUn.db"
        }
    }

    // 根据Locale获取数据库文件名
    fun getDatabaseFileNameForLocale(locale: Locale): String {
        val languageTag = locale.toLanguageTag()

        // 精确匹配
        languageToDatabaseMap[languageTag]?.let { return it }

        // 只匹配语言代码
        languageToDatabaseMap[locale.language]?.let { return it }

        // 默认数据库
        return "bible_default.db"
    }

    // 获取支持的语言列表
    fun getSupportedLanguages(): List<LanguageOption> {
        return listOf(
            LanguageOption("zh", "简体中文", "bible_zh.db"),
            LanguageOption("zh-TW", "繁體中文", "bible_zh_rTW.db"),
            LanguageOption("en", "English", "bible_en.db"),
            LanguageOption("es", "Español", "bible_es.db")
            // 添加更多语言...
        )
    }

    // 获取当前Locale
    private fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            context.resources.configuration.locale
        }
    }
}

data class LanguageOption(
    val code: String,
    val displayName: String,
    val databaseFile: String
)