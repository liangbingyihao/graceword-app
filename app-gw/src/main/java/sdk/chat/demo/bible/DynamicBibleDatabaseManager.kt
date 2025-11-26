package sdk.chat.demo.bible

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.util.Locale
class DynamicBibleDatabaseManager private constructor() {

    companion object {
        @Volatile
        private var instance: DynamicBibleDatabaseManager? = null

        fun getInstance(context: Context): DynamicBibleDatabaseManager {
            return instance ?: synchronized(this) {
                instance ?: DynamicBibleDatabaseManager().also { instance = it }
            }
        }
    }

    // 使用Application Context，避免内存泄漏
    private var applicationContext: Context? = null
    private var currentDatabaseFile: String? = null
    private var currentDatabaseHelper: BibleDatabaseHelper? = null

    // 初始化方法，传入Application Context
    fun initialize(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
    }

    // 获取数据库，需要传入Context
    fun getDatabase(context: Context? = null): SQLiteDatabase? {
        val ctx = context?.applicationContext ?: applicationContext
        if (ctx == null) {
            throw IllegalStateException("DynamicBibleDatabaseManager not initialized. Call initialize() first.")
        }

        if (currentDatabaseHelper == null) {
            initializeDefaultDatabase(ctx)
        }

        return currentDatabaseHelper?.openDataBase()
    }

//    // 切换数据库
//    fun switchToLanguage(languageCode: String, context: Context? = null): Boolean {
//        val ctx = context?.applicationContext ?: applicationContext
//        if (ctx == null) {
//            throw IllegalStateException("DynamicBibleDatabaseManager not initialized.")
//        }
//
//        val databaseFile = LanguageMapping.getDatabaseFileNameForLanguage(languageCode)
//        return switchDatabase(databaseFile, ctx)
//    }

    fun initializeDefaultDatabase(context: Context) {
        val defaultDatabaseFile = LanguageMapping.getDatabaseFileNameForCurrentLanguage(context)
        switchDatabase(defaultDatabaseFile, context)
    }

    private fun switchDatabase(databaseFile: String, context: Context): Boolean {
        Log.e("loadbible","switchDatabase databaseFile:$databaseFile")
        if (databaseFile == currentDatabaseFile && currentDatabaseHelper != null) {
            return true // 已经是当前数据库
        }
        Log.e("loadbible","switchDatabase databaseFile:$databaseFile")

        return try {
            // 关闭当前数据库
            currentDatabaseHelper?.close()

            // 创建新的数据库帮助类
            val newHelper = BibleDatabaseHelper(context, databaseFile)
            newHelper.createDataBase()

            currentDatabaseHelper = newHelper
            currentDatabaseFile = databaseFile

            Log.e("loadbible","databaseFile:$databaseFile done")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getCurrentDatabaseFile(): String? = currentDatabaseFile


//    private fun getSystemLanguageCode(context: Context): String {
//        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            context.resources.configuration.locales[0]
//        } else {
//            context.resources.configuration.locale
//        }
//        return locale.language
//    }
//
//    fun getCurrentLanguage(): String {
//        return currentDatabaseFile?.let { file ->
//            LanguageMapping.getLanguageFromDatabaseFile(file)
//        } ?: "zh"
//    }

    fun close() {
        currentDatabaseHelper?.close()
        currentDatabaseHelper = null
        currentDatabaseFile = null
        applicationContext = null
    }
}