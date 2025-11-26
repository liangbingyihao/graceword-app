package sdk.chat.demo.bible

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

// BibleDatabaseHelper.kt
class BibleDatabaseHelper(
    context: Context,
    private val databaseName: String = "ChiUn.db"  // 默认数据库文件名
) : SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DB_PATH_SUFFIX = "/databases/"
    }

    private var myContext: Context = context

    @Throws(IOException::class)
    fun createDataBase() {
        val dbExist = checkDataBase()
        if (!dbExist) {
            this.readableDatabase
            try {
                copyDataBase()
            } catch (e: IOException) {
                throw Error("Error copying database: ${e.message}")
            }
        }
    }

    private fun checkDataBase(): Boolean {
        val dbFile = File(myContext.getDatabasePath(databaseName).path)
        return dbFile.exists()
    }

    @Throws(IOException::class)
    private fun copyDataBase() {
        val input = try {
            myContext.assets.open("databases/$databaseName")
        } catch (e: FileNotFoundException) {
            // 如果指定数据库不存在，使用默认数据库
            myContext.assets.open("databases/ChiUn.db")
        }

        val outputFile = myContext.getDatabasePath(databaseName)

        // 确保目录存在
        outputFile.parentFile?.mkdirs()

        FileOutputStream(outputFile).use { output ->
            input.copyTo(output)
        }
    }

    @Throws(SQLiteException::class)
    fun openDataBase(): SQLiteDatabase {
        val dbPath = myContext.getDatabasePath(databaseName).path
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // 数据库从assets复制，不需要创建
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // 处理数据库升级
    }
}