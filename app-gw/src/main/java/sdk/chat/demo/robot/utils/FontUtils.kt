package sdk.chat.demo.robot.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import java.io.File

object FontUtils {

    fun loadFont(context: Context, fontFile: File): Typeface? {
        return try {
            // 首选方法: Typeface.createFromFile
            Typeface.createFromFile(fontFile)
        } catch (e: Exception) {
            // 如果失败，尝试其他方法
            loadFontCompat(context, fontFile)
        }
    }

    @SuppressLint("NewApi")
    private fun loadFontCompat(context: Context, fontFile: File): Typeface? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // API 28+: 使用Typeface.Builder
                Typeface.Builder(fontFile).build()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}